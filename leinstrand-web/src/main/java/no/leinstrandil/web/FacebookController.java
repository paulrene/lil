package no.leinstrandil.web;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Map;
import no.leinstrandil.database.model.web.FacebookPage;
import no.leinstrandil.database.model.web.FacebookPost;
import no.leinstrandil.database.model.web.Page;
import no.leinstrandil.database.model.web.User;
import no.leinstrandil.service.FacebookService;
import no.leinstrandil.service.PageService;
import org.apache.velocity.VelocityContext;
import org.json.JSONArray;
import org.json.JSONObject;
import spark.Request;

public class FacebookController implements Controller {

    private FacebookService facebookService;
    private PageService pageService;

    public FacebookController(FacebookService facebookService, PageService pageService) {
        this.facebookService = facebookService;
        this.pageService = pageService;
   }

    @Override
    public void handleGet(User user, Request request, VelocityContext context) {
        String urlName = request.params("urlName");
        Page page = pageService.getPageByUrlName(urlName);
        JSONObject templateConfig = new JSONObject(page.getTemplateConfig());
        JSONArray facebookPageIdList = templateConfig.getJSONArray("facebookPage");

        List<FacebookPage> pageList = new ArrayList<>();
        for(int n=0;n<facebookPageIdList.length();n++) {
            JSONObject fbObj = facebookPageIdList.getJSONObject(n);
            String pageIdentifier = fbObj.getString("id");
            pageList.add(facebookService.getFacebookPageByPageId(pageIdentifier));
        }

        context.put("events", facebookService.getFBFutureEvents(pageList.get(0)));

        List<FacebookPost> newsList = new ArrayList<>();
        List<FacebookPost> photoList = new ArrayList<>();
        for(FacebookPage fbPage : pageList) {
            newsList.addAll(facebookService.getFacebookNews(fbPage, 10));
            photoList.addAll(facebookService.getFBPhotos(fbPage, 10));
        }
        sortFacebookPostList(newsList);
        sortFacebookPostList(photoList);

        while(newsList.size()<20) {
            newsList.add(getEmptyPost());
        }

        context.put("newsList", newsList);
        context.put("photoList", photoList);
    }

    private void sortFacebookPostList(List<FacebookPost> list) {
        Collections.sort(list, new Comparator<FacebookPost>() {
            @Override
            public int compare(FacebookPost o1, FacebookPost o2) {
                return o2.getFacebookCreated().compareTo(o1.getFacebookCreated());
            }
        });
    }

    private FacebookPost getEmptyPost() {
        FacebookPost post = new FacebookPost();
        post.setMessage("Finner ingen Facebook post!");
        post.setFacebookCreated(new Date());
        return post;
    }

    @Override
    public String handlePost(User user, Request request, Map<String, String> errorMap, List<String> infoList) {
        return null;
    }

}
