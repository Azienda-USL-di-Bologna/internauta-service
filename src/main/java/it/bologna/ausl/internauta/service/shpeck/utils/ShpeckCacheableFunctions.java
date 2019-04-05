package it.bologna.ausl.internauta.service.shpeck.utils;

import it.bologna.ausl.eml.handler.EmlHandler;
import it.bologna.ausl.eml.handler.EmlHandlerException;
import it.bologna.ausl.eml.handler.EmlHandlerResult;
import it.bologna.ausl.model.entities.shpeck.Message;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

/**
 *
 * @author gusgus
 */
@Component
public class ShpeckCacheableFunctions {
    
    @Cacheable(value = "info_eml", key = "{#message.getId()}", cacheManager = "emlCacheManager")
    public static EmlHandlerResult getInfoEml(Message message) throws EmlHandlerException {
        // TODO: Gestire idMessage.
        return EmlHandler.handleEml("C:\\Users\\Public\\prova.eml");
    }
}
