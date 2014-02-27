package no.leinstrandil.database.model;

import java.util.Date;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "facebookpage")
public class FacebookPage {

    @Id
    @GeneratedValue
    private Long id;
    private String accessToken;
    private String facebookPageName;
    private String facebookPageIdentifier;
    private Integer syncInterval;
    private Date lastSync;

    public FacebookPage() {
    }

    public Long getId() {
        return id;
    }

    public String getFacebookPageName() {
        return facebookPageName;
    }

    public void setFacebookPageName(String facebookPageName) {
        this.facebookPageName = facebookPageName;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public String getFacebookPageIdentifier() {
        return facebookPageIdentifier;
    }

    public void setFacebookPageIdentifier(String facebookPageIdentifier) {
        this.facebookPageIdentifier = facebookPageIdentifier;
    }

    public Date getLastSync() {
        return lastSync;
    }

    public void setLastSync(Date lastSync) {
        this.lastSync = lastSync;
    }

    public Integer getSyncInterval() {
        return syncInterval;
    }

    public void setSyncInterval(Integer syncInterval) {
        this.syncInterval = syncInterval;
    }

}
