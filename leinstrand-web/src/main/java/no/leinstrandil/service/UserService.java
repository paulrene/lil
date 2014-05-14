package no.leinstrandil.service;

import facebook4j.Facebook;
import facebook4j.FacebookException;
import facebook4j.PictureSize;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import javax.persistence.NoResultException;
import javax.persistence.TypedQuery;
import no.leinstrandil.database.Storage;
import no.leinstrandil.database.model.person.Address;
import no.leinstrandil.database.model.person.EmailAddress;
import no.leinstrandil.database.model.person.Family;
import no.leinstrandil.database.model.person.FamilyInvitation;
import no.leinstrandil.database.model.person.MobileNumber;
import no.leinstrandil.database.model.person.Principal;
import no.leinstrandil.database.model.web.Role;
import no.leinstrandil.database.model.web.User;
import org.joda.time.DateTime;
import org.joda.time.LocalDate;
import org.joda.time.Minutes;
import org.joda.time.Years;
import org.slf4j.Logger;
import spark.Request;

public class UserService {
    private final Logger log = org.slf4j.LoggerFactory.getLogger(UserService.class);
    public static final int FAMILY_INVITAION_EXPIRY_DAYS = 3;

    private Storage storage;
    private MailService mailService;

    public UserService(Storage storage, MailService mailService) {
        this.storage = storage;
        this.mailService = mailService;
    }

    public User ensureFacebookUser(Facebook facebook) throws FacebookException {
        facebook4j.User fbUser = facebook.getMe();
        String fbPictureUrl = facebook.getPictureURL(PictureSize.square).toString();
        User user = getUserByFacebookId(fbUser.getId());
        if (user != null) {
            // Update Facebook profile picture if it has changed.
            Principal principal = user.getPrincipal();
            if (!principal.getPictureUrl().equals(fbPictureUrl)) {
                principal.setPictureUrl(fbPictureUrl);
                storage.begin();
                storage.persist(principal);
                storage.commit();
                log.info("Found new Facebook profile picture for user: " + user.getUsername());
            }
            // TODO: Update user object?
            log.info("Known user " + user.getId() + ":" + user.getUsername() + " found.");
            return user;
        }
        log.info("Creating new facebook user named: " + fbUser.getName());
        return createFacebookUser(fbUser, fbPictureUrl);
    }

    private User createFacebookUser(facebook4j.User fbUser, String facebookPictureUrl) {
        User newUser = new User();
        newUser.setFacebookId(fbUser.getId());
        newUser.setUsername(fbUser.getUsername());
        newUser.setCreated(new Date());
        Principal principal = new Principal();
        principal.setName(fbUser.getName());
        principal.setFirstName(fbUser.getFirstName());
        principal.setMiddleName(fbUser.getMiddleName());
        principal.setLastName(fbUser.getLastName());
        principal.setGender(fbUser.getGender());
        principal.setPictureUrl(facebookPictureUrl);
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

        ensureFamilyForUser(newUser);

        return newUser;
    }

    public ServiceResponse createUser(String username, String name, String email, Date birthDate, String gender, String password) {
        User user = new User();
        user.setUsername(username);
        user.setCreated(new Date());
        try {
            user.setPasswordHash(PasswordHash.createHash(password));
            user.setPasswordHashCreated(new Date());
        } catch (NoSuchAlgorithmException e) {
            log.warn("Could not set password for NEW user due to: " + e.getMessage(), e);
            return new ServiceResponse(false, "Det oppstod en feil da vi skulle sette passord på din nye bruker. Prøv igjen eller kontakt oss om feilen vedvarer.");
        } catch (InvalidKeySpecException e) {
            log.warn("Could not set password for NEW user due to: " + e.getMessage(), e);
            return new ServiceResponse(false, "Det oppstod en feil da vi skulle sette passord på din nye bruker. Prøv igjen eller kontakt oss om feilen vedvarer.");
        }
        Principal principal = new Principal();
        principal.setName(name);
        setPrincipalName(name, principal);
        principal.setGender(gender);
        principal.setPictureUrl(null);
        principal.setCreated(new Date());
        principal.setUpdated(new Date());
        principal.setBirthDate(birthDate);
        user.setPrincipal(principal);

        storage.begin();
        storage.persist(user);
        storage.persist(principal);
        storage.commit();

        if (!updateEmail(user, email).isSuccess()) {
            storage.begin();
            storage.delete(user);
            storage.delete(principal);
            storage.commit();
            return new ServiceResponse(false, "E-postadressen er allerede i bruk. Vennligt bruk en annen.");
        }

        ensureFamilyForUser(user);

        return new ServiceResponse(true, "Din bruker ble opprettet!");
    }

