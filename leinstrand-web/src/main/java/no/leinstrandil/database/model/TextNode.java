package no.leinstrandil.database.model;

import java.util.Date;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

@Entity
@Table(name = "textnode")
public class TextNode {

    @Id @GeneratedValue
    private Long id;
    @ManyToOne @JoinColumn(name = "nodeId")
    private Node node;
    @ManyToOne @JoinColumn(name = "authorId")
    private User author;
    @Column(length = 65535)
    private String source;
    private Date created;

    public TextNode() {
    }

    public Long getId() {
        return id;
    }

    public User getAuthor() {
        return author;
    }

    public void setAuthor(User author) {
        this.author = author;
    }

    public Node getNode() {
        return node;
    }

    public void setNode(Node node) {
        this.node = node;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public Date getCreated() {
        return created;
    }

    public void setCreated(Date created) {
        this.created = created;
    }

}
