package no.leinstrandil;

import java.util.Locale;

import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import no.leinstrandil.database.Storage;
import no.leinstrandil.database.model.Page;
import no.leinstrandil.database.model.TextNode;
import no.leinstrandil.service.MenuService;
import no.leinstrandil.service.PageService;
import no.leinstrandil.service.UserService;
import no.leinstrandil.web.ContactController;
import no.leinstrandil.web.Controller;
import no.leinstrandil.web.ControllerTemplate;
import org.apache.commons.codec.binary.Base64;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.runtime.RuntimeConstants;
import org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spark.Filter;
import spark.Request;
import spark.Response;
import spark.Route;
import spark.Spark;

/**
 * Main entry point for the Leinstrand IL web-app.
 *
 * @author paulrene
 *
 */
public class Main {
    private static final Logger log = LoggerFactory.getLogger(Main.class);

    private final Storage storage;
    private final MenuService menuService;
    private final PageService pageService;
    private final UserService userService;
    private final VelocityEngine velocity;

    private Map<String, Controller> controllers;


    public Main() {
        storage = new Storage();
        menuService = new MenuService(storage);
        pageService = new PageService(storage);
        userService = new UserService(storage);

        velocity = new VelocityEngine();
        velocity.setProperty(RuntimeConstants.RESOURCE_LOADER, "classpath");
        velocity.setProperty("classpath.resource.loader.class", ClasspathResourceLoader.class.getName());
        velocity.setProperty("classpath.resource.loader.cache", "false");
        velocity.setProperty("classpath.resource.loader.modificationCheckInterval", "2");
        velocity.setProperty("velocimacro.library.autoreload", "true");
        velocity.setProperty(RuntimeConstants.RUNTIME_LOG_LOGSYSTEM_CLASS,
                "org.apache.velocity.runtime.log.SimpleLog4JLogSystem");
        velocity.setProperty("velocimacro.context.localscope", "true");
        velocity.init();

        controllers = new HashMap<>();
        controllers.put(ControllerTemplate.CONTACT.getId(), new ContactController());

        Spark.setPort(8080);
        Spark.staticFileLocation("/static");
    }

    private void start() {
        Spark.after(new Filter() {
            @Override
            public void handle(Request request, Response response) {
                storage.close();
            }
        });


        Spark.get(new Route("/") {
            @Override
            public Object handle(Request request, Response response) {
                response.redirect("/page/klubben");
                return new String();
            }
        });


        Spark.get(new Route("/page/:urlName") {
            @Override
            public Object handle(Request request, Response response) {
                response.type("text/html");

                String urlName = request.params("urlName");
                Page page = pageService.getPageByUrlName(urlName);
                if (page == null) {
                    log.info("Could not find Page with urlName: " + urlName);
                    page = create404Page();
                }

                if (page.getRedirectToUrl() != null) {
                    log.info("Redirecting to: " + page.getRedirectToUrl());
                    response.redirect(page.getRedirectToUrl());
                    return new String();
                }

                Controller controller = controllers.get(page.getTemplate());
                if (controller != null) {
                    controller.handleGet(request);
                }

                VelocityContext context = new VelocityContext();
                context.put("menuService", menuService);
                context.put("pageService", pageService);
                context.put("userService", userService);
                context.put("redactorIdList", new ArrayList<String>());
                context.put("redactorAirIdList", new ArrayList<String>());
                context.put("thisPage", page);
                String errorsJson = request.queryParams("errors");
                if (errorsJson !=null) {
                    context.put("errors", decodeMap(errorsJson));
                }
                String infoJson = request.queryParams("info");
                if (infoJson != null) {
                    context.put("info", decodeArray(infoJson));
                }
                String dataJson = request.queryParams("data");
                if (dataJson != null) {
                    context.put("data", flatten(decodeMap(dataJson)));
                }

                Template template = velocity.getTemplate("templates/" + page.getTemplate() + ".vm", "UTF-8");
                StringWriter writer = new StringWriter();
                template.merge(context, writer);
                return writer.toString();
            }
        });


        Spark.post(new Route("/page/:urlName") {
            @Override
            public Object handle(Request request, Response response) {
                response.type("text/html");

                String urlName = request.params("urlName");
                Page page = pageService.getPageByUrlName(urlName);
                if (page == null) {
                    page = create404Page();
                }

                List<String> infoList = new ArrayList<>();
                Map<String, String> errorMap = new HashMap<>();
                Controller controller = controllers.get(page.getTemplate());
                if (controller != null) {
                    controller.handlePost(request, errorMap, infoList);
                }

                StringBuilder pathBuilder = new StringBuilder("/page/").append(urlName);
                if (!errorMap.isEmpty()) {
                    pathBuilder.append("?errors=" + urlEncode(encodeMap(errorMap)));
                    pathBuilder.append("&data=" + urlEncode(encodeMapArray(request.queryMap().toMap())));
                } else if (!infoList.isEmpty()) {
                    pathBuilder.append("?info=" + urlEncode(encodeList(infoList)));
                }

                response.redirect(pathBuilder.toString());
                return new String();
            }
        });


        Spark.get(new Route("/api/load/textnode") {
            @Override
            public Object handle(Request request, Response response) {
                response.type("text/plain");
                String idStr = request.queryParams("id");
                String[] idArray = idStr.split("-");
                String urlName = idArray[0];
                String identifier = idArray[1];
                String textNodeId = idArray[2];

                Page page = pageService.getPageByUrlName(urlName);
                TextNode textNode = pageService.getTextNode(page, identifier);

                if (textNode.getId() != Long.parseLong(textNodeId)) {
                    halt(409, "You are attempting to edit an old version.");
                }

                return textNode.getSource();
            }
        });


        Spark.post(new Route("/api/save/textnode") {
            @Override
            public Object handle(Request request, Response response) {
                response.type("text/html");
                String idStr = request.queryParams("id");
                String[] idArray = idStr.split("-");
                String urlName = idArray[0];
                String identifier = idArray[1];
                String textNodeIdEditOn = idArray[2];

                String sourceCode = request.queryParams("value");
                Page page = pageService.getPageByUrlName(urlName);

                if (!pageService.editTextNode(page, identifier, textNodeIdEditOn, null, sourceCode)) {
                    halt(409, "You are attempting save an edit to an old version.");
                }

                return sourceCode;
            }
        });
    }

