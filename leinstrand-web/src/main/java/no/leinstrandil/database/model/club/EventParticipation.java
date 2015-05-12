package no.leinstrandil.database.model.club;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import no.leinstrandil.database.model.accounting.InvoiceLine;
import no.leinstrandil.database.model.person.Principal;

@Entity
@Table(name = "eventparticipation")
public class EventParticipation {

    @Id @GeneratedValue
    private Long id;
    private Date created;
    private Boolean enrolled;
    @ManyToOne @JoinColumn(name = "principalId")
    private Principal principal;
    @ManyToOne @JoinColumn(name = "eventId")
    private Event event;
    @OneToMany(mappedBy = "eventParticipation")
    private List<InvoiceLine> invoiceLines;

    public EventParticipation() {
        invoiceLines = new ArrayList<>();
    }

    public Long getId() {
        return id;
    }

    public List<InvoiceLine> getInvoiceLines() {
        return invoiceLines;
    }

    public void setInvoiceLines(List<InvoiceLine> invoiceLines) {
        this.invoiceLines = invoiceLines;
    }

    public Date getCreated() {
        return created;
    }

    public void setCreated(Date created) {
        this.created = created;
    }

    public Boolean isEnrolled() {
        return enrolled;
    }

    public void setEnrolled(Boolean enrolled) {
        this.enrolled = enrolled;
    }

    public Principal getPrincipal() {
        return principal;
    }

    public void setPrincipal(Principal principal) {
        this.principal = principal;
    }

    public void setEvent(Event event) {
        this.event = event;
    }

    public Event getEvent() {
        return event;
    }

}
