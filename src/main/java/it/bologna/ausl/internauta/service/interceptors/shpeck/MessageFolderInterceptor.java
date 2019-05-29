package it.bologna.ausl.internauta.service.interceptors.shpeck;

import it.bologna.ausl.internauta.service.interceptors.InternautaBaseInterceptor;
import it.bologna.ausl.model.entities.shpeck.MessageFolder;
import it.nextsw.common.annotations.NextSdrInterceptor;
import it.nextsw.common.interceptors.exceptions.AbortSaveInterceptorException;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 *
 * @author gusgus & jdieme
 */
@Component
@NextSdrInterceptor(name = "messagefolder-interceptor")
public class MessageFolderInterceptor extends InternautaBaseInterceptor {
    private static final Logger LOGGER = LoggerFactory.getLogger(MessageFolderInterceptor.class);
   
    @Override
    public Class getTargetEntityClass() {
        return MessageFolder.class;
    }

    @Override
    public Object beforeUpdateEntityInterceptor(Object entity, Object beforeUpdateEntity, Map<String, String> additionalData, HttpServletRequest request) throws AbortSaveInterceptorException {
        // TODO controllare che chi sta facendo sto update abbia almeno un permesso sulla casella del folder.
        // deve fare il contorllo una volta per più update (per via del batch che fa spostare più message in una volta sola?)
        
//        MessageFolder beforeupdateMessageFolder = (MessageFolder) beforeUpdateEntity;
//        MessageFolder messageFolder = (MessageFolder) entity;
//        messageFolder.setIdPreviousFolder(beforeupdateMessageFolder.getIdFolder());
//        return messageFolder;
        return entity;
    }
    
    
    
    
}
