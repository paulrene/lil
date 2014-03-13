package no.leinstrandil.web;

import java.util.List;
import java.util.Map;
import no.leinstrandil.database.model.web.User;
import no.leinstrandil.service.SearchService;
import no.leinstrandil.service.SearchService.SearchResult;
import org.apache.velocity.VelocityContext;
import spark.Request;

public class SearchResultsController implements Controller {

    private SearchService searchService;

    public SearchResultsController(SearchService searchService) {
        this.searchService = searchService;
    }

    @Override
    public void handleGet(User user, Request request, VelocityContext context) {
        String q = request.queryParams("q");
        String pageStr = request.queryParams("p");
        int page = 0;
        if (pageStr != null) {
            try {
                page = Integer.parseInt(pageStr);
            } catch (NumberFormatException e) {
                // Ignore
            }
        }

        List<SearchResult> searchResult = searchService.search(q);

        int pageSize = 5;
        int pageCount = searchResult.size() / pageSize;

        int prevPage = page - 1;
        if (prevPage < 0) {
            prevPage = 0;
        }
        int nextPage = page + 1;
        if (nextPage > pageCount) {
            nextPage = pageCount;
        }
        int startIndex = page * pageSize;
        int endIndex = (page + 1) * pageSize;
        if (endIndex >= searchResult.size()) {
            endIndex = searchResult.size() - 1;
        }
        context.put("q", q);
        context.put("p", page);
        context.put("pageSize", pageSize);
        context.put("pageCount", pageCount);
        context.put("prevPage", prevPage);
        context.put("nextPage", nextPage);
        context.put("startIndex", startIndex);
        context.put("endIndex", endIndex);
        context.put("searchResult", searchResult);
    }

    @Override
    public void handlePost(User user, Request request, Map<String, String> errorMap, List<String> infoList) {

    }

}
