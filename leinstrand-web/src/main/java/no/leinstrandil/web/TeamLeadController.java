package no.leinstrandil.web;

import java.util.List;
import java.util.Map;
import no.leinstrandil.database.model.club.Team;
import no.leinstrandil.database.model.club.TeamMembership;
import no.leinstrandil.database.model.person.Principal;
import no.leinstrandil.database.model.web.User;
import no.leinstrandil.service.ClubService;
import no.leinstrandil.service.UserService;
import org.apache.velocity.VelocityContext;
import spark.Request;

public class TeamLeadController implements Controller {

    private UserService userService;
    private ClubService clubService;

    public TeamLeadController(UserService userService, ClubService clubService) {
        this.userService = userService;
        this.clubService = clubService;
    }

    @Override
    public void handleGet(User user, Request request, VelocityContext context) {
        String tab = request.queryParams("tab");
        if (tab == null) {
            tab = "lagliste";
        }
        context.put("tab", tab);


        String action = request.queryParams("action");
        if ("list-team-members".equals(action)) {
            listTeamMembers(request, context);
        }
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

        return null;
    }

}
