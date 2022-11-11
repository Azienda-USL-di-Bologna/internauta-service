package it.bologna.ausl.internauta.utils.masterjobs.workers.jobs.managecambiassociazioni;

import it.bologna.ausl.internauta.utils.masterjobs.annotations.MasterjobsWorker;
import it.bologna.ausl.internauta.utils.masterjobs.exceptions.MasterjobsWorkerException;
import it.bologna.ausl.internauta.utils.masterjobs.workers.jobs.JobWorker;
import it.bologna.ausl.internauta.utils.masterjobs.workers.jobs.JobWorkerResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author gdm
 */
@MasterjobsWorker
public class ManageCambiAssociazioniJobWorker extends JobWorker {
    private static final Logger log = LoggerFactory.getLogger(ManageCambiAssociazioniJobWorker.class);

    @Override
    public String getName() {
        return getClass().getSimpleName();
    }
    
    @Override
    public JobWorkerResult doRealWork() throws MasterjobsWorkerException {
        log.info(String.format("job %s started", getName()));
        log.info(String.format("job %s ended", getName()));
        return null;
    }
    
}
