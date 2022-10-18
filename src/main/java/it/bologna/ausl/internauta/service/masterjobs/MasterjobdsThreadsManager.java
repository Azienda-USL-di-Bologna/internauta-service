package it.bologna.ausl.internauta.service.masterjobs;

import it.bologna.ausl.internauta.service.masterjobs.executors.MasterjobsExecutionThread;
import it.bologna.ausl.internauta.service.masterjobs.executors.MasterjobsHighPriorityExecutionThread;
import it.bologna.ausl.internauta.service.masterjobs.executors.MasterjobsHighestPriorityExecutionThread;
import it.bologna.ausl.internauta.service.masterjobs.executors.MasterjobsNormalPriorityExecutionThread;
import it.bologna.ausl.internauta.service.masterjobs.executors.MasterjobsWaitQueueExecutionThread;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 *
 * @author gdm
 */
@Component
public class MasterjobdsThreadsManager {
    
    @Value("${masterjobs.manager.normal-priority-threads-number}")
    private int normalPriorityThreadsNumber;
    
    @Value("${masterjobs.manager.high-priority-threads-number}")
    private int highPriorityThreadsNumber;
    
    @Value("${masterjobs.manager.highest-priority-threads-number}")
    private int highestPriorityThreadsNumber;
    
    @Value("${masterjobs.manager.wait-queue-threads-number}")
    private Integer waitQueueThreadsNumber;
    
    @Autowired
    private BeanFactory beanFactory;
    
    @Autowired
    private MasterjobsObjectsFactory masterjobsObjectsFactory;
    
//    @Autowired
//    TransactionTemplate transactionTemplate;
    
    public void scheduleThreads(){
        ExecutorService executor = Executors.newFixedThreadPool(
                normalPriorityThreadsNumber + 
                highPriorityThreadsNumber + 
                highestPriorityThreadsNumber +
                waitQueueThreadsNumber);
        
        scheduleExecutionThreads(normalPriorityThreadsNumber, executor, MasterjobsNormalPriorityExecutionThread.class);
        scheduleExecutionThreads(highPriorityThreadsNumber, executor, MasterjobsHighPriorityExecutionThread.class);
        scheduleExecutionThreads(highestPriorityThreadsNumber, executor, MasterjobsHighestPriorityExecutionThread.class);
        scheduleExecutionThreads(waitQueueThreadsNumber, executor, MasterjobsWaitQueueExecutionThread.class);
    }
    
    private void scheduleExecutionThreads(int threadsNumber, ExecutorService executor, Class<? extends MasterjobsExecutionThread> classz) {
        for (int i = 0; i < threadsNumber; i++) {
            MasterjobsExecutionThread executionThreadObject = masterjobsObjectsFactory.getExecutionThreadObject(classz);
            executor.execute(executionThreadObject);
        }
    }
    
    private void scheduleServiceThreads() {
    }
}
