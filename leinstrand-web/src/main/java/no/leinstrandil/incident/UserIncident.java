package no.leinstrandil.incident;

import no.leinstrandil.database.model.web.User;

public class UserIncident extends Incident {

    private User user;
    private String action;

    public UserIncident(User user, String action) {
        this.user = user;
        this.action = action;
    }

    @Override
    public String toReport() {
        StringBuffer o = new StringBuffer();
        o.append("user.id: " + user.getId() + "\n");
        o.append("user.facebookId: " + user.getFacebookId() + "\n");
        o.append("user.principal.name: " + user.getPrincipal().getName() + "\n");
        o.append("action: " + action + "\n");
        return o.toString();
    }

}
