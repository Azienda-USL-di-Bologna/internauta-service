package it.bologna.ausl.internauta.service.shpeck.utils;

import it.bologna.ausl.eml.handler.EmlHandler;
import it.bologna.ausl.eml.handler.EmlHandlerException;
import it.bologna.ausl.eml.handler.EmlHandlerResult;
import it.bologna.ausl.internauta.service.exceptions.BadParamsException;
import it.bologna.ausl.internauta.service.utils.MemoryAnalizerService;
import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

/**
 *
 * @author gusgus
 */
@Component
public class ShpeckCacheableFunctions {
    
    @Autowired
    private MemoryAnalizerService memoryAnalizerService;
    
    @Autowired
    private ShpeckUtils shpeckUtils;
    
    @Cacheable(value = "info_eml", key = "{#emlSource.toString(), #id}", cacheManager = "emlCacheManager", condition = "{#emlSource.toString() != 'DRAFT'}")
    public EmlHandlerResult getInfoEml(ShpeckUtils.EmlSource emlSource, Integer id) throws EmlHandlerException, UnsupportedEncodingException, BadParamsException, IOException {
        File downloadEml = null;
        try {
            downloadEml = shpeckUtils.downloadEml(emlSource, id);
            this.memoryAnalizerService.handleIncrementMessage((int) downloadEml.length());
            return EmlHandler.handleEml(downloadEml.getAbsolutePath(), false);
        } finally {
            if (downloadEml != null) {
                downloadEml.delete();
            }
        }
    }
    
    public EmlHandlerResult getInfoEmlWithAttachmentsStreamNoCache(ShpeckUtils.EmlSource emlSource, Integer id) throws EmlHandlerException, UnsupportedEncodingException, BadParamsException, IOException {
        File downloadEml = null;
        try {
            downloadEml = shpeckUtils.downloadEml(emlSource, id);
            this.memoryAnalizerService.handleIncrementMessage((int) downloadEml.length());
            return EmlHandler.handleEml(downloadEml.getAbsolutePath(), true);
        } finally {
            if (downloadEml != null) {
                downloadEml.delete();
            }
        }
    }
    
//    @CacheEvict(value = "info_eml", key = "{#emlSource.toString(), #id}", cacheManager = "emlCacheManager")
//    public void getInfoEmlRemoveCache(ShpeckUtils.EmlSource emlSource, Integer id) {
//    }

//    @Cacheable(value = "gdmgdm", key = "{#id}", cacheManager = "emlCacheManager", condition = "{#id != 1}")
//    public String testCache(Integer id) {
//        return "gdm";
//    }
}