    private User getUserByFacebookId(String facebookId) {
        try {
            return storage.createSingleQuery("from User where facebookId = '" + facebookId + "'", User.class);
        } catch (NoResultException e) {
            log.debug("Could not find local user with facebook id: " + facebookId);
            return null;
        }
    }

    public User getUserById(Long id) {
        try {
            return storage.createSingleQuery("from User where id = " + id, User.class);
        } catch (NoResultException e) {
            log.debug("Could not find user with id: " + id);
            return null;
        }
    }

    public User getUserByUsername(String username) {
        try {
            return storage.createSingleQuery("from User u where u.username = '" + username + "'", User.class);
        } catch (NoResultException e) {
            log.debug("Could not find user with username: " + username);
            return null;
        }
    }

    public Principal getPrincipalByEmail(String email) {
        if (email == null) {
            return null;
        }
        TypedQuery<EmailAddress> query = storage.createQuery("from EmailAddress where email = '" + email.trim() + "' order by created desc", EmailAddress.class);
        List<EmailAddress> emailList = query.getResultList();
        if (emailList.isEmpty()) {
            return null;
        }
        EmailAddress address = emailList.get(0);
        return address.getPrincipal();
    }

    public boolean isValidPassword(User thisUser, String password) {
        try {
            return PasswordHash.validatePassword(password, thisUser.getPasswordHash());
        } catch (NoSuchAlgorithmException e) {
            log.error("Could not validate password", e);
        } catch (InvalidKeySpecException e) {
            log.error("Could not validate password", e);
        }
        return false;
    }

    public boolean hasRole(User user, String roleIdentifier) {
        if (user == null) {
            return false;
        }
        if (roleIdentifier == null) {
            return false;
        }
        for (Role role : user.getRoles()) {
            if (roleIdentifier.equals(role.getIdentifier())) {
                return true;
            }
        }
        return false;
    }

    public boolean hasEditorRole(User user) {
        return hasRole(user, "editor");
    }

    public User getLoggedInUserFromSession(Request request) {
        User user = null;
        Long userId = (Long) request.session().attribute("userId");
        if (userId != null) {
            user = getUserById(userId);
        }
        return user;
    }

    public String toDatePickerValue(Date date) {
        if (date == null) {
            return "";
        }
        return new SimpleDateFormat("yyyy-MM-dd").format(date);
    }

    public String formatBirthDate(Date date) {
        if (date == null) {
            return "";
        }
        return new SimpleDateFormat("dd. MMMM, yyyy").format(date);
    }

    public int getAge(Principal principal) {
        LocalDate birthDate = new LocalDate(principal.getBirthDate());
        LocalDate now = new LocalDate();
        return Years.yearsBetween(birthDate, now).getYears();
    }

    public int getAgeThatYearOnTime(Principal principal, Date startTime) {
        LocalDate birthDate = new LocalDate(principal.getBirthDate());
        return new LocalDate(startTime).getYear() - birthDate.getYear();
    }

    public boolean isPrimaryContactCandidate(Principal principal) {
        if (principal.getUser() == null) {
            return false;
        }
        if (principal.getId().equals(principal.getFamily().getPrimaryPrincipal().getId())) {
            return false;
        }
        if (!isOfAge(principal)) {
            return false;
        }
        return true;
    }

