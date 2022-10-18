package it.bologna.ausl.internauta.service.masterjobs.configuration;

import it.bologna.ausl.internauta.service.masterjobs.exceptions.MaterjobsConfigurationException;
import it.bologna.ausl.internauta.service.masterjobs.workers.Worker;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.reflections.Reflections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 *
 * @author gdm
 */
@Configuration
public class MasterjobsInitialization {
    private static final Logger log = LoggerFactory.getLogger(MasterjobsInitialization.class);
    
    @Autowired
    private BeanFactory beanFactory;
    
    /**
     * Crea una mappa con chiave il nome del Worker e valore la classe corrispondente
     * @return
     * @throws MaterjobsConfigurationException 
     */
    @Bean
    public Map<String, Class<? extends Worker>> workerMap() throws MaterjobsConfigurationException {
        Map<String, Class<? extends Worker>> workerMap = new HashMap();
        
        Set<Class<? extends Worker>> workersSet = new Reflections(Worker.class.getPackage().getName()).getSubTypesOf(Worker.class);
        
        for (Class<? extends Worker> workerClass : workersSet) {
            Worker workerInstance;
            try {
                workerInstance = beanFactory.getBean(workerClass);
            } catch (Exception ex) {
                String errorMessage = "errore nella creazione della mappa dei worker";
                log.error("errore nella creazione della mappa dei worker", ex);
                throw new MaterjobsConfigurationException(errorMessage);
            }
            workerMap.put(workerInstance.getName(), workerClass);
        }
        
        return workerMap;
    }
}
