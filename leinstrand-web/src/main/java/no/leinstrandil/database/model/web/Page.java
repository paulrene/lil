package no.leinstrandil.database.model.web;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import no.leinstrandil.database.model.club.Event;
import no.leinstrandil.database.model.club.Team;

@Entity
@Table(name = "page")
public class Page {

    @Id @GeneratedValue
    private Long id;
    private String title;
    private String urlName;
    private String template;
    private String templateConfig;
    private String favoritePages;
    private Boolean userRequired;
    private Date created;
    private Date updated;
    private String redirectToUrl;
    @OneToMany(mappedBy = "page")
    private Set<Team> teams;
    @OneToMany(mappedBy = "page")
    private Set<Event> events;
    @ManyToOne @JoinColumn(name = "requiredRoleId")
    private Role requireRole;
    @ManyToOne @JoinColumn(name = "lastAuthorId")
    private User lastAuthor;
    @OneToMany(mappedBy = "page")
    private Set<Node> nodes;
    @OneToMany(mappedBy = "page")
    private Set<MenuEntry> menuEntries;

    public Page() {
        menuEntries = new HashSet<>();
        events = new HashSet<>();
        teams = new HashSet<>();
        nodes = new HashSet<>();
    }

    public Long getId() {
        return id;
    }

    public Set<Event> getEvents() {
        return events;
    }

    public void setEvents(Set<Event> events) {
        this.events = events;
    }

    public Set<Team> getTeams() {
        return teams;
    }

    public void setTeams(Set<Team> teams) {
        this.teams = teams;
    }

    public Boolean isUserRequired() {
        return userRequired;
    }

    public void setUserRequired(Boolean userRequired) {
        this.userRequired = userRequired;
    }

    public String getTemplate() {
        return template;
    }

    public void setTemplate(String template) {
        this.template = template;
    }

    public String getTemplateConfig() {
        return templateConfig;
    }

    public void setTemplateConfig(String templateConfig) {
        this.templateConfig = templateConfig;
    }

    public String getFavoritePages() {
        return favoritePages;
    }

    public void setFavoritePages(String favoritePages) {
        this.favoritePages = favoritePages;
    }

    public String getUrlName() {
        return urlName;
    }

    public void setUrlName(String urlName) {
        this.urlName = urlName;
    }

    public Set<MenuEntry> getMenuEntries() {
        return menuEntries;
    }

    public void setMenuEntries(Set<MenuEntry> menuEntries) {
        this.menuEntries = menuEntries;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getRedirectToUrl() {
        return redirectToUrl;
    }

    public void setRedirectToUrl(String redirectToUrl) {
        this.redirectToUrl = redirectToUrl;
    }

    public Set<Node> getNodes() {
        return nodes;
    }

    public void setNodes(Set<Node> nodes) {
        this.nodes = nodes;
    }

    public Role getRequireRole() {
        return requireRole;
    }

    public void setRequireRole(Role requireRole) {
        this.requireRole = requireRole;
    }

    public Date getCreated() {
        return created;
    }

    public void setCreated(Date created) {
        this.created = created;
    }

    public User getLastAuthor() {
        return lastAuthor;
    }

    public void setLastAuthor(User lastAuthor) {
        this.lastAuthor = lastAuthor;
    }

    public Date getUpdated() {
        return updated;
    }

    public void setUpdated(Date updated) {
        this.updated = updated;
    }

    @Override
    public String toString() {
        return "[" + getId() + ":" + getTitle() + "]";
    }

}