    public boolean isOfAge(Principal principal) {
        return getAge(principal) >= 18;
    }

    public boolean isOnlyPrincipal(Principal principal) {
        return principal.getUser() == null;
    }

    public boolean isPrimaryContact(Principal principal) {
        return principal.getId().equals(principal.getFamily().getPrimaryPrincipal().getId());
    }

    public void sendResetPasswordEmail(User user) {
        List<EmailAddress> emailList = user.getPrincipal().getEmailAddressList();
        if (emailList.isEmpty()) {
            log.warn("Could not send password reset email because user " + user.getId()
                    + " does not have any email addresses stored!");
            return;
        }

        String code = generateResetPasswordCode(user);

        StringBuilder text = new StringBuilder();
        text.append("Hei, ").append(user.getPrincipal().getName()).append("!<br><br>");
        text.append("Vi mottok en forespørsel om passordtilbakestilling for Leinstrand IL-kontoen din. ");
        text.append("Bruk lenken nedenfor for å tilbakestille passordet:<br><br>");
        text.append("<strong>Tilbakestill passordet ditt ved hjelp av en nettleser:</strong> ");
        text.append("<a href=\"%baseUrl%page/signin?tab=settpassord&code=").append(code);
        text.append("\">%baseUrl%page/signin?tab=settpassord&code=").append(code).append("</a><br><br>");
        text.append("Om det ikke var deg som bestillte tilbakestilling av ditt passord kan du se bort fra denne e-posten.<br><br>");
        text.append("Med vennlig hilsen,<br>Leinstrand idrettslag.");

        mailService.sendNoReplyHtmlMessage(emailList.get(0).getEmail(), "Passordtilbakestilling for Leinstrand IL", text.toString());
    }

    public String generateResetPasswordCode(User user) {
        user.setResetPasswordCode(UUID.randomUUID().toString());
        user.setResetPasswordCodeCreated(new Date());
        storage.begin();
        storage.persist(user);
        storage.commit();
        return user.getResetPasswordCode();
    }

    public User getUserByResetPasswordCodeAndClear(String code) {
        try {
            User user = storage.createSingleQuery("from User u where u.resetPasswordCode = '" + code + "'", User.class);
            Date codeCreated = user.getResetPasswordCodeCreated();
            if (codeCreated == null) {
                log.warn("ResetPasswordCodeCreated is null for code " + code);
                return null;
            }
            int age = Minutes.minutesBetween(new DateTime(codeCreated), new DateTime()).getMinutes();
            if (age > 60) {
                log.warn("ResetPasswordCode is too old (" + age + " minutes) for code " + code);
                return null;
            }
            user.setResetPasswordCode(null);
            user.setResetPasswordCodeCreated(null);
            storage.begin();
            storage.persist(user);
            storage.commit();
            log.info("Password reset for user " + user.getId() + " has been authorized with code " + code + " and age " + age + " minutes.");
            return user;
        } catch (NoResultException e) {
            log.warn("Could not find any user for ResetPasswordCode " + code);
            return null;
        }
    }

    public boolean setPassword(User user, String password) {
        try {
            user.setPasswordHash(PasswordHash.createHash(password));
            user.setPasswordHashCreated(new Date());
            storage.begin();
            storage.persist(user);
            storage.commit();
            return true;
        } catch (NoSuchAlgorithmException e) {
            log.warn("Could not set password for user " + user.getId() + " due to: " + e.getMessage(), e);
        } catch (InvalidKeySpecException e) {
            log.warn("Could not set password for user " + user.getId() + " due to: " + e.getMessage(), e);
        }
        return false;
    }

