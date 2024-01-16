package it.bologna.ausl.internauta.utils.masterjobs.workers.services.intimuscommand;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.querydsl.jpa.impl.JPAQueryFactory;
import it.bologna.ausl.internauta.service.exceptions.intimus.IntimusSendCommandException;
import it.bologna.ausl.internauta.service.utils.CachedEntities;
import it.bologna.ausl.internauta.service.utils.IntimusUtils;
import static it.bologna.ausl.internauta.service.utils.IntimusUtils.IntimusCommandNames.RefreshAttivita;
import static it.bologna.ausl.internauta.service.utils.IntimusUtils.IntimusCommandNames.RefreshMails;
import it.bologna.ausl.internauta.utils.masterjobs.annotations.MasterjobsWorker;
import it.bologna.ausl.internauta.utils.masterjobs.exceptions.MasterjobsParsingException;
import it.bologna.ausl.internauta.utils.masterjobs.exceptions.MasterjobsQueuingException;
import it.bologna.ausl.internauta.utils.masterjobs.exceptions.MasterjobsRuntimeExceptionWrapper;
import it.bologna.ausl.internauta.utils.masterjobs.exceptions.MasterjobsWorkerException;
import it.bologna.ausl.internauta.utils.masterjobs.executors.jobs.MasterjobsQueueData;
import it.bologna.ausl.internauta.utils.masterjobs.workers.WorkerResult;
import it.bologna.ausl.internauta.utils.masterjobs.workers.jobs.JobWorker;
import it.bologna.ausl.internauta.utils.masterjobs.workers.jobs.JobWorkerDataInterface;
import it.bologna.ausl.internauta.utils.masterjobs.workers.services.ServiceWorker;
import it.bologna.ausl.model.entities.masterjobs.JobNotified;
import it.bologna.ausl.model.entities.tools.IntimusCommand;
import it.bologna.ausl.model.entities.tools.QIntimusCommand;
import java.sql.Connection;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang.NotImplementedException;
import org.hibernate.Session;
import org.postgresql.PGConnection;
import org.postgresql.PGNotification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.TransactionDefinition;

/**
 *
 * @author gdm
 * 
 * Questo servizio si occupa di gestire i comandi intimus.
 * 
 * In generale, ogni volta che si vuole eseguire un comando tramite intimus, questo deve essere accodato nella coda redis di intimus.
 * Tramite questo servizio è possibile accodare ad intimus dei comandi scrivendone le informazioni necessarie nella tabella tools.intimus_command.
 * Il servizio, ad ogni giro legge i comandi indicati in tabella e li accoda ad intimus.
 * 
 * Questo servizio può funzionare sia in modalità polling, che tramite notify.
 * - per funzionare tramite notity è necessario inserire un numero nella colonna wait_notify_millis nella tabella mastertjobs.services.
 *      in questo modo il servizio, ogni every_seconds (colonna sempre della tabella mastertjobs.services) si mette in ascolto della notify per i secondi
 *      indicati e poi il servizio termina e riparte dopo every_seconds. Quando legge una notify accoda ad intimus il contenuto della tabella
 *      Questo funzionamento è per far si che questo servizio non occupi perennemente un threads.
 *      NB: indicando il valore 0 nella colonna wait_notify_millis, il servizio rimane in ascolto di notify per sempre (sarebbe meglio evitarlo)
 * - per funzionare solo in modalità polling e sufficiente inserire null nella colonna wait_notify_millis.
 *      in questo modo il servizio accoda ad intimus ogni every_seconds
 * 
 * NB: la lettura ed accodamento dei comandi intimus in tabella ha un limite di 10000 righe, per cui se per caso i comandi da accodare sono di più,
 * questi verranno accodati al prossimo giro (dopo every_seconds)
 */
@MasterjobsWorker
public class IntimusCommandServiceWorker extends ServiceWorker {
    private static Logger log = LoggerFactory.getLogger(IntimusCommandServiceWorker.class);

