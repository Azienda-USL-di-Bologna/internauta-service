package it.bologna.ausl.internauta.utils.masterjobs.workers.services.cambiassociazioni;

import it.bologna.ausl.internauta.utils.masterjobs.annotations.MasterjobsWorker;
import it.bologna.ausl.internauta.utils.masterjobs.exceptions.MasterjobsQueuingException;
import it.bologna.ausl.internauta.utils.masterjobs.exceptions.MasterjobsRuntimeExceptionWrapper;
import it.bologna.ausl.internauta.utils.masterjobs.exceptions.MasterjobsWorkerException;
import it.bologna.ausl.internauta.utils.masterjobs.workers.WorkerResult;
import it.bologna.ausl.internauta.utils.masterjobs.workers.jobs.managecambiassociazioni.ManageCambiAssociazioniJobWorker;
import it.bologna.ausl.internauta.utils.masterjobs.workers.jobs.managecambiassociazioni.ManageCambiAssociazioniJobWorkerData;
import it.bologna.ausl.internauta.utils.masterjobs.workers.services.ServiceWorker;
import it.bologna.ausl.model.entities.masterjobs.Set;
import java.sql.Connection;
import java.sql.Statement;
import java.time.ZonedDateTime;
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
    private static final String CAMBIAMENTI_ASSOCIAZIONI_WORKER_ID = "cambiamenti_associazioni_worker_id";

    @Override
    public void preWork() throws MasterjobsWorkerException {
        Session session = entityManager.unwrap(Session.class);
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
        Session session = entityManager.unwrap(Session.class);
        session.doWork((Connection connection) -> {
            try {
                PGConnection pgc;
                while (!isStopped()) {
                    if (connection.isWrapperFor(PGConnection.class)) {
                        pgc = (PGConnection) connection.unwrap(PGConnection.class);

                        // attendo una notifica per 10 secondi poi termino. Il service viene poi rischedulato ogni 30 secondi
                        PGNotification notifications[] = pgc.getNotifications(10000);

                        if (notifications != null && notifications.length > 0) {
                            log.info(String.format("received notification: %s with paylod: %s", notifications[0].getName(), notifications[0].getParameter()));
                            log.info("Launching scheduleManageCambiAssociazioniJob...");
                            scheduleManageCambiAssociazioniJob();
                        }
                    }
                }
            } catch (Throwable ex) {
                String errorMessage = String.format("error on managing %s notification", CAMBIAMENTI_ASSOCIAZIONI_NOTIFY);
                log.error(errorMessage, ex);
                throw new MasterjobsRuntimeExceptionWrapper(errorMessage, ex);
            }
        });
        log.info(String.format("%s ended", getName()));
        return null;
    }
    
    private void scheduleManageCambiAssociazioniJob() throws MasterjobsQueuingException, MasterjobsWorkerException {
        log.info("queueing scheduleManageCambiAssociazioniJob...");
        ManageCambiAssociazioniJobWorker worker = masterjobsObjectsFactory.getJobWorker(
                ManageCambiAssociazioniJobWorker.class, new ManageCambiAssociazioniJobWorkerData(ZonedDateTime.now()), false);
        masterjobsJobsQueuer.queue(worker, CAMBIAMENTI_ASSOCIAZIONI_WORKER_ID, null, null, true, Set.SetPriority.NORMAL, null);
        log.info("scheduleManageCambiAssociazioniJob queued");
    }
}
