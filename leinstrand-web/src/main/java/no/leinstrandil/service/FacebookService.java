package no.leinstrandil.service;

import facebook4j.Facebook;
import facebook4j.FacebookException;
import facebook4j.FacebookFactory;
import facebook4j.Post;
import facebook4j.PrivacyType;
import facebook4j.Reading;
import facebook4j.ResponseList;
import facebook4j.auth.AccessToken;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import javax.persistence.TypedQuery;
import no.leinstrandil.database.Storage;
import no.leinstrandil.database.model.FacebookPage;
import no.leinstrandil.database.model.FacebookPost;
import org.joda.time.DateTimeUtils;

public class FacebookService {

    private Storage storage;

    public FacebookService(Storage storage) {
        this.storage = storage;
    }

    public String getSmallPictureUrl(FacebookPost post) {
        return post.getPictureUrl();
    }

    public String getMediumPictureUrl(FacebookPost post) {
        String url = post.getPictureUrl();
        if (url == null) {
            return null;
        }
        if (!url.endsWith("_s.jpg")) {
            return url;
        }
        return url.replace("_s.jpg", "_o.jpg");
    }

    public String getOrginalPictureUrl(FacebookPost post) {
        String url = post.getPictureUrl();
        if (url == null) {
            return null;
        }
        if (!url.endsWith("_s.jpg")) {
            return url;
        }
        return url.replace("_s.jpg", "_o.jpg");
    }

    public String getTitle(FacebookPost post) {
        String message = post.getMessage();
        if (message == null && post.getStory() != null) {
            return post.getStory();
        }
        if (message == null) {
            return new String();
        }
        String[] word = message.split(" ");
        if (word.length > 5) {
            return word[0] + " " + word[1] + " " + word[2] + " " + word[3] + " " + word[4] + "..";
        } else {
            return message;
        }
    }

    public String getShortBody(FacebookPost post) {
        String body = getBody(post);
        if (body.length() > 100) {
            return body.substring(0, 100) + "..";
        }
        return body;
    }

    public String getBody(FacebookPost post) {
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

    public String getAuthor(FacebookPost post) {
        return post.getFacebookPage().getFacebookPageName();
    }

    public String getLinkUrl(FacebookPost post) {
        return post.getLinkUrl() != null ? post.getLinkUrl() : "#";
    }

    public boolean hasPicture(FacebookPost post) {
        return post.getPictureUrl() != null;
    }

    public FacebookPage getFacebookPageByPageId(String pageIdentifier) {
        return storage.createSingleQuery("from FacebookPage p where p.facebookPageIdentifier = '"
                + pageIdentifier + "'", FacebookPage.class);
    }

    public List<FacebookPost> getFBPhotos(FacebookPage facebookPage) {
        TypedQuery<FacebookPost> query = storage.createQuery("from FacebookPost p where p.facebookPage.id = "
                + facebookPage.getId() + " and p.facebookType = 'photo' order by p.facebookCreated desc",
                FacebookPost.class);
        query.setMaxResults(20);
        return query.getResultList();
    }

    public List<FacebookPost> getFBStatus(FacebookPage facebookPage) {
        TypedQuery<FacebookPost> query = storage.createQuery("from FacebookPost p where p.facebookPage.id = "
                + facebookPage.getId() + " and p.facebookType = 'status' order by p.facebookCreated desc",
                FacebookPost.class);
        query.setMaxResults(20);
        return query.getResultList();
    }

    public List<FacebookPost> getFBLinks(FacebookPage facebookPage) {
        TypedQuery<FacebookPost> query = storage.createQuery("from FacebookPost p where p.facebookPage.id = "
                + facebookPage.getId() + " and p.facebookType = 'link' order by p.facebookCreated desc",
                FacebookPost.class);
        query.setMaxResults(20);
        return query.getResultList();
    }

    public List<FacebookPost> getFacebookNews(FacebookPage facebookPage) {
        TypedQuery<FacebookPost> query = storage.createQuery("from FacebookPost p where p.facebookPage.id = "
                + facebookPage.getId() + " and (p.facebookType = 'status' or p.facebookType = 'photo') "
                +"and p.message is not null order by p.facebookCreated desc", FacebookPost.class);
        query.setMaxResults(20);
        return query.getResultList();
    }

    private void syncFacebookPage(FacebookPage facebookPage) throws FacebookException {
        Facebook facebook = getFacebook(facebookPage.getAccessToken());
        Reading reading = new Reading().limit(300);
        if (facebookPage.getLastSync() != null) {
            reading.since(facebookPage.getLastSync());
        }

        facebookPage.setLastSync(new Date());
        ResponseList<Post> posts = facebook.getPosts(facebookPage.getFacebookPageIdentifier(), reading);

        for (Post post : posts) {
            if (post.getPrivacy().getValue() != PrivacyType.EVERYONE) {
                continue;
            }

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

        storage.begin();
        storage.persist(facebookPage);
        storage.commit();
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

    private Facebook getFacebook(String accessTokenString) {
        Facebook facebook = new FacebookFactory().getInstance();
        facebook.setOAuthAppId("", "");
        AccessToken at = new AccessToken(accessTokenString);
        facebook.setOAuthAccessToken(at);
        return facebook;
    }

    private static String urlToString(URL url) {
        if (url == null) {
            return null;
        }
        return url.toString();
    }

}