    public static final String NEW_INTIMUS_COMMAND_NOTIFY = "new_intimus_command_notify";

    @Autowired
    private ObjectMapper objectMapper;
    
    @Autowired
    private IntimusUtils intimusUtils;
    
    @Autowired
    private CachedEntities cachedEntities;
    
//    @Value("${masterjobs.manager.services-executor.polling-seconds:20}")
//    private Boolean pollingSeconds;
    
    private JPAQueryFactory queryFactory;
    private final QIntimusCommand qIntimusCommand = QIntimusCommand.intimusCommand;
    
    @Override
    public void preWork() throws MasterjobsWorkerException {
        queryFactory = new JPAQueryFactory(entityManager);

        /*
        se sono in modalità notify prima di mettermi in listen per le notify, accodo i comandi presenti in tabella.
        questo mi permette di accodare i comandi che sono stati inseriti mentre non ero in listen
        */
        if (serviceEntity.getWaitNotifyMillis() != null) {
            // all'avvio schedulo il job per recuperare il pregresso
            transactionTemplate.executeWithoutResult(a -> {
                try {
                    extractAndSendCommandsToIntimus();
                } catch (MasterjobsWorkerException ex) {
                    throw new MasterjobsRuntimeExceptionWrapper(ex);
                }
            });
            
            // mi metto in listen
            Session session = entityManager.unwrap(Session.class);
            session.doWork((Connection connection) -> {
                try {
                    try (Statement listenStatement = connection.createStatement()) {
                        log.info(String.format("executing LISTEN on %s", NEW_INTIMUS_COMMAND_NOTIFY));
                        listenStatement.execute(String.format("LISTEN %s", NEW_INTIMUS_COMMAND_NOTIFY));
                        log.info("LISTEN completed");
                    }
                } catch (Throwable ex) {
                    String errorMessage = String.format("error executing LISTEN %s", NEW_INTIMUS_COMMAND_NOTIFY);
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
                        PGConnection pgc;
                        if (connection.isWrapperFor(PGConnection.class)) {
                            pgc = (PGConnection) connection.unwrap(PGConnection.class);

                            // attendo una notifica per waitNotifyMillis poi termino e sarò rilanciato dal pool secondo le specifiche del servizio
                            PGNotification notifications[] = pgc.getNotifications(notifyMillis);

                            if (notifications != null && notifications.length > 0) {
                                log.info(String.format("received notification: %s with paylod: %s", notifications[0].getName(), notifications[0].getParameter()));
                                transactionTemplate.executeWithoutResult(a -> {
                                    try {
                                        log.info("Launching extractAndSendCommandsToIntimus()...");
                                        extractAndSendCommandsToIntimus();
                                    } catch (MasterjobsWorkerException ex) {
                                        throw new MasterjobsRuntimeExceptionWrapper(ex);
                                    }
                                });
                            }
                        }
                    }
                } catch (Throwable ex) {
                    String errorMessage = String.format("error on managing %s notification", NEW_INTIMUS_COMMAND_NOTIFY);
                    log.error(errorMessage, ex);
                    throw new MasterjobsRuntimeExceptionWrapper(errorMessage, ex);
                }
            });
        } else {
            log.info(String.format("starting %s with polling...", getName()));
            extractAndSendCommandsToIntimus();
        }
        
