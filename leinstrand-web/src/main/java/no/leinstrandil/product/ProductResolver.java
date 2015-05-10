package no.leinstrandil.product;

import no.leinstrandil.service.InvoiceService;
import java.util.List;
import no.leinstrandil.database.model.accounting.Invoice;
import no.leinstrandil.database.model.accounting.Invoice.Status;
import no.leinstrandil.database.model.accounting.InvoiceLine;
import no.leinstrandil.database.model.club.ClubMembership;
import no.leinstrandil.database.model.person.Family;
import no.leinstrandil.database.model.person.Principal;
import no.leinstrandil.service.UserService;

public class ProductResolver {

    private ProductResolver() {
    }

    public static Product getClubMembershipProduct(ClubMembership clubMembership, int year) {
        Family family = clubMembership.getFamily();
        List<Principal> members = family.getMembers();
        if (members.isEmpty()) {
            return null;
        }

        // Already invoiced club membership this year?
        List<Invoice> invoiceList = family.getInvoices();
        for (Invoice invoice : invoiceList) {
            if (invoice.getStatus() == Status.CREDITED) {
                continue;
            }
            if (!InvoiceService.isThisYear(invoice.getCreated())) {
                continue;
            }
            List<InvoiceLine> lineList = invoice.getInvoiceLines();
            for (InvoiceLine invoiceLine : lineList) {
                if (ProductCode.isCodeBelongingToProductOfType(
                        invoiceLine.getProductCode(),
                        ProductType.CLUB_MEMBERSHIP)) {
                    return null;
                }
            }
        }

        if (members.size() > 1) {
            return new Product(ProductCode.FAMILY_MEMBERSHIP.getCode(), "Familiemedlemskap Leinstrand IL " + year, 200, 0);
        } else {
            Principal principal = members.get(0);
            int age = UserService.getAgeAtEndOfCurrentYear(principal);
            if (age >= 18) {
                return new Product(ProductCode.ADULT_MEMBERSHIP.getCode(), "Klubbmedlemskap voksen Leinstrand IL " + year, 150, 0);
            } else {
                return new Product(ProductCode.YOUTH_MEMBERSHIP.getCode(), "Klubbmedlemskap barn/ungdom Leinstrand IL " + year, 50, 0);
            }
        }
    }

}
