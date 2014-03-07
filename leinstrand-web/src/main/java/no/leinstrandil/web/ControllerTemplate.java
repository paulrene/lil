package no.leinstrandil.web;

public enum ControllerTemplate {

    CONTACT("contact"), MAGAZINE("magazine"), FACEBOOK("facebook"), SEARCHRESULTS("searchresults");

    private String id;

    private ControllerTemplate(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

}
