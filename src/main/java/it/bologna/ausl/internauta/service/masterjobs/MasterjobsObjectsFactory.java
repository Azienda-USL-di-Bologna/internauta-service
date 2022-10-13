package it.bologna.ausl.internauta.service.masterjobs;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.bologna.ausl.internauta.service.masterjobs.exceptions.MasterjobsBadDataException;
import it.bologna.ausl.internauta.service.masterjobs.executors.MasterjobsExecutionThread;
import it.bologna.ausl.internauta.service.masterjobs.workers.Worker;
import it.bologna.ausl.internauta.service.masterjobs.workers.WorkerData;
import it.bologna.ausl.internauta.service.masterjobs.workers.WorkerDataInterface;
import it.bologna.ausl.internauta.service.masterjobs.workers.WorkerDeferredData;
import it.bologna.ausl.model.entities.masterjobs.Set;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 *
 * @author gdm
 */
@Component
public class MasterjobsObjectsFactory {
    private static final Logger log = LoggerFactory.getLogger(MasterjobsObjectsFactory.class);
    
    @Value("${masterjobs.manager.redis-active-threads-set-name}")
    private String activeThreadsSetName;
    
    @Value("${masterjobs.manager.in-redis-queue-normal}")
    private String inQueueNormal;
    
    @Value("${masterjobs.manager.in-redis-queue-high}")
    private String inQueueHigh;
    
    @Value("${masterjobs.manager.in-redis-queue-highest}")
    private String inQueueHighest;
    
    @Value("${masterjobs.manager.work-redis-queue}")
    private String workQueue;
    
    @Value("${masterjobs.manager.error-redis-queue}")
    private String errorQueue;
    
    @Value("${masterjobs.manager.wait-redis-queue}")
    private String waitQueue;
    
    @Value("${masterjobs.manager.out-redis-queue}")
    private String outQueue;
    
    @Value("${masterjobs.manager.sleep-millis}")
    private int sleepMillis;
    
    @Value("${masterjobs.manager.queue-read-timeout-millis}")
    private int queueReadTimeoutMillis;
    
    @Autowired
    private Map<String, Class<? extends Worker>> workerMap;
    
    @Autowired
    private BeanFactory beanFactory;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    public MasterjobsQueueData getMasterjobsQueueDataFromString(String data) throws JsonProcessingException {
        MasterjobsQueueData masterjobsQueueData = this.objectMapper.readValue(data, MasterjobsQueueData.class);
        masterjobsQueueData.setObjectMapper(objectMapper);
        return masterjobsQueueData;
    }
    
    public MasterjobsQueueData buildMasterjobsQueueData(List<Long> jobsId, Long setId) {
        MasterjobsQueueData queueData = new MasterjobsQueueData(objectMapper);
        queueData.setJobs(jobsId);
        queueData.setSet(setId);
        return queueData;
    }
    
    public <T extends MasterjobsExecutionThread> T getExecutionThreadObject(Class<T> classz) {
        T executionThreadObject = beanFactory.getBean(classz);
        executionThreadObject
            .activeThreadsSetName(activeThreadsSetName)
            .inQueueNormal(inQueueNormal)
            .inQueueHigh(inQueueHigh)
            .inQueueHighest(inQueueHighest)
            .workQueue(workQueue)
            .errorQueue(errorQueue)
            .waitQueue(waitQueue)
            .sleepMillis(sleepMillis)
            .queueReadTimeoutMillis(queueReadTimeoutMillis)
            .self(executionThreadObject);
        return executionThreadObject;
    }
    
    public Worker getWorker(String name, WorkerDataInterface workerData, boolean deferred) {
        Worker worker = getWorker(workerMap.get(name), workerData, deferred);
        return worker;
    }
    
    public <T extends Worker> T getWorker(Class<T> workerClass, WorkerDataInterface workerData, boolean deferred) {
        T worker = beanFactory.getBean(workerClass);
        if (deferred) {
            worker.buildDeferred((WorkerDeferredData) workerData);
        } else {
            worker.build((WorkerData) workerData);
        }
        return worker;
    }
    
    public String getQueueBySetPriority(Set.SetPriority setPriority) throws MasterjobsBadDataException {
        String destinationQueue;
        switch (setPriority) {
            case NORMAL:
                destinationQueue = this.inQueueNormal;
                break;
            case HIGH:
                destinationQueue = this.inQueueHigh;
                break;
            case HIGHEST:
                destinationQueue = this.inQueueHighest;
                break;
            default:
                String errorMessage = String.format("priority %s not excepted", setPriority);
                log.error(errorMessage);
                throw new MasterjobsBadDataException(errorMessage);
        }
        return destinationQueue;
    }
}
