package it.bologna.ausl.internauta.utils.masterjobs.workers.services.versatore;

import it.bologna.ausl.internauta.utils.masterjobs.workers.services.foo.*;
import it.bologna.ausl.internauta.utils.masterjobs.annotations.MasterjobsWorker;
import it.bologna.ausl.internauta.utils.masterjobs.exceptions.MasterjobsWorkerException;
import it.bologna.ausl.internauta.utils.masterjobs.workers.WorkerResult;
import it.bologna.ausl.internauta.utils.masterjobs.workers.services.ServiceWorker;
import it.bologna.ausl.internauta.utils.masterjobs.workers.services.VersatoreServiceCore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author gdm
 */
@MasterjobsWorker
public class VersatoreServiceWorker extends ServiceWorker {
    private static Logger log = LoggerFactory.getLogger(VersatoreServiceWorker.class);
    
    private String name = VersatoreServiceWorker.class.getSimpleName();
    
    @Override
    public String getName() {
        return this.name;
    }
    
    @Override
    public WorkerResult doWork() throws MasterjobsWorkerException {
        VersatoreServiceCore versatoreServiceCore = new VersatoreServiceCore(masterjobsJobsQueuer, masterjobsObjectsFactory);
        versatoreServiceCore.queueVersatoreJob("infocert_azienda_zero", false, null, 1, "internauta");
        return null;
    }
}
