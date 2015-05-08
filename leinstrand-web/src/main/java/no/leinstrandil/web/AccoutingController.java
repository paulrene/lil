package no.leinstrandil.web;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import no.leinstrandil.database.model.club.Team;
import no.leinstrandil.database.model.web.User;
import no.leinstrandil.service.ClubService;
import no.leinstrandil.service.InvoiceService;
import no.leinstrandil.service.UserService;
import org.apache.velocity.VelocityContext;
import spark.QueryParamsMap;
import spark.Request;

public class AccoutingController implements Controller {

    private final UserService userService;
    private final ClubService clubService;
    private final InvoiceService invoiceService;

    public AccoutingController(UserService userService, ClubService clubService, InvoiceService invoiceService) {
        this.userService = userService;
        this.clubService = clubService;
        this.invoiceService = invoiceService;
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
        String action = request.queryParams("action");
        if ("create-invoices".equals(action)) {
            createInvoices(user, request, errorMap, infoList);
        }
        return null;
    }

    private void createInvoices(User user, Request request, Map<String, String> errorMap, List<String> infoList) {
        if (!userService.hasRole(user, "accountant")) {
            errorMap.put("doinvoice", "Du må være kasserer for å utføre fakturering.");
            return;
        }

        QueryParamsMap map = request.queryMap("teamid");
        String[] teamIdStrArray = map.values();
        if (teamIdStrArray == null) {
            errorMap.put("doinvoice", "Ingen lag eller aktiviteter ble valgt.");
            return;
        }
        List<Long> teamIdList = new ArrayList<>();
        for(String teamIdStr : teamIdStrArray) {
            try {
                teamIdList.add(Long.parseLong(teamIdStr));
            } catch (NumberFormatException e) {
                errorMap.put("doinvoice", "Tøysekopp, du må velge aktiviteter og lag i listen ovenfor.");
                return;
            }
        }

        for (Long teamId : teamIdList) {
            if (teamId == -1) {
                invoiceService.createClubMembershipInvoice();
                infoList.add("Faktura for klubbavgift opprettet.");
                continue;
            }
            Team team = clubService.getTeamById(teamId);
            if (team != null) {
                invoiceService.createInvoiceForTeam(team);
                infoList.add("Faktura for " + team.getName() + " opprettet.");
            } else {
                errorMap.put("doinvoice", "Kunne ikke finne lag med id = " + teamId);
            }
        }
    }

}
