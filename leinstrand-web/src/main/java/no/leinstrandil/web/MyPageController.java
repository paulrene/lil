package no.leinstrandil.web;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;
import no.leinstrandil.database.model.accounting.Invoice;
import no.leinstrandil.database.model.club.ClubMembership;
import no.leinstrandil.database.model.club.Event;
import no.leinstrandil.database.model.club.EventParticipation;
import no.leinstrandil.database.model.club.Team;
import no.leinstrandil.database.model.club.TeamMembership;
import no.leinstrandil.database.model.person.Address;
import no.leinstrandil.database.model.person.EmailAddress;
import no.leinstrandil.database.model.person.Family;
import no.leinstrandil.database.model.person.MobileNumber;
import no.leinstrandil.database.model.person.Principal;
import no.leinstrandil.database.model.web.User;
import no.leinstrandil.service.ClubService;
import no.leinstrandil.service.InvoiceService;
import no.leinstrandil.service.ServiceResponse;
import no.leinstrandil.service.UserService;
import org.apache.velocity.VelocityContext;
import org.json.JSONObject;
import spark.Request;

public class MyPageController implements Controller {
    private static final int GO_BACK_MONTHS = 12;

    private UserService userService;
    private ClubService clubService;
    private InvoiceService invoiceService;

    public MyPageController(UserService userService, ClubService clubService, InvoiceService invoiceService) {
        this.userService = userService;
        this.clubService = clubService;
        this.invoiceService = invoiceService;
    }

    @Override
    public void handleGet(User user, Request request, VelocityContext context) {
        String tab = request.queryParams("tab");
        if (tab == null) {
            tab = "profil";
        }
        context.put("tab", tab);

        Family family = userService.ensureFamilyForUser(user);
        ClubMembership membership = clubService.ensureClubMembership(user, false);
        context.put("invoiceCount", invoiceService.getInvoiceCountForFamilyWithStatus(family, Invoice.Status.SENT, GO_BACK_MONTHS));

        if (tab.equals("adresse")) {
            List<Address> addressList = user.getPrincipal().getAddressList();
            if (!addressList.isEmpty()) {
                Address address = addressList.get(0);
                JSONObject data = new JSONObject();
                data.put("address1", address.getAddress1());
                data.put("zip", address.getZip());
                data.put("city", address.getCity());
                data.put("country", address.getCountry());
                context.put("data", data);
            }
        } else if (tab.equals("epost")) {
            List<EmailAddress> emailList = user.getPrincipal().getEmailAddressList();
            if (!emailList.isEmpty()) {
                EmailAddress email = emailList.get(0);
                JSONObject data = new JSONObject();
                data.put("email", email.getEmail());
                data.put("verified", email.getVerified() != null);
                context.put("data", data);
            }
        } else if (tab.equals("mobil")) {
            List<MobileNumber> mobileList = user.getPrincipal().getMobileNumberList();
            if (!mobileList.isEmpty()) {
                MobileNumber mobile = mobileList.get(0);
                JSONObject data = new JSONObject();
                data.put("mobile", mobile.getNumber());
                context.put("data", data);
            }
        } else if (tab.equals("familie")) {
            List<Address> addressList = user.getPrincipal().getAddressList();
            if (addressList.size() > 0) {
                Address address = addressList.get(0);
                boolean displayNoCombined = !"7083".equals(address.getZip()) && !"7089".equals(address.getZip());
                context.put("showNoCombinedMembershipOption", displayNoCombined);
            }
            context.put("family", family);
        } else if (tab.equals("medlemskap")) {
            context.put("family", family);
            context.put("membership", membership);
        } else if (tab.equals("aktiviteter")) {
            context.put("family", family);
            context.put("membership", membership);
        } else if (tab.equals("arrangement")) {
            context.put("family", family);
            context.put("membership", membership);
        } else if (tab.equals("faktura")) {
            context.put("invoiceService", invoiceService);
            context.put("goBackMonths", GO_BACK_MONTHS);
            context.put("sentList", invoiceService.getInvoicesForFamilyWithStatus(family, Invoice.Status.SENT, GO_BACK_MONTHS));
            context.put("paidList", invoiceService.getInvoicesForFamilyWithStatus(family, Invoice.Status.PAID, GO_BACK_MONTHS));
        }

    }

