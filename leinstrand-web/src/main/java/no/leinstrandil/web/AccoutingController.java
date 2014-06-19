package no.leinstrandil.web;

import java.util.List;
import java.util.Map;
import no.leinstrandil.database.model.web.User;
import no.leinstrandil.service.ClubService;
import no.leinstrandil.service.MailService;
import no.leinstrandil.service.UserService;
import org.apache.velocity.VelocityContext;
import spark.Request;

public class AccoutingController implements Controller {

    private final MailService mailService;
    private final UserService userService;
    private final ClubService clubService;

    public AccoutingController(MailService mailService, UserService userService, ClubService clubService) {
        this.mailService = mailService;
        this.userService = userService;
        this.clubService = clubService;
    }

    @Override
    public void handleGet(User user, Request request, VelocityContext context) {
        String action = request.queryParams("action");
        String tab = request.queryParams("tab");
        if (tab == null) {
            tab = "oversikt";
        }
        context.put("tab", tab);
    }

    @Override
    public String handlePost(User user, Request request, Map<String, String> errorMap, List<String> infoList) {
        if (user == null) {
            return null;
        }

        return null;
    }

}
