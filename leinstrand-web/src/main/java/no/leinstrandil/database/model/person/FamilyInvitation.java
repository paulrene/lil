package no.leinstrandil.database.model.person;

import java.util.Date;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

@Entity
@Table(name = "familyinvitation")
public class FamilyInvitation {

    @Id @GeneratedValue
    private Long id;
    private String code;
    @ManyToOne @JoinColumn(name = "familyId")
    private Family family;
    @ManyToOne @JoinColumn(name = "principalId")
    private Principal principal;
    private Date created;

    public FamilyInvitation() {
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

    public Family getFamily() {
        return family;
    }

    public void setFamily(Family family) {
        this.family = family;
    }

    public Principal getPrincipal() {
        return principal;
    }

    public void setPrincipal(Principal principal) {
        this.principal = principal;
    }

}
