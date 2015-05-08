package no.leinstrandil.service;

import java.net.MalformedURLException;

import java.util.ArrayList;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.persistence.NoResultException;
import javax.persistence.TypedQuery;
import no.leinstrandil.database.Storage;
import no.leinstrandil.database.model.web.FacebookEvent;
import no.leinstrandil.database.model.web.FacebookPage;
import no.leinstrandil.database.model.web.FacebookPost;
import org.joda.time.DateTime;
import org.joda.time.DateTimeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FacebookService {
    private static final Logger log = LoggerFactory.getLogger(FacebookService.class);

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
                "gjennom", "utenom", "blant", "å", "og", "for", "til", "som", "fra", "da", "når", "opp", "er" };
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
        if (lastChar == '-' || lastChar == ',' || lastChar == '.' || lastChar == ':') {
            titleStr = titleStr.substring(0, titleStr.length() - 1);
        }

        int index = titleStr.indexOf('!');
        if (index > 0) {
            titleStr = titleStr.substring(0, index + 1);
        }
        index = titleStr.indexOf('?');
        if (index > 0) {
            titleStr = titleStr.substring(0, index + 1);
        }
        index = titleStr.indexOf('.');
        if (index > 4) {
//            if (!Character.isDigit(titleStr.charAt(index - 1))) {
                titleStr = titleStr.substring(0, index);
//            }
        }
        index = titleStr.indexOf(',');
        if (index > 0) {
            titleStr = titleStr.substring(0, index);
        }
        index = titleStr.indexOf(':');
        if (index > 0) {
            titleStr = titleStr.substring(0, index);
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
        if (body == null) {
            body = post.getStory();
        }
        if (body == null) {
            body = new String();
        }

        StringBuffer newBody = new StringBuffer();
        String[] bodyParts = body.split("\\s");
        for(String bodyPart : bodyParts) {
            try {
                URL url = new URL(bodyPart);
                newBody.append("<a target=\"_blank\" href=\"");
                newBody.append(url.toString());
                newBody.append("\">");
                newBody.append(url.toString());
                newBody.append("</a>");
            } catch (MalformedURLException e) {
                newBody.append(bodyPart);
            }
            if (newBody.length() > 400 && post.getLinkUrl() !=null) {
                newBody.append("... ");
                newBody.append("<a target=\"_blank\" href=\"");
                newBody.append(post.getLinkUrl());
                newBody.append("\"><strong class=\"color-green\">Les Mer</strong></a>");
                break;
            } else {
                newBody.append(" ");
            }
        }

        return newBody.toString();
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
        DateTime startTime = new DateTime(event.getStartTime());
        if (DateTime.now().getYear() == startTime.getYear()) {
            time.append(new SimpleDateFormat("EEEE (d. MMM) HH:mm").format(event.getStartTime()));
        } else {
            time.append(new SimpleDateFormat("EEEE (d.M.yyyy) HH:mm").format(event.getStartTime()));
        }
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
                + facebookPage.getId() + " and (p.facebookType = 'status' or p.facebookType = 'photo' or p.facebookType = 'video' or p.facebookType='link') "
                + "and p.message is not null order by p.facebookCreated desc", FacebookPost.class);
        query.setMaxResults(maxResults);
        return query.getResultList();
    }

    private void syncFacebookPage(FacebookPage facebookPage) throws FacebookException {
        Facebook facebook = getFacebook(facebookPage.getAppId(), facebookPage.getAppSecret());
        Reading postReading = new Reading().limit(50);
/*        if (facebookPage.getLastSync() != null) {
            reading.since(facebookPage.getLastSync());
        }*/

//        facebookPage.setLastSync(new Date());

        syncPosts(facebookPage, facebook, postReading);

        Reading eventReading = new Reading();
        syncEvents(facebookPage, facebook, eventReading);

        storage.begin();
        storage.persist(facebookPage);
        storage.commit();
    }

    public static String getYoutubeVideoId(String youtubeUrl) {
        String videoId = null;
        if (youtubeUrl != null && youtubeUrl.trim().length() > 0 && youtubeUrl.startsWith("http")) {
            String expression = "^.*((youtu.be"+ "\\/)"
                    + "|(v\\/)|(\\/u\\/w\\/)|(embed\\/)|(watch\\?))\\??v?=?([^#\\&\\?]*).*";
            CharSequence input = youtubeUrl;
            Pattern pattern = Pattern.compile(expression,Pattern.CASE_INSENSITIVE);
            Matcher matcher = pattern.matcher(input);
            if (matcher.matches()) {
                String groupIndex1 = matcher.group(7);
                if (groupIndex1!=null && groupIndex1.length()==11)
                    videoId = groupIndex1;
            }
        }
        return videoId;
    }

    public static List<String> getYoutubeUrls(String text) {
        List<String> list = new ArrayList<String>();
        if (text == null) {
            return list;
        }
        String pattern = "https?:\\/\\/(?:[0-9A-Z-]+\\.)?(?:youtu\\.be\\/|youtube\\.com\\S*[^\\w\\-\\s])"
                +"([\\w\\-]{11})(?=[^\\w\\-]|$)(?![?=&+%\\w]*(?:['\"][^<>]*>|<\\/a>))[?=&+%\\w]*";
        Pattern compiledPattern = Pattern.compile(pattern, Pattern.CASE_INSENSITIVE);
        Matcher matcher = compiledPattern.matcher(text);
        while(matcher.find()) {
            list.add(matcher.group());
        }
        return list;
    }

    private void syncPosts(FacebookPage facebookPage, Facebook facebook, Reading reading) throws FacebookException {
        ResponseList<Post> posts = facebook.getPosts(facebookPage.getFacebookPageIdentifier(), reading);

        for (Post post : posts) {

/*            System.out.println(post.getMessage());
            System.out.println(post.getType());
            System.out.println(post.getStatusType());
            System.out.println("--------------------------");*/

            FacebookPost newPost = null;
            if ("photo".equals(post.getType()) && "added_photos".equals(post.getStatusType())) {
                newPost = new FacebookPost();
            } else if ("status".equals(post.getType()) && "mobile_status_update".equals(post.getStatusType())) {
                newPost = new FacebookPost();
            } else if ("link".equals(post.getType()) && "shared_story".equals(post.getStatusType())) {
                newPost = new FacebookPost();
            } else if ("video".equals(post.getType()) && "shared_story".equals(post.getStatusType())) {
                newPost = new FacebookPost();
            }

            if (newPost != null) {
                FacebookPost existingPost = getFacebookPostByFacebookPostId(post.getId());
                if (existingPost != null) {
                    existingPost.setFacebookPage(facebookPage);
                    existingPost.setFacebookPostId(post.getId());
                    existingPost.setPictureUrl(urlToString(post.getPicture()));
                    existingPost.setLinkUrl(urlToString(post.getLink()));
                    existingPost.setMessage(post.getMessage());
                    existingPost.setStory(post.getStory());
                    existingPost.setCaption(post.getCaption());
                    existingPost.setDescription(post.getDescription());
                    existingPost.setFacebookUpdated(post.getUpdatedTime());
                    if ("photo".equals(post.getType())) {
                        existingPost.setPictureUrl(facebook.getPhotoURL(post.getObjectId()).toString());                    }
                    if ("video".equals(post.getType())) {
                        List<String> utUrlList = getYoutubeUrls(post.getMessage());
                        for(String utUrl : utUrlList) {
                            String utId = getYoutubeVideoId(utUrl);
                            if (utId != null) {
                                StringBuilder picUrl = new StringBuilder("http://i.ytimg.com/vi/");
                                picUrl.append(utId);
                                picUrl.append("/maxresdefault.jpg");
                                existingPost.setPictureUrl(picUrl.toString());
                            }
                        }
                    }
                    log.info("Detecting existing FB " + post.getType() + " with message: " + post.getMessage());
                } else {
                    newPost.setFacebookPage(facebookPage);
                    newPost.setFacebookPostId(post.getId());
                    newPost.setPictureUrl(urlToString(post.getPicture()));
                    newPost.setLinkUrl(urlToString(post.getLink()));
                    newPost.setMessage(post.getMessage());
                    newPost.setStory(post.getStory());
                    newPost.setCaption(post.getCaption());
                    newPost.setDescription(post.getDescription());
                    newPost.setFacebookType(post.getType());
                    newPost.setFacebookCreated(post.getCreatedTime());
                    newPost.setFacebookUpdated(post.getUpdatedTime());
                    newPost.setCreated(new Date());
                    if ("photo".equals(post.getType())) {
                        newPost.setPictureUrl(facebook.getPhotoURL(post.getObjectId()).toString());
                    }
                    if ("video".equals(post.getType())) {
                        List<String> utUrlList = getYoutubeUrls(post.getMessage());
                        for(String utUrl : utUrlList) {
                            String utId = getYoutubeVideoId(utUrl);
                            if (utId != null) {
                                StringBuilder picUrl = new StringBuilder("http://i.ytimg.com/vi/");
                                picUrl.append(utId);
                                picUrl.append("/maxresdefault.jpg");
                                newPost.setPictureUrl(picUrl.toString());
                            }
                        }
                    }
                    storage.begin();
                    storage.persist(newPost);
                    storage.commit();
                    log.info("Detecting new FB " + post.getType() + " with message: " + post.getMessage());
                }
            }
        }
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

    public FacebookPost getFacebookPostByFacebookPostId(String facebookPostId) {
        try {
            return storage.createSingleQuery(
                    "from FacebookPost p where p.facebookPostId = '" + facebookPostId + "'", FacebookPost.class);
        } catch (NoResultException e) {
            return null;
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
