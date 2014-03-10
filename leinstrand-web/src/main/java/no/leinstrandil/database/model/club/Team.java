package no.leinstrandil.database.model.club;

import java.util.List;
import javax.persistence.OneToMany;
import javax.persistence.OrderBy;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import no.leinstrandil.database.model.web.Page;

@Entity
@Table(name = "team")
public class Team {

    @Id @GeneratedValue
    private Long id;
    private String name;
    private String description;
    @ManyToOne @JoinColumn(name = "pageId")
    private Page page;
    @ManyToOne @JoinColumn(name = "sportId")
    private Sport sport;
    @OneToMany(mappedBy = "team") @OrderBy("created DESC")
    private List<TeamMembership> teamMemberships;

    public Team() {
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

    public List<TeamMembership> getTeamMemberships() {
        return teamMemberships;
    }

    public void setTeamMemberships(List<TeamMembership> teamMemberships) {
        this.teamMemberships = teamMemberships;
    }

}
