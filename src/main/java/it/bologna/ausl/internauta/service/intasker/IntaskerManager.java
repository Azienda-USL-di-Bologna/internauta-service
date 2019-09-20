package it.bologna.ausl.internauta.service.intasker;

import java.util.concurrent.ScheduledThreadPoolExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

/**
 *
 * @author gdm
 */
//@Component
public class IntaskerManager {
    private static final Logger log = LoggerFactory.getLogger(IntaskerManager.class);
    
    @Value("${intasker.manager.threads-number}")
    Integer threadsNumber;
    
    @Value("${intasker.manager.sleep-millis}")
    Integer sleepMillis;
    
    @Autowired
    private ScheduledThreadPoolExecutor scheduledThreadPoolExecutor;
    
    public void run() {
        for (int i = 0; i < threadsNumber; i++) {
        }
    }
}
