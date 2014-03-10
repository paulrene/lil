package no.leinstrandil.service;

import facebook4j.Facebook;
import facebook4j.FacebookException;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import javax.persistence.NoResultException;
import no.leinstrandil.database.Storage;
import no.leinstrandil.database.model.person.Principal;
import no.leinstrandil.database.model.web.User;
import org.slf4j.Logger;

public class UserService {

    private final Logger log = org.slf4j.LoggerFactory.getLogger(UserService.class);

    private Storage storage;

    public UserService(Storage storage) {
        this.storage = storage;
    }

    public User ensureFacebookUser(Facebook facebook) throws FacebookException {
        facebook4j.User fbUser = facebook.getMe();
        User user = getUserByFacebookId(fbUser.getId());
        if (user != null) {
            // TODO: Update user object?
            return user;
        }
        log.info("Creating new facebook user named: " + fbUser.getName());
        return createFacebookUser(fbUser, facebook.getPictureURL());
    }

    private User createFacebookUser(facebook4j.User fbUser, URL facebookPictureUrl) {
        User newUser = new User();
        newUser.setFacebookId(fbUser.getId());
        newUser.setUsername(fbUser.getUsername());
        Principal principal = new Principal();
        principal.setName(fbUser.getName());
        principal.setFirstName(fbUser.getFirstName());
        principal.setMiddleName(fbUser.getMiddleName());
        principal.setLastName(fbUser.getLastName());
        principal.setGender(fbUser.getGender());
        principal.setPictureUrl(facebookPictureUrl.toString());
        principal.setCreated(new Date());
        principal.setUpdated(new Date());
        String birthDateStr = fbUser.getBirthday();
        if (birthDateStr != null && !birthDateStr.isEmpty()) {
            try {
                principal.setBirthDate(new SimpleDateFormat("MM/dd/yyyy").parse(birthDateStr));
            } catch (ParseException e) {
                log.info("Could not parse facebook birthdate for user " + fbUser.getName() + " : "
                        + fbUser.getBirthday());
                principal.setBirthDate(null);
            }
        }
        principal.setUser(newUser);
        newUser.setPrincipal(principal);
        storage.begin();
        storage.persist(newUser);
        storage.persist(principal);
        storage.commit();
        return newUser;
    }

    private User getUserByFacebookId(String id) {
        try {
            return storage.createSingleQuery("from User where facebookId = '" + id + "'", User.class);
        } catch (NoResultException e) {
            log.debug("Could not find local user with facebook id: " + id);
            return null;
        }
    }

}
