package no.leinstrandil.web;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;
import no.leinstrandil.database.model.web.User;
import no.leinstrandil.service.ServiceResponse;
import no.leinstrandil.service.UserService;
import org.apache.velocity.VelocityContext;
import org.json.JSONObject;
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
        String title = null;
        String tab = request.queryParams("tab");
        if (tab == null) {
            tab = "logginn";
            title = "Logg inn";
        } else if ("resetpassord".equals(tab)) {
            title = "Reset passord";
        } else if ("settpassord".equals(tab)) {
            title = "Sett passord";
        } else if ("registrer".equals(tab)) {
            title = "Registrer";
        } else if ("verifiserepost".equals(tab)) {
            title = "Verifiser e-post";
            verifyEmail(request, context);
        }
        context.put("tab", tab);
        context.put("pageTitle", title);

        // Reset password or verify email code
        String code = request.queryParams("code");
        if (code != null) {
            JSONObject data = new JSONObject();
            data.put("code", code);
            context.put("data", data);
        }

        // Prefill username
        String username = request.queryParams("username");
        if (username != null) {
            JSONObject data = new JSONObject();
            data.put("username", username);
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
        } else if ("register".equals(action)) {
            return register(user, request, errorMap, infoList);
        }

        return null;
    }

    private void verifyEmail(Request request, VelocityContext context) {
        String code = request.queryParams("code");
        if (code == null) {
            context.put("verified", false);
        }
        context.put("verified", userService.verifyEmailAddressByCode(code));
    }

    private String register(User user, Request request, Map<String, String> errorMap, List<String> infoList) {
        String username = request.queryParams("username");
        String name = request.queryParams("name");
        String email = request.queryParams("email");
        String birthDateStr = request.queryParams("birthdate");
        String gender = request.queryParams("gender");
        String password1 = request.queryParams("password1");
        String password2 = request.queryParams("password2");
        String agreeStr = request.queryParams("agree");

        if (username == null || username.isEmpty()) {
            errorMap.put("username", "Du må velge et brukernavn.");
        } else if (username.length() < 6) {
            errorMap.put("username", "Brukernavnet må minst inneholde 6 tegn.");
        } else if (userService.getUserByUsername(username) != null) {
            errorMap.put("username", "Brukernavnet er opptatt. Du må velge et annet.");
        }

        if (name == null || name.isEmpty()) {
            errorMap.put("name", "Du må oppgi navnet ditt.");
        } else if (name.split(" ").length <=1 ) {
            errorMap.put("name", "Du må oppgi både for- og etternavn.");
        }

        if (email == null || email.isEmpty()) {
            errorMap.put("email", "Du må oppgi en e-postadresse.");
        } else if (!email.contains("@")) {
            errorMap.put("email", "E-postadressen må inneholde en krøllalfa.");
        }

        Date birthDate = null;
        if (birthDateStr == null || birthDateStr.isEmpty()) {
            errorMap.put("birthdate", "Du må oppgi din fødselsdato.");
        } else {
            try {
                birthDate = new SimpleDateFormat("dd.MM.yyyy").parse(birthDateStr);
            } catch (ParseException e) {
                errorMap.put("birthdate", "Du må oppgi korrekt fødselsdato.");
            }
        }

        if (gender == null || gender.isEmpty()) {
            errorMap.put("gender", "Du må oppgi om du er gutt eller jente.");
        } else if (!gender.equals("female") && !gender.equals("male")) {
            errorMap.put("gender", "Du må velge enten gutt eller jente.");
        }

        Boolean agree = new Boolean(agreeStr);
        if (!agree) {
            errorMap.put("agree", "Du må akseptere våre personvern og tjenestebetingelser.");
        }

        if (password1 == null || password1.isEmpty()) {
            errorMap.put("password1", "Du må velge et passord.");
            errorMap.put("password2", "Du må gjenta det samme passordet.");
        }
        if (!errorMap.isEmpty()) {
            return null;
        }
        if (password2 == null || password2.isEmpty()) {
            errorMap.put("password1", "");
            errorMap.put("password2", "Du må gjenta det samme passordet.");
        }
        if (!errorMap.isEmpty()) {
            return null;
        }
        if (!password1.equals(password2)) {
            errorMap.put("password1", "");
            errorMap.put("password2", "Du har ikke skrevet det samme passordet i begge feltene.");
        } else if (!isPasswordStrongEnough(password1)) {
            errorMap.put("password1", "Må være minst 8 tegn og bestå både både tall og bokstaver.");
            errorMap.put("password2", "");
        }

        if (errorMap.isEmpty()) {
            ServiceResponse response = userService.createUser(username, name, email, birthDate, gender, password1);
            if (response.isSuccess()) {
                return "/page/signin?tab=logginn&username=" + username;
            } else {
                errorMap.put("save", response.getMessage());
            }
        }

        return null;
    }

    private boolean isPasswordStrongEnough(String password) {
        if (password == null || password.isEmpty()) {
            return false;
        }
        if (password.length() < 8) {
            return false;
        }
        int characterCount = 0;
        int digitCount = 0;
        int otherCount = 0;
        for (int n=0;n<password.length();n++) {
            char c = password.charAt(n);
            if (Character.isAlphabetic(c)) {
                characterCount++;
            } else if (Character.isDigit(c)) {
                digitCount++;
            } else {
                otherCount++;
            }
        }
        if (characterCount > 0 && (digitCount + otherCount) > 0) {
            return true;
        }
        return false;
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
        } else if (!isPasswordStrongEnough(password1)) {
            errorMap.put("password1", "Må være minst 8 tegn og bestå både både tall og bokstaver.");
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
                userService.sendResetPasswordEmail(thisUser);
            }
        }

        return null;
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
