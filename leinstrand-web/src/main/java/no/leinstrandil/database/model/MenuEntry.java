package no.leinstrandil.database.model;

import java.util.HashSet;
import java.util.Set;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;

@Entity
@Table(name = "menuentry")
public class MenuEntry {

    @Id
    @GeneratedValue
    private Long id;
    @ManyToOne @JoinColumn(name = "parentId")
    private MenuEntry parent;
    @ManyToOne @JoinColumn(name = "pageId")
    private Page page;
    @ManyToOne @JoinColumn(name = "requiredRoleId")
    private Role requireRole;
    private Boolean userRequired;
    private String description;
    private Integer priority;
    private Boolean disabled;
    private Integer favorite;
    @OneToMany(mappedBy = "parent", fetch = FetchType.EAGER)
    private Set<MenuEntry> subMenuEntries;

    public MenuEntry() {
        subMenuEntries = new HashSet<>();
    }

    public Long getId() {
        return id;
    }

    public Integer getFavorite() {
        return favorite;
    }

    public void setFavorite(Integer favorite) {
        this.favorite = favorite;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setUserRequired(Boolean userRequired) {
        this.userRequired = userRequired;
    }

    public Boolean isUserRequired() {
        return userRequired;
    }

    public Page getPage() {
        return page;
    }

    public void setPage(Page page) {
        this.page = page;
    }

    public MenuEntry getParent() {
        return parent;
    }

    public void setParent(MenuEntry parent) {
        this.parent = parent;
    }

    public Role getRequireRole() {
        return requireRole;
    }

    public void setRequireRole(Role requireRole) {
        this.requireRole = requireRole;
    }

    public Integer getPriority() {
        return priority;
    }

    public void setPriority(Integer priority) {
        this.priority = priority;
    }

    public Boolean isDisabled() {
        return disabled;
    }

    public void setDisabled(Boolean disabled) {
        this.disabled = disabled;
    }

    public Set<MenuEntry> getSubMenuEntries() {
        return subMenuEntries;
    }

    public void setSubMenuEntries(Set<MenuEntry> subMenuEntries) {
        this.subMenuEntries = subMenuEntries;
    }

}