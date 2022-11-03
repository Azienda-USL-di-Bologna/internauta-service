package it.bologna.ausl.internauta.utils.masterjobs.workers.services.cambiassociazioni;

import it.bologna.ausl.internauta.utils.masterjobs.MasterjobsObjectsFactory;
import it.bologna.ausl.internauta.utils.masterjobs.annotations.MasterjobsWorker;
import it.bologna.ausl.internauta.utils.masterjobs.exceptions.MasterjobsQueuingException;
import it.bologna.ausl.internauta.utils.masterjobs.exceptions.MasterjobsRuntimeExceptionWrapper;
import it.bologna.ausl.internauta.utils.masterjobs.exceptions.MasterjobsWorkerException;
import it.bologna.ausl.internauta.utils.masterjobs.workers.WorkerResult;
import it.bologna.ausl.internauta.utils.masterjobs.workers.jobs.MasterjobsJobsQueuer;
import it.bologna.ausl.internauta.utils.masterjobs.workers.jobs.managecambiassociazioni.ManageCambiAssociazioniJobWorker;
import it.bologna.ausl.internauta.utils.masterjobs.workers.jobs.managecambiassociazioni.ManageCambiAssociazioniJobWorkerData;
import it.bologna.ausl.internauta.utils.masterjobs.workers.services.ServiceWorker;
import it.bologna.ausl.model.entities.masterjobs.Set;
import java.sql.Connection;
import java.sql.Statement;
import org.hibernate.Session;
import org.postgresql.PGConnection;
import org.postgresql.PGNotification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author gdm
 */
@MasterjobsWorker
public class CambiAssociazioniServiceWorker extends ServiceWorker {
    private static Logger log = LoggerFactory.getLogger(CambiAssociazioniServiceWorker.class);

    public static final String CAMBIAMENTI_ASSOCIAZIONI_NOTIFY = "cambiamenti_associazioni_notify";
    
    private Session session;

    @Override
    public void init(MasterjobsObjectsFactory masterjobsObjectsFactory, MasterjobsJobsQueuer masterjobsJobsQueuer) throws MasterjobsWorkerException {
        super.init(masterjobsObjectsFactory, masterjobsJobsQueuer);
        
        session = entityManager.unwrap(Session.class);
        try {
            // all'avvio schedulo il job per recuperare il pregresso
            scheduleManageCambiAssociazioniJob();
        } catch (MasterjobsQueuingException ex) {
            String errorMessage = String.format("error executing first scheduleManageCambiAssociazioniJob");
            log.error(errorMessage, ex);
            throw new MasterjobsWorkerException(errorMessage, ex);
        }
        session.doWork((Connection connection) -> {
            try {
                try (Statement listenStatement = connection.createStatement()) {
                    log.info(String.format("executing LISTEN on %s", CAMBIAMENTI_ASSOCIAZIONI_NOTIFY));
                    listenStatement.execute(String.format("LISTEN %s", CAMBIAMENTI_ASSOCIAZIONI_NOTIFY));
                    log.info("LISTEN completed");
                }
            } catch (Throwable ex) {
                String errorMessage = String.format("error executing LISTEN %s", CAMBIAMENTI_ASSOCIAZIONI_NOTIFY);
                log.error(errorMessage, ex);
                throw new MasterjobsRuntimeExceptionWrapper(errorMessage, ex);
            }
        });
    }
    
    @Override
    public String getName() {
        return getClass().getSimpleName();
    }
    
    @Override
    public WorkerResult doWork() throws MasterjobsWorkerException {
        log.info(String.format("starting %s...", getName()));
        session.doWork((Connection connection) -> {
            try {
                PGConnection pgc;
                if (connection.isWrapperFor(PGConnection.class)) {
                    pgc = (PGConnection) connection.unwrap(PGConnection.class);
                    
                    // attendo una notifica per 10 secondi poi termino. Il service viene poi rischedulato ogni 30 secondi
                    PGNotification notifications[] = pgc.getNotifications(10000);

                    if (notifications != null && notifications.length > 0) {
                        log.info(String.format("received notification %s. Launching scheduleManageCambiAssociazioniJob...", CAMBIAMENTI_ASSOCIAZIONI_NOTIFY));
                        scheduleManageCambiAssociazioniJob();
                    }
                }
            } catch (Throwable ex) {
                String errorMessage = String.format("error on managing %s notification", CAMBIAMENTI_ASSOCIAZIONI_NOTIFY);
                log.error(errorMessage, ex);
                throw new MasterjobsRuntimeExceptionWrapper(errorMessage, ex);
            }
        });
        return null;
    }
    
    private void scheduleManageCambiAssociazioniJob() throws MasterjobsQueuingException {
        log.info("queueing scheduleManageCambiAssociazioniJob...");
        ManageCambiAssociazioniJobWorker worker = masterjobsObjectsFactory.getJobWorker(
                ManageCambiAssociazioniJobWorker.class, new ManageCambiAssociazioniJobWorkerData(), false);
        masterjobsJobsQueuer.queue(worker, null, null, null, false, Set.SetPriority.HIGH);
        log.info("scheduleManageCambiAssociazioniJob queued");
    }
}
