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
@Table(name = "activity")
public class Event {

    @Id @GeneratedValue
    private Long id;
    private String name;
    private String description;
    private Integer vacancies;
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
}
