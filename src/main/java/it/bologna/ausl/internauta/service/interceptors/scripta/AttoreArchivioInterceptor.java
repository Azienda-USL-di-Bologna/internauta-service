package it.bologna.ausl.internauta.service.interceptors.scripta;

import it.bologna.ausl.internauta.service.interceptors.InternautaBaseInterceptor;
import it.bologna.ausl.internauta.service.krint.KrintScriptaService;
import it.bologna.ausl.internauta.service.krint.KrintUtils;
import it.bologna.ausl.internauta.service.repositories.scripta.AttoreArchivioRepository;
import it.bologna.ausl.internauta.service.repositories.scrivania.AttivitaRepository;
import it.bologna.ausl.model.entities.logs.OperazioneKrint;
import it.bologna.ausl.model.entities.scripta.Archivio;
import it.bologna.ausl.model.entities.scripta.AttoreArchivio;
import it.bologna.ausl.model.entities.scripta.Massimario;
import it.bologna.ausl.model.entities.scripta.QAttoreArchivio;
import it.nextsw.common.data.annotations.NextSdrInterceptor;
import it.nextsw.common.controller.BeforeUpdateEntityApplier;
import it.nextsw.common.controller.exceptions.BeforeUpdateEntityApplierException;
import it.nextsw.common.interceptors.exceptions.AbortSaveInterceptorException;
import it.nextsw.common.interceptors.exceptions.SkipDeleteInterceptorException;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Level;
import javax.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 
 * @author gusgus
 */
@Component
@NextSdrInterceptor(name = "attorearchivio-interceptor")
public class AttoreArchivioInterceptor extends InternautaBaseInterceptor {

    private static final Logger LOGGER = LoggerFactory.getLogger(AttoreArchivioInterceptor.class);
    
    @Autowired
    private KrintScriptaService krintScriptaService;
    
    @Autowired
    private KrintUtils krintUtils;
    
    @Autowired
    private AttivitaRepository attivitaRepository;
    
    @Autowired
    private AttoreArchivioRepository attoreArchivioRepository;
    
    @Override
    public Class getTargetEntityClass() {
        return AttoreArchivio.class;
    }

    @Override
    public Object afterCreateEntityInterceptor(Object entity, Map<String, String> additionalData, HttpServletRequest request, boolean mainEntity, Class projectionClass) throws AbortSaveInterceptorException {
        AttoreArchivio attoreArchivio = (AttoreArchivio) entity;
        
        
        if (krintUtils.doIHaveToKrint(request)) {
           
            Archivio idArchivio = attoreArchivio.getIdArchivio();
            
            if (idArchivio.getLivello() == 1 && (attoreArchivio.getRuolo().equals(AttoreArchivio.RuoloAttoreArchivio.VICARIO)
                    || attoreArchivio.getRuolo().equals(AttoreArchivio.RuoloAttoreArchivio.RESPONSABILE_PROPOSTO))) {
                krintScriptaService.writeAttoreArchivioCreation(attoreArchivio, OperazioneKrint.CodiceOperazione.SCRIPTA_ATTORE_ARCHIVIO_CREATION);
            }        
        }   
        return super.afterCreateEntityInterceptor(entity, additionalData, request, mainEntity, projectionClass); //To change body of generated methods, choose Tools | Templates.
    }

//    @Override
//    public Object afterUpdateEntityInterceptor(Object entity, BeforeUpdateEntityApplier beforeUpdateEntityApplier, Map<String, String> additionalData, HttpServletRequest request, boolean mainEntity, Class projectionClass) throws AbortSaveInterceptorException {
//        AttoreArchivio attoreArchivio = (AttoreArchivio) entity;
//        if (krintUtils.doIHaveToKrint(request)) {
//            if (attoreArchivio.getRuolo().equals(AttoreArchivio.RuoloAttoreArchivio.VICARIO)
//                    || attoreArchivio.getRuolo().equals(AttoreArchivio.RuoloAttoreArchivio.RESPONSABILE_PROPOSTO)) {
//                krintScriptaService.writeAttoreArchivioUpdate(attoreArchivio, OperazioneKrint.CodiceOperazione.SCRIPTA_ATTORE_ARCHIVIO_UPDATE);
//            }
//        }
//        return super.afterUpdateEntityInterceptor(entity, beforeUpdateEntityApplier, additionalData, request, mainEntity, projectionClass); //To change body of generated methods, choose Tools | Templates.
//    }

