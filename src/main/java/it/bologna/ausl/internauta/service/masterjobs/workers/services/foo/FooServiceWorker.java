package it.bologna.ausl.internauta.service.masterjobs.workers.services.foo;

import it.bologna.ausl.internauta.service.masterjobs.annotations.MasterjobsWorker;
import it.bologna.ausl.internauta.service.masterjobs.exceptions.MasterjobsWorkerException;
import it.bologna.ausl.internauta.service.masterjobs.workers.WorkerResult;
import it.bologna.ausl.internauta.service.masterjobs.workers.services.ServiceWorker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author gdm
 */
@MasterjobsWorker
public class FooServiceWorker extends ServiceWorker {
    private static Logger log = LoggerFactory.getLogger(FooServiceWorker.class);
    
    private String name = FooServiceWorker.class.getSimpleName();
    
    @Override
    public String getName() {
        return this.name;
    }
    
    @Override
    public WorkerResult doWork() throws MasterjobsWorkerException {
        log.info("sono il FooServiceWorker e sto funzionando...");
        return null;
    }
}
