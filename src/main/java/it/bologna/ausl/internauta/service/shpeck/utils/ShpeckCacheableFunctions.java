package it.bologna.ausl.internauta.service.shpeck.utils;

import it.bologna.ausl.eml.handler.EmlHandler;
import it.bologna.ausl.eml.handler.EmlHandlerException;
import it.bologna.ausl.eml.handler.EmlHandlerResult;
import java.io.UnsupportedEncodingException;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

/**
 *
 * @author gusgus
 */
@Component
public class ShpeckCacheableFunctions {
    
    @Cacheable(value = "info_eml", key = "{#idMessage}", cacheManager = "emlCacheManager")
    public static EmlHandlerResult getInfoEml(Integer idMessage, String repositoryTemp) throws EmlHandlerException, UnsupportedEncodingException {
        // TODO: Gestire idMessage.
        return EmlHandler.handleEml(repositoryTemp + idMessage + ".eml");
        // prova 2 da problemi
    }
}
