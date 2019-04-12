package it.bologna.ausl.internauta.service.shpeck.utils;

import it.bologna.ausl.eml.handler.EmlHandler;
import it.bologna.ausl.eml.handler.EmlHandlerException;
import it.bologna.ausl.eml.handler.EmlHandlerResult;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

/**
 *
 * @author gusgus
 */
@Component
public class ShpeckCacheableFunctions {
    
    @Cacheable(value = "info_eml", key = "{#idMessage}", cacheManager = "emlCacheManager")
    public static EmlHandlerResult getInfoEml(Integer idMessage) throws EmlHandlerException {
        // TODO: Gestire idMessage.
        return EmlHandler.handleEml("C:\\Users\\Public\\prova8.eml");
        // prova 2 da problemi
    }
}
