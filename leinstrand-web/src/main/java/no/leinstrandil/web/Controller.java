package no.leinstrandil.web;

import java.util.List;
import java.util.Map;
import no.leinstrandil.database.model.web.User;
import org.apache.velocity.VelocityContext;
import spark.Request;

public interface Controller {

    public void handleGet(User user, Request request, VelocityContext context);

    public String handlePost(User user, Request request, Map<String, String> errorMap, List<String> infoList);

}
