package it.bologna.ausl.internauta.service.interceptors.scripta;

import it.bologna.ausl.internauta.service.authorization.AuthenticatedSessionData;
import it.bologna.ausl.internauta.service.configuration.utils.ReporitoryConnectionManager;
import it.bologna.ausl.internauta.service.interceptors.InternautaBaseInterceptor;
import it.bologna.ausl.internauta.service.repositories.scripta.AllegatoRepository;
import it.bologna.ausl.internauta.service.repositories.scripta.DocDetailViewRepository;
import it.bologna.ausl.internauta.service.repositories.scripta.PermessoArchivioRepository;
import it.bologna.ausl.internauta.service.repositories.scripta.PersonaVedenteRepository;
import it.bologna.ausl.internauta.service.utils.InternautaConstants;
import it.bologna.ausl.minio.manager.MinIOWrapper;
import it.bologna.ausl.model.entities.baborg.Persona;
import it.bologna.ausl.model.entities.scripta.Allegato;
import it.bologna.ausl.model.entities.scripta.Doc;
import it.bologna.ausl.model.entities.scripta.views.DocDetailView;
import it.nextsw.common.annotations.NextSdrInterceptor;
import it.nextsw.common.controller.BeforeUpdateEntityApplier;
import it.nextsw.common.interceptors.exceptions.AbortLoadInterceptorException;
import it.nextsw.common.interceptors.exceptions.AbortSaveInterceptorException;
import it.nextsw.common.interceptors.exceptions.SkipDeleteInterceptorException;
import it.nextsw.common.repositories.NextSdrQueryDslRepository;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiFunction;
import javax.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

/**
 *
 * @author gusgus
 */
@Component
@NextSdrInterceptor(name = "allegato-interceptor")
public class AllegatoInterceptor extends InternautaBaseInterceptor {

    private static final Logger LOGGER = LoggerFactory.getLogger(AllegatoInterceptor.class);
    
    @Autowired
    ReporitoryConnectionManager aziendeConnectionManager;

    @Autowired
    AllegatoRepository allegatoRepository;

    @Autowired
    DocDetailViewRepository docDetailViewRepository;
    
    @Autowired
    PersonaVedenteRepository personaVedenteRepository;
    
    @Autowired
    @Qualifier(value = "customRepositoryEntityMap")
    protected Map<String, NextSdrQueryDslRepository> customRepositoryEntityMap;

    @Override
    public Class getTargetEntityClass() {
        return Allegato.class;
    }

    
            
//    @Override
//    public void beforeDeleteEntityInterceptor(Object entity, Map<String, String> additionalData, HttpServletRequest request, boolean mainEntity, Class projectionClass) throws AbortSaveInterceptorException, SkipDeleteInterceptorException {
//        Allegato allegato = (Allegato) entity;
//        MinIOWrapper minIOWrapper = aziendeConnectionManager.getMinIOWrapper();
//        List<DettaglioAllegato> dettagliAllegatiList = allegato.getDettagliAllegatiList();
//        for (DettaglioAllegato dettaglioAllegato : dettagliAllegatiList) {
//            try {
//                
//                restControllerEngine.delete(dettaglioAllegato.getId(), request, additionalData, null, false, projectionClass.getSimpleName(), dettaglioAllegatoRepository);
//            } catch (Exception ex) {
//                LOGGER.error("errore nell'eliminazione dei dettagli allegati",ex);
//                throw new AbortSaveInterceptorException("errore nell'eliminazione dei dettagli allegati",ex);
//            }
//        }
//
//    }

//    @Override
//    public void afterDeleteEntityInterceptor(Object entity, Map<String, String> additionalData, HttpServletRequest request, boolean mainEntity, Class projectionClass) throws AbortSaveInterceptorException, SkipDeleteInterceptorException {
//        /*
//            Sto cancellando un allegato, dunque:
//            - Se è un allegato contenitore devo cancellare i file correlati e anche in questo caso dal repository 
//            - Devo cancellare il file dal repository
//            - Un trigger si occupa di aggiustare la numerazione degli allegati
//         */
//        Allegato allegato = (Allegato) entity;
//        MinIOWrapper minIOWrapper = aziendeConnectionManager.getMinIOWrapper();

