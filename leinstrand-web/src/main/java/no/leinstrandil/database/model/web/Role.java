package no.leinstrandil.database.model.web;

import java.util.HashSet;
import java.util.Set;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToMany;
import javax.persistence.Table;

@Entity
@Table(name = "role")
public class Role {

    @Id
    @GeneratedValue
    private Long id;
    private String identifier;
    private String name;
    @ManyToMany
    private Set<User> usersInRole;

    public Role() {
        usersInRole = new HashSet<>();
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

    public Set<User> getUsersInRole() {
        return usersInRole;
    }

    public void setUsersInRole(Set<User> usersInRole) {
        this.usersInRole = usersInRole;
    }

    public String getIdentifier() {
        return identifier;
    }

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

}
