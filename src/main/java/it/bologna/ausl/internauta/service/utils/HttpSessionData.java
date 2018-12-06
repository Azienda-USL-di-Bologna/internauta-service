package it.bologna.ausl.internauta.service.utils;

import java.util.HashMap;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 *
 * @author Giuseppe Russo <g.russo@nsi.it>
 */
@Component
public class HttpSessionData {
    private final ThreadLocal<Map<String, Object>> dataMap = new ThreadLocal<>();
    
    public void putData(String key, Object data) {
        Map<String, Object> map = this.dataMap.get();
        if (map == null)
            map = new HashMap<>();
        map.put(key, data);
        this.dataMap.set(map);
    }
    
    public Object getData(String key) {
        Map<String, Object> map = this.dataMap.get();
         if (map != null)
            return map.get(key);
         else
             return null;
    }
    
    public void resetDataMap() {
        dataMap.remove();
    }
}
