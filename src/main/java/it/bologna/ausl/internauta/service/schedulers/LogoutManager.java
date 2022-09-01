package it.bologna.ausl.internauta.service.schedulers;

import it.bologna.ausl.internauta.service.schedulers.workers.logoutmanager.LogoutManagerWorker;
import it.bologna.ausl.internauta.service.schedulers.workers.messagesender.MessageSenderWorker;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 *
 * @author gdm
 */
@Component
public class LogoutManager {
    @Autowired
    private ScheduledThreadPoolExecutor scheduledThreadPoolExecutor;
    
//    @Autowired
//    LogoutManagerWorker logoutManagerWorker;
    
    @Autowired
    private BeanFactory beanFactory;
    
    
    public void scheduleLogoutManager() {
        LogoutManagerWorker logoutManagerWorker = beanFactory.getBean(LogoutManagerWorker.class);
        scheduledThreadPoolExecutor.scheduleWithFixedDelay(logoutManagerWorker, 0, 5, TimeUnit.MINUTES);
    }
    

}