    public ServiceResponse updateProfile(User user, String name, Date birthDate, String gender) {
        Principal principal = user.getPrincipal();
        setPrincipalName(name, principal);
        principal.setBirthDate(birthDate);
        principal.setGender(gender);
        storage.begin();
        try {
            storage.persist(principal);
            storage.commit();
        } catch (RuntimeException e) {
            storage.rollback();
            return new ServiceResponse(false, "Det oppstod en feil ved lagring. Vennligst forsøk igjen.");
        }
        return new ServiceResponse(true, "Din profil ble lagret.");
    }

    private void setPrincipalName(String name, Principal principal) {
        principal.setName(name);
        String[] nameParts = name.split(" ");
        if (nameParts.length == 2) {
            principal.setFirstName(nameParts[0]);
            principal.setMiddleName(null);
            principal.setLastName(nameParts[1]);
        } else if (nameParts.length == 3) {
            principal.setFirstName(nameParts[0]);
            principal.setMiddleName(nameParts[1]);
            principal.setLastName(nameParts[2]);
        } else {
            principal.setFirstName(nameParts[0]);
            principal.setMiddleName(nameParts[1]); // TODO Build middle name
            principal.setLastName(nameParts[nameParts.length - 1]);
        }
    }

    public ServiceResponse updateAddress(
            User user, String address1, String address2, String zip, String city, String country) {
        Address address = new Address();
        address.setAddress1(address1);
        address.setAddress2(address2);
        address.setZip(zip);
        address.setCity(city);
        address.setCountry(country);
        address.setCreated(new Date());
        address.setPrime(new Date());
        address.setPrincipal(user.getPrincipal());
        user.getPrincipal().getAddressList().add(address);
        storage.begin();
        try {
            storage.persist(address);
            storage.commit();
        } catch (RuntimeException e) {
            storage.rollback();
            return new ServiceResponse(false, "Det oppstod en feil ved lagring. Vennligst forsøk igjen.");
        }
        return new ServiceResponse(true, "Din postadresse ble lagret.");
    }

    public ServiceResponse updateMobile(User user, String mobile) {
        TypedQuery<MobileNumber> query = storage.createQuery("from MobileNumber m where number = '" + mobile
                + "' and m.principal.id != " + user.getPrincipal().getId(), MobileNumber.class);
        if (!query.getResultList().isEmpty()) {
            return new ServiceResponse(false, "Mobilnummeret er allerede i bruk.");
        }

        MobileNumber mobileNumber = new MobileNumber();
        mobileNumber.setNumber(mobile);
        mobileNumber.setVerified(null);
        mobileNumber.setCreated(new Date());
        mobileNumber.setPrime(new Date());
        mobileNumber.setPrincipal(user.getPrincipal());
        user.getPrincipal().getMobileNumberList().add(mobileNumber);
        storage.begin();
        try {
            storage.persist(mobileNumber);
            storage.commit();
        } catch (RuntimeException e) {
            storage.rollback();
            return new ServiceResponse(false, "Det oppstod en feil ved lagring. Vennligst forsøk igjen.");
        }
        return new ServiceResponse(true, "Ditt mobilnummer ble lagret.");
    }

