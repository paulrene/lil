package no.leinstrandil.service;

import no.leinstrandil.database.Storage;

public class UserService {

    private Storage storage;

    public UserService(Storage storage) {
        this.storage = storage;
    }

}
