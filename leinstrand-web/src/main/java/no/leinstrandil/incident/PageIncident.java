package no.leinstrandil.incident;

import no.leinstrandil.database.model.person.Principal;

public class PageIncident extends PrincipalIncident {

    public PageIncident(Principal principal, String action, String object) {
        super(principal, action, object);
    }

    public PageIncident(Principal principal, String action) {
        super(principal, action);
    }

}
