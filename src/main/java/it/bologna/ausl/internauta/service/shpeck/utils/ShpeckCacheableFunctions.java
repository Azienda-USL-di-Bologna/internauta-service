package it.bologna.ausl.internauta.service.shpeck.utils;

import edu.emory.mathcs.backport.java.util.concurrent.ConcurrentHashMap;
import it.bologna.ausl.eml.handler.EmlHandler;
import it.bologna.ausl.eml.handler.EmlHandlerException;
import it.bologna.ausl.eml.handler.EmlHandlerResult;
import it.bologna.ausl.internauta.service.exceptions.BadParamsException;
import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;
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
    
//    private static final Map<String, EmlHandlerResult> concurrentMap = new ConcurrentHashMap();
    private static final Map<String, EmlHandlerResult> concurrentMap = new HashMap<>();
    private static final Map<Integer, Integer> locks = new HashMap<>();
    private static final Lock lock = new ReentrantLock()
    
    @Cacheable(value = "info_eml", key = "{#emlSource.toString(), #id}", cacheManager = "emlCacheManager")
    public EmlHandlerResult getInfoEml(ShpeckUtils.EmlSource emlSource, Integer id) throws EmlHandlerException, UnsupportedEncodingException, BadParamsException, IOException {
//        synchronized (concurrentMap.computeIfAbsent(emlSource.toString() + "_" + id, k -> {
        //concurrentMap.put(id.toString(), new Object());
        
        Integer res = locks.get(id);
        if (res != null) {
            synchronized (lock.) {
                locks.put(id, id);
                // cose
            }
        } else {
                locks.put(id, id);
                // cose
            
        }
        
        
        if (res != null) {
            synchronized (lock) {
                if (concurrentMap.get(id.toString()) == null) {
                    File downloadedEml = null;
                    EmlHandlerResult handledEml = null;
                    try {
                        downloadedEml = shpeckUtils.downloadEml(emlSource, id);
                        handledEml = EmlHandler.handleEml(downloadedEml.getAbsolutePath());
                    }
                    catch (BadParamsException | IOException | EmlHandlerException ex) {
                        Logger.getLogger(ShpeckCacheableFunctions.class.getName()).log(Level.SEVERE, null, ex);
                        return null ;
                    } finally {
                        if (downloadedEml != null && downloadedEml.exists()) {
                            downloadedEml.delete();
                        }
                    }
                    concurrentMap.put(id.toString(), handledEml);
                }
            }
        }
        return concurrentMap.get(emlSource.toString() + "_" + id);
    }
}
