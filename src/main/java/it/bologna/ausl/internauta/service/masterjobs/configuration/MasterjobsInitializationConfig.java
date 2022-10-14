package it.bologna.ausl.internauta.service.masterjobs.configuration;

import it.bologna.ausl.internauta.service.masterjobs.exceptions.MaterjobsConfigurationException;
import it.bologna.ausl.internauta.service.masterjobs.workers.foo.FooWorker;
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
import org.springframework.data.rest.core.config.Projection;

/**
 *
 * @author gdm
 */
@Configuration
public class MasterjobsInitializationConfig {
    private static final Logger log = LoggerFactory.getLogger(MasterjobsInitializationConfig.class);
    
    @Autowired
    private BeanFactory beanFactory;
    
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
        
        System.out.println("aaaaaaaaaaaaa");
        
        return workerMap;
    }
}