    @Override
    public String handlePost(User user, Request request, Map<String, String> errorMap, List<String> infoList) {
        if (user == null) {
            return null;
        }
        String action = request.queryParams("action");
        if ("save-profile".equals(action)) {
            saveProfile(user, request, errorMap, infoList);
        } else if ("save-address".equals(action)) {
            saveAddress(user, request, errorMap, infoList);
        } else if ("save-email".equals(action)) {
            saveEmail(user, request, errorMap, infoList);
        } else if ("save-mobile".equals(action)) {
            saveMobile(user, request, errorMap, infoList);
        } else if ("add-family-member".equals(action)) {
            addFamilyMember(user, request, errorMap, infoList);
        } else if ("enroll-club".equals(action)) {
            updateClubMembership(user, request, errorMap, infoList, true);
        } else if ("disenroll-club".equals(action)) {
            updateClubMembership(user, request, errorMap, infoList, false);
        } else if ("invite-family-member".equals(action)) {
            inviteFamilyMember(user, request, errorMap, infoList);
        } else if ("delete-principal".equals(action)) {
            deletePrincipal(user, request, errorMap, infoList);
        } else if ("make-primary-contact".equals(action)) {
            makePrimaryContact(user, request, errorMap, infoList);
        } else if ("remove-principal-from-team".equals(action)) {
            removePrincipalFromTeam(user, request, errorMap, infoList);
        } else if ("add-principal-to-team".equals(action)) {
            addPrincipalToTeam(user, request, errorMap, infoList);
        } else if ("add-principal-to-event".equals(action)) {
            addPrincipalToEvent(user, request, errorMap, infoList);
        } else if ("remove-principal-from-event".equals(action)) {
            removePrincipalFromEvent(user, request, errorMap, infoList);
        } else if ("save-only-active-members".equals(action)) {
            saveNoCombinedMembership(user, request, errorMap, infoList);
        }
        return null;
    }

    private void saveNoCombinedMembership(User user, Request request, Map<String, String> errorMap, List<String> infoList) {
        Boolean activeMembers = Boolean.parseBoolean(request.queryParams("active-members"));
        popuplateResponse(errorMap, infoList, userService.updateNoCombinedMembership(user, activeMembers));
    }

