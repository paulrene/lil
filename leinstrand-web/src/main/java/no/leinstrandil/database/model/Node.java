package no.leinstrandil.database.model;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OrderBy;
import javax.persistence.Table;

@Entity
@Table(name = "node")
public class Node {

    @Id @GeneratedValue
    private Long id;
    private String identifier;
    @ManyToOne @JoinColumn(name = "pageId")
    private Page page;
    @OneToMany(mappedBy = "node") @OrderBy("created DESC")
    private List<TextNode> textNodeVersions;
    @OneToMany(mappedBy = "node")
    private Set<Resource> resources;

    public Node() {
        textNodeVersions = new ArrayList<>();
        resources = new HashSet<>();
    }

    public Long getId() {
        return id;
    }

    public String getIdentifier() {
        return identifier;
    }

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    public Page getPage() {
        return page;
    }

    public void setPage(Page page) {
        this.page = page;
    }

    public Set<Resource> getResources() {
        return resources;
    }

    public void setResources(Set<Resource> resources) {
        this.resources = resources;
    }

    public List<TextNode> getTextNodeVersions() {
        return textNodeVersions;
    }

    public void setTextNodeVersions(List<TextNode> textNodeVersions) {
        this.textNodeVersions = textNodeVersions;
    }

}
