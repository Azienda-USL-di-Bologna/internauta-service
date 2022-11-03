package it.bologna.ausl.internauta.utils.masterjobs.workers.jobs.fooexternal;

import it.bologna.ausl.internauta.utils.masterjobs.annotations.MasterjobsWorker;
import it.bologna.ausl.internauta.utils.masterjobs.exceptions.MasterjobsWorkerException;
import it.bologna.ausl.internauta.utils.masterjobs.workers.jobs.JobWorker;
import it.bologna.ausl.internauta.utils.masterjobs.workers.jobs.JobWorkerResult;
import javax.persistence.EntityManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

/**
 *
 * @author gdm
 */
@MasterjobsWorker
public class FooExternalWorker extends JobWorker {
    private static final Logger log = LoggerFactory.getLogger(FooExternalWorker.class);
    private String name = FooExternalWorker.class.getSimpleName();

    @Autowired
    private EntityManager em;
    
    @Override
    public String getName() {
        return this.name;
    }
    
    @Override
    public JobWorkerResult doRealWork() throws MasterjobsWorkerException {
        log.info("sono in do doWork() di " + getName());
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