    private void addPrincipalToEvent(User user, Request request, Map<String, String> errorMap, List<String> infoList) {
        String principalIdStr = request.queryParams("principalid");
        String eventIdStr = request.queryParams("eventid");
        if (principalIdStr == null || eventIdStr == null) {
            errorMap.put("add", "Mangler identifikatorer.");
        }
        Long principalId = null;
        Long eventId = null;
        try {
            principalId = Long.parseLong(principalIdStr);
            eventId = Long.parseLong(eventIdStr);
        } catch(NumberFormatException e) {
            errorMap.put("add", "Tøysekopp, du må velge en person og et arrangement i listen ovenfor for å registrere en påmelding.");
            return;
        }

        if (!userService.isOfAge(user.getPrincipal())) {
            errorMap.put("add", "Du har ikke rettigheter til å foreta denne påmeldingen.");
            return;
        }

        Principal principal = userService.getPrincipalById(principalId);
        Event event = clubService.getEventById(eventId);

        if (!userService.isFamilyMember(user.getPrincipal().getFamily(), principal)) {
            errorMap.put("add", "Du kan ikke melde på noen som ikke er i din familie.");
            return;
        }
        if (event.requireMembership() && !clubService.isEnrolledAsClubMember(user.getPrincipal().getFamily())) {
            errorMap.put("add", "Dette arrangementet krever at man er medlem av idrettslaget. Gå til medlemskapssiden og meld deg inn først.");
        }
        if (event.isClosed()) {
            errorMap.put("add", "Påmeldingen kan ikke gjennomføres fordi arrangementet er lukket av administrator.");
            return;
        }
        if (!clubService.isActiveEvent(event)) {
            errorMap.put("add", "Du kan ikke melde deg på et avsluttet arrangement.");
            return;
        }

        int ageThatYear = userService.getAgeThatYearOnTime(principal, event.getStartTime());

        boolean failed = false;
        StringBuilder str = new StringBuilder();
        str.append("Påmeldingen kan ikke gjennomføres fordi arrangementet krever at deltagerens alder må være ");
        if (event.getMinimumAge() != null && event.getMaximumAge() != null
                && (ageThatYear < event.getMinimumAge() || ageThatYear > event.getMaximumAge())) {
            str.append("fra ").append(event.getMinimumAge()).append(" t.o.m. ").append(event.getMaximumAge()).append(" år.");
            failed = true;
        } else if (event.getMinimumAge() == null && event.getMaximumAge() != null
                && ageThatYear > event.getMaximumAge()) {
            str.append(event.getMaximumAge()).append(" år eller yngre,");
            failed = true;
        } else if (event.getMinimumAge() != null && event.getMaximumAge() == null
                && ageThatYear < event.getMinimumAge()) {
            str.append(event.getMinimumAge()).append(" år eller eldre.");
            failed = true;
        }
        if (failed) {
            errorMap.put("add", str.toString());
            return;
        }

        EventParticipation eventParticipation = clubService.getLastEnrollmentToEventForPrincipal(event, principal);
        if (eventParticipation != null && eventParticipation.isEnrolled()) {
            errorMap.put("add", "Personen er allerede påmeldt dette arrangementet.");
            return;
        }

        ServiceResponse response = clubService.createEventParticipation(principal, event);
        if (response.isSuccess()) {
            if (clubService.hasEventStarted(event)) {
                infoList.add("Vær oppmerksom på at arrangementet har staret.");
            }
            infoList.add(response.getMessage());
        } else {
            errorMap.put("add", response.getMessage());
        }
    }

    private void addPrincipalToTeam(User user, Request request, Map<String, String> errorMap, List<String> infoList) {

        // TODO: Share to FACEBOOK!!

        String principalIdStr = request.queryParams("principalid");
        String teamIdStr = request.queryParams("teamid");
        if (principalIdStr == null || teamIdStr == null) {
            errorMap.put("add", "Mangler identifikatorer.");
        }
        Long principalId = null;
        Long teamId = null;
        try {
            principalId = Long.parseLong(principalIdStr);
            teamId = Long.parseLong(teamIdStr);
        } catch(NumberFormatException e) {
            errorMap.put("add", "Tøysekopp, du må velge en person og en aktivitet i listen ovenfor for å registrere en påmelding.");
            return;
        }

        if (!userService.isOfAge(user.getPrincipal())) {
            errorMap.put("add", "Du har ikke rettigheter til å foreta denne påmeldingen.");
            return;
        }

        Principal principal = userService.getPrincipalById(principalId);
        Team team = clubService.getTeamById(teamId);
        if (!userService.isFamilyMember(user.getPrincipal().getFamily(), principal)) {
            errorMap.put("add", "Du kan ikke melde på noen som ikke er i din familie.");
            return;
        }
        if (team.isClosed()) {
            errorMap.put("add", "Påmeldingen kan ikke opprettes fordi aktiviteten er lukket av administrator.");
            return;
        }
        ServiceResponse response = clubService.createTeamMembership(principal, team);
        if (response.isSuccess()) {
            infoList.add(response.getMessage());
        } else {
            errorMap.put("add", response.getMessage());
        }
    }

