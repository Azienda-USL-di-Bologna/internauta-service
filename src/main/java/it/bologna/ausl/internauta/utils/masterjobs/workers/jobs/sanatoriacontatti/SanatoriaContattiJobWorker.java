package it.bologna.ausl.internauta.utils.masterjobs.workers.jobs.sanatoriacontatti;

import it.bologna.ausl.internauta.service.repositories.rubrica.ContattoRepository;
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
 * @author gusgus
 */
@MasterjobsWorker
public class SanatoriaContattiJobWorker extends JobWorker<JobWorkerData, JobWorkerResult> {
    private static final Logger log = LoggerFactory.getLogger(SanatoriaContattiJobWorker.class);
    private final String name = SanatoriaContattiJobWorker.class.getSimpleName();
    
    @Autowired
    private ContattoRepository contattoRespository;
    
    @Override
    public String getName() {
        return this.name;
    }
    
    @Override
    protected JobWorkerResult doRealWork() throws MasterjobsWorkerException {
        log.info("Inizia il job");
        
        log.info("Aggiorno i contatti struttura");
        contattoRespository.aggiornaContattiStruttura();
        
        log.info("Aggiorno i contatti persona");
        contattoRespository.aggiornaContattiPersona();
        
        log.info("Elimino i protocontatti");
        contattoRespository.eliminaProtocontatti();
        
        return null;
    }
}
