package it.bologna.ausl.internauta.utils.masterjobs.workers.services.foo.ricalcolopermessiarchivi;

import it.bologna.ausl.internauta.utils.masterjobs.annotations.MasterjobsWorker;
import it.bologna.ausl.internauta.utils.masterjobs.exceptions.MasterjobsQueuingException;
import it.bologna.ausl.internauta.utils.masterjobs.exceptions.MasterjobsWorkerException;
import it.bologna.ausl.internauta.utils.masterjobs.workers.WorkerResult;
import it.bologna.ausl.internauta.utils.masterjobs.workers.jobs.ricalcolopermessiarchivi.RicalcoloPermessiArchiviJobWorker;
import it.bologna.ausl.internauta.utils.masterjobs.workers.services.ServiceWorker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author gusgus
 */
@MasterjobsWorker
public class RicalcoloPermessiArchiviServiceWorker extends ServiceWorker {
    private static Logger log = LoggerFactory.getLogger(RicalcoloPermessiArchiviServiceWorker.class);
    
    private String name = RicalcoloPermessiArchiviServiceWorker.class.getSimpleName();
    
    @Override
    public String getName() {
        return this.name;
    }
    
    @Override
    public WorkerResult doWork() throws MasterjobsWorkerException {
        //RicalcoloPermessiArchiviJobWorkerData calcolaPersoneVedentiDocJobWorkerData = new RicalcoloPermessiArchiviJobWorkerData(idDoc);
        RicalcoloPermessiArchiviJobWorker jobWorker = masterjobsObjectsFactory.getJobWorker(
                RicalcoloPermessiArchiviJobWorker.class, 
                null,//calcolaPersoneVedentiDocJobWorkerData, 
                false
        );
        try {
            masterjobsJobsQueuer.queue(
                    jobWorker,
                    null, // ObjectID 
                    null, 
                    "scripta", 
                    false, // waitForObject
                    it.bologna.ausl.model.entities.masterjobs.Set.SetPriority.HIGHEST,
                    true
            );
        } catch (MasterjobsQueuingException ex) {
            String errorMessage = String.format("Errore nell'accodamento di %s", RicalcoloPermessiArchiviJobWorker.class.getSimpleName());
            log.error(errorMessage, ex);
            throw new MasterjobsWorkerException(errorMessage, ex);
        }
        return null;
    }
}
