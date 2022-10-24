package it.bologna.ausl.internauta.utils.masterjobs.workers.services.foo;

import it.bologna.ausl.internauta.utils.masterjobs.annotations.MasterjobsWorker;
import it.bologna.ausl.internauta.utils.masterjobs.exceptions.MasterjobsWorkerException;
import it.bologna.ausl.internauta.utils.masterjobs.workers.WorkerResult;
import it.bologna.ausl.internauta.utils.masterjobs.workers.services.ServiceWorker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author gdm
 */
@MasterjobsWorker
public class FooExternalServiceWorker extends ServiceWorker {
    private static Logger log = LoggerFactory.getLogger(FooExternalServiceWorker.class);
    
    private String name = FooExternalServiceWorker.class.getSimpleName();
    
    @Override
    public String getName() {
        return this.name;
    }
    
    @Override
    public WorkerResult doWork() throws MasterjobsWorkerException {
        log.info("sono il " + getName() + " e sto funzionando...");
        return null;
    }
}
