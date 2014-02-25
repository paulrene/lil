package no.leinstrandil.service;

import no.leinstrandil.database.model.User;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Set;
import javax.persistence.NoResultException;
import no.leinstrandil.database.Storage;
import no.leinstrandil.database.model.Node;
import no.leinstrandil.database.model.Page;
import no.leinstrandil.database.model.TextNode;
import no.leinstrandil.textile.Textile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PageService {
    private static final Logger log = LoggerFactory.getLogger(PageService.class);

    private Storage storage;
    private Textile textile;

    public PageService(Storage storage) {
        this.storage = storage;
        this.textile = new Textile();
    }

    public Page getPageByUrlName(String urlName) {
        try {
            return storage.createSingleQuery("from Page p where p.urlName = '" + sqlEscape(urlName) + "'", Page.class);
        } catch (NoResultException e) {
            log.info("Could not find a Page with urlName: " + urlName);
            return null;
        }
    }

    public String renderText(String source) {
        return textile.process(source);
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
            return null;
        }
        List<TextNode> textNodes = node.getTextNodeVersions();
        if (textNodes.size() > 0) {
            return textNodes.get(0);
        }

        Node noNode = new Node();
        noNode.setIdentifier(identifier);
        TextNode textNode = new TextNode();
        textNode.setSource("{Innholdsfeltet *" + identifier + "* er tomt. Klikk her for å skrive en tekst.}");
        textNode.setNode(noNode);
        return textNode;
    }

    public String formatDate(Date date) {
        SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");
        return sdf.format(date);
    }

    private static String sqlEscape(String str) {
        return str;
    }

    public boolean editTextNode(Page page, String identifier, String textNodeIdEditOn, User author, String sourceCode) {
        storage.begin();

        Node node = getNode(page, identifier);
        if (node == null) {
            node = new Node();
            node.setIdentifier(identifier);
            node.setPage(page);
            storage.begin();
            storage.persist(node);
            storage.commit();
        }

        TextNode newestTextNode = getTextNode(page, identifier);

        if (textNodeIdEditOn != null) {
            if (!newestTextNode.getId().equals(Long.parseLong(textNodeIdEditOn))) {
                storage.rollback();
                return false;
            }
        }

        TextNode textNode = new TextNode();
        textNode.setCreated(new Date());
        textNode.setAuthor(author != null ? author : newestTextNode.getAuthor());
        textNode.setNode(newestTextNode != null ? newestTextNode.getNode() : node);
        textNode.setSource(sourceCode);
        storage.persist(textNode);
        storage.commit();
        return true;
    }

}
