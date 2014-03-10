package no.leinstrandil.database.model.web;

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
    private String appId;
    private String appSecret;
    private String facebookPageName;
    private String facebookPageIdentifier;
    private Integer syncInterval;
    private Date lastSync;

    public FacebookPage() {
    }

    public Long getId() {
        return id;
    }

    public String getAppId() {
        return appId;
    }

    public void setAppId(String appId) {
        this.appId = appId;
    }

    public String getAppSecret() {
        return appSecret;
    }

    public void setAppSecret(String appSecret) {
        this.appSecret = appSecret;
    }

    public String getFacebookPageName() {
        return facebookPageName;
    }

    public void setFacebookPageName(String facebookPageName) {
        this.facebookPageName = facebookPageName;
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
