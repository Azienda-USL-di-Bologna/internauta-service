package it.bologna.ausl.internauta.utils.masterjobs.workers.services.cercaanomaliearchivi;

import it.bologna.ausl.internauta.utils.masterjobs.annotations.MasterjobsWorker;
import it.bologna.ausl.internauta.utils.masterjobs.exceptions.MasterjobsQueuingException;
import it.bologna.ausl.internauta.utils.masterjobs.exceptions.MasterjobsWorkerException;
import it.bologna.ausl.internauta.utils.masterjobs.workers.WorkerResult;
import it.bologna.ausl.internauta.utils.masterjobs.workers.jobs.cercaanomaliearchivi.CercaAnomalieArchiviJobWorker;
import it.bologna.ausl.internauta.utils.masterjobs.workers.services.ServiceWorker;
import it.bologna.ausl.model.entities.configurazione.Applicazione;
import it.bologna.ausl.model.entities.masterjobs.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author gusgus
 */
@MasterjobsWorker
public class CercaAnomalieArchiviServiceWorker extends ServiceWorker {
    private static Logger log = LoggerFactory.getLogger(CercaAnomalieArchiviServiceWorker.class);
    
    private String name = CercaAnomalieArchiviServiceWorker.class.getSimpleName();
    
    @Override
    public String getName() {
        return this.name;
    }
    
    @Override
    public WorkerResult doWork() throws MasterjobsWorkerException {
        log.info("sono il " + getName() + " e sto funzionando...");
        
        try {
            masterjobsJobsQueuer.queue(
                    new CercaAnomalieArchiviJobWorker(),
                    null,
                    null,
                    Applicazione.Applicazioni.scripta.toString(),
                    false,
                    Set.SetPriority.NORMAL
            );
        } catch (MasterjobsQueuingException ex) {
            throw new MasterjobsWorkerException("errore nell'accodamento del job", ex);
        }
        
        return null;
    }
}