    public ServiceResponse updateEmail(User user, String email) {
        // Has this email address been used and verified previously by a different user?
        TypedQuery<EmailAddress> query = storage.createQuery("from EmailAddress m where email = '" + email
                + "' and m.principal.id != " + user.getPrincipal().getId(), EmailAddress.class);
        for (EmailAddress prevAddress : query.getResultList()) {
            if (prevAddress.getVerified() != null) {
                return new ServiceResponse(false, "E-postadressen er allerede i bruk.");
            }
        }

        EmailAddress emailAddress = new EmailAddress();
        emailAddress.setEmail(email);
        emailAddress.setVerified(null);
        emailAddress.setVerificationCode(UUID.randomUUID().toString());
        emailAddress.setCreated(new Date());
        emailAddress.setPrime(new Date());
        emailAddress.setPrincipal(user.getPrincipal());
        user.getPrincipal().getEmailAddressList().add(emailAddress);

        // Has this email address been used by this user previously?
        TypedQuery<EmailAddress> previouslyUsedBySameUserQuery = storage.createQuery(
                "from EmailAddress m where email = '" + email + "' and m.principal.id = "
                        + user.getPrincipal().getId(), EmailAddress.class);
        // Is this email address already used and verified?
        List<EmailAddress> previouslyUsedBySameUser = previouslyUsedBySameUserQuery.getResultList();
        for (EmailAddress previousAddress : previouslyUsedBySameUser) {
            if (email.equals(previousAddress.getEmail())) {
                if (previousAddress.getVerified() != null) {
                    // Pass on the verification to the new instance of the same email address.
                    emailAddress.setVerified(previousAddress.getVerified());
                }
            }
        }

        storage.begin();
        try {
            storage.persist(emailAddress);
            storage.commit();
        } catch (RuntimeException e) {
            storage.rollback();
            return new ServiceResponse(false, "Det oppstod en feil ved lagring. Vennligst forsøk igjen.");
        }

        if (emailAddress.getVerified() == null) {
            sendEmailVerification(emailAddress);
        }
        return new ServiceResponse(true, "Din e-postadresse ble lagret.");
    }

    private void sendEmailVerification(EmailAddress emailAddress) {
        if (emailAddress == null) {
            return;
        }

        StringBuilder text = new StringBuilder();
        text.append("Hei, ").append(emailAddress.getPrincipal().getName()).append("!<br><br>");
        text.append("Vi mottok en forespørsel om å endre e-postadressen din på nettsiden til Leinstrand IL. ");
        text.append("Bruk lenken nedenfor for å bekrefte den nye e-postadressen:<br><br>");
        text.append("<strong>Bekreft e-postadressen:</strong> ");
        text.append("<a href=\"%baseUrl%page/signin?tab=verifiserepost&code=").append(emailAddress.getVerificationCode());
        text.append("\">%baseUrl%page/signin?tab=verifiserepost&code=").append(emailAddress.getVerificationCode()).append("</a><br><br>");
        text.append("Med vennlig hilsen,<br>Leinstrand idrettslag.");

        mailService.sendNoReplyHtmlMessage(emailAddress.getEmail(), "Bekreft ny e-postadresse for Leinstrand IL", text.toString());
    }

    public ServiceResponse addFamilyMember(User user, String name, Date birthDate, String gender) {
        Family family = user.getPrincipal().getFamily();
        Principal principal = new Principal();
        setPrincipalName(name, principal);
        principal.setBirthDate(birthDate);
        principal.setGender(gender);
        principal.setCreated(new Date());
        principal.setUpdated(new Date());
        principal.setFamily(family);
        family.getMembers().add(principal);
        storage.begin();
        try {
            storage.persist(principal);
            storage.persist(family);
            storage.commit();
        } catch (RuntimeException e) {
            storage.rollback();
            return new ServiceResponse(false, "Det oppstod en feil ved lagring. Vennligst forsøk igjen.");
        }
        return new ServiceResponse(true, "Det nye familiemedlemmet ble lagret.");
    }

    public Family ensureFamilyForUser(User user) {
        Family family = user.getPrincipal().getFamily();
        if (family != null) {
            return family;
        }
        family = new Family();
        family.getMembers().add(user.getPrincipal());
        family.setPrimaryPrincipal(user.getPrincipal());
        user.getPrincipal().setFamily(family);
        storage.begin();
        storage.persist(family);
        storage.commit();
        return family;
    }

    public boolean verifyEmailAddressByCode(String code) {
        TypedQuery<EmailAddress> query = storage.createQuery("from EmailAddress m where m.verificationCode = '"
                + code + "'", EmailAddress.class);
        List<EmailAddress> emailAddressList = query.getResultList();
        if (emailAddressList.isEmpty()) {
            return false;
        }
        for (EmailAddress emailAddress : emailAddressList) {
            emailAddress.setVerified(new Date());
            emailAddress.setVerificationCode(null);
            storage.begin();
            storage.persist(emailAddress);
            storage.commit();
        }
        return true;
    }

