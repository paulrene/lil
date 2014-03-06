package no.leinstrandil.service;

import java.util.List;
import javax.persistence.TypedQuery;
import no.leinstrandil.database.Storage;
import no.leinstrandil.database.model.Resource;

public class FileService {

    private Storage storage;

    public FileService(Storage storage) {
        this.storage = storage;
    }

    public List<Resource> getImages() {
        TypedQuery<Resource> query = storage.createQuery("from Resource r order by r.created desc", Resource.class);
        return query.getResultList();
    }

    public Resource getResourceById(Long id) {
        return storage.createSingleQuery("from Resource r where r.id = " + id, Resource.class);
    }

}
