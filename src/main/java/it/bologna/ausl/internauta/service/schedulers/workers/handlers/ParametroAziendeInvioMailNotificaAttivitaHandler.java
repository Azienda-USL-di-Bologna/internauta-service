/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.bologna.ausl.internauta.service.schedulers.workers.handlers;

import it.bologna.ausl.model.entities.configurazione.ParametroAziende;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Salo
 */
public class ParametroAziendeInvioMailNotificaAttivitaHandler {

    private final String LAST_EXECUTION_KEY = "lastExecution";
    private final String DATE_PATTERN = "yyyy-MM-dd HH:mm:ss";
    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern(DATE_PATTERN);

    private static final Logger log = LoggerFactory
            .getLogger(ParametroAziendeInvioMailNotificaAttivitaHandler.class);

    // E' la riga di parametro aziende
    ParametroAziende parametroAziende;

    // E' l'azienda di cui mi sto occupando
    Integer idAzienda;

    private boolean isToday(Date date) {
        LocalDate thatLocalDate = date.toInstant()
                .atZone(ZoneId.systemDefault())
                .toLocalDate();
        LocalDate TODAY = new Date().toInstant()
                .atZone(ZoneId.systemDefault())
                .toLocalDate();
        return thatLocalDate.isEqual(TODAY);
    }

    private Date decodificaLastExecutionTime(String lastExecution) throws ParseException {
//        LocalDateTime lastExecLocalDateTime = LocalDateTime.parse(lastExecution, formatter);
//        return java.util.Date
//                .from(lastExecLocalDateTime.atZone(ZoneId.systemDefault())
//                        .toInstant());
        return new SimpleDateFormat(DATE_PATTERN).parse(lastExecution);

    }

    public boolean isNotificationAlreadySentToday() throws ParseException {
        JSONObject jsonDettagliUltimoInvioAzienda = getDatiUltimaEsecuzioneMestiere();
        boolean alreadyDone = true;
        if (jsonDettagliUltimoInvioAzienda == null) {
            // l'azienda è nell'array delle aziende per cui il servizio è attivo,
            // ma non ho un json, quindi non ha MAI girato per questa, quindi deve girare ora.
            log.info("Mai runnato!");
            alreadyDone = false;
        } else {
            Date lastRun = decodificaLastExecutionTime(jsonDettagliUltimoInvioAzienda.get(LAST_EXECUTION_KEY).toString());
            if (lastRun == null) {
                alreadyDone = false;
            } else {
                // se l'ultimo run lo ha fatto in una data diversa da oggi torno false
                if (!isToday(lastRun)) {
                    alreadyDone = false;
                }
            }
        }
        return alreadyDone;
    }

    public JSONObject getDatiUltimaEsecuzioneMestiere() {
        JSONObject json = new JSONObject(parametroAziende.getValore());
        JSONObject datiEsecuzioneAzienda = null;
        try {
            datiEsecuzioneAzienda = (JSONObject) json.get(idAzienda.toString());
        } catch (JSONException jSONException) {
            log.info("Nella riga di parametri azienda on esiste il json per " + idAzienda);
        }
        return datiEsecuzioneAzienda;
    }

    public ParametroAziendeInvioMailNotificaAttivitaHandler(ParametroAziende parametroAziende, Integer idAzienda) {
        this.parametroAziende = parametroAziende;
        this.idAzienda = idAzienda;
    }

    /**
     * Prende il json con i dati di ultima esecuzione del mestiere e li setta
     * nel JSON ricavato dalla proprietà 'valore' di parametroAziende (proprietà
     * di classe). NB: la proprietà parametroAziende non viene modificata.
     *
     * @return il JSON aggiornato di 'valore' di ParametroAziende
     */
    public JSONObject setDatiUltimaEsecuzioneMestiere() {
        JSONObject json = new JSONObject(parametroAziende.getValore());
        JSONObject datiUltimaEsecuzioneMestiere = getDatiUltimaEsecuzioneMestiere();
        if (datiUltimaEsecuzioneMestiere == null) {
            datiUltimaEsecuzioneMestiere = new JSONObject();
        }
        LocalDateTime lastExecution = new Date().toInstant()
                .atZone(ZoneId.systemDefault())
                .toLocalDateTime();

        String lastRunDateString = new SimpleDateFormat(DATE_PATTERN).format(new Date());
        datiUltimaEsecuzioneMestiere.put(LAST_EXECUTION_KEY, lastRunDateString);
        json.put(idAzienda.toString(), datiUltimaEsecuzioneMestiere);
        return json;
    }

    public Integer getIdAzienda() {
        return idAzienda;
    }

    public ParametroAziende getParametroAziende() {
        return parametroAziende;
    }

    @Override
    public String toString() {
        return "ParametroAziendeInvioMailNotificaAttivitaHandler{" + "parametroAziende=" + parametroAziende.toString() + ", idAzienda=" + idAzienda + '}';
    }

}
