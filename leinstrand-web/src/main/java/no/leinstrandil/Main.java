package no.leinstrandil;

import java.util.UUID;
import java.io.OutputStream;
import java.io.InputStream;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import no.leinstrandil.database.Storage;
import no.leinstrandil.database.model.FacebookPage;
import no.leinstrandil.database.model.Page;
import no.leinstrandil.database.model.Resource;
import no.leinstrandil.database.model.TextNode;
import no.leinstrandil.service.FacebookService;
import no.leinstrandil.service.FileService;
import no.leinstrandil.service.MenuService;
import no.leinstrandil.service.PageService;
import no.leinstrandil.service.StockPhotoService;
import no.leinstrandil.service.UserService;
import no.leinstrandil.web.ContactController;
import no.leinstrandil.web.Controller;
import no.leinstrandil.web.ControllerTemplate;
import no.leinstrandil.web.FacebookController;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.fileupload.FileItemIterator;
import org.apache.commons.fileupload.FileItemStream;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.fileupload.util.Streams;
import org.apache.commons.io.IOUtils;
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
    private final FileService fileService;
    private final StockPhotoService stockPhotoService;
    private final FacebookService facebookService;
    private final VelocityEngine velocity;

    private Map<String, Controller> controllers;

    private URL baseUrl;

    public Main(URL baseUrl) {
        this.baseUrl = baseUrl;

        storage = new Storage();
        menuService = new MenuService(storage);
        pageService = new PageService(storage);
        userService = new UserService(storage);
        fileService = new FileService(storage);
        stockPhotoService = new StockPhotoService();
        facebookService = new FacebookService(storage, stockPhotoService);

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
        controllers.put(ControllerTemplate.FACEBOOK.getId(), new FacebookController(facebookService, pageService));

        Spark.setPort(baseUrl.getPort());
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

                VelocityContext context = new VelocityContext();

                Controller controller = controllers.get(page.getTemplate());
                if (controller != null) {
                    controller.handleGet(request, context);
                }

                FacebookPage lilPage = facebookService.getFacebookPageByPageId("LeinstrandIL");
                context.put("baseHref", baseUrl.toString());
                context.put("menuService", menuService);
                context.put("pageService", pageService);
                context.put("userService", userService);
                context.put("facebookService", facebookService);
                context.put("redactorIdList", new ArrayList<String>());
                context.put("redactorAirIdList", new ArrayList<String>());
                context.put("lilNewsList", facebookService.getFacebookNews(lilPage, 5));
                context.put("thisPage", page);
                String errorsJson = request.queryParams("errors");
                if (errorsJson != null) {
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

        Spark.post(new Route("/api/save/textnode/:id") {
            @Override
            public Object handle(Request request, Response response) {
                String idStr = request.params("id");
                String[] idArray = idStr.split("-");
                String urlName = idArray[0];
                String identifier = idArray[1];
                String textNodeIdEditOn = idArray[2];

                String sourceCode = urlDecode(request.body());
                Page page = pageService.getPageByUrlName(urlName);

                if (!pageService.editTextNode(page, identifier, textNodeIdEditOn, null, sourceCode)) {
                    halt(409, "You are attempting save a change to an old version.");
                }

                return new String();
            }

        });

        Spark.get(new Route("/api/fb/sync") {
            @Override
            public Object handle(Request request, Response response) {
                facebookService.syncFacebookPagePosts();
                return new String();
            }
        });

        Spark.post(new Route("/api/upload") {
            @Override
            public Object handle(Request request, Response response) {
                response.type("application/json");
                if (!ServletFileUpload.isMultipartContent(request.raw())) {
                    halt(400);
                }
                try {
                    ServletFileUpload upload = new ServletFileUpload();
                    FileItemIterator fii = upload.getItemIterator(request.raw());
                    while (fii.hasNext()) {
                        FileItemStream item = fii.next();
                        String name = item.getFieldName();
                        InputStream stream = item.openStream();
                        if (item.isFormField()) {
                            log.debug("Form field " + name + " with value " + Streams.asString(stream) + " detected.");
                        } else {
                            Resource resource = new Resource();
                            resource.setContentType(item.getContentType());
                            resource.setOriginalFileName(item.getName());
                            resource.setData(IOUtils.toByteArray(stream));
                            resource.setUploader(null);
                            resource.setFileName(UUID.randomUUID() + "." + getFileEnding(item));
                            resource.setCreated(new Date());
                            storage.begin();
                            storage.persist(resource);
                            storage.commit();
                            log.info("Received new resource named " + item.getName() + " with content-type "
                                    + item.getContentType());
                            response.type("application/json");
                            JSONObject o = new JSONObject();
                            o.put("filename", resource.getFileName());
                            o.put("filelink", "/resources/" + resource.getFileName());
                            return o.toString();
                        }
                    }
                } catch (Exception e) {
                    return new JSONObject().put("error", e.getClass().getName() + ": " + e.getMessage()).toString();
                }
                return new JSONObject().put("error", "Request does not contain any multipart content!").toString();
            }

        });

        Spark.get(new Route("/api/images") {
            @Override
            public Object handle(Request request, Response response) {
                JSONArray list = new JSONArray();
                List<Resource> imageList = fileService.getImages();
                for (Resource image : imageList) {
                    JSONObject o = new JSONObject();
                    o.put("title", image.getFileName());
                    o.put("thumb", "/resources/" + image.getFileName());
                    o.put("image", "/resources/" + image.getFileName());
                    o.put("folder", "Bilder");
                    list.put(o);
                }
                return list.toString();
            }
        });

        Spark.get(new Route("/resources/:filename") {
            @Override
            public Object handle(Request request, Response response) {
                Resource resource = fileService.getResourceByFilename(request.params("filename"));
                response.type(resource.getContentType());
                try {
                    OutputStream out = response.raw().getOutputStream();
                    out.write(resource.getData());
                } catch (Exception e) {
                    halt(503, e.getMessage());
                }
                return null;
            }
        });
    }

    private static JSONObject flatten(final JSONObject obj) {
        if (obj == null) {
            return null;
        }
        for (Object key : obj.keySet()) {
            Object value = obj.get((String) key);
            if (value instanceof JSONObject) {
                obj.put((String) key, flatten((JSONObject) value));
            } else if (value instanceof JSONArray) {
                JSONArray list = (JSONArray) value;
                if (list.length() == 1) {
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

    private static String urlDecode(String text) {
        try {
            return URLDecoder.decode(text, "UTF-8");
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

    private static String getFileEnding(FileItemStream item) {
        int index = item.getName().lastIndexOf('.');
        if (index != -1) {
            return item.getName().substring(index + 1);
        }

        String type = item.getContentType().toLowerCase();
        switch(type) {
            case "image/gif": return "gif";
            case "image/jpeg": return "jpg";
            case "image/png": return "png";
        }

        return null;
    }

    public static void main(String[] args) throws MalformedURLException {
        URL baseUrl = new URL("http://localhost:8080/");
        if (args.length > 0) {
            baseUrl = new URL(args[0]);
        }
        Locale.setDefault(new Locale("nb", "no"));
        new Main(baseUrl).start();
    }

}
