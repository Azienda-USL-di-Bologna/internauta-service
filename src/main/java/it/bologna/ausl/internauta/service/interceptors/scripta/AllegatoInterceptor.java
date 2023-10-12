package it.bologna.ausl.internauta.service.interceptors.scripta;

import it.bologna.ausl.internauta.service.authorization.AuthenticatedSessionData;
import it.bologna.ausl.internauta.service.authorization.UserInfoService;
import it.bologna.ausl.internauta.service.configuration.utils.ReporitoryConnectionManager;
import it.bologna.ausl.internauta.service.interceptors.InternautaBaseInterceptor;
import it.bologna.ausl.internauta.service.repositories.scripta.PersonaVedenteRepository;
import it.bologna.ausl.internauta.service.utils.InternautaConstants;
import it.bologna.ausl.minio.manager.MinIOWrapper;
import it.bologna.ausl.model.entities.baborg.Persona;
import it.bologna.ausl.model.entities.baborg.Ruolo;
import it.bologna.ausl.model.entities.baborg.Utente;
import it.bologna.ausl.model.entities.scripta.Allegato;
import it.bologna.ausl.model.entities.scripta.Doc;
import it.nextsw.common.annotations.NextSdrInterceptor;
import it.nextsw.common.controller.BeforeUpdateEntityApplier;
import it.nextsw.common.interceptors.exceptions.AbortLoadInterceptorException;
import it.nextsw.common.interceptors.exceptions.AbortSaveInterceptorException;
import it.nextsw.common.interceptors.exceptions.SkipDeleteInterceptorException;
import it.nextsw.common.repositories.NextSdrQueryDslRepository;
import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
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
    private ReporitoryConnectionManager aziendeConnectionManager;
 
    @Autowired
    private PersonaVedenteRepository personaVedenteRepository;

    @Autowired
    private UserInfoService userInfoService;
    
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
        Utente utente = authenticatedSessionData.getUser();
        Boolean pienaVisibilitaUtente = pienaVisibilita(doc, persona);
//        while (pienaVisibilitaUtente == null) {
//            LOGGER.warn("sono pienaVisibilitaUtente e sono NULL ");
//            pienaVisibilitaUtente = pienaVisibilita(doc, persona);
//            try {
//                Thread.sleep(100);
//            } catch (InterruptedException ex) {
//                
//            }
//        }
        // controllo se si tratta di un documento con visibilità normale
        if (doc.getVisibilita() == Doc.VisibilitaDoc.NORMALE){
          // controllo se siamo un attore del documento
          if (pienaVisibilitaUtente) {
            return entity;
          // nel caso in cui si tratti di un documento con visibilità limitata controllo se abbiamo il ruolo di osservatore o di Super Demiurgo
          } else if (userInfoService.isSD(utente) || userInfoService.isOS(utente) || userInfoService.isMOS(utente)){
            return entity;
          }else{
            throw new AbortLoadInterceptorException("non posso vedere gli allegati");
          }
        }
        // controllo se si tratta di un documento con visibilità limitata
        if (doc.getIdDocDetail().getVisibilitaLimitata() && !doc.getIdDocDetail().getRiservato()){
          // controllo se siamo un attore del documento
          if (pienaVisibilitaUtente) {
            return entity;
          // nel caso in cui si tratti di un documento con visibilità limitata controllo se abbiamo il ruolo di osservatore o di Super Demiurgo
          } else if (userInfoService.isSD(utente) || userInfoService.isOS(utente)){
            return entity;
          }else{
            throw new AbortLoadInterceptorException("non posso vedere gli allegati");
          }
        }
        // controllo se si tratta di un documento riservato
        if (doc.getIdDocDetail().getRiservato()){
          // nel caso in cui si tratti di un documento riservato o sono un attore o ho il ruolo SD
          if (pienaVisibilitaUtente) {
            return entity;
          } else if (userInfoService.isSD(utente)){
            return entity;
          } else {
            throw new AbortLoadInterceptorException("non posso vedere gli allegati");
          }
        }
        
        throw new AbortLoadInterceptorException("non posso vedere gli allegati");
//        if (pienaVisibilitaUtente) {
//            return entity;
//        } else {
//            throw new AbortLoadInterceptorException("non posso vedere gli allegati");
//        }
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
        LOGGER.info("id_doc"+doc.getId().toString());
        LOGGER.info("id_persona"+persona.getId().toString());
        hasPienaVisibilita = personaVedenteRepository.hasPienaVisibilita(doc.getId(), persona.getId());
//        JPAQueryFactory queryFactory = new JPAQueryFactory(entityManager);
//        hasPienaVisibilita = queryFactory
//            .select(QPersonaVedente.personaVedente.pienaVisibilita)
//            .from(QPersonaVedente.personaVedente)
//            .where(QPersonaVedente.personaVedente.idDocDetail.id.eq(doc.getId())
//                    .and(QPersonaVedente.personaVedente.idPersona.id.eq(persona.getId()))
//            )
//            .fetchOne();
        return hasPienaVisibilita == null ? false : hasPienaVisibilita;
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
            //commento la prossima riga perche non e' usata
//            MinIOWrapper minIOWrapper = aziendeConnectionManager.getMinIOWrapper();
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
            } catch (IllegalAccessException | IllegalArgumentException | NoSuchMethodException | InvocationTargetException ex) {
                LOGGER.error("errore nell'eliminazione del file su minIO", ex);
                throw new AbortSaveInterceptorException("errore nell'eliminazione del file su minIO", ex);
            }
        }
    }
}
