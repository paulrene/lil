package no.leinstrandil.service;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import no.leinstrandil.Config;
import no.leinstrandil.database.Storage;
import no.leinstrandil.database.model.accounting.Invoice.Status;
import no.leinstrandil.database.model.person.Principal;
import no.sws.client.SwsBatchIdTooLongException;
import no.sws.client.SwsClient;
import no.sws.client.SwsMissingCreditedIdException;
import no.sws.client.SwsMissingRequiredElementInResponseException;
import no.sws.client.SwsNoInvoiceLinesForInvoiceException;
import no.sws.client.SwsNoRecipientForInvoiceException;
import no.sws.client.SwsNotValidRecipientException;
import no.sws.client.SwsParsingServerResponseException;
import no.sws.client.SwsRequiredBatchIdException;
import no.sws.client.SwsRequiredInvoiceValueException;
import no.sws.client.SwsResponseCodeException;
import no.sws.client.SwsTooManyInvoiceLinesException;
import no.sws.invoice.Invoice;
import no.sws.invoice.InvoiceFactory;
import no.sws.invoice.InvoiceStatus;
import no.sws.invoice.InvoiceStatusPayment;
import no.sws.invoice.line.InvoiceLine;
import no.sws.invoice.line.InvoiceLineFactory;
import no.sws.invoice.recipient.Recipient;
import no.sws.invoice.recipient.RecipientFactory;
import no.sws.invoice.shipment.Shipment;
import no.sws.invoice.shipment.ShipmentFactory;
import no.sws.invoice.shipment.ShipmentType;
import org.apache.commons.httpclient.HttpException;

public class SendRegningService {
    private static final String COPY_EMAIL_ADDRESS = "fakturakopi@leinstrandil.no";

    private Storage storage;
    private SwsClient swsClient;

    public SendRegningService(Storage storage, Config config) throws HttpException, IOException {
        this.storage = storage;
        swsClient = new SwsClient(config.getSrsUsername(), config.getSrsPassword());
        swsClient.setTest(false);
    }

    public ServiceResponse sendInvoices(List<no.leinstrandil.database.model.accounting.Invoice> lilInvoiceList)  {
        String batchId = UUID.randomUUID().toString();
        List<Invoice> srsInvoiceList = new ArrayList<Invoice>();
        Map<String, no.leinstrandil.database.model.accounting.Invoice> lilInvoiceMap = new HashMap<>();
        try {
            for(no.leinstrandil.database.model.accounting.Invoice lilInvoice : lilInvoiceList) {
                Invoice srsInvoice = createInvoice(lilInvoice);
                srsInvoiceList.add(srsInvoice);
                lilInvoiceMap.put(srsInvoice.getOurRef(), lilInvoice);
            }
            List<Invoice> resultInvoiceList = swsClient.sendInvoices(srsInvoiceList, batchId);
            storage.begin();
            try {
                for (Invoice srsInvoice : resultInvoiceList) {
                    System.out.println(srsInvoice);
                    String ourRef = srsInvoice.getOurRef();
                    no.leinstrandil.database.model.accounting.Invoice lilInvoice = lilInvoiceMap.get(ourRef);
                    if (lilInvoice == null) {
                        System.out.println("Null invoice for orderNo: " + ourRef);
                        continue;
                    }
                    String srsState = srsInvoice.getState();
                    if (srsState.equals("sent")) {
                        lilInvoice.setStatus(Status.SENT);
                        lilInvoice.setExternalBatchId(batchId);
                        lilInvoice.setExternalInvoiceDate(srsInvoice.getInvoiceDate());
                        lilInvoice.setExternalInvoiceDue(srsInvoice.getDueDate());
                        lilInvoice.setExternalInvoiceNumber(String.valueOf(srsInvoice.getInvoiceNo()));
                    } else {
                        lilInvoice.setStatus(Status.SEND_FAILED);
                    }
                    storage.persist(lilInvoice);
                }
                storage.commit();
            } catch (RuntimeException e) {
                storage.rollback();
            }
            return new ServiceResponse(true, "Sendregning.no - Fakturaene ble sendt!");
        } catch (SwsRequiredInvoiceValueException | SwsNoRecipientForInvoiceException
                | SwsNoInvoiceLinesForInvoiceException | SwsTooManyInvoiceLinesException | IOException
                | SwsParsingServerResponseException | SwsMissingRequiredElementInResponseException
                | SwsNotValidRecipientException | SwsMissingCreditedIdException | SwsResponseCodeException
                | SwsRequiredBatchIdException | SwsBatchIdTooLongException e) {
            e.printStackTrace();
            return new ServiceResponse(false, "Sendregning.no - sending av fakturaer feilet (" + e.getClass().getName() + "): " + e.getMessage());
        }
    }

