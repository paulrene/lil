package no.leinstrandil.database;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import javax.persistence.TypedQuery;

public class Storage {

    private ThreadLocal<EntityManager> localEntityManager;
    private EntityManagerFactory factory;

    public Storage() {
        localEntityManager = new ThreadLocal<>();
        factory = Persistence.createEntityManagerFactory("LeinstrandIL");
    }

    private EntityManager getManager() {
        synchronized (localEntityManager) {
            EntityManager manager = localEntityManager.get();
            if (manager == null) {
                manager = factory.createEntityManager();
                localEntityManager.set(manager);
            }
            return manager;
        }
    }

    public void close() {
        synchronized (localEntityManager) {
            EntityManager manager = localEntityManager.get();
            if (manager != null) {
                manager.close();
                localEntityManager.remove();
            }
        }
    }

    public void begin() {
        getManager().getTransaction().begin();
    }

    public void rollback() {
        getManager().getTransaction().rollback();
    }

    public void commit() {
        getManager().getTransaction().commit();
    }

    public void persist(Object obj) {
        getManager().persist(obj);
    }

    public void delete(Object obj) {
        getManager().remove(obj);
    }

    public <T> TypedQuery<T> createQuery(String qlString, Class<T> resultClass) {
        TypedQuery<T> query = getManager().createQuery(qlString, resultClass);
        return query;
    }

    public <T> T createSingleQuery(String qlString, Class<T> resultClass) {
        TypedQuery<T> query = getManager().createQuery(qlString, resultClass);
        return query.getSingleResult();
    }

}
