package it.bologna.ausl.internauta.service.permessi;

import it.bologna.ausl.internauta.utils.bds.types.EntitaStoredProcedure;
import it.bologna.ausl.internauta.utils.bds.types.PermessoEntitaStoredProcedure;
import it.bologna.ausl.model.entities.EntityInterface;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.PostConstruct;
import javax.persistence.Entity;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Table;
import org.reflections.Reflections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

/**
 *
 * @author gusgus
 */
@Component
public class PermessiUtils {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(PermessiUtils.class);

    private static Map<String, Class> entitiesClassMap;
    
    @PersistenceContext
    private EntityManager entityManager;
    
    @Autowired
    private PermessiUtils permessiUtils;
    
    /**
     * Preparo una mappa con la lista della classi delle entità
     */
    @PostConstruct
    public void buildEntitiesClassMap() {
        if (entitiesClassMap == null) {
            Set<Class<?>> entityClasses = new Reflections("it.bologna.ausl.model.entities").getTypesAnnotatedWith(Entity.class);

            entitiesClassMap = new HashMap<>();

            for (Class entityClass : entityClasses) {
                LOGGER.info("classe trovata: " + entityClass.getName());
                Table annotation = (Table) entityClass.getAnnotation(Table.class);
                String schema = annotation.schema();
                String tableName = annotation.name();
                entitiesClassMap.put(schema + "--" + tableName, entityClass);
            }
        }
    }
    
    /**
     * Carica l'entità definita dai tre parametri
     * @param schema
     * @param table
     * @param id 
     */
    @Cacheable(
            value = "loadEntity__ribaltorg__", 
            key = "{#schema, #table, #idProvenienza.toString()}")
    public Object loadEntity(String schema, String table, Object idProvenienza) {
        String key = schema + "--" + table;
        Class entityClass = entitiesClassMap.get(key);
        return entityManager.find(entityClass, idProvenienza);
    }
    
    /**
     * Cicla la lista di permessi "PermessoEntitaStoredProcedure" e popola
     * descrizione e additional data come richiesto
     * @param oggettonePermessiList
     * @param additionalDataParam
     * @return 
     */
    private List<PermessoEntitaStoredProcedure> setDescriptions(List<PermessoEntitaStoredProcedure> oggettonePermessiList, String entitaCoinvolta, String additionalDataParamSoggetto, String additionalDataParamOggetto) {
        for (PermessoEntitaStoredProcedure oggettonePermessi : oggettonePermessiList) {
            if (entitaCoinvolta.equals("soggetto") || entitaCoinvolta.equals("soggetto_oggetto")) {
                EntitaStoredProcedure soggetto = oggettonePermessi.getSoggetto();
                EntityInterface entity = (EntityInterface) permessiUtils.loadEntity(soggetto.getSchema(), soggetto.getTable(), soggetto.getIdProvenienza());
                soggetto.setDescrizione(entity.getEntityDescription());
                soggetto.setAdditionalData(entity.getEntityAdditionalData(additionalDataParamSoggetto));
            }
            if (entitaCoinvolta.equals("oggetto") || entitaCoinvolta.equals("soggetto_oggetto")) {
                EntitaStoredProcedure oggetto = oggettonePermessi.getOggetto();
                EntityInterface entity = (EntityInterface) permessiUtils.loadEntity(oggetto.getSchema(), oggetto.getTable(), oggetto.getIdProvenienza());
                oggetto.setDescrizione(entity.getEntityDescription());
                oggetto.setAdditionalData(entity.getEntityAdditionalData(additionalDataParamOggetto));
            }
            
        }
        return oggettonePermessiList;
    }

    public List<PermessoEntitaStoredProcedure> setDescriptionsSoggetto(List<PermessoEntitaStoredProcedure> oggettonePermessiList, String additionalDataParamSoggetto) {
        return setDescriptions(oggettonePermessiList, "soggetto", additionalDataParamSoggetto, null);
    }
    
    public List<PermessoEntitaStoredProcedure> setDescriptionsOggetto(List<PermessoEntitaStoredProcedure> oggettonePermessiList, String additionalDataParamOggetto) {
        return setDescriptions(oggettonePermessiList, "oggetto", null, additionalDataParamOggetto);
    }
    
    public List<PermessoEntitaStoredProcedure> setDescriptionsSoggettoAndOggetto(List<PermessoEntitaStoredProcedure> oggettonePermessiList, String additionalDataParamSoggetto, String additionalDataParamOggetto) {
        return setDescriptions(oggettonePermessiList, "soggetto_oggetto", additionalDataParamSoggetto, additionalDataParamOggetto);
    }
}