    @Override
    public void beforeDeleteEntityInterceptor(Object entity, Map<String, String> additionalData, HttpServletRequest request, boolean mainEntity, Class projectionClass) throws AbortSaveInterceptorException, SkipDeleteInterceptorException {
        AttoreArchivio attoreArchivio = (AttoreArchivio) entity;
        if (krintUtils.doIHaveToKrint(request)) {
            QAttoreArchivio qAttoreArchivio = QAttoreArchivio.attoreArchivio;
            Optional<AttoreArchivio> findOne = attoreArchivioRepository.findOne(qAttoreArchivio.idArchivio.id.eq(attoreArchivio.getIdArchivio().getId()).and(qAttoreArchivio.id.ne(attoreArchivio.getId()))
                    .and(qAttoreArchivio.idPersona.id.eq(attoreArchivio.getIdPersona().getId())).and(qAttoreArchivio.ruolo.eq("RESPONSABILE")));
            if (findOne.isPresent() && attoreArchivio.getRuolo().equals(AttoreArchivio.RuoloAttoreArchivio.VICARIO)) {
                //qui sto sostuendo un vicario con un nuovo responsabile e non lo voglio krintare
            }   
            else if (attoreArchivio.getRuolo().equals(AttoreArchivio.RuoloAttoreArchivio.RESPONSABILE)
                    || attoreArchivio.getRuolo().equals(AttoreArchivio.RuoloAttoreArchivio.VICARIO)
                    || attoreArchivio.getRuolo().equals(AttoreArchivio.RuoloAttoreArchivio.RESPONSABILE_PROPOSTO)) {
                krintScriptaService.writeAttoreArchivioDelete(attoreArchivio, OperazioneKrint.CodiceOperazione.SCRIPTA_ATTORE_ARCHIVIO_DELETE);
            }
            
            
        }
        
        
        
        super.beforeDeleteEntityInterceptor(entity, additionalData, request, mainEntity, projectionClass); //To change body of generated methods, choose Tools | Templates.
    }
    
    @Override
    public Object beforeUpdateEntityInterceptor(Object entity, BeforeUpdateEntityApplier beforeUpdateEntityApplier, Map<String, String> additionalData, HttpServletRequest request, boolean mainEntity, Class projectionClass) throws AbortSaveInterceptorException {
        AttoreArchivio attoreArchivio = (AttoreArchivio) entity;
        QAttoreArchivio qAttoreArchivio = QAttoreArchivio.attoreArchivio;
        Optional<AttoreArchivio> findOne = attoreArchivioRepository.findOne(
                    qAttoreArchivio.ruolo.eq("RESPONSABILE")
                            .and(qAttoreArchivio.idArchivio.id.eq(attoreArchivio.getIdArchivio().getId()).and(qAttoreArchivio.id.ne(attoreArchivio.getId()))));
            boolean[] isResponsabileGetIntoVicario = { false };
            try {
            beforeUpdateEntityApplier.beforeUpdateApply(oldEntity -> {
                AttoreArchivio attoreOld = (AttoreArchivio) oldEntity;
                if(attoreOld.getRuolo().equals(AttoreArchivio.RuoloAttoreArchivio.RESPONSABILE) && attoreArchivio.getRuolo().equals(AttoreArchivio.RuoloAttoreArchivio.VICARIO)) {
                    isResponsabileGetIntoVicario[0] = true;
                }
            });
        } catch (BeforeUpdateEntityApplierException ex) {
 
        }
        if (findOne.isPresent() && attoreArchivio.getRuolo().equals(AttoreArchivio.RuoloAttoreArchivio.RESPONSABILE)) {
            // è un responsabile proposto che sta diventando responsabile
            AttoreArchivio attoreVecchio = findOne.get();
            krintScriptaService.writeAttoreArchivioAccetataResp(attoreArchivio,attoreVecchio.getIdPersona(), OperazioneKrint.CodiceOperazione.SCRIPTA_ATTORE_ACCETTATA_RESP);
            
        } else if (isResponsabileGetIntoVicario[0]){
            //non voglio krintare nulla perché è un responsabile che sta essendo trasformato in vicario 
        } else if (attoreArchivio.getRuolo().equals(AttoreArchivio.RuoloAttoreArchivio.VICARIO)
                || attoreArchivio.getRuolo().equals(AttoreArchivio.RuoloAttoreArchivio.RESPONSABILE_PROPOSTO) ||
                 attoreArchivio.getRuolo().equals(AttoreArchivio.RuoloAttoreArchivio.RESPONSABILE)) {
            krintScriptaService.writeAttoreArchivioUpdate(attoreArchivio, OperazioneKrint.CodiceOperazione.SCRIPTA_ATTORE_ARCHIVIO_UPDATE);

        }
        
        return super.beforeUpdateEntityInterceptor(attoreArchivio, beforeUpdateEntityApplier, additionalData, request, mainEntity, projectionClass);
    }
    

    @Override
    public void afterDeleteEntityInterceptor(Object entity, Map<String, String> additionalData, HttpServletRequest request, boolean mainEntity, Class projectionClass) throws AbortSaveInterceptorException, SkipDeleteInterceptorException {
        AttoreArchivio attoreArchivio = (AttoreArchivio) entity;
        if (attoreArchivio.getRuolo().equals(AttoreArchivio.RuoloAttoreArchivio.RESPONSABILE_PROPOSTO)) {
            attivitaRepository.deleteByAttoreArchivio(attoreArchivio.getIdPersona().getId(), attoreArchivio.getIdArchivio().getId().toString(), "ArchivioInternauta");
        }
        
        super.afterDeleteEntityInterceptor(entity, additionalData, request, mainEntity, projectionClass); //To change body of generated methods, choose Tools | Templates.
    }
    
    

}
