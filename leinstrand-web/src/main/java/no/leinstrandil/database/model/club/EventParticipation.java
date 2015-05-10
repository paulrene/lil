package no.leinstrandil.database.model.club;

import java.util.Date;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToOne;
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
    @OneToOne(mappedBy = "eventParticipation")
    private InvoiceLine invoiceLine;

    public EventParticipation() {
    }

    public Long getId() {
        return id;
    }

    public InvoiceLine getInvoiceLine() {
        return invoiceLine;
    }

    public void setInvoiceLine(InvoiceLine invoiceLine) {
        this.invoiceLine = invoiceLine;
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
