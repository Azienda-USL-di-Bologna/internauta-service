package it.bologna.ausl.internauta.service.interceptors.scripta;

import it.bologna.ausl.internauta.service.interceptors.InternautaBaseInterceptor;
import it.bologna.ausl.internauta.service.krint.KrintRubricaService;
import it.bologna.ausl.internauta.service.krint.KrintUtils;
import it.bologna.ausl.model.entities.logs.OperazioneKrint;
import it.bologna.ausl.model.entities.scripta.AttoreArchivio;
import it.nextsw.common.annotations.NextSdrInterceptor;
import it.nextsw.common.interceptors.exceptions.AbortSaveInterceptorException;
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
    private KrintRubricaService krintRubricaService;
    
    @Autowired
    private KrintUtils krintUtils;
    
    @Override
    public Class getTargetEntityClass() {
        return AttoreArchivio.class;
    }

//    @Override
//    public Object afterCreateEntityInterceptor(Object entity, Map<String, String> additionalData, HttpServletRequest request, boolean mainEntity, Class projectionClass) throws AbortSaveInterceptorException {
//        AttoreArchivio attoreArchivio = (AttoreArchivio) entity;
//        if (krintUtils.doIHaveToKrint(request)) {
//            if (attoreArchivio.getRuolo().equals(AttoreArchivio.RuoloAttoreArchivio.RESPONSABILE)
//                    || attoreArchivio.getRuolo().equals(AttoreArchivio.RuoloAttoreArchivio.VICARIO)
//                    || attoreArchivio.getRuolo().equals(AttoreArchivio.RuoloAttoreArchivio.RESPONSABILE_PROPOSTO)) {
//                krintRubricaService.writeContactDetailCreation(dettaglioContatto, OperazioneKrint.CodiceOperazione.RUBRICA_CONTACT_DETAIL_CREATION);
//            }
//        }
//        return super.afterCreateEntityInterceptor(entity, additionalData, request, mainEntity, projectionClass); //To change body of generated methods, choose Tools | Templates.
//    }

}
