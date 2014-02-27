package no.leinstrandil.web;

import java.util.List;
import java.util.Map;
import no.leinstrandil.database.model.FacebookPage;
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
            context.put("statusList", facebookService.getFBStatus(facebookPage));
            context.put("photoList",  facebookService.getFBPhotos(facebookPage));
            context.put("linkList",  facebookService.getFBLinks(facebookPage));
            context.put("newsList", facebookService.getFacebookNews(facebookPage));
        }
    }

    @Override
    public void handlePost(Request request, Map<String, String> errorMap, List<String> infoList) {
    }

}
