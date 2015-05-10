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
import no.leinstrandil.database.model.person.Family;

@Entity
@Table(name = "clubmembership")
public class ClubMembership {

    @Id @GeneratedValue
    private Long id;
    private Date created;
    private Boolean enrolled;
    @ManyToOne @JoinColumn(name = "familyId")
    private Family family;
    @OneToOne(mappedBy = "clubMembership")
    private InvoiceLine invoiceLine;

    public ClubMembership() {
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

    public Family getFamily() {
        return family;
    }

    public void setFamily(Family family) {
        this.family = family;
    }

}
