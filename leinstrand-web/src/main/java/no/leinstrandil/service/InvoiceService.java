package no.leinstrandil.service;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.persistence.NoResultException;
import javax.persistence.Query;
import javax.persistence.TypedQuery;
import no.leinstrandil.database.Storage;
import no.leinstrandil.database.model.accounting.Invoice;
import no.leinstrandil.database.model.accounting.Invoice.Status;
import no.leinstrandil.database.model.accounting.InvoiceLine;
import no.leinstrandil.database.model.club.ClubMembership;
import no.leinstrandil.database.model.club.Event;
import no.leinstrandil.database.model.club.EventParticipation;
import no.leinstrandil.database.model.club.Team;
import no.leinstrandil.database.model.club.TeamMembership;
import no.leinstrandil.database.model.person.Family;
import no.leinstrandil.database.model.person.Principal;
import no.leinstrandil.product.Product;
import no.leinstrandil.product.ProductCode;
import no.leinstrandil.product.ProductResolver;
import no.leinstrandil.product.ProductType;
import org.joda.time.DateTime;
import org.joda.time.DateTimeFieldType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class InvoiceService {
    private static final Logger log = LoggerFactory.getLogger(InvoiceService.class);

    private SendRegningService sendRegningService;
    private UserService userService;
    private ClubService clubService;
    private Storage storage;

    public InvoiceService(Storage storage, UserService userService, ClubService clubService,
            SendRegningService sendRegningService) {
        this.storage = storage;
        this.userService = userService;
        this.clubService = clubService;
        this.sendRegningService = sendRegningService;
    }

    public ServiceResponse createInvoiceForTeam(Team team, int year) {
        Map<Principal, TeamMembership> memberships = clubService.getTeamMembershipsForTeam(team);
        for (Principal principal : memberships.keySet()) {
            TeamMembership membership = memberships.get(principal);
            if (!membership.isEnrolled()) {
                continue;
            }
            if (isPrincipalAlreadyInvoicedTeamParticipation(principal, team, year)) {
                continue;
            }
            int feeCount = getTeamFeeInvoicedCountForPrincipal(principal, year);
            Family family = principal.getFamily();
            Invoice invoice = ensureOpenInvoiceForFamily(family);
            InvoiceLine line = new InvoiceLine();
            Product product = ProductResolver.getTeamMembershipProduct(principal, team, feeCount, year);
            line.setCreated(new Date());
            line.setTeamMembership(membership);
            membership.getInvoiceLines().add(line);
            line.setUnitPrice(product.getUnitPrice());
            line.setDescription(product.getDescription());
            line.setProductCode(product.getProductCode());
            line.setDiscountInPercent(product.getDiscountInPercent());
            line.setPrincipal(principal);
            principal.getInvoiceLines().add(line);
            line.setValidYear(year);
            line.setTaxPercent(0);
            line.setQuantity(1);
            line.setInvoice(invoice);
            invoice.getInvoiceLines().add(line);

            storage.begin();
            try {
                storage.persist(line);
                storage.commit();
            } catch (RuntimeException e) {
                storage.rollback();
                log.warn("Error during invoicing of team membership", e);
                return new ServiceResponse(false, "Det oppstod en feil under fakturering.");
            }
        }
        return new ServiceResponse(true, "Aktivitetsavgift for " + team.getName() + " fakturert.");
    }

    private int getTeamFeeInvoicedCountForPrincipal(Principal principal, int year) {
        storage.refresh(principal);
        int count = 0;
        List<InvoiceLine> invoiceLineList = principal.getInvoiceLines();
        for (InvoiceLine invoiceLine : invoiceLineList) {
            if (invoiceLine.getInvoice().getStatus() == Status.CREDITED) {
                continue;
            }
            if (invoiceLine.getValidYear() == null) {
                continue;
            }
            if (invoiceLine.getValidYear() != year) {
                continue;
            }
            if (ProductCode.isCodeBelongingToProductOfType(
                    invoiceLine.getProductCode(),
                    ProductType.TEAM_FEE)) {
                count++;
            }
        }
        return count;
    }

    private boolean isPrincipalAlreadyInvoicedTeamParticipation(Principal principal, Team team, int year) {
        List<InvoiceLine> invoiceLineList = principal.getInvoiceLines();
        for (InvoiceLine invoiceLine : invoiceLineList) {
            if (invoiceLine.getInvoice().getStatus() == Status.CREDITED) {
                continue;
            }
            if (invoiceLine.getValidYear() == null) {
                continue;
            }
            if (invoiceLine.getValidYear() != year) {
                continue;
            }
            if (!ProductCode.isCodeBelongingToProductOfType(
                    invoiceLine.getProductCode(),
                    ProductType.TEAM_FEE)) {
                continue;
            }
            if (!principal.getId().equals(invoiceLine.getTeamMembership().getPrincipal().getId())) {
                continue;
            }
            if (!team.getId().equals(invoiceLine.getTeamMembership().getTeam().getId())) {
                continue;
            }
            return true;
        }
        return false;
    }

    public ServiceResponse createClubMembershipInvoice(int year) {
        List<Family> familyList = userService.getAllFamilies();
        for (Family family : familyList) {
            ServiceResponse response = createClubMembershipInvoiceForFamily(family, year);
            if (!response.isSuccess()) {
                return response;
            }
        }
        return new ServiceResponse(true, "Fakturaer for klubbavgift ble opprettet uten feil.");
    }

    public ServiceResponse createClubMembershipInvoiceForFamily(Family family, int year) {
        if (!ClubService.isEnrolledAsClubMember(family)) {
            new ServiceResponse(true, "Ingen faktura opprettet da familien ikke er klubbmedlemmer.");
        }
        if (family.isNoCombinedMembership() != null && family.isNoCombinedMembership()) {
            return createNoCombinedClubMembershipInvoiceForFamily(family, year);
        } else {
            return createCombinedClubMembershipInvoiceForFamily(family, year);
        }
    }

    private ServiceResponse createCombinedClubMembershipInvoiceForFamily(Family family, int year) {
        storage.refresh(family);
        if (!ClubService.isEnrolledAsClubMember(family)) {
            return new ServiceResponse(true, "Ingenting å fakturere siden familien ikke er medlem.");
        }
        if (family.getMembers().isEmpty()) {
            return new ServiceResponse(true, "Ingen faktura å lage siden familien ikke har noen familiemedlemmer.");
        }
        if (isAlreadyInvoicedFamilyClubMembership(family, year)) {
            return new ServiceResponse(true, "Klubbmedlemskap allerede fakturert for familie med primærkontakt " + family.getPrimaryPrincipal().getName());
        }

        Invoice invoice = ensureOpenInvoiceForFamily(family);
        InvoiceLine line = new InvoiceLine();
        Product product;
        if (family.getMembers().size() == 1) {
            Principal principal = family.getPrimaryPrincipal();
            product = ProductResolver.getPrincipalClubMembershipProductByAge(principal, year);
            line.setPrincipal(principal);
            principal.getInvoiceLines().add(line);
        } else {
            product = ProductResolver.getFamilyClubMembershipProduct(year);
            line.setFamily(family);
            family.getInvoiceLines().add(line);
        }
        ClubMembership membership = clubService.getClubMembership(family);
        line.setClubMembership(membership);
        membership.getInvoiceLines().add(line);
        line.setCreated(new Date());
        line.setUnitPrice(product.getUnitPrice());
        line.setDescription(product.getDescription());
        line.setProductCode(product.getProductCode());
        line.setDiscountInPercent(product.getDiscountInPercent());
        line.setValidYear(year);
        line.setTaxPercent(0);
        line.setQuantity(1);
        line.setInvoice(invoice);
        invoice.getInvoiceLines().add(line);

        storage.begin();
        try {
            storage.persist(line);
            storage.commit();
        } catch (RuntimeException e) {
            storage.rollback();
            return new ServiceResponse(false, "Kunne ikke opprette faktura for klubbmedlemskap for familie med primærkontakt " + family.getPrimaryPrincipal().getName());
        }
        log.info("ClubMembership invoice created for family with primary principal " + family.getPrimaryPrincipal().getName());
        return new ServiceResponse(true, "Klubbmedlemskap fakturert for familie med primærkontakt " + family.getPrimaryPrincipal().getName());
    }

    private ServiceResponse createNoCombinedClubMembershipInvoiceForFamily(Family family, int year) {
        storage.refresh(family);
        List<InvoiceLine> invoiceLinesToBeAdded = new ArrayList<>();
        List<Principal> principalList = family.getMembers();
        for (Principal principal : principalList) {
            if (clubService.getActiveTeamMembershipCountForPrincipal(principal) == 0) {
                continue;
            }
            if (isAlreadyInvoicedFamilyClubMembership(family, year)) {
                continue;
            }
            if (isAlreadyInvoicedPrincipalClubMembership(principal, year)) {
                continue;
            }
            Product product = ProductResolver.getPrincipalClubMembershipProductByAge(principal, year);
            InvoiceLine line = new InvoiceLine();
            ClubMembership membership = clubService.getClubMembership(family);
            line.setClubMembership(membership);
            membership.getInvoiceLines().add(line);
            line.setCreated(new Date());
            line.setPrincipal(principal);
            principal.getInvoiceLines().add(line);
            line.setUnitPrice(product.getUnitPrice());
            line.setDescription(product.getDescription());
            line.setProductCode(product.getProductCode());
            line.setDiscountInPercent(product.getDiscountInPercent());
            line.setValidYear(year);
            line.setTaxPercent(0);
            line.setQuantity(1);
            invoiceLinesToBeAdded.add(line);
        }
        if (invoiceLinesToBeAdded.isEmpty()) {
            return new ServiceResponse(true, "Klubbmedlemskap allerede fakturert for familie med primærkontakt " + family.getPrimaryPrincipal().getName());
        }
        Invoice invoice = ensureOpenInvoiceForFamily(family);
        storage.begin();
        try {
            for(InvoiceLine line : invoiceLinesToBeAdded) {
                line.setInvoice(invoice);
                invoice.getInvoiceLines().add(line);
                storage.persist(line);
            }
            storage.commit();
        } catch (RuntimeException e) {
            storage.rollback();
            return new ServiceResponse(false, "Kunne ikke opprette faktura for klubbmedlemskap for kun aktive medlemmer for familie med primærkontakt " + family.getPrimaryPrincipal().getName());
        }
        log.info("ClubMembership invoice with no combined memberships created for family with primary principal " + family.getPrimaryPrincipal().getName());
        return new ServiceResponse(true, "Klubbmedlemskap for kun aktive medlemmer laget for familie med primærkontakt " + family.getPrimaryPrincipal().getName());
    }

    private boolean isAlreadyInvoicedPrincipalClubMembership(Principal principal, int year) {
        List<InvoiceLine> invoiceLineList = principal.getInvoiceLines();
        for (InvoiceLine invoiceLine : invoiceLineList) {
            if (invoiceLine.getInvoice().getStatus() == Status.CREDITED) {
                continue;
            }
            if (invoiceLine.getValidYear() != year) {
                continue;
            }
            if (ProductCode.isCodeBelongingToProductOfType(
                    invoiceLine.getProductCode(),
                    ProductType.CLUB_MEMBERSHIP)) {
                return true;
            }
        }
        return false;
    }

    private boolean isAlreadyInvoicedFamilyClubMembership(Family family, int year) {
        List<Invoice> invoiceList = family.getInvoices();
        for (Invoice invoice : invoiceList) {
            if (invoice.getStatus() == Status.CREDITED) {
                continue;
            }
            List<InvoiceLine> lineList = invoice.getInvoiceLines();
            for (InvoiceLine invoiceLine : lineList) {
                if (invoiceLine.getValidYear() == null) {
                    continue;
                }
                if (invoiceLine.getValidYear() != year) {
                    continue;
                }
                if (ProductCode.isCodeBelongingToProductOfType(
                        invoiceLine.getProductCode(),
                        ProductType.CLUB_MEMBERSHIP)) {
                    return true;
                }
            }
        }
        return false;
    }

/*  private ServiceResponse createCombinedClubMembershipInvoiceForFamily(Family family, int year) {
        // Check if Family has been invoiced the club membership this year
        if (!clubService.isEnrolledAsClubMember(family)) {
            if (family.getPrimaryPrincipal() != null) {
                log.info("Family with primary principal " + family.getPrimaryPrincipal().getName()
                        + " is not a club member.");
                return new ServiceResponse(true, "Familie med primærkontakt " + family.getPrimaryPrincipal().getName() + " er ikke klubbmedlem.");
            } else {
                log.warn("Family with id " + family.getId() + " do not have a primary principal.");
                return new ServiceResponse(false, "Familie med id " + family.getId() + " har ingen primærkontakt!");
            }
        }

        ClubMembership clubMembership = family.getClubMemberships().get(0);
        Product product = ProductResolver.getClubMembershipProduct(clubMembership, getCurrentYear());
        if (product == null) {
            log.info("Family with id " + family.getId() + " has already been invoiced club membership this year.");
            return new ServiceResponse(true, "Familien har allerede blitt fakturert klubbmedlemskap på en annen faktura.");
        }

        Invoice openInvoice = ensureOpenInvoiceForFamily(family);
        if (openInvoice == null) {
            return new ServiceResponse(false, "Kunne ikke opprette ny faktura for familie med id " + family.getId());
        }

        InvoiceLine invoiceLine = new InvoiceLine();
        invoiceLine.setClubMembership(clubMembership);
        invoiceLine.setFamily(family);
        invoiceLine.setCreated(new Date());
        invoiceLine.setDescription(product.getDescription());
        invoiceLine.setUnitPrice(product.getUnitPrice());
        invoiceLine.setProductCode(product.getProductCode());
        invoiceLine.setDiscountInPercent(product.getDiscountInPercent());
        invoiceLine.setQuantity(1);
        invoiceLine.setTaxPercent(0);
        invoiceLine.setInvoice(openInvoice);
        openInvoice.getInvoiceLines().add(invoiceLine);
        storage.begin();
        try {
            storage.persist(invoiceLine);
            storage.commit();
            log.info("Fakturalinje for klubbmedlemskap opprettet for familie med primærkontakt " + family.getPrimaryPrincipal().getName());
        } catch (RuntimeException e) {
            storage.rollback();
            return new ServiceResponse(false, "Kunne ikke opprette fakturalinje for familie med id " + family.getId());
        }

        return new ServiceResponse(true, "Faktura for klubbmedlemskap er opprettet for familie med primærkontakt " + family.getPrimaryPrincipal().getName());
    } */

    private Invoice ensureOpenInvoiceForFamily(Family family) {
        List<Invoice> invoiceList = family.getInvoices();
        for (Invoice invoice : invoiceList) {
            if (invoice.getStatus() == Status.OPEN) {
                log.info("Found OPEN invoice for family with primary contact " + family.getPrimaryPrincipal().getName());
                return invoice;
            }
        }
        Invoice invoice = new Invoice();
        invoice.setCreated(new Date());
        invoice.setFamily(family);
        family.getInvoices().add(invoice);
        invoice.setStatus(Status.OPEN);
        storage.begin();
        try {
            storage.persist(invoice);
            storage.commit();
            log.info("Created new invoice for family with primary contact " + family.getPrimaryPrincipal().getName());
            return invoice;
        } catch (RuntimeException e) {
            storage.rollback();
            log.error("Could not create invoice for family with primary contact " + family.getPrimaryPrincipal().getName(), e);
        }
        return null;
    }

    public static boolean isThisYear(Date date) {
        int yearToCheck = new DateTime(date).get(DateTimeFieldType.year());
        return getCurrentYear() == yearToCheck;
    }

    public static int getCurrentYear() {
        return new DateTime().get(DateTimeFieldType.year());
    }

    public Map<String, Long> getInvoiceCountPerStatusReport() {
        Map<String, Long> report = new HashMap<>();
        Query query = storage.createQuery("SELECT status, COUNT(id) FROM Invoice GROUP BY status");
        @SuppressWarnings("rawtypes")
        List list = query.getResultList();
        for (Object object : list) {
            Object[] reportLine = (Object[]) object;
            report.put(((Invoice.Status) reportLine[0]).name(), (Long) reportLine[1]);
        }
        return report;
    }

    public List<Invoice> getInvoicesWithStatus(Status status) {
        TypedQuery<Invoice> query = storage.createQuery("from Invoice i where status=" + status.ordinal() + " order by i.family.primaryPrincipal.lastName", Invoice.class);
        return query.getResultList();
    }

    public String formatDate(Date date) {
        if (date == null) {
            return new String();
        }
        return new SimpleDateFormat("dd.MM.yyyy").format(date);
    }

    public static boolean isStatusDeletable(Invoice.Status status) {
        return status == Invoice.Status.OPEN;
    }

    public static boolean isStatusSendable(Invoice.Status status) {
        return status == Status.OPEN;
    }

    public ServiceResponse deleteInvoice(long invoiceId) {
        Invoice invoice = getInvoiceById(invoiceId);
        if (invoice == null) {
            return new ServiceResponse(false, "Finner ingen faktura med id " + invoiceId);
        }
        if (!isStatusDeletable(invoice.getStatus())) {
            return new ServiceResponse(false, "Faktura med id " + invoiceId + " er ikke i en slettbar tilstand.");
        }
        storage.begin();
        try {
            storage.delete(invoice);
            storage.commit();
            return new ServiceResponse(true, "Faktura med id " + invoiceId + " ble slettet.");
        } catch (RuntimeException e) {
            storage.rollback();
            return new ServiceResponse(false, "Det oppstod en feil under sletting av faktura med id " + invoiceId);
        }
    }

    public Invoice getInvoiceById(long invoiceId) {
        try {
            return storage.createSingleQuery("from Invoice where id=" + invoiceId, Invoice.class);
        } catch (NoResultException e) {
            return null;
        }
    }

    public static int getInvoiceDueAmount(Invoice invoice) {
        int due = 0;
        List<InvoiceLine> invoiceLines = invoice.getInvoiceLines();
        for (InvoiceLine invoiceLine : invoiceLines) {
            due += getInvoiceLineSum(invoiceLine);
        }
        return due;
    }

    public static int getInvoiceLineSum(InvoiceLine invoiceLine) {
        return Math.round(((invoiceLine.getUnitPrice() * invoiceLine.getQuantity()) * (100.0f - invoiceLine.getDiscountInPercent())) / 100.0f);
    }

    public ServiceResponse deleteAllInvoicesWithStatus(Status status) {
        if (!isStatusDeletable(status)) {
            return new ServiceResponse(false, "Fakturaer med status " + status + " kan ikke slettes.");
        }
        List<Invoice> list = getInvoicesWithStatus(status);
        storage.begin();
        try {
            for (Invoice invoice : list) {
                storage.delete(invoice);
            }
            storage.commit();
            return new ServiceResponse(true, "Alle fakturaer med status " + status + " ble slettet.");
        }catch (RuntimeException e) {
            storage.rollback();
            return new ServiceResponse(false, "Det oppstod en fail under sletting av alle fakturaer med status " + status + ".");
        }
    }

    public ServiceResponse sendInvoice(long invoiceId) {
        Invoice invoice = getInvoiceById(invoiceId);
        if (invoice == null) {
            return new ServiceResponse(false, "Finner ingen faktura med id " + invoiceId);
        }
        if (!isStatusSendable(invoice.getStatus())) {
            return new ServiceResponse(false, "Faktura med id " + invoiceId + " er ikke i en sendbar tilstand.");
        }
        List<Invoice> invoiceList = new ArrayList<>();
        invoiceList.add(invoice);
        return sendRegningService.sendInvoices(invoiceList);
    }

    public ServiceResponse sendAllInvoicesWithStatus(Status status) {
        if (!isStatusSendable(status)) {
            return new ServiceResponse(false, "Fakturaer med status " + status + " kan ikke sendes.");
        }
        if (true) {
            throw new RuntimeException("Not yet implemented!");
        }
        return sendRegningService.sendInvoices(getInvoicesWithStatus(status));
    }

    public int getInvoiceCountForFamilyWithStatus(Family family, Status status, int goBackMonths) {
        return getInvoicesForFamilyWithStatus(family, status, goBackMonths).size();
    }

    public List<Invoice> getInvoicesForFamilyWithStatus(Family family, Status status, int goBackMonths) {
        List<Invoice> resultList = new ArrayList<>();
        List<Invoice> invoiceList = family.getInvoices();
        for (Invoice invoice : invoiceList) {
            if (invoice.getStatus() == status) {
                Date date = invoice.getExternalInvoiceDate();
                if (date == null) {
                    date = invoice.getCreated();
                }
                DateTime dateTime = new DateTime(date);
                if (dateTime.isAfter(DateTime.now().minusMonths(goBackMonths))) {
                    resultList.add(invoice);
                }
            }
        }
        return resultList;
    }

    public boolean isPrincipalAlreadyInvoicedEventParticipation(Principal principal, Event event) {
        List<InvoiceLine> invoiceLineList = principal.getInvoiceLines();
        for (InvoiceLine invoiceLine : invoiceLineList) {
            if (invoiceLine.getInvoice().getStatus() == Status.CREDITED) {
                continue;
            }
            if (!ProductCode.isCodeBelongingToProductOfType(
                    invoiceLine.getProductCode(),
                    ProductType.EVENT_FEE)) {
                continue;
            }
            if (!principal.getId().equals(invoiceLine.getEventParticipation().getPrincipal().getId())) {
                continue;
            }
            if (!event.getId().equals(invoiceLine.getEventParticipation().getEvent().getId())) {
                continue;
            }
            return true;
        }
        return false;
    }

    public Status getPrincipalEventParticipationInvoiceStatus(Principal principal, Event event) {
        List<InvoiceLine> invoiceLineList = principal.getInvoiceLines();
        for (InvoiceLine invoiceLine : invoiceLineList) {
            if (!ProductCode.isCodeBelongingToProductOfType(
                    invoiceLine.getProductCode(),
                    ProductType.EVENT_FEE)) {
                continue;
            }
            if (!principal.getId().equals(invoiceLine.getEventParticipation().getPrincipal().getId())) {
                continue;
            }
            if (!event.getId().equals(invoiceLine.getEventParticipation().getEvent().getId())) {
                continue;
            }
            return invoiceLine.getInvoice().getStatus();
        }
        return null;
    }

    public ServiceResponse createInvoiceForEvent(Event event) {
        Map<Principal, EventParticipation> participations = clubService.getEventParticipationForEvent(event);
        for (Principal principal : participations.keySet()) {
            EventParticipation participation = participations.get(principal);
            if (!participation.isEnrolled()) {
                continue;
            }
            if (isPrincipalAlreadyInvoicedEventParticipation(principal, event)) {
                continue;
            }
            Family family = principal.getFamily();
            if (event.requireMembership()) {
                if (!ClubService.isEnrolledAsClubMember(family)) {
                    continue;
                }
            }
            Invoice invoice= ensureOpenInvoiceForFamily(family);
            InvoiceLine line = new InvoiceLine();
            Product product = ProductResolver.getEventParticipationProduct(principal, event);
            line.setCreated(new Date());
            line.setEventParticipation(participation);
            participation.getInvoiceLines().add(line);
            line.setUnitPrice(product.getUnitPrice());
            line.setDescription(product.getDescription());
            line.setProductCode(product.getProductCode());
            line.setDiscountInPercent(product.getDiscountInPercent());
            line.setPrincipal(principal);
            principal.getInvoiceLines().add(line);
            line.setTaxPercent(0);
            line.setQuantity(1);
            line.setInvoice(invoice);
            invoice.getInvoiceLines().add(line);

            storage.begin();
            try {
                storage.persist(line);
                storage.commit();
            } catch (RuntimeException e) {
                storage.rollback();
                log.warn("Error during invoicing of event participation", e);
                return new ServiceResponse(false, "Det oppstod en feil under fakturering.");
            }
        }
        return new ServiceResponse(true, "Deltageravgift for " + event.getName() + " fakturert.");
    }

    public void syncInvoiceStatus() {
        List<Invoice> invoiceList = getInvoicesWithStatus(Status.SENT);
        if (invoiceList.isEmpty()) {
            return;
        }
        sendRegningService.syncInvoiceStatus(invoiceList);
    }

}
