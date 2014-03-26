package no.leinstrandil.service;

import no.leinstrandil.database.model.web.FacebookEvent;
import no.leinstrandil.database.model.web.FacebookPage;
import no.leinstrandil.database.model.web.FacebookPost;

import org.joda.time.DateTime;
import facebook4j.Event;
import facebook4j.Facebook;
import facebook4j.FacebookException;
import facebook4j.FacebookFactory;
import facebook4j.Post;
import facebook4j.Reading;
import facebook4j.ResponseList;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import javax.persistence.TypedQuery;
import no.leinstrandil.database.Storage;
import org.joda.time.DateTimeUtils;

public class FacebookService {

    private Storage storage;
    private StockPhotoService stockPhotoService;

    public FacebookService(Storage storage, StockPhotoService stockPhotoService) {
        this.stockPhotoService = stockPhotoService;
        this.storage = storage;
    }

    public String getSmallPictureUrl(FacebookPost post) {
        String url = post.getPictureUrl();
        if (url == null) {
            return stockPhotoService.getStockPhoto(getBody(post));
        }
        return url;
    }

    public String getMediumPictureUrl(FacebookPost post) {
        String url = post.getPictureUrl();
        if (url == null) {
            return stockPhotoService.getStockPhoto(getBody(post));
        }
        if (!url.endsWith("_s.jpg")) {
            return url;
        }
        return url.replace("_s.jpg", "_o.jpg");
    }

    public String getOrginalPictureUrl(FacebookPost post) {
        String url = post.getPictureUrl();
        if (url == null) {
            return stockPhotoService.getStockPhoto(getBody(post));
        }
        if (!url.endsWith("_s.jpg")) {
            return url;
        }
        return url.replace("_s.jpg", "_o.jpg");
    }

    public String getTitle(FacebookPost post) {
        String[] stopWords = new String[] { "i", "på", "mellom", "over", "under", "av", "bak", "før", "etter", "hos",
                "gjennom", "utenom", "blant", "å", "og", "for", "til", "som", "fra", "da", "når", "opp" };
        String message = getBody(post);
        if (message.length() < 6) {
            return message;
        }
        String[] word = message.split("\\s");
        StringBuilder title = new StringBuilder();
        if (word.length > 5) {
            title.append(word[0] + " " + word[1] + " " + word[2] + " " + word[3]);
            if (!isWordInArray(word[4], stopWords)) {
                title.append(" ").append(word[4]);
            }
        } else {
            title.append(message);
        }

        String titleStr = title.toString();
        char lastChar = titleStr.charAt(titleStr.length() - 1);
        if (lastChar == '-' || lastChar == ',' || lastChar == '.') {
            titleStr = titleStr.substring(0, titleStr.length() - 1);
        }

        int index = titleStr.indexOf('!');
        if (index > 0) {
            titleStr = titleStr.substring(0, index + 1);
        }

        return titleStr;
    }

    private static boolean isWordInArray(String word, String[] wordArray) {
        for (String stopWord : wordArray) {
            if (word.equalsIgnoreCase(stopWord)) {
                return true;
            }
        }
        return false;
    }

    public String getShortBody(FacebookPost post) {
        String body = getBody(post);
        if (body.length() > 100) {
            return body.substring(0, 100) + "..";
        }
        return body;
    }

    public String getBody(FacebookPost post) {
        if (post == null) {
            return new String();
        }

        String body = post.getMessage();
        if (body == null && post.getStory() != null) {
            return post.getStory();
        }
        if (body == null) {
            body = new String();
        }
        return body;
    }

    public String getPublished(FacebookPost post) {
        return new SimpleDateFormat("d. MMMM, yyyy").format(post.getFacebookCreated());
    }

    public boolean isEventEnded(FacebookEvent event) {
        if (event.getEndTime() != null) {
            return new Date().after(event.getEndTime());
        } else {
            return new Date().after(event.getStartTime());
        }
    }

    public String getEventTime(FacebookEvent event) {
        StringBuilder time = new StringBuilder();
        time.append(new SimpleDateFormat("EEEE (d.M.yy) HH:mm").format(event.getStartTime()));
        if (event.getEndTime() != null) {
            time.append(" - ");
            time.append(new SimpleDateFormat("HH:mm").format(event.getEndTime()));
        }
        return time.toString();
    }

    public String getAuthor(FacebookPost post) {
        if (post.getFacebookPage() == null) {
            return "Redaksjonen";
        }
        return post.getFacebookPage().getFacebookPageName();
    }

    public String getLinkUrl(FacebookPost post) {
        return post.getLinkUrl() != null ? post.getLinkUrl() : "#";
    }

    public boolean hasPicture(FacebookPost post) {
        return post.getPictureUrl() != null;
    }

    public FacebookPage getFacebookPageByPageId(String pageIdentifier) {
        return storage.createSingleQuery("from FacebookPage p where p.facebookPageIdentifier = '" + pageIdentifier
                + "'", FacebookPage.class);
    }

    public List<FacebookEvent> getFBFutureEvents(FacebookPage facebookPage) {
        Date yesterday = new DateTime().minusDays(1).toDate();
        TypedQuery<FacebookEvent> query = storage.createQuery("from FacebookEvent e where e.facebookPage.id = "
                + facebookPage.getId() + " and e.startTime > :dt order by e.startTime", FacebookEvent.class);
        query.setParameter("dt", yesterday);
        return query.getResultList();
    }