    private Invoice createInvoice(no.leinstrandil.database.model.accounting.Invoice lilInvoice) throws SwsNotValidRecipientException {
        Invoice srsInvoice = InvoiceFactory.getInstance().getInvoice();
        Recipient recipient = createRecipient(lilInvoice.getFamily().getPrimaryPrincipal());
        srsInvoice.setRecipient(recipient);
        Shipment shipment = ShipmentFactory.getInstance(ShipmentType.email);
        shipment.addEmailAddress(recipient.getEmail());
        shipment.addCopyAddress(COPY_EMAIL_ADDRESS);
        srsInvoice.setShipment(shipment);
//        srsInvoice.setOurRef("IID_" + lilInvoice.getId());
        srsInvoice.setOrderNo("IID_" + lilInvoice.getId());
        srsInvoice.setInvoiceLines(createInvoiceLines(lilInvoice));
        return srsInvoice;
    }

    private List<InvoiceLine> createInvoiceLines(no.leinstrandil.database.model.accounting.Invoice lilInvoice) {
        List<InvoiceLine> lineList = new ArrayList<InvoiceLine>();
        for (no.leinstrandil.database.model.accounting.InvoiceLine lilInvoiceLine : lilInvoice.getInvoiceLines()) {
            lineList.add(createInvoiceLine(lilInvoiceLine));
        }
        return lineList;
    }

    private InvoiceLine createInvoiceLine(no.leinstrandil.database.model.accounting.InvoiceLine lilInvoiceLine) {
        InvoiceLine line = InvoiceLineFactory.getInstance();
        line.setQty(toBigDecimal(lilInvoiceLine.getQuantity()));
        line.setDesc(lilInvoiceLine.getDescription());
        line.setUnitPrice(toBigDecimal(lilInvoiceLine.getUnitPrice()));
        line.setTax(lilInvoiceLine.getTaxPercent());
        if (lilInvoiceLine.getDiscountInPercent() > 0) {
            line.setDiscount(toBigDecimal(lilInvoiceLine.getDiscountInPercent()));
        }
        line.setProdCode(lilInvoiceLine.getProductCode());
        return line;
    }

    private BigDecimal toBigDecimal(Integer integer) {
        return BigDecimal.valueOf(integer);
    }

    private Recipient createRecipient(Principal principal) {
        // TODO: Should do lookup of recipient by no instead of creating??
        Recipient recipient = RecipientFactory.getInstance();
        recipient.setRecipientNo("PID_" + principal.getId());
        recipient.setName(principal.getName());
        recipient.setEmail(principal.getEmail());
        recipient.setMobile(principal.getMobile());
        recipient.setAddress1(principal.getAddress().getAddress1());
        recipient.setCity(principal.getAddress().getCity());
        recipient.setZip(principal.getAddress().getZip());
        String country = principal.getAddress().getCountry();
        if (!country.equals("Norge")) {
            recipient.setCountry(principal.getAddress().getCountry());
        }
        if (ClubService.isEnrolledAsClubMember(principal.getFamily())) {
            recipient.setCategory("MEDLEM");
        } else {
            recipient.setCategory("IKKE_MEDLEM");
        }
        return recipient;
    }

    public void syncInvoiceStatus(List<no.leinstrandil.database.model.accounting.Invoice> lilInvoiceList) {
        for (no.leinstrandil.database.model.accounting.Invoice lilInvoice : lilInvoiceList) {
            try {
                Date paidDate = null;
                Status newState = null;
                InvoiceStatus invoiceStatus = swsClient.getInvoiceStatus(Integer.valueOf(lilInvoice.getExternalInvoiceNumber()));
                if ("paid".equalsIgnoreCase(invoiceStatus.getState())) {
                    newState = Status.PAID;
                    List<InvoiceStatusPayment> list = invoiceStatus.getPayments();
                    for (InvoiceStatusPayment payment : list) {
                        paidDate = payment.getPaymentDate();
                        if ("CRED".equalsIgnoreCase(payment.getPaymentType())) {
                            newState = Status.CREDITED;
                            paidDate = payment.getPaymentDate();
                            break;
                        }
                    }
                }
                if (newState == null) {
                    continue;
                }
                lilInvoice.setExternalInvoicePaid(paidDate);
                lilInvoice.setStatus(newState);
                storage.begin();
                try {
                    storage.persist(lilInvoice);
                    storage.commit();
                } catch (RuntimeException e) {
                    storage.rollback();
                    e.printStackTrace();
                }
            } catch (NumberFormatException | IOException | SwsResponseCodeException | SwsParsingServerResponseException e) {
                e.printStackTrace();
            }
        }


    }


}