    private void removePrincipalFromTeam(User user, Request request, Map<String, String> errorMap, List<String> infoList) {
        String teamMembershipIdStr = request.queryParams("teammembershipid");
        if (teamMembershipIdStr == null) {
            errorMap.put("remove", "Ingen påmeldingsidentifikator oppgitt");
            return;
        }
        Long teamMembershipId = null;
        try {
            teamMembershipId = Long.parseLong(teamMembershipIdStr);
        } catch(NumberFormatException e) {
            errorMap.put("remove", "Påmeldingsindikatoren er ikke gyldig.");
            return;
        }
        TeamMembership teamMembership = clubService.getTeamMembershipById(teamMembershipId);
        if (teamMembership == null) {
            errorMap.put("remove", "Ingen påmelding funnet.");
            return;
        }
        if (!userService.isOfAge(user.getPrincipal())) {
            errorMap.put("remove", "Du har ikke rettigheter til å slette denne påmeldingen.");
            return;
        }
        if (!userService.isFamilyMember(user.getPrincipal().getFamily(), teamMembership.getPrincipal())) {
            errorMap.put("remove", "Du kan ikke slette påmeldingen til noen som ikke er i din familie.");
            return;
        }
        if (teamMembership.getTeam().isLocked()) {
            errorMap.put("remove", "Du kan ikke slette denne påmeldingen fordi den er låst av administrator.");
            return;
        }
        ServiceResponse response = clubService.deleteTeamMembership(teamMembership);
        if (response.isSuccess()) {
            infoList.add(response.getMessage());
        } else {
            errorMap.put("remove", response.getMessage());
        }
    }

    private void removePrincipalFromEvent(User user, Request request, Map<String, String> errorMap, List<String> infoList) {
        String eventParticipationIdStr = request.queryParams("eventparticipationid");
        if (eventParticipationIdStr == null) {
            errorMap.put("remove", "Ingen påmeldingsindikator oppgitt");
            return;
        }
        Long eventParticipationId = null;
        try {
            eventParticipationId = Long.parseLong(eventParticipationIdStr);
        } catch(NumberFormatException e) {
            errorMap.put("remove", "Påmeldingsindikatoren er ikke gyldig.");
            return;
        }
        EventParticipation eventParticipation = clubService.getEventParticipationById(eventParticipationId);
        if (eventParticipation == null) {
            errorMap.put("remove", "Ingen påmelding funnet.");
            return;
        }
        if (!userService.isOfAge(user.getPrincipal())) {
            errorMap.put("remove", "Du har ikke rettigheter til å slette denne påmeldingen.");
            return;
        }
        if (!userService.isFamilyMember(user.getPrincipal().getFamily(), eventParticipation.getPrincipal())) {
            errorMap.put("remove", "Du kan ikke slette påmeldingen til noen som ikke er i din familie.");
            return;
        }
        if (eventParticipation.getEvent().isLocked()) {
            errorMap.put("remove", "Du kan ikke slette denne påmeldingen fordi den er låst av administrator.");
            return;
        }
        if (clubService.hasEventStarted(eventParticipation.getEvent())) {
            errorMap.put("remove", "Du kan ikke melde deg av et arrangement som er har startet.");
            return;
        }
        if (!clubService.isActiveEvent(eventParticipation.getEvent())) {
            errorMap.put("remove", "Du kan ikke melde deg av et arrangement som er ferdig.");
            return;
        }
        ServiceResponse response = clubService.deleteEventParticipation(eventParticipation);
        if (response.isSuccess()) {
            infoList.add(response.getMessage());
        } else {
            errorMap.put("remove", response.getMessage());
        }
    }

    private void makePrimaryContact(User user, Request request, Map<String, String> errorMap, List<String> infoList) {
        Principal principal = getPrincipalFromUsersFamilyByIdStr(user, request.queryParams("principalid"));
        if (principal == null) {
            errorMap.put("save", "Personen er ukjent.");
            return;
        }
        ServiceResponse response = userService.setPrimaryPrincipal(principal, user.getPrincipal().getFamily());
        if (response.isSuccess()) {
            infoList.add(response.getMessage());
        } else {
            errorMap.put("save", response.getMessage());
        }
    }

