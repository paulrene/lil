package no.leinstrandil.database.model.web;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToMany;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import no.leinstrandil.database.model.person.Principal;

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
    private String username;
    private String passwordHash;
    private Date passwordHashCreated;
    private String resetPasswordCode;
    private Date resetPasswordCodeCreated;
    private String facebookId;
    @OneToOne @JoinColumn(name = "principalId")
    private Principal principal;

    public User() {
        textNodeSet = new HashSet<>();
        resourceSet = new HashSet<>();
        roles = new HashSet<>();
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

    public String getFacebookId() {
        return facebookId;
    }

    public void setFacebookId(String facebookId) {
        this.facebookId = facebookId;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public Principal getPrincipal() {
        return principal;
    }

    public void setPrincipal(Principal principal) {
        this.principal = principal;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getResetPasswordCode() {
        return resetPasswordCode;
    }

    public void setResetPasswordCode(String resetPasswordCode) {
        this.resetPasswordCode = resetPasswordCode;
    }

    public Date getResetPasswordCodeCreated() {
        return resetPasswordCodeCreated;
    }

    public void setResetPasswordCodeCreated(Date resetPasswordCodeCreated) {
        this.resetPasswordCodeCreated = resetPasswordCodeCreated;
    }

    public Date getPasswordHashCreated() {
        return passwordHashCreated;
    }

    public void setPasswordHashCreated(Date passwordHashCreated) {
        this.passwordHashCreated = passwordHashCreated;
    }

    @Override
    public String toString() {
        return "{User id:"+getId()+", un:"+getUsername()+"}";
    }

}