        log.info(String.format("%s ended", getName()));
        return null;
    }
    
    /**
     * Legge i comandi (max 10000) inseriti in tabella e li accoda ad intimus
     * @throws MasterjobsWorkerException 
     */
    private void extractAndSendCommandsToIntimus() throws MasterjobsWorkerException  {
        log.info("reading command to send...");
        boolean done = false;
        do {
            List<IntimusCommand> intimusCommands = queryFactory
                .select(qIntimusCommand)
                .from(qIntimusCommand)
                .orderBy(qIntimusCommand.id.asc())
                .limit(1000)
                .fetch();
            if (intimusCommands != null && !intimusCommands.isEmpty()) {
                for (IntimusCommand intimusCommand : intimusCommands) {
                    try {
                        /*
                        qua ci sono i comandi da accodare ad intimus.
                        Ogni riga della tabella potrebbe comportare l'accodamento di più comandi a seconda della colonna dests_objects
                        */
                        List<IntimusUtils.IntimusCommand> buildedCommands = new ArrayList<>();
                        
                        String commandString = intimusCommand.getCommand();
                        IntimusUtils.IntimusCommandNames command = IntimusUtils.IntimusCommandNames.valueOf(commandString);
                        
                        List<IntimusUtils.DestObject> destObjects = null;
                        if (intimusCommand.getDestObjects() != null && !intimusCommand.getDestObjects().isEmpty()) {
                            destObjects = objectMapper.convertValue(intimusCommand.getDestObjects(), new TypeReference<List<IntimusUtils.DestObject>>(){});
                        }
                        
                        switch (command) {
                            case RefreshAttivita:
                                IntimusUtils.RefreshAttivitaParams refreshAttivitaParams = objectMapper.convertValue(intimusCommand.getParams(), IntimusUtils.RefreshAttivitaParams.class);
                                if (destObjects != null && !destObjects.isEmpty()) {
                                    // per ogni destinatario del comando, creo un comando
                                    for (IntimusUtils.DestObject destObject : destObjects) {
                                        buildedCommands.add(intimusUtils.buildRefreshAttivitaCommand(
                                            destObject.getIdPersona(),
                                            refreshAttivitaParams.getIdAttivita(),
                                            refreshAttivitaParams.getOperation())
                                        );
                                    }
                                }
                                break;
                            case RefreshMails:
//                                IntimusUtils.RefreshMailsParams refreshMailsParams = objectMapper.convertValue(intimusCommand.getParams(), IntimusUtils.RefreshMailsParams.class);
                                buildedCommands.add(intimusUtils.buildRefreshMailsCommand(
                                    intimusCommand.getParams())
                                );
                                break;
                            case Logout:
                                IntimusUtils.LogoutParams logoutParams = objectMapper.convertValue(intimusCommand.getParams(), IntimusUtils.LogoutParams.class);
                                if (destObjects != null && !destObjects.isEmpty()) {
                                    for (IntimusUtils.DestObject destObject : destObjects) {    
                                        buildedCommands.add(intimusUtils.buildLogoutCommand(
                                            cachedEntities.getPersona(destObject.getIdPersona()), destObject.getApps(), logoutParams.getRedirectUrl()));
                                    }
                                }
                                break;
                            case ShowMessage:
                                // TODO: da implementare prima o poi, anche se difficilmente la ShowMessage è accodata con questo meccanismo
                                throw new NotImplementedException("non è possibile eseguire la Showmessage con questo meccasismo");
                        }
                        if (!buildedCommands.isEmpty()) {
                            for (IntimusUtils.IntimusCommand buildedCommand : buildedCommands) {
                                if (buildedCommand != null) {
                                    try {
                                        // accodamento effettivo a intimus del comando
                                        intimusUtils.sendCommand(buildedCommand);
                                    } catch (IntimusSendCommandException ex) {
                                        log.info(ex.getMessage());
                                    }
                                }
                            }
                        }
                        
                        transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
                        transactionTemplate.executeWithoutResult(a -> {
                            // cancellazione della riga dalla tabella tools.intimus_commands
                            deleteIntimusCommand(intimusCommand.getId());
                        });
                    } catch (Exception ex) {
                        String errorMessage = "error on manage jobs_notified";
                        log.error(errorMessage, ex);
                        throw new MasterjobsWorkerException(errorMessage, ex);
                    }
                }
            } else {
                done = true;
            }
        } while (!done);
    }
    
    private void deleteIntimusCommand(Integer id) {
//        transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
//        transactionTemplate.executeWithoutResult( a -> {
            queryFactory.delete(qIntimusCommand).where(qIntimusCommand.id.eq(id)).execute();
//        });
    }
}
