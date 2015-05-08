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
        o.append("principal.id: " + principal.getId() + "<br>\n");
        o.append("principal.name: " + principal.getName() + "<br>\n");
        o.append("principal.birthDate: " + principal.getBirthDate() + "<br>\n");
        o.append("action: " + action + "<br>\n");
        o.append("object: " + object + "<br>\n");
        return o.toString();
    }

}
