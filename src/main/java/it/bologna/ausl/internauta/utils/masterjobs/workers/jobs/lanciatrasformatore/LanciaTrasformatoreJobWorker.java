package it.bologna.ausl.internauta.utils.masterjobs.workers.jobs.lanciatrasformatore;

import it.bologna.ausl.internauta.service.controllers.ribaltoneutils.ChiamateATrasformatore;
import it.bologna.ausl.internauta.utils.masterjobs.annotations.MasterjobsWorker;
import it.bologna.ausl.internauta.utils.masterjobs.exceptions.MasterjobsWorkerException;
import it.bologna.ausl.internauta.utils.masterjobs.workers.jobs.JobWorker;
import it.bologna.ausl.internauta.utils.masterjobs.workers.jobs.JobWorkerResult;
import java.util.logging.Level;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

/**
 *
 * @author gdm
 */
@MasterjobsWorker
public class LanciaTrasformatoreJobWorker extends JobWorker<LanciaTrasformatoreJobWorkerData, JobWorkerResult> {
    private static final Logger log = LoggerFactory.getLogger(LanciaTrasformatoreJobWorker.class);
    private final String name = LanciaTrasformatoreJobWorker.class.getSimpleName();
    
    @Autowired
    ChiamateATrasformatore chiamateATrasformatore;
    
    @Override
    public String getName() {
        return this.name;
    }
    
    @Override
    public JobWorkerResult doRealWork() throws MasterjobsWorkerException {
        log.info("sono in do doWork() di " + getName());

        try {
            chiamateATrasformatore.lanciaTrasformatore(
                    getWorkerData().getIdAzienda(),
                    getWorkerData().getRibaltaArgo(),
                    getWorkerData().getRibaltaInternauta(),
                    getWorkerData().getEmail(),
                    getWorkerData().getFonteRibaltone(),
                    getWorkerData().getTrasforma(),
                    getWorkerData().getIdUtente(),
                    getWorkerData().getNote()
            );
        } catch (Throwable ex) {
            throw new MasterjobsWorkerException("errore nella chiata al Trasformatore", ex);
        }
        return null;
    }
}
