package it.bologna.ausl.internauta.service.schedulers;

import it.bologna.ausl.internauta.service.repositories.tools.PendingJobRepository;
import it.bologna.ausl.internauta.service.schedulers.workers.gedi.FascicolatoreAutomaticoGediLocaleWorker;
import it.bologna.ausl.internauta.service.schedulers.workers.gedi.wrappers.FascicolatoreAutomaticoGediParams;
import it.bologna.ausl.model.entities.baborg.Azienda;
import it.bologna.ausl.model.entities.baborg.Persona;
import it.bologna.ausl.model.entities.baborg.Utente;
import it.bologna.ausl.model.entities.tools.PendingJob;
import it.bologna.ausl.model.entities.tools.QPendingJob;
import java.math.BigInteger;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

/**
 *
 * @author Salo
 */
@Component
public class FascicolatoreOutboxGediLocaleManager {

    private static final Logger log = LoggerFactory.getLogger(FascicolatoreOutboxGediLocaleManager.class);

    @Autowired
    private ScheduledThreadPoolExecutor scheduledThreadPoolExecutor;

    @Autowired
    private BeanFactory beanFactory;
    
    @Autowired
    private PendingJobRepository pendingJobRepository;

    private long initialDelay = 10;
    private long period = 30000;

    public void scheduleAutoFascicolazioneOutboxAtBoot() throws Exception {
        Iterable<PendingJob> pendingJobsToReschedule = pendingJobRepository.findAll(
                QPendingJob.pendingJob.service.eq(PendingJob.PendigJobsServices.FASCICOLATORE_SAI.toString())
            .and(
                QPendingJob.pendingJob.state.eq(PendingJob.PendigJobsState.RETRY.toString())
            ), Sort.by(Sort.Direction.ASC, "id"));
        for (PendingJob pendingJob : pendingJobsToReschedule) {
            log.info(String.format("schedulo al boot il job %s con stato %s...", pendingJob.getId(), pendingJob.getState().toString()));
            this.scheduleAutoFascicolazioneOutbox(pendingJob);
        }
    }
    
    /**
     * Schedula un thread internauta per la fascicolazione automatica di un messaggio (outbox)accodato in Shpeck per l'invio.
     *
     * @param pendingJob nel campo data contiene:
     * * idOutbox idOutbox della messaggio da protocollare
     * * azienda l'azienda
     * * cf codice fiscale presente nell'oggetto del sottofascicolo in cui avviene l'autofascicolazione
     * * mittente indirizzo del mittente del messaggio da fascicolare
     * * numerazioneGerarchica (opzionale) numerazione gerarchica GEDI del fascicolo padre in cui deve essere presente il sottofascicolo in cui si vuole fascicolare il messaggio
     * * utente l'utente applicativo
     * * persona la persona applicativa
     */
    public void scheduleAutoFascicolazioneOutbox(PendingJob pendingJob) throws Exception {
        log.info(String.format("Chiamato scheduleAutoFascicolazioneOutbox per job %s...", pendingJob.getId()));
//        FascicolatoreAutomaticoGediParams params = new FascicolatoreAutomaticoGediParams(pendingJobId, idOutbox, azienda, null, null, numerazioneGerarchica, utente, persona);
        FascicolatoreAutomaticoGediLocaleWorker worker = beanFactory.getBean(FascicolatoreAutomaticoGediLocaleWorker.class);
//        worker.setParams(params);
        worker.setPendingJob(pendingJob);
        ScheduledFuture<?> schedule = scheduledThreadPoolExecutor.scheduleAtFixedRate(worker, initialDelay, period, TimeUnit.MILLISECONDS);
        worker.setScheduleObject(schedule);
    }

}
