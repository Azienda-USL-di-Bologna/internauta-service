package it.bologna.ausl.internauta.service.masterjobs.workers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.querydsl.core.QueryFactory;
import it.bologna.ausl.internauta.service.masterjobs.MasterjobsObjectsFactory;
import it.bologna.ausl.internauta.service.masterjobs.MasterjobsQueueData;
import it.bologna.ausl.internauta.service.masterjobs.exceptions.MasterjobsBadDataException;
import it.bologna.ausl.internauta.service.masterjobs.exceptions.MasterjobsQueuingException;
import it.bologna.ausl.model.entities.configurazione.Applicazione;
import it.bologna.ausl.model.entities.masterjobs.Job;
import it.bologna.ausl.model.entities.masterjobs.Set;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.annotation.PostConstruct;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

/**
 *
 * @author gdm
 * 
 * Classe che permette di accodare dei job per l'esecuzione dal parte del Masterjobs
 */
@Component
public class MasterjobsQueuer {
private static final Logger log = LoggerFactory.getLogger(MasterjobsQueuer.class);

    @PersistenceContext
    private EntityManager entityManager;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @Autowired
    @Qualifier(value = "redisMaterjobs")
    private RedisTemplate redisTemplate;
    
    @Autowired
    private BeanFactory beanFactory;
    
//    private PlatformTransactionManager transactionManager;
    
    @Autowired 
    private TransactionTemplate transactionTemplate;
    
//    private MasterjobsQueuer self;
    
    @Autowired
    private MasterjobsObjectsFactory masterjobsObjectsFactory;
    
//    @PostConstruct
//    public void init() {
//        this.self = beanFactory.getBean(MasterjobsQueuer.class);
//    }
    
    /**
     * accoda dei jobs
     * @param workers l'elenco dei workers(normali o deferred), con i loro dati (WorkerData o WorkerDeferredData)
     * @param objectId l'id dell'oggetto al quale i jobs fanno riferimento. Può essere null
     * @param objectType il tipo dell'oggetto al quale i jobs fanno riferimento. Può essere null
     * @param appId l'id dell'app al quale i jobs fanno riferimento. Può essere null
     * @param waitForObject se true, vuol dire che questi jobs dovranno aspettare l'effettiva esecuzione 
     * degli altri job per lo stesso oggetto. (l'oggetto viene indentificato per objectId, objectType (se presente), appId(se presente)
     * se non viene passato almeno objectId, questo parametro non ha effetto
     * @param priority la priortà con il quale il job deve essere eseguito
     * @throws MasterjobsQueuingException nel caso ci sia un errore nell'inserimento in coda
     */
    public void queue(List<Worker> workers, String objectId, String objectType, String appId, Boolean waitForObject, Set.SetPriority priority) throws MasterjobsQueuingException {
//        MasterjobsQueuer self = beanFactory.getBean(MasterjobsQueuer.class);
//        MasterjobsQueueData queueData = self.insertInDatabase(workers, objectId, objectType, appId, waitForObject, priority);
//        transactionTemplate = new TransactionTemplate(transactionManager);
        transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        MasterjobsQueueData queueData = transactionTemplate.execute(action -> {
            return this.insertInDatabase(workers, objectId, objectType, appId, waitForObject, priority);
        });
        try {
            insertInQueue(queueData, priority);
        } catch (Exception ex) {
            String errorMessage = String.format("error queuing job with object id %s and object type %s ", objectId, objectType);
            log.error(errorMessage, ex);
            throw new MasterjobsQueuingException(errorMessage, ex);
        }
    }
    
    public void queue(Worker worker, String objectId, String objectType, String appId, Boolean waitForObject, Set.SetPriority priority) throws MasterjobsQueuingException {
        queue(Arrays.asList(worker), objectId, objectType, appId, waitForObject, priority);
    }
    
    /**
     * crea un set di jobs e lo inserisce nella coda redis di esecuzione
     * @param queueData
     * @param priority
     * @throws JsonProcessingException
     * @throws MasterjobsBadDataException 
     */
    private void insertInQueue(MasterjobsQueueData queueData, Set.SetPriority priority) throws JsonProcessingException, MasterjobsBadDataException {
        String queue = masterjobsObjectsFactory.getQueueBySetPriority(priority);
        redisTemplate.opsForList().rightPush(queue, queueData.dump());
    }
    
    /**
     * inserisce i jobs e un seti che li raggruppa nel database
     * @param workers
     * @param objectId
     * @param objectType
     * @param appId
     * @param waitForObject
     * @param priority
     * @return 
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Throwable.class)
    public MasterjobsQueueData insertInDatabase(List<Worker> workers, String objectId, String objectType, String appId, Boolean waitForObject, Set.SetPriority priority) {

        Set set = new Set();
        if (objectId != null)
            set.setObjectId(objectId);
        if (objectType != null)
            set.setObjectType(objectType);
        if (appId != null) {
            Applicazione app = entityManager.find(Applicazione.class, appId);
            set.setApp(app);
        }
        if (waitForObject != null)
            set.setWaitObject(waitForObject);
        if (priority != null)
            set.setPriority(priority);
        entityManager.persist(set);
        
        List<Job> jobs = new ArrayList<>(); 
        for (Worker worker : workers) {
            Job job = new Job();
            job.setDeferred(worker.isDeferred());
            WorkerDataInterface workerData = worker.getData();
            if (workerData != null)
                job.setData(workerData.toJobData(objectMapper));
            job.setName(worker.getName());
            job.setState(Job.JobState.READY);
            job.setSet(set);
            entityManager.persist(job);
            jobs.add(job);
        }
        
        List<Long> jobsId = new ArrayList<>();
        for (Job job : jobs) {
            jobsId.add(job.getId());
        }
        
        MasterjobsQueueData queueData = masterjobsObjectsFactory.buildMasterjobsQueueData(jobsId, set.getId());
        return queueData;
    }
}
