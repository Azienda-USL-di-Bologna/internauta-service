package it.bologna.ausl.internauta.service.masterjobs.workers.foo;

import it.bologna.ausl.internauta.service.masterjobs.exceptions.MasterjobsWorkerException;
import it.bologna.ausl.internauta.service.masterjobs.workers.Worker;
import it.bologna.ausl.internauta.service.masterjobs.workers.WorkerData;
import it.bologna.ausl.internauta.service.masterjobs.workers.WorkerDataInterface;
import it.bologna.ausl.internauta.service.masterjobs.workers.WorkerResult;
import it.bologna.ausl.model.entities.masterjobs.ObjectStatus;
import javax.persistence.EntityManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 *
 * @author gdm
 */
@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class FooWorker extends Worker {
    private static final Logger log = LoggerFactory.getLogger(FooWorker.class);
    private String name = "Foo";

    @Autowired
    private EntityManager em;
    
    @Override
    public String getName() {
        return this.name;
    }
    
    @Override
    public WorkerResult doRealWork() throws MasterjobsWorkerException {
        log.info("sono in doWork()");
//        ObjectStatus o3 = new ObjectStatus();
//        FooWorkerData data = (FooWorkerData) getData();
//        o3.setObjectId(data.getParams1().toString());
//        o3.setState(ObjectStatus.ObjectState.IDLE);
//        em.persist(o3);
        if (false) {
            throw new MasterjobsWorkerException("prova errore");
        }
        return null;
    }
    
}
