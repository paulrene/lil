package no.leinstrandil.database.model.person;

import java.util.Date;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

@Entity
@Table(name = "mobilenumber")
public class MobileNumber {

    @Id @GeneratedValue
    private Long id;
    @ManyToOne @JoinColumn(name = "principalId")
    private Principal principal;
    private String number;
    private String code;
    private Date verified;
    private Date prime;
    private Date created;

    public MobileNumber() {
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

    public String getNumber() {
        return number;
    }

    public void setNumber(String number) {
        this.number = number;
    }

    public Date getPrime() {
        return prime;
    }

    public void setPrime(Date prime) {
        this.prime = prime;
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
