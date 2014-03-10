package no.leinstrandil.database.model.club;

import java.util.Date;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import no.leinstrandil.database.model.person.Principal;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "activityparticipation")
public class ActivityParticipation {

    @Id @GeneratedValue
    private Long id;
    private Date created;
    private Boolean enrolled;
    @ManyToOne @JoinColumn(name = "principalId")
    private Principal principal;
    @ManyToOne @JoinColumn(name = "activityId")
    private Activity activity;

    public ActivityParticipation() {
    }

    public Long getId() {
        return id;
    }

    public Date getCreated() {
        return created;
    }

    public void setCreated(Date created) {
        this.created = created;
    }

    public Boolean isEnrolled() {
        return enrolled;
    }

    public void setEnrolled(Boolean enrolled) {
        this.enrolled = enrolled;
    }

    public Principal getPrincipal() {
        return principal;
    }

    public void setPrincipal(Principal principal) {
        this.principal = principal;
    }

    public Activity getActivity() {
        return activity;
    }

    public void setActivity(Activity activity) {
        this.activity = activity;
    }

}
