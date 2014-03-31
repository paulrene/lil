package no.leinstrandil.service;

public class ServiceResponse {

    private boolean success;
    private String message;

    public ServiceResponse(boolean success, String message) {
        this.success = success;
        this.message = message;
    }

    public String getMessage() {
        return message;
    }

    public boolean isSuccess() {
        return success;
    }

}
