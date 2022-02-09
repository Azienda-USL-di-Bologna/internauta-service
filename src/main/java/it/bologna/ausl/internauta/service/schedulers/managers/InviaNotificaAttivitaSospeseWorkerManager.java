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
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
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

    private final String DATE_PATTERN = "yyyy-MM-dd HH:mm:ss";

    private List<Long> idPersoneAvvisate = new ArrayList<>();

    private static final Logger log = LoggerFactory
            .getLogger(InviaNotificaAttivitaSospeseWorkerManager.class);

    @Autowired
    ParametriAziendeReader par;

    @Autowired
    InviaNotificaAttivitaSospeseWorker worker;

    @Autowired
    AziendaRepository aziendaRepository;

    @Autowired
    ParametroAziendeRepository parametroAziendeRepository;

    private ParametroAziende parametroAziende;

    private final String nomeParametroDB = "inviaMailNotificaAttivitàSospese";

    private boolean isToday(Date date) {
        LocalDate thatLocalDate = date.toInstant()
                .atZone(ZoneId.systemDefault())
                .toLocalDate();
        LocalDate TODAY = new Date().toInstant()
                .atZone(ZoneId.systemDefault())
                .toLocalDate();
        return thatLocalDate.isEqual(TODAY);
    }

    private boolean isUltimoGiroPerTuttiPrimaDiOggi() {
        boolean beforeToday = true;
        String valore = parametroAziende.getValore();
        if (valore != null && !valore.trim().equalsIgnoreCase("")) {
            JSONObject datiTutteAziende = new JSONObject(valore);
            Set<String> keySet = datiTutteAziende.keySet();

            for (Iterator<String> iterator = keySet.iterator(); iterator.hasNext();) {
                String key = iterator.next();
                JSONObject datiUltimoGiroAzienda = (JSONObject) datiTutteAziende.get(key);
                String lastExecutionString = (String) datiUltimoGiroAzienda.get("lastExecution");
                try {
                    Date lastExecutionDate = new SimpleDateFormat(DATE_PATTERN).parse(lastExecutionString);
                    if (isToday(lastExecutionDate)) {
                        return false;
                    }

                } catch (ParseException ex) {
                    java.util.logging.Logger.getLogger(InviaNotificaAttivitaSospeseWorkerManager.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        } else {
            beforeToday = false;
        }
        return beforeToday;

    }

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
        parametroAziende = parametroAziendeRepository.saveAndFlush(parametroAziende);

    }

    private void updateAndReloadParametroAziende(ParametroAziende parametroAziende, ParametroAziendeInvioMailNotificaAttivitaHandler handler) {
        updateParametroAziendeNuovoCicloEsecuzione(parametroAziende, handler);
        parametroAziende = loadParametroAziende();
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
                //worker.setParameter(idPersoneAvvisate, handler);
                log.info("Runno per azienda " + handler.getIdAzienda().toString());
                worker.run();
                log.info("Mestiere finito su azienda " + handler.getIdAzienda().toString());
//                log.info("Setto il nuovo stato di esecuzione su azienda " + handler.getIdAzienda().toString());
//                updateAndReloadParametroAziende(parametroAziende, handler);
                parametroAziende = loadParametroAziende();

            }
            System.out.println("ok");
            idPersoneAvvisate = worker.loadPersoneNotificate(aziendaRepository.getById(idAziende[i]));
            log.info("Persone avvisate per azienda " + idAziende[i].toString() + ": " + idPersoneAvvisate.size());
        }
    }

    private boolean isInAziendeAttive(String idAzienda) {
        boolean attiva = false;
        Integer[] array = parametroAziende.getIdAziende();
        for (int i = 0; i < array.length; i++) {
            Integer integer = array[i];
            if (integer.equals(Integer.parseInt(idAzienda))) {
                attiva = true;
                break;
            }
        }
        return attiva;
    }

    private ParametroAziende cleanUpValoreParametroAziende() {
        System.out.println("cleanUpValoreParametroAziende()");
        String valoreString = parametroAziende.getValore();
        JSONObject valoreJSON = new JSONObject(valoreString);
        if (valoreJSON != null) {
            for (Iterator<String> keys = valoreJSON.keys(); keys.hasNext();) {
                String idAziendaString = keys.next();
                if (!isInAziendeAttive(idAziendaString)) {
                    JSONObject datiUltimaEsecuzione = (JSONObject) valoreJSON.get(idAziendaString);
                    String lastExecutionString = (String) datiUltimaEsecuzione.get("lastExecution");
                    try {
                        Date lastExecutionDate = new SimpleDateFormat(DATE_PATTERN).parse(lastExecutionString);
                        if (!isToday(lastExecutionDate)) {
                            log.info("Rimuovo i dati di ultima esecuzione di azienda " + idAziendaString);
                            valoreJSON.remove(idAziendaString);
                        }
                    } catch (ParseException ex) {
                        java.util.logging.Logger.getLogger(InviaNotificaAttivitaSospeseWorkerManager.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            }
            log.info("ParametroAziende.valore pulito: " + valoreJSON.toString(4));
            parametroAziende.setValore(valoreJSON.toString());
        }

        return parametroAziende;
    }

    private ParametroAziende loadParametroAziende() {
        return parametroAziendeRepository.findOne(
                QParametroAziende.parametroAziende.nome.eq(nomeParametroDB)
        ).get();
    }

    public void run() {
        log.info("Start running...");
        try {

            parametroAziende = loadParametroAziende();

            log.info(parametroAziende.toString());

            log.info("aziende attive " + parametroAziende.getIdAziende().length);
            System.out.println("persone avvisate: " + idPersoneAvvisate);
            System.out.println("Pulisco parametro aziende");

            parametroAziende = cleanUpValoreParametroAziende();
            boolean everyAziendaDone = isEveryAziendaDone(parametroAziende);
            if (isUltimoGiroPerTuttiPrimaDiOggi()) {
                log.info("Svuotopersone array avvisate");
                idPersoneAvvisate = new ArrayList<>();
            }
            System.out.println("everyAziendaDone " + everyAziendaDone);
            if (!everyAziendaDone) {
                loopAziendeAndManageWorker(parametroAziende);
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
