package no.leinstrandil.database.model.person;

import java.util.ArrayList;
import java.util.List;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OrderBy;
import javax.persistence.Table;

@Entity
@Table(name = "family")
public class Family {

    @Id @GeneratedValue
    private Long id;
    @ManyToOne @JoinColumn(name = "primaryPrincipalId")
    private Principal primaryPrincipal;
    @OneToMany(mappedBy = "family") @OrderBy("birthdate")
    private List<Principal> members;

    public Family() {
        members = new ArrayList<>();
    }

    public Long getId() {
        return id;
    }

    public Principal getPrimaryPrincipal() {
        return primaryPrincipal;
    }

    public void setPrimaryPrincipal(Principal primaryPrincipal) {
        this.primaryPrincipal = primaryPrincipal;
    }

    public List<Principal> getMembers() {
        return members;
    }

    public void setMembers(List<Principal> members) {
        this.members = members;
    }

}
