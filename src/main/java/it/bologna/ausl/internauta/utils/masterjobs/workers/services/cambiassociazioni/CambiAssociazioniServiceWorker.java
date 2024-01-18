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
        /*
        se sono in modalità notify prima di mettermi in listen per le notify, accodo i comandi presenti in tabella.
        questo mi permette di accodare i comandi che sono stati inseriti mentre non ero in listen
        */
        if (serviceEntity.getWaitNotifyMillis() != null) {
            try {
                // all'avvio schedulo il job per recuperare il pregresso
                scheduleManageCambiAssociazioniJob();
            } catch (MasterjobsQueuingException ex) {
                String errorMessage = String.format("error executing first scheduleManageCambiAssociazioniJob");
                log.error(errorMessage, ex);
                throw new MasterjobsWorkerException(errorMessage, ex);
            }
            
            // mi metto in listen
            Session session = entityManager.unwrap(Session.class);
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
    }
    
    @Override
    public String getName() {
        return getClass().getSimpleName();
    }
    
    @Override
    public WorkerResult doWork() throws MasterjobsWorkerException {
        log.info(String.format("starting %s...", getName()));
        Integer waitNotifyMillis = serviceEntity.getWaitNotifyMillis();
        if (waitNotifyMillis != null) {
             /*
            Se sono in modalità notify mi metto in attesa di notify per wait_notify_millis millisecondi.
            se wait_notify_millis è 0, allora vuol dire che voglio che questo servizio non termini mai restando sempre in listen
            in ogni caso faccio una getNotifications ogni 10 secondi per far si che se è stato fermato il masterjobs (isStopped == true) 
            il servizio riesca a terminare.
            */
            log.info(String.format("starting %s with notify...", getName()));
            Session session = entityManager.unwrap(Session.class);
            session.doWork((Connection connection) -> {
                try {
                    boolean stopLoop = false;
                    int notifyMillis;
                    while (!stopLoop && !isStopped()) {
                        if (waitNotifyMillis == 0) {
                            notifyMillis = 10000;
                            stopLoop = false;
                        } else {
                            notifyMillis = waitNotifyMillis;
                            stopLoop = true;
                        }
                        if (connection.isWrapperFor(PGConnection.class)) {
                            PGConnection pgc = (PGConnection) connection.unwrap(PGConnection.class);

                            // attendo una notifica per waitNotifyMillis poi termino e sarò rilanciato dal pool secondo le specifiche del servizio
                            PGNotification notifications[] = pgc.getNotifications(notifyMillis);

                            if (notifications != null && notifications.length > 0) {
                                log.info(String.format("received notification: %s with paylod: %s", notifications[0].getName(), notifications[0].getParameter()));
                                log.info("Launching scheduleManageCambiAssociazioniJob()...");
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
        } else {
            log.info(String.format("starting %s with polling...", getName()));
            try {
                // all'avvio schedulo il job per recuperare il pregresso
                log.info("Launching scheduleManageCambiAssociazioniJob()...");
                scheduleManageCambiAssociazioniJob();
            } catch (MasterjobsQueuingException ex) {
                String errorMessage = String.format("error executing scheduleManageCambiAssociazioniJob");
                log.error(errorMessage, ex);
                throw new MasterjobsWorkerException(errorMessage, ex);
            }
        }
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
