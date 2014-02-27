package no.leinstrandil.web;

import java.util.List;
import java.util.Map;
import org.apache.velocity.VelocityContext;
import spark.Request;

public interface Controller {

    public void handleGet(Request request, VelocityContext context);

    public void handlePost(Request request, Map<String, String> errorMap, List<String> infoList);

}
