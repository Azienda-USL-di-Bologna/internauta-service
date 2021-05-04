package it.bologna.ausl.internauta.service.interceptors.scripta;

import it.bologna.ausl.internauta.service.configuration.nextsdr.RestControllerEngineImpl;
import it.bologna.ausl.internauta.service.configuration.utils.ReporitoryConnectionManager;
import it.bologna.ausl.internauta.service.interceptors.InternautaBaseInterceptor;
import it.bologna.ausl.internauta.service.repositories.scripta.AllegatoRepository;
import it.bologna.ausl.internauta.service.repositories.scripta.DettaglioAllegatoRepository;
import it.bologna.ausl.minio.manager.MinIOWrapper;
import it.bologna.ausl.model.entities.scripta.Allegato;
import it.nextsw.common.annotations.NextSdrInterceptor;
import it.nextsw.common.interceptors.exceptions.AbortSaveInterceptorException;
import it.nextsw.common.interceptors.exceptions.SkipDeleteInterceptorException;
import it.nextsw.common.repositories.NextSdrQueryDslRepository;
import java.util.List;
import java.util.Map;
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
    private RestControllerEngineImpl restControllerEngine;
    
    @Autowired
    ReporitoryConnectionManager aziendeConnectionManager;

    @Autowired
    AllegatoRepository allegatoRepository;
    
    @Autowired
    DettaglioAllegatoRepository dettaglioAllegatoRepository;
    
    
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

    @Override
    public void afterDeleteEntityInterceptor(Object entity, Map<String, String> additionalData, HttpServletRequest request, boolean mainEntity, Class projectionClass) throws AbortSaveInterceptorException, SkipDeleteInterceptorException {
//        /*
//            Sto cancellando un allegato, dunque:
//            - Se è un allegato contenitore devo cancellare i file correlati e anche in questo caso dal repository 
//            - Devo cancellare il file dal repository
//            - Un trigger si occupa di aggiustare la numerazione degli allegati
//         */
        Allegato allegato = (Allegato) entity;
        MinIOWrapper minIOWrapper = aziendeConnectionManager.getMinIOWrapper();

        // Cancello gli eventuali allegati figli.
        List<Allegato> allegatiFigliList = allegato.getAllegatiFigliList();
        for (Allegato a : allegatiFigliList) {
            allegatoRepository.delete(a);
        }
        super.afterDeleteEntityInterceptor(entity, additionalData, request, mainEntity, projectionClass); //To change body of generated methods, choose Tools | Templates.
    }
}