    private static JSONObject flatten(final JSONObject obj) {
        if (obj == null) {
            return null;
        }
        for(Object key : obj.keySet()) {
            Object value = obj.get((String) key);
            if (value instanceof JSONObject) {
                obj.put((String) key, flatten((JSONObject) value));
            } else if (value instanceof JSONArray) {
                JSONArray list = (JSONArray) value;
                if(list.length() == 1) {
                    obj.put((String) key, list.get(0));
                }
            }
        }
        return obj;
    }

    private static String urlEncode(final String text) {
        try {
            return URLEncoder.encode(text, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    private static String encodeList(List<String> listOfStrings) {
        try {
            return Base64.encodeBase64URLSafeString(new JSONArray(listOfStrings).toString().getBytes("UTF-8"));
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    };

    private static String encodeMap(Map<String, String> mapWithStrings) {
        try {
            return Base64.encodeBase64URLSafeString(new JSONObject(mapWithStrings).toString().getBytes("UTF-8"));
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    private static String encodeMapArray(Map<String, String[]> mapWithStrings) {
        try {
            return Base64.encodeBase64URLSafeString(new JSONObject(mapWithStrings).toString().getBytes("UTF-8"));
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    private static JSONObject decodeMap(final String jsonData) {
        if (jsonData == null || jsonData.isEmpty()) {
            return null;
        }
        try {
            return new JSONObject(new String(Base64.decodeBase64(jsonData), "UTF-8"));
        } catch (JSONException e) {
            throw new RuntimeException(e);
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    private static JSONArray decodeArray(final String jsonData) {
        if (jsonData == null || jsonData.isEmpty()) {
            return null;
        }
        try {
            return new JSONArray(new String(Base64.decodeBase64(jsonData), "UTF-8"));
        } catch (JSONException e) {
            throw new RuntimeException(e);
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    private static Page create404Page() {
        Page page = new Page();
        page.setTemplate("404");
        page.setTitle("404: Ukjent Side");
        page.setUrlName("404");
        return page;
    }

    public static void main(String[] args) {
        Locale.setDefault(new Locale("nb", "no"));
        new Main().start();
    }

}