    private void inviteFamilyMember(User user, Request request, Map<String, String> errorMap, List<String> infoList) {
        String email = request.queryParams("email");
        Principal principal = userService.getPrincipalByEmail(email.trim());
        if (principal == null) {
            errorMap.put("email", "Vi har ingen brukere som er registrert med den e-postadressen.");
            return;
        }
        ServiceResponse response = userService.inviteFamilyMember(principal, user.getPrincipal().getFamily());
        if (response.isSuccess()) {
            infoList.add(response.getMessage());
        } else {
            errorMap.put("save", response.getMessage());
        }
    }

    private void deletePrincipal(User user, Request request, Map<String, String> errorMap, List<String> infoList) {
        Principal principal = getPrincipalFromUsersFamilyByIdStr(user, request.queryParams("principalid"));
        if (principal == null) {
            errorMap.put("save", "Personen er ukjent.");
            return;
        }
        if (clubService.isDeletable(principal)) {
            ServiceResponse response = userService.destroyPrincipal(principal);
            if (response.isSuccess()) {
                infoList.add(response.getMessage());
            } else {
                errorMap.put("save", response.getMessage());
            }
        } else {
            errorMap.put("save", "Personen kan ikke slettes på nåværende tidspunkt.");
        }
    }

    private Principal getPrincipalFromUsersFamilyByIdStr(User user, String principalIdStr) {
        if (principalIdStr == null) {
            return null;
        }
        Long principalId = null;
        try {
            principalId = Long.parseLong(principalIdStr);
        } catch(NumberFormatException e) {
            return null;
        }
        Family family = user.getPrincipal().getFamily();
        List<Principal> list = family.getMembers();
        for (Principal principal : list) {
            if (principal.getId().longValue() == principalId.longValue()) {
                return principal;
            }
        }
        return null;
    }

    private void updateClubMembership(User user, Request request, Map<String, String> errorMap, List<String> infoList, boolean wantToEnroll) {
        if (!userService.isOfAge(user.getPrincipal())) {
            errorMap.put("save", "Du har ikke rettigheter til å foreta denne endringen.");
            return;
        }
        Boolean confirm = Boolean.parseBoolean(request.queryParams("confirm"));
        if (!confirm) {
            errorMap.put("save", "Du må bekrefte handlingen ved å hake av boksen.");
            return;
        }
        ServiceResponse response = clubService.updateClubMembership(user, wantToEnroll);
        if (response.isSuccess()) {
            infoList.add(response.getMessage());
        } else {
            errorMap.put("save", response.getMessage());
        }
    }

    private void addFamilyMember(User user, Request request, Map<String, String> errorMap, List<String> infoList) {
        String name = request.queryParams("name");
        String birthDateStr = request.queryParams("birthdate");
        String gender = request.queryParams("gender");

        if (name == null || name.isEmpty()) {
            errorMap.put("name", "Du må oppgi navn på familiemedlemmet.");
        }
        if (name.split(" ").length <=1 ) {
            errorMap.put("name", "Du må oppgi både fornavn og etternavn.");
        }

        if (birthDateStr == null || birthDateStr.isEmpty()) {
            errorMap.put("birthdate", "Vi trenger fødselsdatoen for å kunne beregne riktig treningsavgift.");
        }
        Date birthDate = null;
        try {
            birthDate = new SimpleDateFormat("dd.MM.yyyy").parse(birthDateStr);
        } catch (ParseException e) {
            errorMap.put("birthdate", "Vi trenger fødselsdatoen for å kunne beregne riktig treningsavgift.");
        }

        if (gender == null || gender.isEmpty()) {
            errorMap.put("gender", "Vi trenger å vite kjønn for å vise de mest relevante treningstilbudene først.");
        }

        if (errorMap.isEmpty()) {
            popuplateResponse(errorMap, infoList, userService.addFamilyMember(user, name, birthDate, gender));
        }


    }

