package it.bologna.ausl.internauta.service.interceptors.scripta;

import it.bologna.ausl.internauta.service.interceptors.InternautaBaseInterceptor;
import it.bologna.ausl.internauta.service.krint.KrintScriptaService;
import it.bologna.ausl.internauta.service.krint.KrintUtils;
import it.bologna.ausl.internauta.service.repositories.scrivania.AttivitaRepository;
import it.bologna.ausl.model.entities.logs.OperazioneKrint;
import it.bologna.ausl.model.entities.scripta.Archivio;
import it.bologna.ausl.model.entities.scripta.AttoreArchivio;
import it.nextsw.common.annotations.NextSdrInterceptor;
import it.nextsw.common.controller.BeforeUpdateEntityApplier;
import it.nextsw.common.interceptors.exceptions.AbortSaveInterceptorException;
import it.nextsw.common.interceptors.exceptions.SkipDeleteInterceptorException;
import java.util.Map;
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

    @Override
    public Object afterUpdateEntityInterceptor(Object entity, BeforeUpdateEntityApplier beforeUpdateEntityApplier, Map<String, String> additionalData, HttpServletRequest request, boolean mainEntity, Class projectionClass) throws AbortSaveInterceptorException {
        AttoreArchivio attoreArchivio = (AttoreArchivio) entity;
        if (krintUtils.doIHaveToKrint(request)) {
            if (attoreArchivio.getRuolo().equals(AttoreArchivio.RuoloAttoreArchivio.RESPONSABILE)
                    || attoreArchivio.getRuolo().equals(AttoreArchivio.RuoloAttoreArchivio.VICARIO)
                    || attoreArchivio.getRuolo().equals(AttoreArchivio.RuoloAttoreArchivio.RESPONSABILE_PROPOSTO)) {
                krintScriptaService.writeAttoreArchivioUpdate(attoreArchivio, OperazioneKrint.CodiceOperazione.SCRIPTA_ATTORE_ARCHIVIO_UPDATE);
            }
        }
        return super.afterUpdateEntityInterceptor(entity, beforeUpdateEntityApplier, additionalData, request, mainEntity, projectionClass); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void beforeDeleteEntityInterceptor(Object entity, Map<String, String> additionalData, HttpServletRequest request, boolean mainEntity, Class projectionClass) throws AbortSaveInterceptorException, SkipDeleteInterceptorException {
        AttoreArchivio attoreArchivio = (AttoreArchivio) entity;
        if (krintUtils.doIHaveToKrint(request)) {
            if (attoreArchivio.getRuolo().equals(AttoreArchivio.RuoloAttoreArchivio.RESPONSABILE)
                    || attoreArchivio.getRuolo().equals(AttoreArchivio.RuoloAttoreArchivio.VICARIO)
                    || attoreArchivio.getRuolo().equals(AttoreArchivio.RuoloAttoreArchivio.RESPONSABILE_PROPOSTO)) {
                krintScriptaService.writeAttoreArchivioDelete(attoreArchivio, OperazioneKrint.CodiceOperazione.SCRIPTA_ATTORE_ARCHIVIO_DELETE);
            }
        }
        
        
        
        super.beforeDeleteEntityInterceptor(entity, additionalData, request, mainEntity, projectionClass); //To change body of generated methods, choose Tools | Templates.
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
