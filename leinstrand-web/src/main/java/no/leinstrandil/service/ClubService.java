package no.leinstrandil.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.persistence.NoResultException;
import javax.persistence.TypedQuery;
import no.leinstrandil.database.Storage;
import no.leinstrandil.database.model.club.ClubMembership;
import no.leinstrandil.database.model.club.Sport;
import no.leinstrandil.database.model.club.Team;
import no.leinstrandil.database.model.club.TeamMembership;
import no.leinstrandil.database.model.person.Family;
import no.leinstrandil.database.model.person.Principal;
import no.leinstrandil.database.model.web.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ClubService {
    private static final Logger log = LoggerFactory.getLogger(ClubService.class);

    private Storage storage;
    private UserService userService;
    private MailService mailService;

    public ClubService(Storage storage, UserService userService, MailService mailService) {
        this.storage = storage;
        this.userService = userService;
        this.mailService = mailService;
    }

    public ClubMembership ensureClubMembership(User user, boolean enrolled) {
        List<ClubMembership> membershipList = user.getPrincipal().getFamily().getClubMemberships();
        if (!membershipList.isEmpty()) {
            return membershipList.get(0);
        }
        ClubMembership membership = new ClubMembership();
        membership.setCreated(new Date());
        membership.setEnrolled(enrolled);
        membership.setFamily(user.getPrincipal().getFamily());
        user.getPrincipal().getFamily().getClubMemberships().add(membership);
        storage.begin();
        storage.persist(membership);
        storage.commit();
        return membership;
    }

    public boolean hasChangeClubMembershipRights(User user) {
        return userService.isPrimaryContact(user.getPrincipal());
    }

    public ServiceResponse updateClubMembership(User user, boolean wantToEnroll) {
        if (wantToEnroll && isEnrolledAsClubMember(user.getPrincipal().getFamily())) {
            return new ServiceResponse(false, "Du er allerede medlem.");
        }
        if (!wantToEnroll && !isEnrolledAsClubMember(user.getPrincipal().getFamily())) {
            return new ServiceResponse(false, "Du trenger ikke melde deg ut, da du ikke er medlem.");
        }
        if (!hasChangeClubMembershipRights(user)) {
            return new ServiceResponse(false, "Du må ha status som primærkontakt for å kunne endre medlemsstatus.");
        }

        List<String> errorList = new ArrayList<>();
        if (wantToEnroll) {
            if (!userService.hasValidEmail(user)) errorList.add("e-postadresse");
            if (!userService.hasValidAddress(user)) errorList.add("postadresse");
            if (!userService.hasValidMobile(user)) errorList.add("mobilnummer");
            if (!errorList.isEmpty()) {
                StringBuilder message = new StringBuilder();
                message.append("Du må registrere ");
                appendList(message, errorList);
                message.append(" før du kan foreta innmelding.");
                return new ServiceResponse(false, message.toString());
            }
        } else { // Utmelding
            if (getActiveTeamMembershipCountForFamily(user.getPrincipal().getFamily()) > 0) {
                return new ServiceResponse(false, "Du kan ikke melde deg ut av idrettslaget før du har meldt eventuelle familiemedlemmer og deg selv av alle aktiviteter.");
            }
        }

        ClubMembership membership = new ClubMembership();
        membership.setCreated(new Date());
        membership.setEnrolled(wantToEnroll);
        membership.setFamily(user.getPrincipal().getFamily());

        storage.begin();
        try {
            storage.persist(membership);
            storage.commit();
            if (wantToEnroll) {
                StringBuilder text = new StringBuilder();
                text.append("Hei, ").append(user.getPrincipal().getName()).append("!<br><br>");
                text.append("<strong>Velkommen som medlem i Leinstrand idrettslag!</strong><br><br>");
                Family family = user.getPrincipal().getFamily();
                if (family.getMembers().size() > 1) {
                    text.append("Du og din familie er nå registrert som <i>familiemedlemmer</i>. ");
                    text.append("Du kan melde deg selv eller de andre familiemedlemmene på aktiviteter eller ");
                    text.append("arrangementer hos oss via <a href=\"%baseUrl%page/minside?tab=aktiviteter\">min side</a>. ");
                    text.append("Vi håper dere vil få en flott tid som medlemmer hos oss. Har du spørsmål eller ");
                    text.append("noe du lurer på kan du når som helst ta kontakt via vår ");
                    text.append("<a href=\"http://facebook.com/LeinstrandIL\">Facebook side</a> eller sende en e-post ");
                    text.append("til <a href=\"mailto:kontakt@leinstrandil.no\">kontakt@leinstrandil.no</a>.");
                } else {
                    text.append("Du er på nåværende tidspunkt registrert som <i>enkeltmedlem</i>. ");
                    text.append("Om du ønsker å legge til ");
                    text.append("andre familiemedlemmer og endre til et familiemedlemskap kan du gjøre det ved å ");
                    text.append("registrere flere personer under din familie på ");
                    text.append("<a href=\"%baseUrl%page/minside?tab=familie\">min side</i>.<br><br>");
                    text.append("Du kan melde deg på aktiviteter eller arrangementer hos oss via ");
                    text.append("<a href=\"%baseUrl%page/minside?tab=aktiviteter\">aktiviteter på min side</a>. ");
                    text.append("Vi håper du vil få en flott tid som medlem hos oss. Har du spørsmål eller noe du ");
                    text.append("lurer på kan du når som helst ta kontakt via vår ");
                    text.append("<a href=\"http://facebook.com/LeinstrandIL\">Facebook side</a> eller sende en e-post ");
                    text.append("til <a href=\"mailto:kontakt@leinstrandil.no\">kontakt@leinstrandil.no</a>.");
                }
                text.append("<br><br>Med vennlig hilsen,<br>Leinstrand idrettslag.");
                mailService.sendNoReplyHtml(
                        user.getPrincipal().getFamily().getPrimaryPrincipal().getEmailAddressList().get(0).getEmail(),
                        "Velkommen som medlem i Leinstrand idrettslag!", text.toString());
                log.info("User " + user.getUsername() + " has been enrolled as club member.");
                return new ServiceResponse(true, "Din medlemsstatus er blitt oppdatert. Velkommen som medlem!");
            } else {
                log.info("User "+ user.getUsername()+ " has been DISenrolled as club member.");
                return new ServiceResponse(true, "Din medlemsstatus er blitt oppdatert. Du er ikke lenger registrert som medlem.");
            }
        } catch (RuntimeException e) {
            storage.rollback();
            return new ServiceResponse(false, "Det oppstod en feil ved lagring. Vennligst forsøk igjen.");
        }
    }

    public boolean isDeletable(Principal principal) {
        return getActiveTeamMembershipCountForPrincipal(principal) == 0;
    }

    public int getActiveTeamMembershipCountForFamily(Family family) {
        int count = 0;
        List<Principal> principalList = family.getMembers();
        for (Principal principal : principalList) {
            count += getActiveTeamMembershipCountForPrincipal(principal);
        }
        return count;
    }

    public int getActiveTeamMembershipCountForPrincipal(Principal principal) {
        int count = 0;
        Map<Team, TeamMembership> status = getTeamMembershipStatusForPrincipal(principal);
        for (TeamMembership membership : status.values()) {
            if (membership.isEnrolled()) {
                count++;
            }
        }
        return count;
    }

    public Map<Team, TeamMembership> getTeamMembershipStatusForPrincipal(Principal principal) {
        Map<Team, TeamMembership> status = new HashMap<>();
        List<TeamMembership> teamMemberships = principal.getTeamMemberships();
        for (TeamMembership teamMembership : teamMemberships) {
            Team team = teamMembership.getTeam();
            if (!status.containsKey(team)) {
                status.put(team, teamMembership);
            }
        }
        return status;
    }

    public boolean isEnrolledAsClubMember(Family family) {
        List<ClubMembership> list = family.getClubMemberships();
        if (list == null || list.isEmpty()) {
            return false;
        }
        return list.get(0).isEnrolled();
    }

    private void appendList(StringBuilder message, List<String> elements) {
        if (elements.size() == 1) {
            message.append(elements.get(0));
            return;
        }
        for (int n=0;n<elements.size();n++) {
            message.append(elements.get(n));
            if ((n+1) < elements.size() && (n+2) == elements.size()) {
                message.append(" og ");
            } else if ((n+1) < elements.size()) {
                message.append(", ");
            }
        }
    }

    public List<Sport> getSports() {
        TypedQuery<Sport> query = storage.createQuery("from Sport order by name", Sport.class);
        return query.getResultList();
    }

    public TeamMembership getTeamMembershipById(Long id) {
        try {
            return storage.createSingleQuery("from TeamMembership where id = " + id, TeamMembership.class);
        } catch (NoResultException e) {
            return null;
        }
    }

    public Team getTeamById(Long id) {
        try {
            return storage.createSingleQuery("from Team where id = " + id, Team.class);
        } catch (NoResultException e) {
            return null;
        }
    }

    public ServiceResponse deleteTeamMembership(TeamMembership teamMembership) {
        TeamMembership membership = new TeamMembership();
        membership.setCreated(new Date());
        membership.setEnrolled(false);
        membership.setPrincipal(teamMembership.getPrincipal());
        membership.setTeam(teamMembership.getTeam());
        storage.begin();
        try {
            storage.persist(membership);
            storage.commit();
            return new ServiceResponse(true, "Du er nå meldt av denne aktiviteten.");
        } catch (RuntimeException e) {
            storage.rollback();
            return new ServiceResponse(false, "Påmeldingen kunne ikke endres på nåværende tidspunkt.");
        }
    }

    public ServiceResponse createTeamMembership(Principal principal, Team team) {
        if (team == null || principal == null) {
            return new ServiceResponse(false, "Påmelding krever en gyldig person og en gyldig aktivitet.");
        }
        TeamMembership membership = new TeamMembership();
        membership.setCreated(new Date());
        membership.setEnrolled(true);
        membership.setPrincipal(principal);
        membership.setTeam(team);
        storage.begin();
        try {
            storage.persist(membership);
            storage.commit();
            return new ServiceResponse(true, "Du er nå påmeldt denne aktiviteten.");
        } catch (RuntimeException e) {
            storage.rollback();
            return new ServiceResponse(false, "Påmeldingen kunne ikke opprettes på nåværende tidspunkt.");
        }
    }

    public Map<Principal, TeamMembership> getTeamMembershipsForTeam(Team team) {
        // Assume the list is in descending order (newest first)
        List<TeamMembership> teamMemberships = team.getTeamMemberships();
        Map<Principal, TeamMembership> map = new HashMap<>();
        for (TeamMembership teamMembership : teamMemberships) {
            if(!map.containsKey(teamMembership.getPrincipal())) {
                map.put(teamMembership.getPrincipal(), teamMembership);
            }
        }
        return map;
    }

    public int getEnrolledCountForTeam(Team team) {
        int count = 0;
        Map<Principal, TeamMembership> status = getTeamMembershipsForTeam(team);
        for (TeamMembership membership : status.values()) {
            if (membership.isEnrolled()) {
                count ++;
            }
        }
        return count;
    }

    public List<Principal> sortSetOfPrincipalsByLastName(Set<Principal> principalSet) {
        List<Principal> list = new ArrayList<>(principalSet);
        Collections.sort(list, new Comparator<Principal>() {
            @Override
            public int compare(Principal o1, Principal o2) {
                int value = o1.getLastName().compareTo(o2.getLastName());
                if (value == 0) {
                    value = o1.getFirstName().compareTo(o2.getFirstName());
                }
                return value;
            }
        });
        return list;
    }

    public TeamMembership getLastEnrollmentToTeamForPrincipal(Team team, Principal principal) {
        List<TeamMembership> list = team.getTeamMemberships();
        for (TeamMembership teamMembership : list) {
            if (teamMembership.getPrincipal().getId().equals(principal.getId())) {
                if (teamMembership.isEnrolled()) {
                    return teamMembership;
                }
            }
        }
        return null;
    }
}
