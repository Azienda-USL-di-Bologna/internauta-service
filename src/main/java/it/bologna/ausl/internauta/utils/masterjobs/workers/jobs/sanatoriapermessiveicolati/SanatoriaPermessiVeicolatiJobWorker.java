package it.bologna.ausl.internauta.utils.masterjobs.workers.jobs.sanatoriapermessiveicolati;

import it.bologna.ausl.blackbox.repositories.PermessoRepository;
import it.bologna.ausl.internauta.utils.masterjobs.annotations.MasterjobsWorker;
import it.bologna.ausl.internauta.utils.masterjobs.exceptions.MasterjobsWorkerException;
import it.bologna.ausl.internauta.utils.masterjobs.workers.jobs.JobWorker;
import it.bologna.ausl.internauta.utils.masterjobs.workers.jobs.JobWorkerData;
import it.bologna.ausl.internauta.utils.masterjobs.workers.jobs.JobWorkerResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

/**
 *
 * @author Michele D'Onza
 */@MasterjobsWorker
public class SanatoriaPermessiVeicolatiJobWorker extends JobWorker<JobWorkerData, JobWorkerResult>{
    private static final Logger log = LoggerFactory.getLogger(SanatoriaPermessiVeicolatiJobWorker.class);
    private final String name = SanatoriaPermessiVeicolatiJobWorker.class.getSimpleName();
    
    @Autowired
    private PermessoRepository permessoRepository;
    
    @Override
    public String getName() {
        return this.name;
    }

    @Override
    protected JobWorkerResult doRealWork() throws MasterjobsWorkerException {
        log.info("sono il " + getName() + " e sto funzionando...");
        permessoRepository.spegniPermessiVeicolatiInvalidi();
        return null;
    }
}
