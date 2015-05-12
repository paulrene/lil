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
@Table(name = "teammembership")
public class TeamMembership {

    @Id @GeneratedValue
    private Long id;
    private Date created;
    private Boolean enrolled;
    @ManyToOne @JoinColumn(name = "principalId")
    private Principal principal;
    @ManyToOne @JoinColumn(name = "teamId")
    private Team team;
    @OneToMany(mappedBy = "teamMembership")
    private List<InvoiceLine> invoiceLines;

    public TeamMembership() {
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

    public Team getTeam() {
        return team;
    }

    public void setTeam(Team team) {
        this.team = team;
    }

}
