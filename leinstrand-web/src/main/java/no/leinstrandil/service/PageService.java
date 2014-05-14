package no.leinstrandil.service;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import javax.persistence.NoResultException;
import no.leinstrandil.database.Storage;
import no.leinstrandil.database.model.person.Principal;
import no.leinstrandil.database.model.web.MenuEntry;
import no.leinstrandil.database.model.web.Node;
import no.leinstrandil.database.model.web.Page;
import no.leinstrandil.database.model.web.TextNode;
import no.leinstrandil.database.model.web.User;
import org.joda.time.DateTime;
import org.json.JSONArray;
import org.ocpsoft.prettytime.PrettyTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PageService {
    private static final Logger log = LoggerFactory.getLogger(PageService.class);

    private Storage storage;

    public PageService(Storage storage) {
        this.storage = storage;
    }

    private Page getPageById(Long pageId) {
        try {
            return storage.createSingleQuery("from Page p where p.id = " + pageId, Page.class);
        } catch (NoResultException e) {
            log.info("Could not find a Page with id: " + pageId);
            return null;
        }
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

    public String formatDateTime(Date date) {
        return new SimpleDateFormat("dd.MM.yyyy, HH:mm:ss").format(date);
    }

    public String formatDateTimeShort(Date date) {
        return new SimpleDateFormat("d. MMM, HH:mm").format(date);
    }

    public String formatYear(Date date) {
        return new SimpleDateFormat("yyyy").format(date);
    }

    public String prettyTimeForInvitationExpiry(Date created) {
        Date expiryDate = new DateTime(created).plusDays(UserService.FAMILY_INVITAION_EXPIRY_DAYS).toDate();
        return new PrettyTime(new Date(), Locale.forLanguageTag("no")).format(expiryDate);
    }

    public String prettyTime(Date reference, Date then) {
        String str = new PrettyTime(reference, Locale.forLanguageTag("no")).format(then);
        if (str.endsWith(" siden")) {
            return str.substring(0, str.length() - " siden".length());
        }
        return str;
    }

    public String prettyTime(Date then) {
        return new PrettyTime(new Date(), Locale.forLanguageTag("no")).format(then);
    }

    public String getAuthors(Page page) {
        if (page.getLastAuthor() == null) {
            return "Redaksjonen";
        } else {
            Principal principal = page.getLastAuthor().getPrincipal();
            if (principal == null) {
                return page.getLastAuthor().getUsername();
            } else {
                return principal.getName();
            }
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
        textNode.setAuthor(author);
        textNode.setNode(newestTextNode.getNode());
        textNode.setSource(sourceCode);
        storage.persist(textNode);
        page.setLastAuthor(author);
        page.setUpdated(new Date());
        storage.persist(page);
        storage.commit();
        log.info("TextNode saved: urlName="+page.getUrlName()+", identifier="+identifier+", author="+author);
        return true;
    }

    public List<Page> getFavoritesForPage(Page page) {
        List<Page> favList = new ArrayList<>();
        String favJsonStr = page.getFavoritePages();
        if (favJsonStr == null) {
            return favList;
        }
        JSONArray favArray = new JSONArray(favJsonStr);
        for (int n=0;n<favArray.length();n++) {
            Long pageId = favArray.getLong(n);
            Page favPage = getPageById(pageId);
            if (favPage != null) {
                favList.add(favPage);
            }
        }
        return favList;
    }

    public List<Page> getBreadCrumbs(Page thisPage) {
        List<Page> list = new ArrayList<>();

        Set<MenuEntry> menuEntriesForThisPage = thisPage.getMenuEntries();
        if(menuEntriesForThisPage == null || menuEntriesForThisPage.isEmpty()) {
            list.add(thisPage);
            return list;
        }

        MenuEntry menuEntry = thisPage.getMenuEntries().iterator().next();
        while(menuEntry != null) {
            if (menuEntry.getPage() != null) {
                list.add(menuEntry.getPage());
            }
            menuEntry  = menuEntry.getParent();
        }
        Collections.reverse(list);
        return list;
    }

}
