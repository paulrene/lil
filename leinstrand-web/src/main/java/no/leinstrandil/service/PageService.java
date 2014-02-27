package no.leinstrandil.service;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Set;
import javax.persistence.NoResultException;
import no.leinstrandil.database.Storage;
import no.leinstrandil.database.model.Node;
import no.leinstrandil.database.model.Page;
import no.leinstrandil.database.model.TextNode;
import no.leinstrandil.database.model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PageService {
    private static final Logger log = LoggerFactory.getLogger(PageService.class);

    private Storage storage;

    public PageService(Storage storage) {
        this.storage = storage;
    }

    public Page getPageByUrlName(String urlName) {
        try {
            return storage.createSingleQuery("from Page p where p.urlName = '" + sqlEscape(urlName) + "'", Page.class);
        } catch (NoResultException e) {
            log.info("Could not find a Page with urlName: " + urlName);
            return null;
        }
    }

    public Node getNode(Page page, String identifier) {
        Set<Node> nodes = page.getNodes();
        for (Node node : nodes) {
            if (identifier.equals(node.getIdentifier())) {
                return node;
            }
        }
        return null;
    }

    public TextNode getTextNode(Page page, String identifier) {
        Node node = getNode(page, identifier);
        if (node == null) {
            storage.begin();
            node = new Node();
            node.setIdentifier(identifier);
            node.setPage(page);
            storage.persist(node);

            TextNode textNode = new TextNode();
            textNode.setAuthor(null);
            textNode.setCreated(new Date());
            textNode.setNode(node);
            textNode.setSource("{ Dette er en ny tekstnode som heter *" + identifier + "*. }");
            node.getTextNodeVersions().add(textNode);
            storage.persist(textNode);
            storage.commit();
        }

        List<TextNode> textNodes = node.getTextNodeVersions();
        if (textNodes.isEmpty()) {
            throw new RuntimeException("Database inconsistency! No text node for node identifier " + identifier);
        }

        return textNodes.get(0);
    }

    public String formatDate(Date date) {
        return new SimpleDateFormat("d. MMMM, yyyy").format(date);
    }

    public String getLastAuthorName(Page page) {
        if (page.getLastAuthor() == null) {
            return "Redaksjonen";
        } else {
            return "User ID: " + page.getLastAuthor().getId(); // TODO: Return real name of user.
        }
    }

    private static String sqlEscape(String str) {
        return str;
    }

    public boolean editTextNode(Page page, String identifier, String textNodeIdEditOn, User author, String sourceCode) {
        TextNode newestTextNode = getTextNode(page, identifier);

        if (textNodeIdEditOn != null) {
            if (!newestTextNode.getId().equals(Long.parseLong(textNodeIdEditOn))) {
                return false;
            }
        }

        storage.begin();
        TextNode textNode = new TextNode();
        textNode.setCreated(new Date());
        textNode.setAuthor(author != null ? author : newestTextNode.getAuthor());
        textNode.setNode(newestTextNode.getNode());
        textNode.setSource(sourceCode);
        storage.persist(textNode);
        page.setLastAuthor(author != null ? author : newestTextNode.getAuthor());
        page.setUpdated(new Date());
        storage.persist(page);
        storage.commit();
        log.info("TextNode saved: urlName="+page.getUrlName()+", identifier="+identifier+", author="+author);
        return true;
    }

}
