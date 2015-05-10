package no.leinstrandil.service;

import java.text.SimpleDateFormat;
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
import no.leinstrandil.database.model.club.Team;
import no.leinstrandil.database.model.person.Family;
import no.leinstrandil.product.Product;
import no.leinstrandil.product.ProductResolver;
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

    public ServiceResponse createInvoiceForTeam(Team team) {




        return null;






    }

    public ServiceResponse createClubMembershipInvoice() {
        List<Family> familyList = userService.getAllFamilies();
        for (Family family : familyList) {
            ServiceResponse response = createClubMembershipInvoiceForFamily(family);
            if (!response.isSuccess()) {
                return response;
            }
        }
        return new ServiceResponse(true, "Fakturaer for klubbavgift ble opprettet uten feil.");
    }

    private ServiceResponse createClubMembershipInvoiceForFamily(Family family) {
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

/*
        log.info("Considering family with primary principal " + family.getPrimaryPrincipal().getName()
                + " for club member invoicing.");
        List<ClubMembership> clubMembershipList = family.getClubMemberships();
        for (ClubMembership clubMembership : clubMembershipList) {
            if (clubMembership.isEnrolled()
                    && clubMembership.getInvoiceLine() != null
                    && isThisYear(clubMembership.getInvoiceLine().getCreated())) {
                log.info("Family with primary principal " + family.getPrimaryPrincipal().getName()
                        + " has already been inviced for the club membership on invoice dated "
                        + clubMembership.getInvoiceLine().getInvoice().getCreated());
                return new ServiceResponse(true, "Klubbmedlemskap er allerede fakturert dette år for familie med primærkontakt " + family.getPrimaryPrincipal().getName());
            }
        }
*/

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
    }

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

}
