/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.bologna.ausl.internauta.service.schedulers.workers;

import it.bologna.ausl.internauta.service.schedulers.managers.InviaNotificaAttivitaSospeseWorkerManager;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

/**
 *
 * @author Salo
 *
 * Schedula l'esecuzione una volta al giorno di uno schedulatore dell'invio
 * notifiche
 */
@Configuration
@EnableAsync
@EnableScheduling
public class InvioMailNotificaAttivitaSospeseScheduler {

    private static final Logger log = LoggerFactory.getLogger(InvioMailNotificaAttivitaSospeseScheduler.class);

    @Autowired
    InviaNotificaAttivitaSospeseWorkerManager manager;


    @Scheduled(cron = "${internauta.scheduled.invio-mail-notifica-attivita-sospese.scheduled-chron-exp}",
            zone = "Europe/Rome")
    public void chroneScheduleManager() {
        log.info("InvioMailNotificaAttivitaSospeseScheduler chrone schedule...");
        schedulaInvioMailNotificaAttivitaSospese();
        log.info("InvioMailNotificaAttivitaSospeseScheduler chrone schedule end");
    }

    public void scheduleOnBoot() {
        log.info("Scheduling Schedule Josb On Boot");
    }

    public void schedulaInvioMailNotificaAttivitaSospese() {
        manager.run();
    }

}
