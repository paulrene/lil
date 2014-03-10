package no.leinstrandil.database.model.web;

import java.util.Date;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

@Entity
@Table(name = "facebookevent")
public class FacebookEvent {

    @Id @GeneratedValue
    private Long id;
    @ManyToOne @JoinColumn(name = "facebookPageId")
    private FacebookPage facebookPage;
    private Date created;
    private String name;
    private String description;
    private Date startTime;
    private Date endTime;
    private String location;
    private Date facebookUpdated;
    private String facebookEventId;

    public FacebookEvent() {
    }

    public Long getId() {
        return id;
    }

    public String getFacebookEventId() {
        return facebookEventId;
    }

    public void setFacebookEventId(String facebookEventId) {
        this.facebookEventId = facebookEventId;
    }

    public Date getFacebookUpdated() {
        return facebookUpdated;
    }

    public void setFacebookUpdated(Date facebookUpdated) {
        this.facebookUpdated = facebookUpdated;
    }

    public Date getCreated() {
        return created;
    }

    public void setCreated(Date created) {
        this.created = created;
    }

    public FacebookPage getFacebookPage() {
        return facebookPage;
    }

    public void setFacebookPage(FacebookPage facebookPage) {
        this.facebookPage = facebookPage;
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

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public Date getStartTime() {
        return startTime;
    }

    public void setStartTime(Date startTime) {
        this.startTime = startTime;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
