package no.leinstrandil.incident;

import no.leinstrandil.database.model.person.Principal;

public class ClubIncident extends PrincipalIncident {

    public ClubIncident(Principal principal, String action, String object) {
        super(principal, action, object);
    }

    public ClubIncident(Principal principal, String action) {
        super(principal, action);
    }

}
