package no.leinstrandil.service;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import no.leinstrandil.database.Storage;
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
import no.sws.invoice.line.InvoiceLine;
import no.sws.invoice.line.InvoiceLineFactory;
import no.sws.invoice.recipient.Recipient;
import no.sws.invoice.recipient.RecipientFactory;
import no.sws.invoice.shipment.Shipment;
import no.sws.invoice.shipment.ShipmentFactory;
import no.sws.invoice.shipment.ShipmentType;
import org.apache.commons.httpclient.HttpException;

public class SendRegningService {

    private Storage storage;
    private UserService userService;

    public SendRegningService(Storage storage, UserService userService) {
        this.storage = storage;
        this.userService = userService;
    }

    public static void main(String[] args) throws HttpException, IOException, SwsNotValidRecipientException,
            SwsRequiredInvoiceValueException, SwsNoRecipientForInvoiceException, SwsNoInvoiceLinesForInvoiceException,
            SwsTooManyInvoiceLinesException, SwsParsingServerResponseException,
            SwsMissingRequiredElementInResponseException, SwsMissingCreditedIdException, SwsResponseCodeException,
            SwsRequiredBatchIdException, SwsBatchIdTooLongException {

        SwsClient sws = new SwsClient("faktura@leinstrandil.no", "Dystmp?!");
        sws.setTest(true);


        Invoice invoice = InvoiceFactory.getInstance().getInvoice();

        Recipient recipient = sws.getRecipientByRecipientNo("Medlem1");
        if (recipient == null || !"Medlem2".equals(recipient.getRecipientNo())) {
            System.out.println("Creating new recipient.");
            recipient = RecipientFactory.getInstance();
            recipient.setRecipientNo("Medlem2");
            recipient.setName("Kalle Balle");
            recipient.setAddress1("Ole Nypansvei 21");
            recipient.setZip("7083");
            recipient.setCity("Leinstrand");
            recipient.setEmail("paulrene@gmail.com");
            recipient.setMobile("95994795");
            recipient.setCategory("leinstrandil.no user");
        }
        invoice.setRecipient(recipient);

/*
        invoice.addInvoiceLine(
                new BigDecimal("1.00"), "Famliemedlemskap Leinstrand idrettslag 2014", new BigDecimal("200.00"));
        invoice.addInvoiceLine(
                new BigDecimal("1.00"), "Treningsavgift 2014 5-12 år -- Balle Klorin", new BigDecimal("250.00"));
        invoice.addInvoiceLine(
                new BigDecimal("1.00"), "Treningsavgift 2014 13-16 år -- Kalle Balle", new BigDecimal("800.00"));
*/

        List<InvoiceLine> lineList = new ArrayList<>();

        InvoiceLine line1 = InvoiceLineFactory.getInstance();
        line1.setQty(new BigDecimal("1.0"));
        line1.setDesc("Familiemedlemskap 2014 Leinstrand idrettslag");
        line1.setUnitPrice(new BigDecimal("200.0"));
        line1.setTax(0);
        line1.setProdCode("MDA-FAM14");
        lineList.add(line1);

        InvoiceLine line2 = InvoiceLineFactory.getInstance();
        line2.setQty(new BigDecimal("1.0"));
        line2.setDesc("Treningsavgift 2014 5-12 år -- Balle Klorin");
        line2.setUnitPrice(new BigDecimal("250"));
        line2.setTax(0);
        line2.setProdCode("TRA-BAR14");
        lineList.add(line2);

        InvoiceLine line3 = InvoiceLineFactory.getInstance();
        line3.setQty(new BigDecimal("1.0"));
        line3.setDesc("Treningsavgift 2014 13-16 år -- Kalle Balle");
        line3.setUnitPrice(new BigDecimal("800"));
        line3.setTax(0);
        line3.setProdCode("TRA-JUN14");
        lineList.add(line3);

        InvoiceLine line4 = InvoiceLineFactory.getInstance();
        line4.setQty(new BigDecimal("1.0"));
        line4.setDesc("Treningsavgift 2014 16-19 år -- Trond Kalle");
        line4.setUnitPrice(new BigDecimal("1800"));
        line4.setTax(0);
        line4.setProdCode("TRA-UNG14");
        lineList.add(line4);

        InvoiceLine line5 = InvoiceLineFactory.getInstance();
        line5.setQty(new BigDecimal("1.0"));
        line5.setDesc("Treningsavgift 2014 Senior -- Ola Klorin");
        line5.setUnitPrice(new BigDecimal("2200"));
        line5.setTax(0);
        line5.setProdCode("TRA-SEN14");
        lineList.add(line5);

        invoice.setInvoiceLines(lineList);

        Shipment shipment = ShipmentFactory.getInstance(ShipmentType.email);
        shipment.addEmailAddress("paulrene@gmail.com");
        invoice.setShipment(shipment);

        invoice.setComment("Dette er en fakturakommentar.");
        invoice.setInvoiceText("Dette er en fakturatekst.");
        invoice.setOurRef("leinstrandil.no");

        List<Invoice> invoiceList = new ArrayList<>();
        invoiceList.add(invoice);

        List<Invoice> deliveredList = sws.sendInvoices(invoiceList, "TESTBATCHID6");
        System.out.println(deliveredList);

    }


}