    public boolean hasValidEmail(User user) {
        return !user.getPrincipal().getEmailAddressList().isEmpty();
    }

    public boolean hasValidMobile(User user) {
        return !user.getPrincipal().getMobileNumberList().isEmpty();
    }

    public boolean hasValidAddress(User user) {
        return !user.getPrincipal().getAddressList().isEmpty();
    }

    public ServiceResponse destroyPrincipal(Principal principal) {
        storage.begin();
        try {
            storage.delete(principal);
            storage.commit();
            return new ServiceResponse(true, "Personen ble slettet.");
        } catch(RuntimeException e) {
            storage.rollback();
            return new ServiceResponse(false, "Personen kunne ikke slettes på nåværende tidspunkt.");
        }
    }

    public ServiceResponse setPrimaryPrincipal(Principal principal, Family family) {
        if (isPrimaryContactCandidate(principal)) {
            if (isFamilyMember(family, principal)) {
                family.setPrimaryPrincipal(principal);
                storage.begin();
                try {
                    storage.persist(family);
                    storage.commit();
                    return new ServiceResponse(true, "Ny primærkontakt er satt.");
                } catch(RuntimeException e) {
                    storage.rollback();
                    return new ServiceResponse(false, "Primærkontakt kunne ikke endres på nåværende tidspunkt.");
                }
            } else {
                return new ServiceResponse(false, "Personen er ikke medlem av familien.");
            }
        } else {
            return new ServiceResponse(false, "Personen kan ikke settes som primærkontakt.");
        }

    }

    public boolean isFamilyMember(Family family, Principal principal) {
        for (Principal member : family.getMembers()) {
            if (member.getId().equals(principal.getId())) {
                return true;
            }
        }
        return false;
    }

    public boolean isPendingFamilyMember(Family family, Principal principal) {
        TypedQuery<FamilyInvitation> query = storage.createQuery("from FamilyInvitation where family.id = " + family.getId() + " and principal.id = " + principal.getId(), FamilyInvitation.class);
        List<FamilyInvitation> list = query.getResultList();
        if (list.isEmpty()) {
            return false;
        }
        for (FamilyInvitation familyInvitation : list) {
            if (!isFamilyInvitationExpired(familyInvitation)) {
                return true;
            }
        }
        return false;
    }

    private boolean isFamilyInvitationExpired(FamilyInvitation familyInvitation) {
        DateTime expiry = DateTime.now().plusDays(FAMILY_INVITAION_EXPIRY_DAYS);
        return expiry.isBefore(new DateTime(familyInvitation.getCreated()));
    }

