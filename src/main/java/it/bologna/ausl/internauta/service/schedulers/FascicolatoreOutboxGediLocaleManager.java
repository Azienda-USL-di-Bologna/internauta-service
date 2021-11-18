/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.bologna.ausl.internauta.service.schedulers;

import it.bologna.ausl.internauta.service.schedulers.workers.gedi.FascicolatoreAutomaticoGediLocaleWorker;
import it.bologna.ausl.internauta.service.schedulers.workers.gedi.wrappers.FascicolatoreAutomaticoGediParams;
import it.bologna.ausl.model.entities.baborg.Azienda;
import it.bologna.ausl.model.entities.baborg.Persona;
import it.bologna.ausl.model.entities.baborg.Utente;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.annotation.Autowired;
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

    private long initialDelay = 10;
    private long period = 30000;

    /**
     * Schedula un thread internauta per la fascicolazione automatica di un
     * messaggio (outbox)accodato in Shpeck per l'invio.
     *
     * @param idOutbox idOutbox della messaggio da protocollare
     * @param azienda l'azienda
     * @param cf codice fiscale presente nell'oggetto del sottofascicolo in cui
     * avviene l'autofascicolazione
     * @param mittente indirizzo del mittente del messaggio da fascicolare
     * @param numerazioneGerarchica (opzionale) numerazione gerarchica GEDI del
     * fascicolo padre in cui deve essere presente il sottofascicolo in cui si
     * vuole fascicolare il messaggio
     * @param utente l'utente applicativo
     * @param persona la persona applicativa
     */
    public void scheduleAutoFascicolazioneOutbox(Integer idOutbox,
            Azienda azienda,
            String cf,
            String mittente,
            String numerazioneGerarchica,
            Utente utente,
            Persona persona) throws Exception {
        log.info("Chiamato scheduleAutoFascicolazioneOutbox");
        log.info(String.format("Parametri:\n"
                + "idOutbox %s \n"
                + "cf %s \n"
                + "mittente %s \n"
                + "numerazioneGerarchica %s",
                idOutbox.toString(), cf, mittente, numerazioneGerarchica));
        FascicolatoreAutomaticoGediParams params = new FascicolatoreAutomaticoGediParams(idOutbox, azienda, cf, mittente, numerazioneGerarchica, utente, persona);
        FascicolatoreAutomaticoGediLocaleWorker worker = beanFactory.getBean(FascicolatoreAutomaticoGediLocaleWorker.class);
        worker.setParams(params);
        ScheduledFuture<?> schedule = scheduledThreadPoolExecutor.scheduleAtFixedRate(worker, initialDelay, period, TimeUnit.MILLISECONDS);
        worker.setScheduleObject(schedule);
    }

    public void scheduleAutoFascicolazioneOutbox(Integer idOutbox,
            Azienda azienda,
            String numerazioneGerarchica,
            Utente utente,
            Persona persona) throws Exception {
        log.info("Chiamato scheduleAutoFascicolazioneOutbox");
        log.info(String.format("Parametri:\n"
                + "idAzienda %s \n"
                + "idOutbox %s \n"
                + "numerazioneGerarchica %s", azienda.getId(),
                idOutbox.toString(), numerazioneGerarchica));
        FascicolatoreAutomaticoGediParams params = new FascicolatoreAutomaticoGediParams(idOutbox, azienda, null, null, numerazioneGerarchica, utente, persona);
        FascicolatoreAutomaticoGediLocaleWorker worker = beanFactory.getBean(FascicolatoreAutomaticoGediLocaleWorker.class);
        worker.setParams(params);
        ScheduledFuture<?> schedule = scheduledThreadPoolExecutor.scheduleAtFixedRate(worker, initialDelay, period, TimeUnit.MILLISECONDS);
        worker.setScheduleObject(schedule);
    }

}