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
import org.hibernate.annotations.Cascade;
import org.hibernate.annotations.CascadeType;

@Entity
@Table(name = "invoice")
public class Invoice {

    public static enum Status {
        OPEN("Utarbeides"),
        SENT("Ikke betalt"),
        PAID("Betalt"),
        CREDITED("Kreditert"),
        SEND_FAILED("Utsending feilet");

        private String description;
        private Status(String description) {
            this.description = description;
        }
        public String getDescription() {
            return description;
        }
    };

    @Id @GeneratedValue
    private Long id;
    @ManyToOne @JoinColumn(name = "familyId")
    private Family family;
    @OneToMany(mappedBy = "invoice") @OrderBy("created")
    @Cascade({CascadeType.REMOVE})
    private List<InvoiceLine> invoiceLines;
    private String externalInvoiceNumber;
    private Date externalInvoiceDate;
    private Date externalInvoiceDue;
    private Date externalInvoicePaid;
    private String externalBatchId;
    private Status status;
    private Date created;

    public Invoice() {
        this.invoiceLines = new ArrayList<>();
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

    public List<InvoiceLine> getInvoiceLines() {
        return invoiceLines;
    }

    public void setInvoiceLines(List<InvoiceLine> invoiceLines) {
        this.invoiceLines = invoiceLines;
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

    public Date getExternalInvoicePaid() {
        return externalInvoicePaid;
    }

    public void setExternalInvoicePaid(Date externalInvoicePaid) {
        this.externalInvoicePaid = externalInvoicePaid;
    }

    public String getExternalBatchId() {
        return externalBatchId;
    }

    public void setExternalBatchId(String externalBatchId) {
        this.externalBatchId = externalBatchId;
    }

}