        // Cancello gli eventuali allegati figli.
//        List<Allegato> allegatiFigliList = allegato.getAllegatiFigliList();
//        for (Allegato a : allegatiFigliList) {
//            allegatoRepository.delete(a);
//        }
//        super.afterDeleteEntityInterceptor(entity, additionalData, request, mainEntity, projectionClass); //To change body of generated methods, choose Tools | Templates.
//    }
    @Override
    public Object afterSelectQueryInterceptor(Object entity, Map<String, String> additionalData, HttpServletRequest request, boolean mainEntity, Class projectionClass) throws AbortLoadInterceptorException {
        Allegato allegato = (Allegato) entity;
        Doc doc = allegato.getIdDoc();
//        DocDetailView ddv;
//        ddv = docDetailViewRepository.getById(doc.getId());
        AuthenticatedSessionData authenticatedSessionData = getAuthenticatedUserProperties();
        Persona persona = authenticatedSessionData.getPerson();
        Boolean pienaVisibilitaUtente = pienaVisibilita(doc, persona);
        if(pienaVisibilitaUtente) {
            return entity;
        } else {
            throw new AbortLoadInterceptorException("non posso vedere gli allegati");
        }
    }

    @Override
    public Collection<Object> afterSelectQueryInterceptor(Collection<Object> entities, Map<String, String> additionalData, HttpServletRequest request, boolean mainEntity, Class projectionClass) throws AbortLoadInterceptorException {
       Collection<Object> entitiesAfter = null;
       for (Object entity : entities) {
        //Azienda azienda = (Azienda) entity;
        entity = afterSelectQueryInterceptor(entity, additionalData, request, mainEntity, projectionClass);
//        if(entity == null) {
//            LOGGER.info("non ha il permesso per questo allegato");
//        } else {
//            entitiesAfter.add(entity);
//           }
       }
        return entities;
    }



         /**
     * Controlla se l'utente connesso ha pienaVisbilita: true nella colonna
     * personeVedenti del doc passato.
     *
     * @param doc
     * @return
     */
    private Boolean pienaVisibilita(Doc doc, Persona persona) {
        Boolean hasPienaVisibilita = false;
        
        hasPienaVisibilita = personaVedenteRepository.hasPienaVisibìlita(doc.getId(), persona.getId());
        
        return hasPienaVisibilita;
    }
    
    /**
     * TODO: Quando faccio un update di un allegato potrebbe essere che ho cancellato il dettaglio allegato.
     * Devo capire quale ho cancellato per poterlo cancellare anche dal repository
     * @param entity
     * @param beforeUpdateEntityApplier
     * @param additionalData
     * @param request
     * @param mainEntity
     * @param projectionClass
     * @return
     * @throws AbortSaveInterceptorException 
     */
    
    @Override
    public Object beforeUpdateEntityInterceptor(Object entity, BeforeUpdateEntityApplier beforeUpdateEntityApplier, Map<String, String> additionalData, HttpServletRequest request, boolean mainEntity, Class projectionClass) throws AbortSaveInterceptorException {
        
        return super.beforeUpdateEntityInterceptor(entity, beforeUpdateEntityApplier, additionalData, request, mainEntity, projectionClass); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void beforeDeleteEntityInterceptor(Object entity, Map<String, String> additionalData, HttpServletRequest request, boolean mainEntity, Class projectionClass) throws AbortSaveInterceptorException, SkipDeleteInterceptorException {
        Allegato allegato = (Allegato) entity;
        Allegato.DettagliAllegato dettagli = allegato.getDettagli();
        if (dettagli != null) {
            //TODO: in caso di fallimento quando si hanno piu dettagli allegati se uno fallisce bisogna fare l'undelete degli altri
            MinIOWrapper minIOWrapper = aziendeConnectionManager.getMinIOWrapper();
            try {
                Set<String> data = (Set) super.httpSessionData.getData(InternautaConstants.HttpSessionData.Keys.DettagliAllegatiDaEliminare);
                if (data == null){
                    data = new HashSet();
                    super.httpSessionData.putData(InternautaConstants.HttpSessionData.Keys.DettagliAllegatiDaEliminare, data);
                }

                for (Allegato.DettagliAllegato.TipoDettaglioAllegato tipoDettaglioAllegato : Allegato.DettagliAllegato.TipoDettaglioAllegato.values()) { 
                    Allegato.DettaglioAllegato dettaglioAllegato = allegato.getDettagli().getDettaglioAllegato(tipoDettaglioAllegato);
                    if (dettaglioAllegato != null) {
                        data.add(dettaglioAllegato.getIdRepository());
                    }
                }                      
            } catch (Exception ex) {
                LOGGER.error("errore nell'eliminazione del file su minIO", ex);
                throw new AbortSaveInterceptorException("errore nell'eliminazione del file su minIO", ex);
            }
        }
    }
}
