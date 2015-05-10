package no.leinstrandil.web;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import no.leinstrandil.database.model.club.Event;
import no.leinstrandil.database.model.club.EventParticipation;
import no.leinstrandil.database.model.club.Team;
import no.leinstrandil.database.model.club.TeamMembership;
import no.leinstrandil.database.model.person.Principal;
import no.leinstrandil.database.model.web.User;
import no.leinstrandil.service.ClubService;
import no.leinstrandil.service.MailService;
import no.leinstrandil.service.UserService;
import org.apache.velocity.VelocityContext;
import spark.Request;

public class TeamLeadController implements Controller {

    private ClubService clubService;
    private MailService mailService;
    private UserService userService;

    public TeamLeadController(MailService mailService, UserService userService, ClubService clubService) {
        this.mailService = mailService;
        this.userService = userService;
        this.clubService = clubService;
    }

    @Override
    public void handleGet(User user, Request request, VelocityContext context) {
        String action = request.queryParams("action");
        String tab = request.queryParams("tab");
        if (tab == null) {
            tab = "oversikt";
        }
        context.put("tab", tab);

        if (tab.equals("lagliste")) {
            if ("list-team-members".equals(action)) {
                listTeamMembers(request, context);
            }
        }
        if (tab.equals("deltagerliste")) {
            if ("list-event-participants".equals(action)) {
                listEventParticipants(request, context);
            }
        }
        if (tab.equals("sok")) {
            if ("search-name".equals(action)) {
                String query = request.queryParams("query");
                List<Principal> results = clubService.queryPrincipal(query);
                context.put("results", results);
                context.put("query", query);
            }
        }

    }

    private void listEventParticipants(Request request, VelocityContext context) {
        String eventIdStr = request.queryParams("eventid");
        if (eventIdStr == null) {
            context.put("error", "Du må velge et arrangement å vise deltagerlisten for.");
            return;
        }
        Long eventId = null;
        try {
            eventId = Long.parseLong(eventIdStr);
        } catch(RuntimeException e) {
            context.put("error", "Du må velge et arrangement å vise deltagerlisten for.");
            return;
        }
        Event event = clubService.getEventById(eventId);
        Map<Principal, EventParticipation> selectedEventParticipationMap = clubService.getEventParticipationForEvent(event);
        context.put("selectedEvent", event);
        context.put("selectedEventParticipationMap", selectedEventParticipationMap);

        Boolean showContactInfo = Boolean.parseBoolean(request.queryParams("showcontactinfo"));
        Boolean showDisenrolled = Boolean.parseBoolean(request.queryParams("showdisenrolled"));
        context.put("showContactInfo", showContactInfo);
        context.put("showDisenrolled", showDisenrolled);
    }

    private void listTeamMembers(Request request, VelocityContext context) {
        String teamIdStr = request.queryParams("teamid");
        if (teamIdStr == null) {
            context.put("error", "Du må velge et lag å vise lagslisten for.");
            return;
        }
        Long teamId = null;
        try {
            teamId = Long.parseLong(teamIdStr);
        } catch(RuntimeException e) {
            context.put("error", "Du må velge et lag å vise lagslisten for.");
            return;
        }
        Team team = clubService.getTeamById(teamId);
        Map<Principal, TeamMembership> selectedTeamMembershipMap = clubService.getTeamMembershipsForTeam(team);
        context.put("selectedTeam", team);
        context.put("selectedTeamMembershipMap", selectedTeamMembershipMap);

        Boolean showContactInfo = Boolean.parseBoolean(request.queryParams("showcontactinfo"));
        Boolean showDisenrolled = Boolean.parseBoolean(request.queryParams("showdisenrolled"));
        context.put("showContactInfo", showContactInfo);
        context.put("showDisenrolled", showDisenrolled);
    }

