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
    @OneToMany(mappedBy = "uploader")
    private Set<Resource> resourceSet;
    @ManyToMany(mappedBy = "usersInRole")
    private Set<Role> roles;

    public User() {
        textNodeSet = new HashSet<>();
        resourceSet = new HashSet<>();
    }

    public Long getId() {
        return id;
    }

    public Set<Resource> getResourceSet() {
        return resourceSet;
    }

    public void setResourceSet(Set<Resource> resourceSet) {
        this.resourceSet = resourceSet;
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
