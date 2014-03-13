package no.leinstrandil.web;

import java.util.List;
import java.util.Map;
import java.util.Random;
import no.leinstrandil.database.model.web.User;
import org.apache.velocity.VelocityContext;
import spark.Request;

public class ContactController implements Controller {

    @Override
    public void handleGet(User user, Request request, VelocityContext context) {
    }

    @Override
    public void handlePost(User user, Request request, Map<String, String> errorMap, List<String> infoList) {
        String action = request.queryParams("action");
        if ("sendmessage".equals(action)) {
            String name = checkNotNullOrEmpty(request, "name", errorMap, "Du må fortelle oss hva du heter.");
            String email = checkNotNullOrEmpty(request, "email", errorMap, "Du må fylle ut din e-postadresse.");
            String message = checkNotNullOrEmpty(request, "message", errorMap, "Du kan ikke sende en tom melding.");
            if (errorMap.isEmpty()) {
                if(sendMessage(name, email, message)) {
                    infoList.add("<strong>Tusen takk for meldingen din.</strong> Vil vil svare deg så raskt vi kan.");
                } else {
                    errorMap.put("action",
                            "Det oppstod en feil som gjorde at vi ikke mottok meldingen din. Du må prøve på nytt.");
                }
            }
        }
    }

    private boolean sendMessage(String name, String email, String message) {
        System.out.println("sendMessage(" + name + ", " + email + ", " + message + ")");
        return new Random().nextBoolean();
    }

    private static String checkNotNullOrEmpty(Request request, String name, Map<String, String> errorMap,
            String errorMessage) {
        String value = request.queryParams(name);
        if (value == null || value.isEmpty()) {
            errorMap.put(name, errorMessage);
        }
        return value;
    }

}
