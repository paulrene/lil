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
import javax.persistence.Query;
import javax.persistence.TemporalType;
import javax.persistence.TypedQuery;
import no.leinstrandil.database.Storage;
import no.leinstrandil.database.model.accounting.Invoice.Status;
import no.leinstrandil.database.model.accounting.InvoiceLine;
import no.leinstrandil.database.model.club.ClubMembership;
import no.leinstrandil.database.model.club.Event;
import no.leinstrandil.database.model.club.EventParticipation;
import no.leinstrandil.database.model.club.Sport;
import no.leinstrandil.database.model.club.Team;
import no.leinstrandil.database.model.club.TeamMembership;
import no.leinstrandil.database.model.person.Family;
import no.leinstrandil.database.model.person.Principal;
import no.leinstrandil.database.model.web.User;
import no.leinstrandil.incident.ClubIncident;
import no.leinstrandil.incident.IncidentHub;
import org.joda.time.DateTime;
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

    public ClubMembership getClubMembership(Family family) {
        Principal principal = family.getPrimaryPrincipal();
        User user = principal.getUser();
        return ensureClubMembership(user, false);
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
                IncidentHub.report(new ClubIncident(user.getPrincipal(), "could_not_enroll", errorList.toString()));
                return new ServiceResponse(false, message.toString());
            }
        } else { // Utmelding
            StringBuilder str = new StringBuilder();
            if (getActiveTeamMembershipCountForFamily(user.getPrincipal().getFamily()) > 0) {
                str.append("Du kan ikke melde deg ut av idrettslaget før du har meldt eventuelle familiemedlemmer og deg selv av alle aktiviteter");
                if (getActiveEventParticipationCountForFamilyThatRequireMembership(user.getPrincipal().getFamily()) > 0) {
                    str.append(" og arrangement som krever medlemskap.");
                } else {
                    str.append(".");
                }
            } else if(getActiveEventParticipationCountForFamilyThatRequireMembership(user.getPrincipal().getFamily()) > 0) {
                str.append("Du kan ikke melde deg ut av idrettslaget før du har meldt eventuelle familiemedlemmer og deg selv av alle arrangement som krever medlemskap.");
            }
            if (str.length() > 0) {
                return new ServiceResponse(false, str.toString());
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
                mailService.sendNoReplyHtmlMessage(
                        user.getPrincipal().getFamily().getPrimaryPrincipal().getEmailAddressList().get(0).getEmail(),
                        "Velkommen som medlem i Leinstrand idrettslag!", text.toString());
                log.info("User " + user.getUsername() + " has been enrolled as club member.");
                IncidentHub.report(new ClubIncident(user.getPrincipal(), "enrolled_as_member"));
                return new ServiceResponse(true, "Din medlemsstatus er blitt oppdatert. Velkommen som medlem!");
            } else {
                log.info("User "+ user.getUsername()+ " has been DISenrolled as club member.");
                IncidentHub.report(new ClubIncident(user.getPrincipal(), "DISenrolled_as_member"));
                return new ServiceResponse(true, "Din medlemsstatus er blitt oppdatert. Du er ikke lenger registrert som medlem.");
            }
        } catch (RuntimeException e) {
            storage.rollback();
            return new ServiceResponse(false, "Det oppstod en feil ved lagring. Vennligst forsøk igjen.");
        }
    }

    public boolean isDeletable(Principal principal) {
        return getActiveTeamMembershipCountForPrincipal(principal) == 0
                && getActiveEventParticipationCountForPrincipal(principal) == 0;
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

    public int getActiveEventParticipationCountForFamilyThatRequireMembership(Family family) {
        int count = 0;
        List<Principal> principalList = family.getMembers();
        for (Principal principal : principalList) {
            count += getActiveEventParticipationCountForPrincipalThatRequireMembership(principal);
        }
        return count;
    }

    public int getActiveEventParticipationCountForPrincipalThatRequireMembership(Principal principal) {
        int count = 0;
        Map<Event, EventParticipation> status = getEventParticipationStatusForPrincipal(principal);
        for (EventParticipation participation : status.values()) {
            if (participation.isEnrolled()
                    && isActiveEvent(participation.getEvent())
                    && participation.getEvent().requireMembership()) {
                count++;
            }
        }
        return count;
    }

    public boolean isActiveEvent(Event event) {
        if (event.getEndTime() == null) {
            return event.getStartTime().after(new Date());
        }
        return event.getEndTime().after(new Date());
    }

    public boolean hasEventStarted(Event event) {
        return event.getStartTime().before(new Date());
    }

    public int getActiveEventParticipationCountForFamily(Family family) {
        int count = 0;
        List<Principal> principalList = family.getMembers();
        for (Principal principal : principalList) {
            count += getActiveEventParticipationCountForPrincipal(principal);
        }
        return count;
    }

    public int getActiveEventParticipationCountForPrincipal(Principal principal) {
        int count = 0;
        Map<Event, EventParticipation> status = getEventParticipationStatusForPrincipal(principal);
        for (EventParticipation participation : status.values()) {
            if (participation.isEnrolled() && participation.getEvent().getStartTime().after(new Date())) {
                count++;
            }
        }
        return count;
    }

    public Map<Event, EventParticipation> getEventParticipationStatusForPrincipal(Principal principal) {
        Map<Event, EventParticipation> status = new HashMap<>();
        List<EventParticipation> eventParticipations = principal.getEventParticipations();
        for (EventParticipation eventParticipation : eventParticipations) {
            Event event = eventParticipation.getEvent();
            if (!status.containsKey(event)) {
                status.put(event, eventParticipation);
            }
        }
        return status;
    }

    public Map<Event, EventParticipation> getRecentAndFutureEventParticipationStatusForPrincipal(Principal principal) {
        Date cutOffDate = new DateTime().minusMonths(3).toDate();
        Map<Event, EventParticipation> status = new HashMap<>();
        List<EventParticipation> eventParticipations = principal.getEventParticipations();
        for (EventParticipation eventParticipation : eventParticipations) {
            Event event = eventParticipation.getEvent();
            if (event.getStartTime().before(cutOffDate)) {
                continue;
            }
            if (status.containsKey(event)) {
                continue;
            }
            status.put(event, eventParticipation);
        }
        return status;
    }

    public static boolean isEnrolledAsClubMember(Family family) {
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

    public List<Event> getFutureEvents() {
        TypedQuery<Event> query = storage.createQuery("from Event where startTime > now() order by startTime", Event.class);
        return query.getResultList();
    }

    public List<Event> getEvents() {
        TypedQuery<Event> query = storage.createQuery("from Event order by startTime desc", Event.class);
        return query.getResultList();
    }

    @SuppressWarnings("unchecked")
    public List<Event> getRecentAndFutureEvents() {
        DateTime cutOffDate = new DateTime().minusMonths(3);
        Query query = storage.createQuery("from Event where startTime > :cutOffDate order by startTime desc");
        query.setParameter("cutOffDate", cutOffDate.toDate(), TemporalType.DATE);
        return query.getResultList();
    }

    public boolean isEventEnded(Event event) {
        Date end = event.getEndTime();
        if (end == null) {
            end = event.getStartTime();
        }
        if (end == null) {
            return true;
        }
        return end.before(new Date());
    }

    public int getEnrolledCountForEvent(Event event) {
        int count = 0;
        Map<Principal, EventParticipation> status = getEventParticipationForEvent(event);
        for (EventParticipation participation : status.values()) {
            if (participation.isEnrolled()) {
                count ++;
            }
        }
        return count;
    }

    public Integer getSpotsLeft(Event event) {
        if (event == null) {
            return null;
        }
        Integer vacancies = event.getVacancies();
        if (vacancies == null) {
            return -1;
        }
        return vacancies - getEnrolledCountForEvent(event);
    }

    public Map<Principal, EventParticipation> getEventParticipationForEvent(Event event) {
        // Assume the list is in descending order (newest first)
        List<EventParticipation> eventParticipations = event.getEventParticipations();
        Map<Principal, EventParticipation> map = new HashMap<>();
        for (EventParticipation eventParticipation : eventParticipations) {
            if(!map.containsKey(eventParticipation.getPrincipal())) {
                map.put(eventParticipation.getPrincipal(), eventParticipation);
            }
        }
        return map;
    }

    public TeamMembership getTeamMembershipById(Long id) {
        try {
            return storage.createSingleQuery("from TeamMembership where id = " + id, TeamMembership.class);
        } catch (NoResultException e) {
            return null;
        }
    }

    public EventParticipation getEventParticipationById(Long id) {
        try {
            return storage.createSingleQuery("from EventParticipation where id = " + id, EventParticipation.class);
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

    public Event getEventById(Long id) {
        try {
            return storage.createSingleQuery("from Event where id = " + id, Event.class);
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
            IncidentHub.report(new ClubIncident(teamMembership.getPrincipal(), "delete_team_membership", teamMembership.getTeam().getName()));
            return new ServiceResponse(true, "Personen er nå meldt av denne aktiviteten.");
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
            IncidentHub.report(new ClubIncident(principal, "create_team_membership", team.getName()));
            return new ServiceResponse(true, "Personen er nå påmeldt denne aktiviteten.");
        } catch (RuntimeException e) {
            storage.rollback();
            return new ServiceResponse(false, "Påmeldingen kunne ikke opprettes på nåværende tidspunkt.");
        }
    }

    public ServiceResponse deleteEventParticipation(EventParticipation eventParticipation) {
        EventParticipation participation = new EventParticipation();
        participation.setCreated(new Date());
        participation.setEnrolled(false);
        participation.setPrincipal(eventParticipation.getPrincipal());
        participation.setEvent(eventParticipation.getEvent());
        storage.begin();
        try {
            storage.persist(participation);
            storage.commit();
            IncidentHub.report(new ClubIncident(eventParticipation.getPrincipal(), "delete_event_participation", eventParticipation.getEvent().getName()));
            return new ServiceResponse(true, "Personen er nå meldt av dette arrangementet.");
        } catch (RuntimeException e) {
            storage.rollback();
            return new ServiceResponse(false, "Påmeldingen kunne ikke endres på nåværende tidspunkt.");
        }
    }

    public ServiceResponse createEventParticipation(Principal principal, Event event) {
        if (event == null || principal == null) {
            return new ServiceResponse(false, "Påmelding krever en gyldig person og et gyldig arrangement.");
        }
        EventParticipation participation = new EventParticipation();
        participation.setCreated(new Date());
        participation.setEnrolled(true);
        participation.setPrincipal(principal);
        participation.setEvent(event);
        storage.begin();
        try {
            storage.persist(participation);
            storage.commit();
            IncidentHub.report(new ClubIncident(principal, "create_event_participation", event.getName()));
            return new ServiceResponse(true, "Personen er nå påmeldt dette arrangementet.");
        } catch (RuntimeException e) {
            storage.rollback();
            return new ServiceResponse(false, "Påmeldingen kunne ikke opprettes på nåværende tidspunkt.");
        }
    }

    public Map<Principal, TeamMembership> getTeamMembershipsForTeam(Team team) {
        storage.refresh(team);
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

    public int getTeamCount() {
        TypedQuery<Team> teamQuery = storage.createQuery("from Team", Team.class);
        return teamQuery.getResultList().size();
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

    public EventParticipation getLastEnrollmentToEventForPrincipal(Event event, Principal principal) {
        List<EventParticipation> list = event.getEventParticipations();
        for (EventParticipation participation : list) {
            if (participation.getPrincipal().getId().equals(principal.getId())) {
                if (participation.isEnrolled()) {
                    return participation;
                }
            }
        }
        return null;
    }

    public List<Principal> queryPrincipal(String query) {
        TypedQuery<Principal> q = storage.createQuery("from Principal where name like '%" + query + "%' order by lastName, firstName", Principal.class);
        return q.getResultList();
    }

    public List<Family> queryFamily(String query) {
        TypedQuery<Family> q = storage.createQuery("from Family f where f.primaryPrincipal.name like '%" + query + "%' order by f.primaryPrincipal.lastName, f.primaryPrincipal.firstName", Family.class);
        return q.getResultList();
    }

    public static enum PaymentStatus {
        OK("Betalt"),
        INVOICED_NOT_PAID("Venter på betaling"),
        INVOICED_OVERDUE("Betalingsfrist utløpt"),
        REFUNDED("Refundert"),
        PROCESSING("Faktura under utarbeidelse"),
        NOT_INVOICED("Ikke fakturert"),
        NOT_ABLE_TO_SEND("Mangler gyldig mottaker");

        private String description;

        private PaymentStatus(String description) {
            this.description = description;
        }
        public String getDescription() {
            return description;
        }
    }

    public PaymentStatus getTeamMembershipPaymentStatusCurrentYear(TeamMembership currentTeamMembership) {
        if (currentTeamMembership == null) {
            return null;
        }
        PaymentStatus paymentStatus = PaymentStatus.NOT_INVOICED;
        Principal principal = currentTeamMembership.getPrincipal();
        List<TeamMembership> teamMemberships = principal.getTeamMemberships();
        for (TeamMembership teamMembership : teamMemberships) {
            if (currentTeamMembership.getTeam().getId() == teamMembership.getTeam().getId()) {
                PaymentStatus temp = getPaymentStatusFromTeamMembership(teamMembership, InvoiceService.getCurrentYear());
                if (temp != null) {
                    paymentStatus = temp;
                }
                if (paymentStatus == PaymentStatus.OK) {
                    break;
                }
            }
        }
        return paymentStatus;
    }

    public PaymentStatus getPaymentStatusFromTeamMembership(TeamMembership teamMembership, int yearToConsider) {
        List<InvoiceLine> invoiceLines = teamMembership.getInvoiceLines();
        for (InvoiceLine invoiceLine : invoiceLines) {
            if (invoiceLine.getValidYear() != yearToConsider) {
                continue;
            }
            Status status = invoiceLine.getInvoice().getStatus();
            switch (status) {
            case CREDITED: return PaymentStatus.REFUNDED;
            case OPEN: return PaymentStatus.PROCESSING;
            case PAID: return PaymentStatus.OK;
            case SEND_FAILED: return PaymentStatus.NOT_ABLE_TO_SEND;
            case SENT:
                if (new Date().after(invoiceLine.getInvoice().getExternalInvoiceDue())) {
                    return PaymentStatus.INVOICED_OVERDUE;
                } else {
                    return PaymentStatus.INVOICED_NOT_PAID;
                }
            }
        }
        return PaymentStatus.NOT_INVOICED;
    }

}
