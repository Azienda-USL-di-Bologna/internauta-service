package it.bologna.ausl.internauta.service.interceptors.shpeck;

import it.bologna.ausl.internauta.service.interceptors.InternautaBaseInterceptor;
import it.bologna.ausl.model.entities.shpeck.MessageTag;
import it.nextsw.common.annotations.NextSdrInterceptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 *
 * @author Giuseppe Russo <g.russo@nsi.it> with GDM and Gus collaboration
 */
@Component
@NextSdrInterceptor(name = "messagetag-interceptor")
public class MessageTagInterceptor extends InternautaBaseInterceptor {
    private static final Logger LOGGER = LoggerFactory.getLogger(MessageTagInterceptor.class);

    
    @Override
    public Class getTargetEntityClass() {
        return MessageTag.class;
    }

//    @Override
//    public Object beforeCreateEntityInterceptor(Object entity, Map<String, String> additionalData, HttpServletRequest request, boolean mainEntity, Class projectionClass) throws AbortSaveInterceptorException {
//        LOGGER.info("in: beforeCreateEntityInterceptor di Message-Tag");
//        getAuthenticatedUserProperties();       
//        MessageTag mt = (MessageTag)entity;
//        if(additionalData.get("setUtenteProtocollante") != null){
//            mt.setIdUtente(user);           
//        }        
//        return mt;
//    }
    
}