package no.leinstrandil.database.model.accounting;

import java.util.Date;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import no.leinstrandil.database.model.club.ClubMembership;
import no.leinstrandil.database.model.club.EventParticipation;
import no.leinstrandil.database.model.club.TeamMembership;
import no.leinstrandil.database.model.person.Family;
import no.leinstrandil.database.model.person.Principal;

@Entity
@Table(name = "invoiceline")
public class InvoiceLine {

    @Id @GeneratedValue
    private Long id;
    @ManyToOne @JoinColumn(name = "invoiceId")
    private Invoice invoice;
    private Integer quantity;
    private String description;
    private Integer unitPrice;
    private Integer taxPercent;
    private String productCode; // 9 chars
    private Integer discountInPercent;
    private Date created;

    @ManyToOne @JoinColumn(name = "familyId")
    private Family family;
    @OneToOne @JoinColumn(name = "clubMembershipId")
    private ClubMembership clubMembership;

    @ManyToOne @JoinColumn(name = "principalId")
    private Principal principal;
    @OneToOne @JoinColumn(name = "eventParticipationId")
    private EventParticipation eventParticipation;
    @OneToOne @JoinColumn(name = "teamMembershipId")
    private TeamMembership teamMembership;

    public InvoiceLine() {
    }

    public Long getId() {
        return id;
    }

    public Invoice getInvoice() {
        return invoice;
    }

    public void setInvoice(Invoice invoice) {
        this.invoice = invoice;
    }

    public Date getCreated() {
        return created;
    }

    public void setCreated(Date created) {
        this.created = created;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getProductCode() {
        return productCode;
    }

    public void setProductCode(String productCode) {
        this.productCode = productCode;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public Integer getTaxPercent() {
        return taxPercent;
    }

    public void setTaxPercent(Integer taxPercent) {
        this.taxPercent = taxPercent;
    }

    public Integer getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(Integer unitPrice) {
        this.unitPrice = unitPrice;
    }

    public Family getFamily() {
        return family;
    }

    public void setFamily(Family family) {
        this.family = family;
    }

    public Principal getPrincipal() {
        return principal;
    }

    public void setPrincipal(Principal principal) {
        this.principal = principal;
    }

    public ClubMembership getClubMembership() {
        return clubMembership;
    }

    public void setClubMembership(ClubMembership clubMembership) {
        this.clubMembership = clubMembership;
    }

    public EventParticipation getEventParticipation() {
        return eventParticipation;
    }

    public void setEventParticipation(EventParticipation eventParticipation) {
        this.eventParticipation = eventParticipation;
    }

    public TeamMembership getTeamMembership() {
        return teamMembership;
    }

    public void setTeamMembership(TeamMembership teamMembership) {
        this.teamMembership = teamMembership;
    }

    public void setDiscountInPercent(int discountInPercent) {
        this.discountInPercent = discountInPercent;
    }

    public Integer getDiscountInPercent() {
        return discountInPercent;
    }

}
