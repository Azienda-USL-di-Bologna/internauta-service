/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.bologna.ausl.internauta.service.schedulers.workers.gedi;

import it.bologna.ausl.internauta.service.argo.raccolta.Fascicolo;
import it.bologna.ausl.internauta.service.argo.utils.FascicoloUtils;
import it.bologna.ausl.internauta.service.argo.utils.GddocUtils;
import it.bologna.ausl.internauta.service.exceptions.sai.FascicoloNotFoundException;
import it.bologna.ausl.internauta.service.repositories.shpeck.MessageRepository;
import it.bologna.ausl.internauta.service.schedulers.workers.gedi.wrappers.FascicolatoreAutomaticoGediParams;
import it.bologna.ausl.model.entities.shpeck.Message;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 *
 * @author Salo
 */
@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class FascicolatoreAutomaticoGediLocaleWorker implements Runnable {

    private static final Logger log = LoggerFactory.getLogger(FascicolatoreAutomaticoGediLocaleWorker.class);

    @Autowired
    private BeanFactory beanFactory;

    @Autowired
    MessageRepository messageRepository;

    @Autowired
    GddocUtils gddocUtils;

    private ScheduledFuture<?> scheduleObject;

    private FascicolatoreAutomaticoGediParams params;

    public void setScheduleObject(ScheduledFuture<?> schedule) {
        this.scheduleObject = schedule;
    }

    public FascicolatoreAutomaticoGediParams getParams() {
        return params;
    }

    public void setParams(FascicolatoreAutomaticoGediParams params) {
        this.params = params;
    }

    private String getFascicolo() throws Exception {
        FascicoloUtils fascicoloUtils = beanFactory.getBean(FascicoloUtils.class);
        String idFascicolo = fascicoloUtils.getIdFascicoloByNumerazioneGerarchica(params.getIdAzienda(), params.getNumerazioneGerarchica());
        if (idFascicolo != null) {
            log.info("Id found " + idFascicolo);
        } else {
            throw new FascicoloNotFoundException("Fascicolo destinazione non trovato: " + params.getNumerazioneGerarchica());
        }
        log.info("Fascicolo found " + idFascicolo);

        // creare gdddoc
        // fascicolare gddoc
        return idFascicolo;
    }

    private String getOggettoMail() {
        Message message = messageRepository.findByIdOutbox(params.getIdOutbox());
        return message.getName();
    }

    @Override
    public void run() {
        try {

            log.info("Runno...");
            log.info("Params: " + params.toString());
            String idFascicolo = getFascicolo();
            String nome = getOggettoMail();
            Map<String, Object> gddoc = gddocUtils.createGddoc(params.getIdAzienda(), nome, null);

        } catch (Exception ex) {
            log.error(ex.toString());
            ex.printStackTrace();
        } finally {
            if (scheduleObject != null) {
                log.info("Setto cancel true");
                scheduleObject.cancel(true);
            }
        }
    }

}
