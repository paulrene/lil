package no.leinstrandil.database.model.club;

import java.util.HashSet;
import java.util.Set;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.Table;

@Entity
@Table(name = "sport")
public class Sport {

    @Id @GeneratedValue
    private Long id;
    private String name;
    private String description;
    @OneToMany(mappedBy = "sport")
    private Set<Team> teams;
    @OneToMany(mappedBy = "sport")
    private Set<Event> events;

    public Sport() {
        teams = new HashSet<>();
        events = new HashSet<>();
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Set<Event> getEvents() {
        return events;
    }

    public void setEvents(Set<Event> events) {
        this.events = events;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Set<Team> getTeams() {
        return teams;
    }

    public void setTeams(Set<Team> teams) {
        this.teams = teams;
    }

}
