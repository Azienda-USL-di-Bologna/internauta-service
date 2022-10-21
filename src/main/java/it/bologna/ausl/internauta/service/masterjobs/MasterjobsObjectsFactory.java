package it.bologna.ausl.internauta.service.masterjobs;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.bologna.ausl.internauta.service.masterjobs.exceptions.MasterjobsBadDataException;
import it.bologna.ausl.internauta.service.masterjobs.executors.jobs.MasterjobsJobsExecutionThread;
import it.bologna.ausl.internauta.service.masterjobs.workers.Worker;
import it.bologna.ausl.internauta.service.masterjobs.workers.jobs.JobWorker;
import it.bologna.ausl.internauta.service.masterjobs.workers.jobs.JobWorkerData;
import it.bologna.ausl.internauta.service.masterjobs.workers.jobs.JobWorkerDeferredData;
import it.bologna.ausl.model.entities.masterjobs.Set;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import it.bologna.ausl.internauta.service.masterjobs.workers.jobs.JobWorkerDataInterface;
import it.bologna.ausl.internauta.service.masterjobs.workers.services.ServiceWorker;

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
    
    public <T extends MasterjobsJobsExecutionThread> T getJobsExecutionThreadObject(Class<T> classz) {
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
    
     /**
     * Torna un JobWorker del nome passato, costruito con i dati passati
     * @param name il nome del JobWorker (quello che viene tornato dal metodo getName())
     * @param workerData i dati del job (JobWorkerDeferredData se il job è deferred JobWorkerData se non lo è)
     * @param deferred "true" se il job è deferred, "false" altrimenti
     * @return un JobWorker con del nome passato, costruito con i dati passati
     */
    public JobWorker getJobWorker(String name, JobWorkerDataInterface workerData, boolean deferred) {
        Class<? extends JobWorker> jobWorkerClass = (Class<? extends JobWorker>)workerMap.get(name);
        JobWorker worker = getJobWorker(jobWorkerClass, workerData, deferred);
        return worker;
    }
    
    /**
     * Torna un JobWorker della classe passata, costruito con i dati passati
     * @param <T>
     * @param jobWorkerClass la classe concreta del JobWorker desiderato
     * @param workerData i dati del job (JobWorkerDeferredData se il job è deferred JobWorkerData se non lo è)
     * @param deferred "true" se il job è deferred, "false" altrimenti
     * @return un JobWorker della classe passata, costruito con i dati passati
     */
    public <T extends JobWorker> T getJobWorker(Class<T> jobWorkerClass, JobWorkerDataInterface workerData, boolean deferred) {
        T worker = beanFactory.getBean(jobWorkerClass);
        if (deferred) {
            worker.buildDeferred((JobWorkerDeferredData) workerData);
        } else {
            worker.build((JobWorkerData) workerData);
        }
        return worker;
    }
    
    /**
     * Torna un ServiceWorker del nome passato
     * @param name il nome del ServiceWorker (quello che viene tornato dal metodo getName())
     * @return un ServiceWorker del nome passato
     */
    public ServiceWorker getServiceWorker(String name) {
        Class<? extends Worker> serviceWorkerClass = workerMap.get(name);
        ServiceWorker serviceWorker = (ServiceWorker)beanFactory.getBean(serviceWorkerClass);
        return serviceWorker;
    }
    
    /**
     * Torna il nome della coda relativa alla priorità passata
     * @param setPriority la priorità
     * @return il nome della coda relativa alla priorità passata
     * @throws MasterjobsBadDataException 
     */
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
