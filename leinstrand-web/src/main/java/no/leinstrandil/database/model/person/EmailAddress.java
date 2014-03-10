package no.leinstrandil.database.model.person;

import java.util.Date;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

@Entity
@Table(name = "emailaddress")
public class EmailAddress {

    @Id @GeneratedValue
    private Long id;
    @ManyToOne @JoinColumn(name = "principalId")
    private Principal principal;
    private String email;
    private String code;
    private Date verified;
    private Date primary;
    private Date created;

    public EmailAddress() {
    }

    public Long getId() {
        return id;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public Date getCreated() {
        return created;
    }

    public void setCreated(Date created) {
        this.created = created;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public Date getPrimary() {
        return primary;
    }

    public void setPrimary(Date primary) {
        this.primary = primary;
    }

    public Principal getPrincipal() {
        return principal;
    }

    public void setPrincipal(Principal principal) {
        this.principal = principal;
    }

    public Date getVerified() {
        return verified;
    }

    public void setVerified(Date verified) {
        this.verified = verified;
    }

}
