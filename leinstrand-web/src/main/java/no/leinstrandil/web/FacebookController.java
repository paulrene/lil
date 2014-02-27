package no.leinstrandil.web;

import java.util.List;
import java.util.Map;
import no.leinstrandil.database.model.FacebookPage;
import no.leinstrandil.database.model.FacebookPost;
import no.leinstrandil.service.FacebookService;
import org.apache.velocity.VelocityContext;
import spark.Request;

public class FacebookController implements Controller {

    private FacebookService facebookService;

    public FacebookController(FacebookService facebookService) {
        this.facebookService = facebookService;
    }

    @Override
    public void handleGet(Request request, VelocityContext context) {
        String urlName = request.params("urlName");
        if ("klubben".equals(urlName)) {
            FacebookPage facebookPage = facebookService.getFacebookPageByPageId("LeinstrandIL");
            List<FacebookPost> postList = facebookService.getFacebookPosts(facebookPage);
            context.put("postList", postList);
        }
    }

    @Override
    public void handlePost(Request request, Map<String, String> errorMap, List<String> infoList) {
    }

}
