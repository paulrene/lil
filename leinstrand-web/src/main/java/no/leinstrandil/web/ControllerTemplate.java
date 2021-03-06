package no.leinstrandil.web;

public enum ControllerTemplate {

    CONTACT("contact"),
    MAGAZINE("magazine"),
    FACEBOOK("facebook"),
    SEARCHRESULTS("searchresults"),
    MYPAGE("mypage"),
    SIGNIN("signin"),
    TEAMLEAD("teamlead"),
    ACCOUNTING("accounting");

    private String id;

    private ControllerTemplate(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

}
