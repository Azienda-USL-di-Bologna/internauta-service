package it.bologna.ausl.internauta.service.controllers.tip;

import java.util.HashMap;
import java.util.Map;
import javax.persistence.EntityManager;

/**
 * Questa classe permette di salvare e caricare da una cache in memoria delle entità
 * Tramite il metodo getEntityById è possibile caricare delle entità tramite il loro id. Ad ogni chiamata l'entità viene poi cachata in una mappa, in modo che dalla
 *  seconda richiesta della stessa, questa venga tornata direttamente da lì.
 * Inoltre è possibile cachare e poi caricare delle entità tramite una chiave qualsiasi tramite il metodo cacheEntityByKey e getCachedEntityByKey.
 * @author gdm
 */
public class TipTransferCachedEntities {
    
    private final Map<Object, Object> cachedEntities;
    private final EntityManager entityManager;

    public TipTransferCachedEntities(EntityManager entityManager) {
        this.entityManager = entityManager;
        this.cachedEntities = new HashMap<>();
    }
    
    /**
     * cacha un entità con la chiave passata
     * @param key la chiave tramite la quale sarà possibile reperire l'entità
     * @param value l'entità da cachare
     */
    public void cacheEntityByKey(Object key, Object value) {
        cachedEntities.put(key, value);
    }
    
    /**
     * reperisce l'entità con la chiave passata
     * @param <T>
     * @param key la chiave dell'entità
     * @param entityClass la classe dell'entità
     * @return l'entità identificata dalla chiave passata, null se non presente in cache
     */
    public <T> T getCachedEntityByKey(Object key, Class<T> entityClass) {
        Object entity = cachedEntities.get(key);
        if (entity != null)
            return (T) entity;
        else
            return null;
    }
    
    /**
     * reperisce l'entità tramite l'id passato, tornandola dalla cache se presente, caricandola dal DB e poi chachandola se non presente
     * @param <T>
     * @param id l'id dell'entità
     * @param entityClass la classe dell'entità
     * @return l'entità con l'id richiesto
     */
    public <T> T getEntityById(Object id, Class<T> entityClass) {
        Object entity = cachedEntities.get(id);
        if (entity == null) {
            entity = entityManager.find(entityClass, id);
            cachedEntities.put(id, entity);
        }
        return (T) entity;
    }
}
