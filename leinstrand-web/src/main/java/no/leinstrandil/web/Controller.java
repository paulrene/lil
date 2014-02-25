package no.leinstrandil.web;

import java.util.List;
import java.util.Map;
import spark.Request;

public interface Controller {

    public void handleGet(Request request);

    public void handlePost(Request request, Map<String, String> errorMap, List<String> infoList);

}
