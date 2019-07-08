package it.bologna.ausl.internauta.service.configuration.nextsdr;

import it.nextsw.common.controller.RestControllerEngine;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import org.springframework.stereotype.Service;

/**
 *
 * @author gdm
 */
@Service
public class RestControllerEngineImpl extends RestControllerEngine {

    @PersistenceContext
    EntityManager entityManager;

    @Override
    protected Object retriveEntity(Class entityClass, Object entityKey) {
//        return entityManager.find(entityClass, ((Integer)entityKey).longValue());
        return super.retriveEntity(entityClass, entityKey);
    }

    @Override
    protected boolean isKeysEquals(Object key1, Object key2) {
        return super.isKeysEquals(key1, key2);
    }
}
