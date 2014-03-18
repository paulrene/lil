package no.leinstrandil.web;

import java.util.List;
import java.util.Map;
import no.leinstrandil.database.model.web.User;
import no.leinstrandil.service.UserService;
import org.apache.velocity.VelocityContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spark.Request;

public class SignInController implements Controller {
    private static final Logger log = LoggerFactory.getLogger(SignInController.class);

    private UserService userService;

    public SignInController(UserService userService) {
        this.userService = userService;
    }

    @Override
    public void handleGet(User user, Request request, VelocityContext context) {
        String tab = request.queryParams("tab");
        if (tab == null) {
            tab = "logginn";
        }
        context.put("tab", tab);


    }

    @Override
    public String handlePost(User user, Request request, Map<String, String> errorMap, List<String> infoList) {
        if (user != null) {
            return null;
        }

        String action = request.queryParams("action");
        if ("signin".equals(action)) {
            return signin(user, request, errorMap, infoList);
        } else if("resetpassword".equals(action)) {
            return resetPassword(user, request, errorMap, infoList);
        }

        return null;
    }

    private String resetPassword(User user, Request request, Map<String, String> errorMap, List<String> infoList) {
        String username = request.queryParams("username");

        if (username == null || username.isEmpty()) {
            errorMap.put("username", "Du må oppgi brukernavnet ditt.");
        }

        if(errorMap.isEmpty()) {
            sendResetPasswordEmail(user);
            infoList.add("<strong>Åpne e-posten din og se etter en melding fra oss.</strong> Den inneholder informasjon om hvordan du kan sette et nytt passord.");
        }

        return null;
    }

    private void sendResetPasswordEmail(User user) {
        // TODO
    }

    private String signin(User user, Request request, Map<String, String> errorMap, List<String> infoList) {
        String username = request.queryParams("username");
        String password = request.queryParams("password");

        if (username == null || username.isEmpty()) {
            errorMap.put("username", "Du må oppgi brukernavnet ditt.");
        }

        if (password == null || password.isEmpty()) {
            errorMap.put("password", "Du må oppgi passordet ditt.");
        }

        if (errorMap.isEmpty()) {
            User thisUser = userService.getUserByUsername(username);
            if (thisUser == null) {
                errorMap.put("username", "<strong>Prøv igjen!</strong><br>Brukernavnet er ukjent eller passordet er ikke riktig.");
                return null;
            }
            if (!userService.isValidPassword(thisUser, password)) {
                errorMap.put("username", "<strong>Prøv igjen!</strong><br>Brukernavnet er ukjent eller passordet er ikke riktig.");
                return null;
            }
            request.session().attribute("userId", thisUser.getId());
            log.info("User with id:username " + thisUser.getId() + ":" + thisUser.getUsername() + " logged.");
            return "/";
        }

        return null;
    }

}
