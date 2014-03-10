package no.leinstrandil.database.model.web;

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
@Table(name = "node")
public class Node {

    @Id @GeneratedValue
    private Long id;
    private String identifier;
    @ManyToOne @JoinColumn(name = "pageId")
    private Page page;
    @OneToMany(mappedBy = "node") @OrderBy("created DESC")
    private List<TextNode> textNodeVersions;

    public Node() {
        textNodeVersions = new ArrayList<>();
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

    public List<TextNode> getTextNodeVersions() {
        return textNodeVersions;
    }

    public void setTextNodeVersions(List<TextNode> textNodeVersions) {
        this.textNodeVersions = textNodeVersions;
    }

}
