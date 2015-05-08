package no.leinstrandil.incident;

import no.leinstrandil.database.model.person.Principal;

public class PrincipalIncident extends Incident {

    private Principal principal;
    private String action;
    private String object;

    public PrincipalIncident(Principal principal, String action) {
        this.principal = principal;
        this.action = action;
    }

    public PrincipalIncident(Principal principal, String action, String object) {
        this(principal, action);
        this.object = object;
    }

    @Override
    public String toReport() {
        StringBuffer o = new StringBuffer();
        o.append("principal.id: " + principal.getId() + "\n");
        o.append("principal.name: " + principal.getName() + "\n");
        o.append("principal.birthDate: " + principal.getBirthDate() + "\n");
        o.append("action: " + action + "\n");
        o.append("object: " + object + "\n");
        return o.toString();
    }

}
