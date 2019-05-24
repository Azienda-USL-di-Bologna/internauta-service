package it.bologna.ausl.internauta.service.shpeck.utils;

import it.bologna.ausl.eml.handler.EmlHandler;
import it.bologna.ausl.eml.handler.EmlHandlerException;
import it.bologna.ausl.eml.handler.EmlHandlerResult;
import it.bologna.ausl.internauta.service.exceptions.BadParamsException;
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
    private ShpeckUtils shpeckUtils;

    public EmlHandlerResult getInfoEmlNonCacheable (ShpeckUtils.EmlSource emlSource, Integer id) throws EmlHandlerException, UnsupportedEncodingException, BadParamsException, IOException {
        File downloadEml = null;
        try {
            downloadEml = shpeckUtils.downloadEml(emlSource, id);
            return EmlHandler.handleEml(downloadEml.getAbsolutePath());
        } finally {
            if (downloadEml != null) {
                downloadEml.delete();
            }
        }
    }
    
    @Cacheable(value = "info_eml", key = "{#emlSource.toString(), #id}", cacheManager = "emlCacheManager")
    public EmlHandlerResult getInfoEmlCacheable (ShpeckUtils.EmlSource emlSource, Integer id) throws EmlHandlerException, UnsupportedEncodingException, BadParamsException, IOException {
        File downloadEml = null;
        try {
            downloadEml = shpeckUtils.downloadEml(emlSource, id);
            return EmlHandler.handleEml(downloadEml.getAbsolutePath());
        } finally {
            if (downloadEml != null) {
                downloadEml.delete();
            }
        }
        
    }
        
    /**
     * Effettua il download dell' eml richiesto 
     * @param emlSource
     * @param id
     * @return
     * @throws EmlHandlerException
     * @throws UnsupportedEncodingException
     * @throws BadParamsException
     * @throws IOException 
     */
//    public File downloadEml(ShpeckUtils.EmlSource emlSource, Integer id) throws EmlHandlerException, UnsupportedEncodingException, BadParamsException, IOException {
//
//        File downloadedEml = null;
//        try {
//            downloadedEml = shpeckUtils.downloadEml(emlSource, id);
//        }
////                    catch (BadParamsException | IOException | EmlHandlerException ex) {
////                        Logger.getLogger(ShpeckCacheableFunctions.class.getName()).log(Level.SEVERE, null, ex);
////                        return null ;
////                    } 
//        finally {
//            if (downloadedEml != null && downloadedEml.exists()) {
//                downloadedEml.delete();
//            }
//        }
////                    concurrentMap.put(id.toString(), handledEml);
//        return  downloadedEml;
//    }
}
