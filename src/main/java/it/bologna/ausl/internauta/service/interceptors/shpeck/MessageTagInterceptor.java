package it.bologna.ausl.internauta.service.interceptors.shpeck;

import it.bologna.ausl.internauta.service.authorization.AuthenticatedSessionData;
import it.bologna.ausl.internauta.service.interceptors.InternautaBaseInterceptor;
import it.bologna.ausl.internauta.service.krint.KrintShpeckService;
import it.bologna.ausl.internauta.service.krint.KrintUtils;
import it.bologna.ausl.model.entities.logs.OperazioneKrint;
import it.bologna.ausl.model.entities.shpeck.MessageTag;
import it.bologna.ausl.model.entities.shpeck.Tag;
import it.nextsw.common.annotations.NextSdrInterceptor;
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
 * @author Giuseppe Russo <g.russo@nsi.it> with GDM and Gus collaboration
 */
@Component
@NextSdrInterceptor(name = "messagetag-interceptor")
public class MessageTagInterceptor extends InternautaBaseInterceptor {
    private static final Logger LOGGER = LoggerFactory.getLogger(MessageTagInterceptor.class);
    
    @Autowired
    KrintShpeckService krintShpeckService;
    
    @Override
    public Class getTargetEntityClass() {
        return MessageTag.class;
    }

    @Override
    public Object beforeCreateEntityInterceptor(Object entity, Map<String, String> additionalData, HttpServletRequest request, boolean mainEntity, Class projectionClass) throws AbortSaveInterceptorException {
        LOGGER.info("in: beforeCreateEntityInterceptor di Message-Tag");
        AuthenticatedSessionData authenticatedSessionData = getAuthenticatedUserProperties();       
        MessageTag mt = (MessageTag) entity;
        if(additionalData != null && additionalData.get("setUtenteProtocollante") != null){
            mt.setIdUtente(authenticatedSessionData.getUser());           
        }
        
        // KRINT del tag aggiunto purché sia custom o "assigned" o "in_error"
        if (mainEntity && KrintUtils.doIHaveToKrint(request)) {
            if (mt.getIdTag().getName().equals(Tag.SystemTagName.in_error.toString())) {
                krintShpeckService.writeMessageTag(mt.getIdMessage(), mt.getIdTag(), OperazioneKrint.CodiceOperazione.PEC_MESSAGE_ERRORE_NON_VISTO);
            } else if (mt.getIdTag().getName().equals(Tag.SystemTagName.assigned.toString()) || mt.getIdTag().getType().toString().equals(Tag.TagType.CUSTOM.toString())) {
                krintShpeckService.writeMessageTag(mt.getIdMessage(), mt.getIdTag(), OperazioneKrint.CodiceOperazione.PEC_MESSAGE_AGGIUNTA_TAG);
            }
        }
        return mt;
    }

    @Override
    public void beforeDeleteEntityInterceptor(Object entity, Map<String, String> additionalData, HttpServletRequest request, boolean mainEntity, Class projectionClass) throws AbortSaveInterceptorException, SkipDeleteInterceptorException {
        // KRINT dell'eliminazione del tag purché sia custom o "assigned" o "in_error"
        MessageTag mt = (MessageTag) entity;
        
        if (mainEntity && KrintUtils.doIHaveToKrint(request)) {
            if (mt.getIdTag().getName().equals(Tag.SystemTagName.in_error.toString())) {
                krintShpeckService.writeMessageTag(mt.getIdMessage(), mt.getIdTag(), OperazioneKrint.CodiceOperazione.PEC_MESSAGE_ERRORE_VISTO);
            } else if (mt.getIdTag().getName().equals(Tag.SystemTagName.assigned.toString()) || mt.getIdTag().getType().toString().equals(Tag.TagType.CUSTOM.toString())) {
                krintShpeckService.writeMessageTag(mt.getIdMessage(), mt.getIdTag(), OperazioneKrint.CodiceOperazione.PEC_MESSAGE_ELIMINAZIONE_TAG);
            }
        }
        
        super.beforeDeleteEntityInterceptor(entity, additionalData, request, mainEntity, projectionClass); //To change body of generated methods, choose Tools | Templates.
    }
    
    
    
}