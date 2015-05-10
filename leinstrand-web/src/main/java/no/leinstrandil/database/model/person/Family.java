package no.leinstrandil.database.model.person;

import java.util.ArrayList;
import java.util.List;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OrderBy;
import javax.persistence.Table;
import no.leinstrandil.database.model.accounting.Invoice;
import no.leinstrandil.database.model.accounting.InvoiceLine;
import no.leinstrandil.database.model.club.ClubMembership;

@Entity
@Table(name = "family")
public class Family {

    @Id @GeneratedValue
    private Long id;
    @ManyToOne @JoinColumn(name = "primaryPrincipalId")
    private Principal primaryPrincipal;
    @OneToMany(mappedBy = "family") @OrderBy("birthdate")
    private List<Principal> members;
    @OneToMany(mappedBy = "family") @OrderBy("created DESC")
    private List<ClubMembership> clubMemberships;
    @OneToMany(mappedBy = "family") @OrderBy("created DESC")
    private List<Invoice> invoices;
    @OneToMany(mappedBy = "family") @OrderBy("created DESC")
    private List<InvoiceLine> invoiceLines;
    private Boolean noCombinedMembership;

    public Family() {
        members = new ArrayList<>();
        invoices = new ArrayList<>();
        invoiceLines = new ArrayList<>();
        clubMemberships = new ArrayList<>();
    }

    public Long getId() {
        return id;
    }

    public List<Invoice> getInvoices() {
        return invoices;
    }

    public void setInvoices(List<Invoice> invoices) {
        this.invoices = invoices;
    }

    public List<InvoiceLine> getInvoiceLines() {
        return invoiceLines;
    }

    public void setInvoiceLines(List<InvoiceLine> invoiceLines) {
        this.invoiceLines = invoiceLines;
    }

    public Principal getPrimaryPrincipal() {
        return primaryPrincipal;
    }

    public void setPrimaryPrincipal(Principal primaryPrincipal) {
        this.primaryPrincipal = primaryPrincipal;
    }

    public List<Principal> getMembers() {
        return members;
    }

    public void setMembers(List<Principal> members) {
        this.members = members;
    }

    public List<ClubMembership> getClubMemberships() {
        return clubMemberships;
    }

    public void setClubMemberships(List<ClubMembership> clubMemberships) {
        this.clubMemberships = clubMemberships;
    }

    public void setNoCombinedMembership(Boolean noCombinedMembership) {
        this.noCombinedMembership = noCombinedMembership;
    }

    public Boolean isNoCombinedMembership() {
        return noCombinedMembership;
    }

}
