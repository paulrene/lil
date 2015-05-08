package no.leinstrandil.database.model.accounting;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OrderBy;
import javax.persistence.Table;
import no.leinstrandil.database.model.person.Family;

@Entity
@Table(name = "invoice")
public class Invoice {

    public static enum Status { OPEN, CLOSED, SENT, PAID, CREDITED };

    @Id @GeneratedValue
    private Long id;
    @ManyToOne @JoinColumn(name = "familyId")
    private Family family;
    @OneToMany(mappedBy = "invoice") @OrderBy("created")
    private List<InvoiceLine> invoiceLineList;
    private String externalInvoiceNumber;
    private Date externalInvoiceDate;
    private Date externalInvoiceDue;
    private Status status;
    private Date created;

    public Invoice() {
        this.invoiceLineList = new ArrayList<>();
    }

    public Long getId() {
        return id;
    }

    public Date getCreated() {
        return created;
    }

    public void setCreated(Date created) {
        this.created = created;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public Family getFamily() {
        return family;
    }

    public void setFamily(Family family) {
        this.family = family;
    }

    public String getExternalInvoiceNumber() {
        return externalInvoiceNumber;
    }

    public void setExternalInvoiceNumber(String externalInvoiceNumber) {
        this.externalInvoiceNumber = externalInvoiceNumber;
    }

    public List<InvoiceLine> getInvoiceLineList() {
        return invoiceLineList;
    }

    public void setInvoiceLineList(List<InvoiceLine> invoiceLineList) {
        this.invoiceLineList = invoiceLineList;
    }

    public Date getExternalInvoiceDate() {
        return externalInvoiceDate;
    }

    public void setExternalInvoiceDate(Date externalInvoiceDate) {
        this.externalInvoiceDate = externalInvoiceDate;
    }

    public Date getExternalInvoiceDue() {
        return externalInvoiceDue;
    }

    public void setExternalInvoiceDue(Date externalInvoiceDue) {
        this.externalInvoiceDue = externalInvoiceDue;
    }

}