    private void saveMobile(User user, Request request, Map<String, String> errorMap, List<String> infoList) {
        String mobile = request.queryParams("mobile");
        if (mobile == null || mobile.isEmpty()) {
            String newMobile = mobile.replace("+", "");
            try { Long.parseLong(newMobile); }  catch (NumberFormatException e) {
                errorMap.put("mobile", "Mobilnummeret kan kun bestå av tall.");
            }
            if (newMobile.length()<8) {
                errorMap.put("mobile", "Mobilnummeret må bestå av minst 8 siffer.");
            }
        }

        if (errorMap.isEmpty()) {
            popuplateResponse(errorMap, infoList, userService.updateMobile(user, mobile));
        }
    }

    private void saveEmail(User user, Request request, Map<String, String> errorMap, List<String> infoList) {
        String email = request.queryParams("email");
        if (email == null || email.isEmpty()) {
            errorMap.put("email", "Du må oppgi en e-postadresse.");
        } else if (!email.contains("@")) {
            errorMap.put("email", "E-postadressen må inneholde en krøllalfa.");
        }

        if (errorMap.isEmpty()) {
            popuplateResponse(errorMap, infoList, userService.updateEmail(user, email));
        }
    }

    private void saveAddress(User user, Request request, Map<String, String> errorMap, List<String> infoList) {
        String address1 = request.queryParams("address1");
        String city = request.queryParams("city");
        String zip = request.queryParams("zip");
        String country = request.queryParams("country");

        if (address1 == null || address1.isEmpty()) {
            // We allow empty street addresses.
        }
        if (zip == null || zip.isEmpty()) {
            errorMap.put("zip", "Postnummer må fylles ut.");
        }
        if (city == null || city.isEmpty()) {
            errorMap.put("city", "Poststed må fylles ut.");
        }
        if (country == null || country.isEmpty()) {
            country = "Norge";
        }

        if (errorMap.isEmpty()) {
            popuplateResponse(errorMap, infoList, userService.updateAddress(user, address1, "", zip, city, country));
        }
    }

    private void saveProfile(User user, Request request, Map<String, String> errorMap, List<String> infoList) {
        String name = request.queryParams("name");
        String birthDateStr = request.queryParams("birthdate");
        String gender = request.queryParams("gender");

        if (name == null || name.isEmpty()) {
            errorMap.put("name", "Du må oppgi navnet ditt. Vi trenger det for å holde medlemsregistret oppdatert.");
        }
        if (name.split(" ").length <=1 ) {
            errorMap.put("name", "Du må oppgi både fornavn og etternavn.");
        }

        if (birthDateStr == null || birthDateStr.isEmpty()) {
            errorMap.put("birthdate", "Vi trenger fødselsdatoen din for å kunne beregne riktig treningsavgift.");
        }
        Date birthDate = null;
        try {
            birthDate = new SimpleDateFormat("yyyy-MM-dd").parse(birthDateStr);
        } catch (ParseException e) {
            errorMap.put("birthdate", "Vi trenger fødselsdatoen din for å kunne beregne riktig treningsavgift.");
        }

        if (gender == null || gender.isEmpty()) {
            errorMap.put("gender", "Vi trenger å vite ditt kjønn for å vise de mest relevante treningstilbudene først.");
        }

        if (errorMap.isEmpty()) {
            popuplateResponse(errorMap, infoList, userService.updateProfile(user, name, birthDate, gender));
        }
    }

    private void popuplateResponse(Map<String, String> errorMap, List<String> infoList, ServiceResponse response) {
        if (response.isSuccess()) {
            infoList.add(response.getMessage());
        } else {
            errorMap.put("save", response.getMessage());
        }
    }

}