    public List<FacebookPost> getFBPhotos(FacebookPage facebookPage, int maxResults) {
        TypedQuery<FacebookPost> query = storage.createQuery("from FacebookPost p where p.facebookPage.id = "
                + facebookPage.getId() + " and p.facebookType = 'photo' order by p.facebookCreated desc",
                FacebookPost.class);
        query.setMaxResults(maxResults);
        return query.getResultList();
    }

    public List<FacebookPost> getFBStatus(FacebookPage facebookPage, int maxResults) {
        TypedQuery<FacebookPost> query = storage.createQuery("from FacebookPost p where p.facebookPage.id = "
                + facebookPage.getId() + " and p.facebookType = 'status' order by p.facebookCreated desc",
                FacebookPost.class);
        query.setMaxResults(maxResults);
        return query.getResultList();
    }

    public List<FacebookPost> getFBLinks(FacebookPage facebookPage, int maxResults) {
        TypedQuery<FacebookPost> query = storage.createQuery("from FacebookPost p where p.facebookPage.id = "
                + facebookPage.getId() + " and p.facebookType = 'link' order by p.facebookCreated desc",
                FacebookPost.class);
        query.setMaxResults(maxResults);
        return query.getResultList();
    }

    public List<FacebookPost> getFacebookNews(FacebookPage facebookPage, int maxResults) {
        TypedQuery<FacebookPost> query = storage.createQuery("from FacebookPost p where p.facebookPage.id = "
                + facebookPage.getId() + " and (p.facebookType = 'status' or p.facebookType = 'photo') "
                + "and p.message is not null order by p.facebookCreated desc", FacebookPost.class);
        query.setMaxResults(maxResults);
        return query.getResultList();
    }

    private void syncFacebookPage(FacebookPage facebookPage) throws FacebookException {
        Facebook facebook = getFacebook(facebookPage.getAppId(), facebookPage.getAppSecret());
        Reading reading = new Reading().limit(300);
        if (facebookPage.getLastSync() != null) {
            reading.since(facebookPage.getLastSync());
        }

        facebookPage.setLastSync(new Date());

        syncPosts(facebookPage, facebook, reading);
        syncEvents(facebookPage, facebook, reading);

        storage.begin();
        storage.persist(facebookPage);
        storage.commit();
    }

    private void syncEvents(FacebookPage facebookPage, Facebook facebook, Reading reading) throws FacebookException {
        ResponseList<Event> events = facebook.getEvents(facebookPage.getFacebookPageIdentifier(), reading);

        for (Event event : events) {
            TypedQuery<FacebookEvent> query = storage.createQuery(
                    "from FacebookEvent e where e.facebookEventId = '" + event.getId() + "'", FacebookEvent.class);
            if (query.getResultList().isEmpty()) {
                FacebookEvent fe = new FacebookEvent();
                fe.setFacebookPage(facebookPage);
                fe.setName(event.getName());
                fe.setStartTime(event.getStartTime());
                fe.setEndTime(event.getEndTime());
                fe.setDescription(event.getDescription());
                fe.setLocation(event.getLocation());
                fe.setFacebookUpdated(event.getUpdatedTime());
                fe.setFacebookEventId(event.getId());
                fe.setCreated(new Date());
                storage.begin();
                storage.persist(fe);
                storage.commit();
            }
        }
    }

    private void syncPosts(FacebookPage facebookPage, Facebook facebook, Reading reading) throws FacebookException {
        ResponseList<Post> posts = facebook.getPosts(facebookPage.getFacebookPageIdentifier(), reading);

        for (Post post : posts) {
            FacebookPost fp = null;
            if ("photo".equals(post.getType()) && "added_photos".equals(post.getStatusType())) {
                fp = new FacebookPost();
            } else if ("status".equals(post.getType()) && "mobile_status_update".equals(post.getStatusType())) {
                fp = new FacebookPost();
            } else if ("link".equals(post.getType()) && "shared_story".equals(post.getStatusType())) {
                fp = new FacebookPost();
            }

            if (fp != null) {
                fp.setFacebookPage(facebookPage);
                fp.setPictureUrl(urlToString(post.getPicture()));
                fp.setLinkUrl(urlToString(post.getLink()));
                fp.setMessage(post.getMessage());
                fp.setStory(post.getStory());
                fp.setCaption(post.getCaption());
                fp.setDescription(post.getDescription());
                fp.setFacebookType(post.getType());
                fp.setFacebookCreated(post.getCreatedTime());
                fp.setFacebookUpdated(post.getUpdatedTime());
                fp.setCreated(new Date());
                storage.begin();
                storage.persist(fp);
                storage.commit();
            }
        }
    }

    public void syncFacebookPagePosts() {
        TypedQuery<FacebookPage> query = storage.createQuery("from FacebookPage", FacebookPage.class);
        List<FacebookPage> facebookPageList = query.getResultList();
        for (FacebookPage facebookPage : facebookPageList) {
            Date lastSync = facebookPage.getLastSync();
            Long syncIntervalMs = facebookPage.getSyncInterval() * 1000L;
            if (lastSync == null || lastSync.getTime() < (DateTimeUtils.currentTimeMillis() - syncIntervalMs)) {
                try {
                    syncFacebookPage(facebookPage);
                } catch (FacebookException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private Facebook getFacebook(String appId, String appSecret) throws FacebookException {
        Facebook facebook = new FacebookFactory().getInstance();
        facebook.setOAuthAppId(appId, appSecret);
        facebook.setOAuthAccessToken(facebook.getOAuthAppAccessToken());
        return facebook;
    }

    private static String urlToString(URL url) {
        if (url == null) {
            return null;
        }
        return url.toString();
    }

    public static void main(String[] args) {
        FacebookService fs = new FacebookService(new Storage(), null);
        fs.syncFacebookPagePosts();
    }

}
