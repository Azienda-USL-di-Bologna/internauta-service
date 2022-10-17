package it.bologna.ausl.internauta.service.masterjobs.workers;

import com.google.common.base.CaseFormat;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 *
 * @author gdm
 */
@Component
public class WorkerFactory {

    @Autowired
    private Map<String, Class> workerMap;
    
    public Worker getWorker(String name) {
//        CaseFormat.LOWER_CAMEL.to(CaseFormat.UPPER_CAMEL, name.toLowerCase() + )
    return null;
    }
}