    @Override
    public String handlePost(User user, Request request, Map<String, String> errorMap, List<String> infoList) {
        if (user == null) {
            return null;
        }

        String action = request.queryParams("action");
        if ("send-message".equals(action)) {
            sendMessage(user, request, errorMap, infoList);
        }

        return null;
    }

    private void sendMessage(User user, Request request, Map<String, String> errorMap, List<String> infoList) {
        String teamIdStr = request.queryParams("teamid");
        String selectionStr = request.queryParams("selection");
        String subjectStr = request.queryParams("subject");
        String messageStr = request.queryParams("message");

        boolean copyMe = Boolean.parseBoolean(request.queryParams("copyme"));

        Long teamId = null;
        if (teamIdStr == null || teamIdStr.isEmpty()) {
            errorMap.put("teamid", "Du må velge en mottakerliste.");
        } else {
            try {
                teamId = Long.parseLong(teamIdStr);
            } catch (NumberFormatException e) {
                errorMap.put("teamid", "Mottakerlisteidentifikatoren er ugyldig.");
            }
        }
        boolean sendToAthletes = false;
        boolean sendToGuardians = false;
        if (selectionStr == null || selectionStr.isEmpty()) {
            errorMap.put("selection", "Du må velge et utvalg for mottakerlisten.");
        } else {
            if ("all".equals(selectionStr)) {
                sendToAthletes = true;
                sendToGuardians = true;
            } else if ("guardians".equals(selectionStr)) {
                sendToGuardians = true;
            } else if ("athletes".equals(selectionStr)) {
                sendToAthletes = true;
            } else {
                errorMap.put("selection", "Du må velge et utvalg som meldingen skal sendes til.");
            }
        }
        if (subjectStr == null || subjectStr.isEmpty()) {
            errorMap.put("subject", "Du må fylle ut emnefeltet.");
        }
        if (messageStr == null || messageStr.isEmpty()) {
            errorMap.put("message", "Du må skrive en melding.");
        }

        if (errorMap.isEmpty()) {
            Team team = clubService.getTeamById(teamId);
            if (team == null) {
                errorMap.put("teamid", "Ukjent lag eller aktivitet.");
                return;
            }

            Map<String, String> toMap = new HashMap<>();
            Map<Principal, TeamMembership> teamMemberships = clubService.getTeamMembershipsForTeam(team);
            for (Principal member : teamMemberships.keySet()) {
                TeamMembership membership = teamMemberships.get(member);
                if (membership.isEnrolled()) {
                    if (sendToGuardians) {
                        if (userService.isOnlyPrincipal(member) || !userService.isOfAge(member)) {
                            Principal guardian = member.getFamily().getPrimaryPrincipal();
                            toMap.put(guardian.getEmailAddressList().get(0).getEmail(), member.getName() + " c/o " + guardian.getName());
                        } else { // If member is of age AND has user he is his OWN guardian.
                            toMap.put(member.getEmailAddressList().get(0).getEmail(), member.getName());
                        }
                    }
                    if (sendToAthletes) {
                        if (userService.isOnlyPrincipal(member)) {
                            Principal guardian = member.getFamily().getPrimaryPrincipal();
                            toMap.put(guardian.getEmailAddressList().get(0).getEmail(), member.getName() + " c/o " + guardian.getName());
                        } else {
                            toMap.put(member.getEmailAddressList().get(0).getEmail(), member.getName());
                        }
                    }
                }
            }

            if (toMap.isEmpty()) {
                errorMap.put("save", "Dette laget eller aktiviteten har for øyeblikket ingen påmeldte.");
                return;
            }

            Principal principal = user.getPrincipal();
            if (mailService.sendHtmlMessage(principal.getName(), principal.getEmailAddressList().get(0).getEmail(), copyMe, toMap, subjectStr, messageStr)) {
                infoList.add("Meldingen ble sendt til " + toMap.size() + " mottakere.");
            } else {
                errorMap.put("save", "Det oppstod en feil. Meldingen ble ikke sendt.");
            }
        }
    }

}
