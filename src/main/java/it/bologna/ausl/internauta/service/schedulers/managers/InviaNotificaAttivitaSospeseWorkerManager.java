/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.bologna.ausl.internauta.service.schedulers.managers;

import it.bologna.ausl.internauta.service.repositories.baborg.AziendaRepository;
import it.bologna.ausl.internauta.service.repositories.configurazione.ParametroAziendeRepository;
import it.bologna.ausl.internauta.service.schedulers.workers.InviaNotificaAttivitaSospeseWorker;
import it.bologna.ausl.internauta.service.schedulers.workers.handlers.ParametroAziendeInvioMailNotificaAttivitaHandler;
import it.bologna.ausl.internauta.service.utils.ParametriAziendeReader;
import it.bologna.ausl.model.entities.configurazione.ParametroAziende;
import it.bologna.ausl.model.entities.configurazione.QParametroAziende;
import java.text.ParseException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 *
 * @author Salo
 */
@Component
public class InviaNotificaAttivitaSospeseWorkerManager implements Runnable {

    private static int times = 0;

    private static List<Integer> idPersoneAvvisate = new ArrayList<>();

    private static final Logger log = LoggerFactory
            .getLogger(InviaNotificaAttivitaSospeseWorkerManager.class);

    @Autowired
    ParametriAziendeReader par;

    @Autowired
    InviaNotificaAttivitaSospeseWorker worker;

    @Autowired
    ParametroAziendeRepository parametroAziendeRepository;

    private final String nomeParametroDB = "inviaMailNotificaAttivitàSospese";

    /**
     * Controlla dal JSON di ParametroAziende se tutte l'aziende hanno già
     * passato il ciclo oggi.
     */
    private boolean isEveryAziendaDone(ParametroAziende parametro) throws ParseException {
        Integer[] idAziende = parametro.getIdAziende();
        JSONObject jsonDatiAziende = new JSONObject(parametro.getValore());
        System.out.println(jsonDatiAziende);
        for (int i = 0; i < idAziende.length; i++) {
            //JSONObject jsonDettagliUltimoInvioAzienda = jsonDatiAziende.getJSONObject(idAziende[i].toString());
            ParametroAziendeInvioMailNotificaAttivitaHandler handler = new ParametroAziendeInvioMailNotificaAttivitaHandler(parametro, idAziende[i]);
            if (!handler.isNotificationAlreadySentToday()) {
                return false;
            }
        }
        return true;
    }

    private void updateParametroAziendeNuovoCicloEsecuzione(ParametroAziende parametroAziende, ParametroAziendeInvioMailNotificaAttivitaHandler handler) {
        JSONObject setDatiUltimaEsecuzioneMestiere = handler.setDatiUltimaEsecuzioneMestiere();
        parametroAziende.setValore(setDatiUltimaEsecuzioneMestiere.toString(4));
        parametroAziende = parametroAziendeRepository.save(parametroAziende);
    }

    private void loopAziendeAndManageWorker(ParametroAziende parametroAziende) throws ParseException {
        Integer[] idAziende = parametroAziende.getIdAziende();
        System.out.println("Ciclo le aziende");
        for (int i = 0; i < idAziende.length; i++) {

            log.info("Creo handler ParametroAziende per azienda " + idAziende[i]);
            ParametroAziendeInvioMailNotificaAttivitaHandler handler
                    = new ParametroAziendeInvioMailNotificaAttivitaHandler(parametroAziende, idAziende[i]);
            System.out.println(handler.toString());
            System.out.println("Gia' runnato oggi?");
            if (!handler.isNotificationAlreadySentToday()) {
                System.out.println("No");
                // esegui il worker
                log.info("Setto parametri worker");
                worker.setParameter(idPersoneAvvisate, handler.getIdAzienda());
                log.info("Runno per azienda " + handler.getIdAzienda().toString());
                worker.run();
                log.info("Mestiere finito su azienda " + handler.getIdAzienda().toString());
                log.info("Setto il nuovo stato di esecuzione su azienda " + handler.getIdAzienda().toString());
                updateParametroAziendeNuovoCicloEsecuzione(parametroAziende, handler);
            }
            System.out.println("ok");
            log.info("Persone avvisate finora: " + idPersoneAvvisate.size());
        }
    }

    public void run() {
        log.info("Start running...");
        try {

            ParametroAziende parametro = parametroAziendeRepository.findOne(
                    QParametroAziende.parametroAziende.nome.eq(nomeParametroDB)
            ).get();
            System.out.println(parametro.toString());
            System.out.println(parametro.getIdAziende().length);
            System.out.println("persone avvisate: " + idPersoneAvvisate);
            boolean everyAziendaDone = isEveryAziendaDone(parametro);
            System.out.println("everyAziendaDone " + everyAziendaDone);
            if (!everyAziendaDone) {
                loopAziendeAndManageWorker(parametro);
            }
            log.info("Totale persone avvisate: " + idPersoneAvvisate.size());
            System.out.println(idPersoneAvvisate);

        } catch (Throwable t) {
            log.error(t.getMessage());
            log.error(t.toString());
            t.printStackTrace();
        }
        log.info("Running out.");
        log.info(" * * * * * * ");
    }

}
