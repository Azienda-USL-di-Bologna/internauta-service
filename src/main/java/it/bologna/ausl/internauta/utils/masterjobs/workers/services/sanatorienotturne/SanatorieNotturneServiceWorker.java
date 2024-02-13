package it.bologna.ausl.internauta.utils.masterjobs.workers.services.sanatorienotturne;

import it.bologna.ausl.internauta.utils.masterjobs.annotations.MasterjobsWorker;
import it.bologna.ausl.internauta.utils.masterjobs.exceptions.MasterjobsQueuingException;
import it.bologna.ausl.internauta.utils.masterjobs.exceptions.MasterjobsWorkerException;
import it.bologna.ausl.internauta.utils.masterjobs.workers.WorkerResult;
import it.bologna.ausl.internauta.utils.masterjobs.workers.jobs.sanatoriacontatti.SanatoriaContattiJobWorker;
import it.bologna.ausl.internauta.utils.masterjobs.workers.jobs.sanatoriapermessiveicolati.SanatoriaPermessiVeicolatiJobWorker;
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
public class SanatorieNotturneServiceWorker extends ServiceWorker {
    private static Logger log = LoggerFactory.getLogger(SanatorieNotturneServiceWorker.class);
    
    private String name = SanatorieNotturneServiceWorker.class.getSimpleName();
    
    @Override
    public String getName() {
        return this.name;
    }
    
    @Override
    public WorkerResult doWork() throws MasterjobsWorkerException {
        log.info("sono il " + getName() + " e sto funzionando...");
        
        try {
            masterjobsJobsQueuer.queue(
                    new SanatoriaContattiJobWorker(),
                    null,
                    null,
                    Applicazione.Applicazioni.rubrica.toString(),
                    false,
                    Set.SetPriority.NORMAL,
                    null
            );
        } catch (MasterjobsQueuingException ex) {
            throw new MasterjobsWorkerException("errore nell'accodamento del SanatoriaContattiJobWorker", ex);
        }
        try {
            masterjobsJobsQueuer.queue(
                    new SanatoriaPermessiVeicolatiJobWorker(),
                    null,
                    null,
                    Applicazione.Applicazioni.gediInt.toString(),
                    false,
                    Set.SetPriority.NORMAL,
                    null
            );
        } catch (MasterjobsQueuingException ex) {
            throw new MasterjobsWorkerException("errore nell'accodamento del SanatoriaPermessiVeicolatiJobWorker", ex);
        }
        return null;
    }
}
