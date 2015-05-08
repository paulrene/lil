package no.leinstrandil.incident;

import java.util.Date;

public abstract class Incident {

    private Date created;

    public Incident() {
        this.created = new Date();
    }

    public abstract String toReport();

    public Date getCreated() {
        return created;
    }

}
