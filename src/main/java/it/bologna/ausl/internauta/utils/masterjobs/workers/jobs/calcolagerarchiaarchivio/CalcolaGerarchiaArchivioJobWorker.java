package it.bologna.ausl.internauta.utils.masterjobs.workers.jobs.calcolagerarchiaarchivio;

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
public class CalcolaGerarchiaArchivioJobWorker extends JobWorker<CalcolaGerarchiaArchivioJobWorkerData, JobWorkerResult>{
    private static final Logger log = LoggerFactory.getLogger(CalcolaGerarchiaArchivioJobWorker.class);
    private final String name = CalcolaGerarchiaArchivioJobWorker.class.getSimpleName();
    
    @Autowired
    private ArchivioRepository archivioRepository;

    @Override
    public String getName() {
        return this.name;
    }
    
    @Override
    protected JobWorkerResult doRealWork() throws MasterjobsWorkerException {
        log.info("Inizio job");

        CalcolaGerarchiaArchivioJobWorkerData data = getWorkerData();
        log.info("Calcolo gerarchia archivioRadice: " + data.getIdArchivioRadice().toString());
        
        try {
            archivioRepository.calcolaGerarchiaArchivio(data.getIdArchivioRadice());
        } catch (Exception ex){
           String errore = "Errore nel calcolo della gerarchia degli archivi";
           log.error(errore, ex);
           throw new MasterjobsWorkerException(errore, ex);
        }
        
        return null;
    }
}
