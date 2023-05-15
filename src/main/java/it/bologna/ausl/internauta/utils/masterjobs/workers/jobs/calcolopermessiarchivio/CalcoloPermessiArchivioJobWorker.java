package it.bologna.ausl.internauta.utils.masterjobs.workers.jobs.calcolopermessiarchivio;

import it.bologna.ausl.internauta.utils.masterjobs.annotations.MasterjobsWorker;
import it.bologna.ausl.internauta.utils.masterjobs.exceptions.MasterjobsWorkerException;
import it.bologna.ausl.internauta.utils.masterjobs.workers.jobs.JobWorker;
import it.bologna.ausl.internauta.utils.masterjobs.workers.jobs.JobWorkerResult;
import it.bologna.ausl.internauta.service.repositories.scripta.ArchivioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

/**
 *
 * @author gusgus
 */
@MasterjobsWorker
public class CalcoloPermessiArchivioJobWorker extends JobWorker<CalcoloPermessiArchivioJobWorkerData, JobWorkerResult>{
    private static final Logger log = LoggerFactory.getLogger(CalcoloPermessiArchivioJobWorker.class);
    private final String name = CalcoloPermessiArchivioJobWorker.class.getSimpleName();
    
    @Autowired
    private ArchivioRepository archivioRepository;

    @Override
    public String getName() {
        return this.name;
    }
    
    @Override
    protected JobWorkerResult doRealWork() throws MasterjobsWorkerException {
        log.info("Inizio job");

        CalcoloPermessiArchivioJobWorkerData data = getWorkerData();
        log.info("Calcolo permessi archivio: " + data.getIdArchivio().toString());
        
        try {
            archivioRepository.calcolaPermessiEspliciti(data.getIdArchivio());
        } catch (Exception ex){
           String errore = "Errore nel calcolo dei permessi espliciti dello archivio";
           log.error(errore, ex);
           throw new MasterjobsWorkerException(errore, ex);
        }
        
        return null;
    }
}
