package it.bologna.ausl.internauta.utils.masterjobs.workers.jobs.calcolapersonevedentidoc;

import it.bologna.ausl.internauta.service.repositories.scripta.PersonaVedenteRepository;
import it.bologna.ausl.internauta.utils.masterjobs.annotations.MasterjobsWorker;
import it.bologna.ausl.internauta.utils.masterjobs.exceptions.MasterjobsWorkerException;
import it.bologna.ausl.internauta.utils.masterjobs.workers.jobs.JobWorker;
import it.bologna.ausl.internauta.utils.masterjobs.workers.jobs.JobWorkerResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

/**
 *
 * @author gusgus
 */
@MasterjobsWorker
public class CalcolaPersoneVedentiDocJobWorker extends JobWorker<CalcolaPersoneVedentiDocJobWorkerData, JobWorkerResult> {
    private static final Logger log = LoggerFactory.getLogger(CalcolaPersoneVedentiDocJobWorker.class);
    private final String name = CalcolaPersoneVedentiDocJobWorker.class.getSimpleName();
    
    @Autowired
    private PersonaVedenteRepository personaVedenteRepository;

    @Override
    public String getName() {
        return this.name;
    }
    
    @Override
    protected JobWorkerResult doRealWork() throws MasterjobsWorkerException {
        log.info("Inizio job");
        
        CalcolaPersoneVedentiDocJobWorkerData data = getWorkerData();
        personaVedenteRepository.calcolaPersoneVedenti(data.getIdDoc());
        
        return null;
    }
}
