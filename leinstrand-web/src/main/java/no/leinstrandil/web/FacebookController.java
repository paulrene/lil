package no.leinstrandil.web;

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
        String identifier = templateConfig.getString("facebookPageIdentifier");
        FacebookPage facebookPage = facebookService.getFacebookPageByPageId(identifier);

        context.put("statusList", facebookService.getFBStatus(facebookPage, 20));
        context.put("photoList", facebookService.getFBPhotos(facebookPage, 20));
        context.put("linkList", facebookService.getFBLinks(facebookPage, 20));
        context.put("events", facebookService.getFBFutureEvents(facebookPage));

        List<FacebookPost> newsList = facebookService.getFacebookNews(facebookPage, 20);
        while(newsList.size()<20) {
            newsList.add(getEmptyPost());
        }
        context.put("newsList", newsList);
    }

    private FacebookPost getEmptyPost() {
        FacebookPost post = new FacebookPost();
        post.setMessage("Finner ingen Facebook post!");
        post.setFacebookCreated(new Date());
        return post;
    }

    @Override
    public void handlePost(User user, Request request, Map<String, String> errorMap, List<String> infoList) {
    }

}
