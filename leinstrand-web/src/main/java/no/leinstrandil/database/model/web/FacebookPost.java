package no.leinstrandil.database.model.web;

import javax.persistence.Column;

import java.util.Date;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

@Entity
@Table(name = "facebookpost")
public class FacebookPost {

    @Id @GeneratedValue
    private Long id;
    @ManyToOne @JoinColumn(name = "facebookPageId")
    private FacebookPage facebookPage;
    private String facebookPostId;
    private String pictureUrl;
    private String linkUrl;
    private String caption;
    @Column(length = 65535)
    private String message;
    @Column(length = 65535)
    private String description;
    @Column(length = 65535)
    private String story;
    private String facebookType;
    private Date facebookCreated;
    private Date facebookUpdated;
    private Date created;

    public FacebookPost() {
    }

    public Long getId() {
        return id;
    }

    public String getFacebookPostId() {
        return facebookPostId;
    }

    public void setFacebookPostId(String facebookPostId) {
        this.facebookPostId = facebookPostId;
    }

    public String getCaption() {
        return caption;
    }

    public void setCaption(String caption) {
        this.caption = caption;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getStory() {
        return story;
    }

    public String getLinkUrl() {
        return linkUrl;
    }

    public void setLinkUrl(String linkUrl) {
        this.linkUrl = linkUrl;
    }

    public void setStory(String story) {
        this.story = story;
    }

    public String getFacebookType() {
        return facebookType;
    }

    public void setFacebookType(String facebookType) {
        this.facebookType = facebookType;
    }

    public FacebookPage getFacebookPage() {
        return facebookPage;
    }

    public void setFacebookPage(FacebookPage facebookPage) {
        this.facebookPage = facebookPage;
    }

    public Date getCreated() {
        return created;
    }

    public void setCreated(Date created) {
        this.created = created;
    }

    public Date getFacebookCreated() {
        return facebookCreated;
    }

    public void setFacebookCreated(Date facebookCreated) {
        this.facebookCreated = facebookCreated;
    }

    public Date getFacebookUpdated() {
        return facebookUpdated;
    }

    public void setFacebookUpdated(Date facebookUpdated) {
        this.facebookUpdated = facebookUpdated;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getPictureUrl() {
        return pictureUrl;
    }

    public void setPictureUrl(String pictureUrl) {
        this.pictureUrl = pictureUrl;
    }

}
