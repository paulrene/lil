package no.leinstrandil.web;

import org.json.JSONObject;

import java.util.List;
import java.util.Map;
import no.leinstrandil.database.model.person.EmailAddress;
import no.leinstrandil.database.model.web.User;
import no.leinstrandil.service.MailService;
import no.leinstrandil.service.UserService;
import org.apache.velocity.VelocityContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spark.Request;

public class SignInController implements Controller {
    private static final Logger log = LoggerFactory.getLogger(SignInController.class);

    private UserService userService;
    private MailService mailService;

    public SignInController(UserService userService, MailService mailService) {
        this.userService = userService;
        this.mailService = mailService;
    }

    @Override
    public void handleGet(User user, Request request, VelocityContext context) {
        String tab = request.queryParams("tab");
        if (tab == null) {
            tab = "logginn";
        }
        context.put("tab", tab);

        // Reset password code
        String code = request.queryParams("code");
        if (code != null) {
            JSONObject data = new JSONObject();
            data.put("code", code);
            context.put("data", data);
        }
    }

    @Override
    public String handlePost(User user, Request request, Map<String, String> errorMap, List<String> infoList) {
        if (user != null) {
            return null;
        }

        String action = request.queryParams("action");
        if ("signin".equals(action)) {
            return signin(user, request, errorMap, infoList);
        } else if ("resetpassword".equals(action)) {
            return resetPassword(user, request, errorMap, infoList);
        } else if ("setpassword".equals(action)) {
            return setPassword(user, request, errorMap, infoList);
        }

        return null;
    }

    private String setPassword(User user, Request request, Map<String, String> errorMap, List<String> infoList) {
        String password1 = request.queryParams("password1");
        String password2 = request.queryParams("password2");
        String code = request.queryParams("code");

        if (password1 == null || password1.isEmpty()) {
            errorMap.put("password1", "Du må oppgi et nytt passord.");
            errorMap.put("password", "Du må oppgi et nytt passord.");
        }

        if (password2 == null || password2.isEmpty()) {
            errorMap.put("password2", "Du må oppgi et nytt passord.");
            errorMap.put("password", "Du må oppgi et nytt passord.");
        }

        if (!password1.equals(password2)) {
            errorMap.put("password", "Du har ikke skrevet det samme passordet i begge feltene.");
            errorMap.put("password1", "");
            errorMap.put("password2", "");
        }

        if (errorMap.isEmpty()) {
            if (code == null || code.isEmpty()) {
                errorMap.put("password", "Sikkerhetskoden er ikke oppgitt! <a href=\"/page/signin?tab=resetpassord\">Klikk her</a> for å starte på nytt.");
            }
        }

        if (errorMap.isEmpty()) {
            User thisUser = userService.getUserByResetPasswordCodeAndClear(code);
            if (thisUser == null) {
                errorMap.put("password", "Sikkerhetskoden er ikke gyldig! <a href=\"/page/signin?tab=resetpassord\">Klikk her</a> for å starte på nytt.");
                return null;
            }
            if (userService.setPassword(thisUser, password1)) {
                infoList.add("<strong>Det nye passordet er nå aktivt.</strong><br><a href=\"/page/signin\">Klikk her</a> for å logge inn.");
                return null;
            }
            errorMap.put("password", "Det oppstod en uforutsett feil da vi forsøkte å lagre ditt nye passord. <a href=\"/page/signin?tab=resetpassord\">Klikk her</a> for å starte på nytt.");
            return null;
        }

        return null;
    }

    private String resetPassword(User user, Request request, Map<String, String> errorMap, List<String> infoList) {
        String username = request.queryParams("username");

        if (username == null || username.isEmpty()) {
            errorMap.put("username", "Du må oppgi brukernavnet ditt.");
        }

        if (errorMap.isEmpty()) {
            infoList.add("<strong>Åpne e-posten din og se etter en melding fra oss.</strong> Den inneholder informasjon om hvordan du kan sette et nytt passord.");
            User thisUser = userService.getUserByUsername(username);
            if (thisUser != null) {
                sendResetPasswordEmail(thisUser);
            }
        }

        return null;
    }

    private void sendResetPasswordEmail(User user) {
        List<EmailAddress> emailList = user.getPrincipal().getEmailAddressList();
        if (emailList.isEmpty()) {
            log.warn("Could not send password reset email because user " + user.getId()
                    + " does not have any email addresses stored!");
            return;
        }

        String code = userService.generateResetPasswordCode(user);

        StringBuilder text = new StringBuilder();
        text.append("Hei, ").append(user.getPrincipal().getName()).append("!<br><br>");
        text.append("Vi mottok en forespørsel om passordtilbakestilling for Leinstrand IL-kontoen din. ");
        text.append("Bruk lenken nedenfor for å tilbakestille passordet:<br><br>");
        text.append("<strong>Tilbakestill passordet ditt ved hjelp av en nettleser:</strong> ");
        text.append("<a href=\"%baseUrl%page/signin?tab=settpassord&code=").append(code);
        text.append("\">%baseUrl%page/signin?tab=settpassord&code=").append(code).append("</a><br><br>");
        text.append("Om det ikke var deg som bestillte tilbakestilling av ditt passord kan du se bort fra denne e-posten.<br><br>");
        text.append("Med vennlig hilsen,<br>Leinstrand idrettslag.");

        mailService.sendNoReplyHtml(emailList.get(0).getEmail(), "Passordtilbakestilling for Leinstrand IL", text.toString());
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
            if (thisUser == null || thisUser.getPasswordHash() == null
                    || !userService.isValidPassword(thisUser, password)) {
                errorMap.put("username",
                        "<strong>Prøv igjen!</strong><br>Brukernavnet er ukjent eller passordet er ikke riktig.");
                return null;
            }
            request.session().attribute("userId", thisUser.getId());
            log.info("User with id:username " + thisUser.getId() + ":" + thisUser.getUsername() + " logged.");
            return "/";
        }

        return null;
    }

}
