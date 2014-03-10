package no.leinstrandil.service;

import no.leinstrandil.database.model.web.Resource;

import java.util.List;
import javax.persistence.TypedQuery;
import no.leinstrandil.database.Storage;

public class FileService {

    private Storage storage;

    public FileService(Storage storage) {
        this.storage = storage;
    }

    public List<Resource> getImages() {
        TypedQuery<Resource> query = storage.createQuery("from Resource r where r.contentType like 'image/%' order by r.created desc", Resource.class);
        return query.getResultList();
    }

    public Resource getResourceById(Long id) {
        return storage.createSingleQuery("from Resource r where r.id = " + id, Resource.class);
    }

    public Resource getResourceByFilename(String fileName) {
        return storage.createSingleQuery("from Resource r where r.fileName = '" + fileName + "'", Resource.class);
    }

}
