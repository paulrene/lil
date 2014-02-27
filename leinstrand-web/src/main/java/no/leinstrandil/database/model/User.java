package no.leinstrandil.database.model;

import java.util.HashSet;
import java.util.Set;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToMany;
import javax.persistence.OneToMany;
import javax.persistence.Table;

@Entity
@Table(name = "user")
public class User {

    @Id
    @GeneratedValue
    private Long id;
    @OneToMany(mappedBy = "author")
    private Set<TextNode> textNodeSet;
    @OneToMany(mappedBy = "author")
    private Set<Resource> resources;
    @ManyToMany(mappedBy = "usersInRole")
    private Set<Role> roles;

    public User() {
        textNodeSet = new HashSet<>();
    }

    public Long getId() {
        return id;
    }

    public Set<Resource> getResources() {
        return resources;
    }

    public void setResources(Set<Resource> resources) {
        this.resources = resources;
    }

    public Set<Role> getRoles() {
        return roles;
    }

    public void setRoles(Set<Role> roles) {
        this.roles = roles;
    }

    public Set<TextNode> getTextNodeSet() {
        return textNodeSet;
    }

    public void setTextNodeSet(Set<TextNode> textNodeSet) {
        this.textNodeSet = textNodeSet;
    }

    @Override
    public String toString() {
        return "{User id:"+getId()+"}";
    }

}