    public ServiceResponse inviteFamilyMember(Principal principal, Family family) {
        if (isOnlyPrincipal(principal)) {
            return new ServiceResponse(false, "Personen med denne e-postadresse har ingen bruker.");
        }
        if (isFamilyMember(family, principal)) {
            return new ServiceResponse(false, "Brukeren med denne e-postadressen er allerede medlem av din familie.");
        }
        if (isPendingFamilyMember(family, principal)) {
            return new ServiceResponse(false, "Brukeren med denne e-postadressen er allerede blitt invitert til din familie.");
        }

        FamilyInvitation invitation = new FamilyInvitation();
        invitation.setCreated(new Date());
        invitation.setFamily(family);
        invitation.setPrincipal(principal);
        invitation.setCode(UUID.randomUUID().toString());
        storage.begin();
        try {
            storage.persist(invitation);
            storage.commit();

            StringBuilder text = new StringBuilder();
            text.append("Hei, ").append(principal.getName()).append("!<br><br>");
            text.append("Dette er en invitasjon fra ");
            text.append(family.getPrimaryPrincipal().getName() + " ");
            text.append("om å bli medlem av hans familiemedlemskap på Leinstrand IL.<br><br>");
            text.append("Bruk lenken nedenfor hvis du ønsker å takke ja til denne invitasjonen:<br><br>");
            text.append("<strong>Takk ja til invitasjonen:</strong> ");
            text.append("<a href=\"%baseUrl%page/signin?tab=takkjatilmedlemskap&code=").append(invitation.getCode());
            text.append("\">%baseUrl%page/signin?tab=takkjatilmedlemskap&code=").append(invitation.getCode());
            text.append("</a><br><br>");
            text.append("Om du ikke ønsker å takke ja trenger du ikke gjøre noe. Denne invitasjonen er kun gyldig i ");
            text.append(FAMILY_INVITAION_EXPIRY_DAYS + " dager.<br><br>");
            text.append("Med vennlig hilsen,<br>Leinstrand idrettslag.");
            mailService.sendNoReplyHtmlMessage(principal.getEmailAddressList().get(0).getEmail(),
                    "Invitasjon til familiemedlemskap i Leinstrand IL", text.toString());

            return new ServiceResponse(true, "Brukeren er blitt invitert til din familie.");
        } catch(RuntimeException e) {
            storage.rollback();
            return new ServiceResponse(false, "Kunne ikke invitere brukeren på nåværende tidspunkt.");
        }
    }

    public List<FamilyInvitation> getFamilyInvitations(Family family) {
        TypedQuery<FamilyInvitation> query = storage.createQuery("from FamilyInvitation where family.id = " + family.getId(), FamilyInvitation.class);
        List<FamilyInvitation> list = query.getResultList();
        return list;
    }

    public List<FamilyInvitation> getInvitationsForPrincipal(Principal principal) {
        TypedQuery<FamilyInvitation> query = storage.createQuery("from FamilyInvitation where principal.id = " + principal.getId(), FamilyInvitation.class);
        List<FamilyInvitation> list = query.getResultList();
        return list;
    }

    public ServiceResponse acceptFamilyInvitation(String code) {
        if (code == null) {
            return new ServiceResponse(false, "Forespørselen kan ikke behandles siden den mangler den nødvendige koden.");
        }
        FamilyInvitation invitation = storage.createSingleQuery("from FamilyInvitation where code = '" + code + "'", FamilyInvitation.class);
        if (invitation == null) {
            return new ServiceResponse(false, "Invitasjonskoden er ukjent.");
        }
        if (isFamilyInvitationExpired(invitation)) {
            return new ServiceResponse(false, "Beklager, men invitasjonen har utløpt. Den var bare gyldig i " + FAMILY_INVITAION_EXPIRY_DAYS + " dager.");
        }
        Principal principal = invitation.getPrincipal();
        Family family = invitation.getFamily();
        principal.setFamily(family);
        family.getMembers().add(principal);
        storage.begin();
        try {
            storage.persist(principal);
            storage.delete(invitation);
            storage.commit();
            return new ServiceResponse(true, "Du har akseptert invitasjonen og er nå medlem av familien til " + family.getPrimaryPrincipal().getName() + ".");
        } catch(RuntimeException e) {
            storage.rollback();
            return new ServiceResponse(false, "Invitasjonen kunne ikke behandles på nåværende tidspunkt.");
        }
    }

    public Principal getPrincipalById(Long id) {
        try {
            return storage.createSingleQuery("from Principal where id = " + id, Principal.class);
        } catch (NoResultException e) {
            return null;
        }
    }

    public List<String> checkUserProfileCompleteness(User user) {
        List<String> errorList = new ArrayList<>();
        if (!hasValidEmail(user)) errorList.add("<i class=\"icon-envelope\"></i> e-postadresse");
        if (!hasValidAddress(user)) errorList.add("<i class=\"icon-home\"></i> postadresse");
        if (!hasValidMobile(user)) errorList.add("<i class=\"icon-mobile-phone\"></i> mobilnummer");
        return errorList;
    }

}