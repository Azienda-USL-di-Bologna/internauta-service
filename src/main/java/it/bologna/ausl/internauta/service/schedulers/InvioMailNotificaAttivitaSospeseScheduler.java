/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.bologna.ausl.internauta.service.schedulers;

import it.bologna.ausl.internauta.service.schedulers.workers.InviaNotificaAttivitaSospeseWorker;
import java.util.Date;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

/**
 *
 * @author Salo
 *
 * Schedula l'esecuzione una volta al giorno di uno schedulatore dell'invio
 * notifiche
 */
public class InvioMailNotificaAttivitaSospeseScheduler {

    private static final Logger log = LoggerFactory.getLogger(InvioMailNotificaAttivitaSospeseScheduler.class);

    @Value("${internauta.invio.mail.notifica.attivita.sospese.scheduled.day.hour}")
    Integer oraDiSchedulazioneJobInvioMailNotificaAttivitaSospese;

    @Value("${internauta.scheduled.invio-mail-notifica-attivita-sospese.scheduled-chron-exp}")
    String scheduledChronExp;

    @Autowired
    private ScheduledThreadPoolExecutor scheduledThreadPoolExecutor;

    @Autowired
    private BeanFactory beanFactory;

    public void scheduleOnBoot() {
        log.info("Scheduling Schedule Josb On Boot");
    }

    public void traQuandiMinutiParto() {
        Date adesso = new Date();

    }

    public void schedulaInvioMailNotificaAttivitaSospese() {
        InviaNotificaAttivitaSospeseWorker worker = beanFactory
                .getBean(InviaNotificaAttivitaSospeseWorker.class);
        // inserisci nel pool
        // esegui il pool
    }

}
