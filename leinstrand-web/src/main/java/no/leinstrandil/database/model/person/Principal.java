package no.leinstrandil.database.model.person;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.OrderBy;
import javax.persistence.Table;
import no.leinstrandil.database.model.club.ActivityParticipation;
import no.leinstrandil.database.model.club.TeamMembership;
import no.leinstrandil.database.model.web.User;

@Entity
@Table(name = "principal")
public class Principal {

    @Id @GeneratedValue
    private Long id;
    @OneToOne(mappedBy = "principal")
    private User user;
    @ManyToOne @JoinColumn(name = "familyId")
    private Family family;
    private String name;
    private String firstName;
    private String middleName;
    private String lastName;
    private String gender;
    private String pictureUrl;
    private Date birthDate;
    @OneToMany(mappedBy = "principal") @OrderBy("prime DESC")
    private List<Address> addressList;
    @OneToMany(mappedBy = "principal") @OrderBy("prime DESC")
    private List<EmailAddress> emailAddressList;
    @OneToMany(mappedBy = "principal") @OrderBy("prime DESC")
    private List<MobileNumber> mobileNumberList;
    @OneToMany(mappedBy = "principal") @OrderBy("created DESC")
    private List<TeamMembership> teamMemberships;
    @OneToMany(mappedBy = "principal") @OrderBy("created DESC")
    private List<ActivityParticipation> activityParticipations;
    private Date created;
    private Date updated;

    public Principal() {
        addressList = new ArrayList<>();
        emailAddressList = new ArrayList<>();
        mobileNumberList = new ArrayList<>();
        teamMemberships = new ArrayList<>();
        activityParticipations = new ArrayList<>();
    }

    public Long getId() {
        return id;
    }

    public List<Address> getAddressList() {
        return addressList;
    }

    public void setAddressList(List<Address> addressList) {
        this.addressList = addressList;
    }

    public Date getBirthDate() {
        return birthDate;
    }

    public void setBirthDate(Date birthDate) {
        this.birthDate = birthDate;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getMiddleName() {
        return middleName;
    }

    public void setMiddleName(String middleName) {
        this.middleName = middleName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public String getPictureUrl() {
        return pictureUrl;
    }

    public void setPictureUrl(String pictureUrl) {
        this.pictureUrl = pictureUrl;
    }

    public List<EmailAddress> getEmailAddressList() {
        return emailAddressList;
    }

    public void setEmailAddressList(List<EmailAddress> emailAddressList) {
        this.emailAddressList = emailAddressList;
    }

    public List<MobileNumber> getMobileNumberList() {
        return mobileNumberList;
    }

    public void setMobileNumberList(List<MobileNumber> mobileNumberList) {
        this.mobileNumberList = mobileNumberList;
    }

    public Date getCreated() {
        return created;
    }

    public void setCreated(Date created) {
        this.created = created;
    }

    public Date getUpdated() {
        return updated;
    }

    public void setUpdated(Date updated) {
        this.updated = updated;
    }

}
