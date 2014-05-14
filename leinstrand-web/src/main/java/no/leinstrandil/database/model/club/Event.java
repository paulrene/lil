package no.leinstrandil.database.model.club;

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
import no.leinstrandil.database.model.web.Page;

@Entity
@Table(name = "event")
public class Event {

    @Id @GeneratedValue
    private Long id;
    private String name;
    private String description;
    private Integer vacancies;
    private Integer priceMember;
    private Integer priceNonMember;
    private Integer minimumAge;
    private Integer maximumAge;
    private Boolean requireMembership;
    private Boolean closed;
    private Boolean locked;
    private Date startTime;
    private Date endTime;
    @ManyToOne @JoinColumn(name = "pageId")
    private Page page;
    @ManyToOne @JoinColumn(name = "sportId")
    private Sport sport;
    @OneToMany(mappedBy = "event") @OrderBy("created DESC")
    private List<EventParticipation> eventParticipations;

    public Event() {
    }

    public Long getId() {
        return id;
    }

    public Sport getSport() {
        return sport;
    }

    public void setSport(Sport sport) {
        this.sport = sport;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Date getEndTime() {
        return endTime;
    }

    public void setEndTime(Date endTime) {
        this.endTime = endTime;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Page getPage() {
        return page;
    }

    public void setPage(Page page) {
        this.page = page;
    }

    public Date getStartTime() {
        return startTime;
    }

    public void setStartTime(Date startTime) {
        this.startTime = startTime;
    }

    public Integer getVacancies() {
        return vacancies;
    }

    public void setVacancies(Integer vacancies) {
        this.vacancies = vacancies;
    }

    public List<EventParticipation> getEventParticipations() {
        return eventParticipations;
    }

    public void setEventParticipations(List<EventParticipation> eventParticipations) {
        this.eventParticipations = eventParticipations;
    }

    public Integer getPriceMember() {
        return priceMember;
    }

    public void setPriceMember(Integer priceMember) {
        this.priceMember = priceMember;
    }

    public Integer getPriceNonMember() {
        return priceNonMember;
    }

    public void setPriceNonMember(Integer priceNonMember) {
        this.priceNonMember = priceNonMember;
    }

    public Boolean requireMembership() {
        return requireMembership;
    }

    public void setRequireMembership(Boolean requireMembership) {
        this.requireMembership = requireMembership;
    }

    public Integer getMaximumAge() {
        return maximumAge;
    }

    public void setMaximumAge(Integer maximumAge) {
        this.maximumAge = maximumAge;
    }

    public Integer getMinimumAge() {
        return minimumAge;
    }

    public void setMinimumAge(Integer minimumAge) {
        this.minimumAge = minimumAge;
    }

    public Boolean isClosed() {
        return closed;
    }

    public void setClosed(Boolean closed) {
        this.closed = closed;
    }

    public Boolean isLocked() {
        return locked;
    }

    public void setLocked(Boolean locked) {
        this.locked = locked;
    }

}
