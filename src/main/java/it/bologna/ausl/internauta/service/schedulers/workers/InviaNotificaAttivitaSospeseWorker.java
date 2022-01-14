/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.bologna.ausl.internauta.service.schedulers.workers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

/**
 *
 * @author Salo
 */
@Configuration
@EnableAsync
@EnableScheduling
public class InviaNotificaAttivitaSospeseWorker implements Runnable {

    @Value("${internauta.scheduled.invio-mail-notifica-attivita-sospese.scheduled-chron-exp}")
    String scheduledChronExp;

    private static final Logger log = LoggerFactory
            .getLogger(InviaNotificaAttivitaSospeseWorker.class);

    @Override
    @Scheduled(cron = "${internauta.scheduled.invio-mail-notifica-attivita-sospese.scheduled-chron-exp}")
    public void run() {
        log.info("Start running...");

        log.info("Running out.");
        log.info(" * * * * * * ");
    }

}
