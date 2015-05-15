package no.leinstrandil.web;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import no.leinstrandil.database.model.accounting.Invoice;
import no.leinstrandil.database.model.club.Event;
import no.leinstrandil.database.model.club.Team;
import no.leinstrandil.database.model.web.User;
import no.leinstrandil.service.ClubService;
import no.leinstrandil.service.InvoiceService;
import no.leinstrandil.service.ServiceResponse;
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

        if (tab.equals("oversikt")) {
            context.put("invoiceCountReport", invoiceService.getInvoiceCountPerStatusReport());
        } else if (tab.equals("behandling")) {
            context.put("statusList", Invoice.Status.values());
            if ("list-invoice-of-type".equals(action)) {
                String subAction = request.queryParams("sub-action");
                context.put("subAction", subAction);
                String invoiceIdStr = request.queryParams("invoiceid");
                if ("delete-invoice".equals(subAction)) {
                    ServiceResponse response = invoiceService.deleteInvoice(Long.parseLong(invoiceIdStr));
                    populateServiceResponse(context, response);
                }
                if ("view-invoice".equals(subAction)) {
                    Invoice invoice = invoiceService.getInvoiceById(Long.parseLong(invoiceIdStr));
                    context.put("invoice", invoice);
                }
                if ("send-invoice".equals(subAction)) {
                    ServiceResponse response = invoiceService.sendInvoice(Long.parseLong(invoiceIdStr));
                    populateServiceResponse(context, response);
                }
                String statusStr = request.queryParams("invoice-status");
                Invoice.Status status = Invoice.Status.valueOf(statusStr);
                context.put("selectedStatus", status);
                context.put("invoiceService", invoiceService);
                context.put("invoiceList", invoiceService.getInvoicesWithStatus(status));
            } else if ("delete-all-invoices".equals(action)) {
                String statusStr = request.queryParams("invoice-status");
                Invoice.Status status = Invoice.Status.valueOf(statusStr);
                ServiceResponse response = invoiceService.deleteAllInvoicesWithStatus(status);
                populateServiceResponse(context, response);
                context.put("selectedStatus", status);
                context.put("invoiceService", invoiceService);
                context.put("invoiceList", invoiceService.getInvoicesWithStatus(status));
            } else if ("send-all-invoices".equals(action)) {
                String statusStr = request.queryParams("invoice-status");
                Invoice.Status status = Invoice.Status.valueOf(statusStr);
                ServiceResponse response = invoiceService.sendAllInvoicesWithStatus(status);
                populateServiceResponse(context, response);
                context.put("selectedStatus", status);
                context.put("invoiceService", invoiceService);
                context.put("invoiceList", invoiceService.getInvoicesWithStatus(status));
            }
        }
    }

    private void populateServiceResponse(VelocityContext context, ServiceResponse response) {
        if (response.isSuccess()) {
            context.put("info", response.getMessage());
        } else {
            context.put("error", response.getMessage());
        }
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

        QueryParamsMap map = request.queryMap("idlist");
        String[] idStrArray = map.values();
        if (idStrArray == null) {
            errorMap.put("doinvoice", "Ingen lag eller aktiviteter ble valgt.");
            return;
        }
        List<Long> teamIdList = new ArrayList<>();
        List<Long> eventIdList = new ArrayList<>();
        for(String idStr : idStrArray) {
            try {
                String[] idValue = idStr.split(":");
                if (idValue[0].equals("team")) {
                    teamIdList.add(Long.parseLong(idValue[1]));
                } else {
                    eventIdList.add(Long.parseLong(idValue[1]));
                }
            } catch (NumberFormatException e) {
                errorMap.put("doinvoice", "Tøysekopp, du må velge noe i listen ovenfor.");
                return;
            }
        }

        for (Long teamId : teamIdList) {
            if (teamId == -1) {
                ServiceResponse response = invoiceService.createClubMembershipInvoice(InvoiceService.getCurrentYear());
                if (response.isSuccess()) {
                    infoList.add(response.getMessage());
                } else {
                    errorMap.put("doinvoice", response.getMessage());
                    return;
                }
                continue;
            }
            Team team = clubService.getTeamById(teamId);
            if (team != null) {
                ServiceResponse response = invoiceService.createInvoiceForTeam(team, InvoiceService.getCurrentYear());
                if (response.isSuccess()) {
                    infoList.add(response.getMessage());
                } else {
                    errorMap.put("doinvoice", response.getMessage());
                    return;
                }
            }
        }
        for (Long eventId : eventIdList) {
            Event event = clubService.getEventById(eventId);
            ServiceResponse response = invoiceService.createInvoiceForEvent(event);
            if (response.isSuccess()) {
                infoList.add(response.getMessage());
            } else {
                errorMap.put("doinvoice", response.getMessage());
                return;
            }

        }
    }

}
