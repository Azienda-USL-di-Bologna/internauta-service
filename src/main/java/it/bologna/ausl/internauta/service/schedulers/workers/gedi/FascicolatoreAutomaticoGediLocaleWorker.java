/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.bologna.ausl.internauta.service.schedulers.workers.gedi;

import it.bologna.ausl.internauta.service.argo.raccolta.Fascicolo;
import it.bologna.ausl.internauta.service.argo.utils.gd.FascicoloGddocUtils;
import it.bologna.ausl.internauta.service.argo.utils.gd.FascicoloUtils;
import it.bologna.ausl.internauta.service.argo.utils.gd.GddocUtils;
import it.bologna.ausl.internauta.service.argo.utils.gd.SottoDocumentiUtils;
import it.bologna.ausl.internauta.service.configuration.utils.ReporitoryConnectionManager;
import it.bologna.ausl.internauta.service.exceptions.FascicolatoreAutomaticoGediLocaleWorkerException;
import it.bologna.ausl.internauta.service.exceptions.sai.FascicoloNotFoundException;
import it.bologna.ausl.internauta.service.repositories.shpeck.MessageRepository;
import it.bologna.ausl.internauta.service.repositories.shpeck.OutboxRepository;
import it.bologna.ausl.internauta.service.schedulers.workers.gedi.wrappers.FascicolatoreAutomaticoGediParams;
import it.bologna.ausl.minio.manager.MinIOWrapper;
import it.bologna.ausl.minio.manager.MinIOWrapperFileInfo;
import it.bologna.ausl.model.entities.shpeck.Message;
import it.bologna.ausl.model.entities.shpeck.Outbox;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ScheduledFuture;
import javax.validation.constraints.Min;
import org.apache.log4j.lf5.LogLevel;
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
// @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class FascicolatoreAutomaticoGediLocaleWorker implements Runnable {
    
    private static final Logger log = LoggerFactory.getLogger(FascicolatoreAutomaticoGediLocaleWorker.class);
    
    @Autowired
    private BeanFactory beanFactory;
    
    @Autowired
    private MessageRepository messageRepository;
    
    @Autowired
    private OutboxRepository outboxRepository;
    
    @Autowired
    private GddocUtils gddocUtils;
    
    @Autowired
    SottoDocumentiUtils sottoDocumentiUtils;
    
    @Autowired
    private FascicoloGddocUtils fascicoloGddocUtils;
    
    @Autowired
    ReporitoryConnectionManager aziendeConnectionManager;
    
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
    
    private Map<String, Object> getFascicolo() throws Exception {
        FascicoloUtils fascicoloUtils = beanFactory.getBean(FascicoloUtils.class);
        Map<String, Object> fascicolo = fascicoloUtils.getFascicoloByNumerazioneGerarchica(params.getIdAzienda(), params.getNumerazioneGerarchica());
        if (fascicolo != null) {
            log.info("Id found " + fascicolo);
        } else {
            throw new FascicoloNotFoundException("Fascicolo destinazione non trovato: " + params.getNumerazioneGerarchica());
        }
        log.info("Fascicolo found " + fascicolo.toString());
        return fascicolo;
    }
    
    private String getOggettoMail() throws Exception {
        Message message = null;
        try {
            message = messageRepository.findByIdOutbox(params.getIdOutbox());
        } catch (Exception e) {
            throw new Exception("Errore nel recuperare oggetto del Message con id_outbox " + params.getIdOutbox(), e);
        }
        
        return message.getName();
    }
    
    private String getMessageUuid() throws Exception {
        
        Message message = null;
        try {
            message = messageRepository.findByIdOutbox(params.getIdOutbox());
        } catch (Exception e) {
            throw new Exception("Errore nel recuperare oggetto del Message con id_outbox " + params.getIdOutbox(), e);
        }
        
        return message.getUuidRepository();
        
    }
    
    private boolean isOutboxSent() throws Exception {
        Outbox outbox = null;
        try {
            outbox = outboxRepository.findById(params.getIdOutbox()).get();
        } catch (Exception e) {
            throw new Exception("Errore nel recuperare l'outbox con id " + params.getIdOutbox(), e);
        }
        return outbox.getIgnore();
        
    }
    
    public MinIOWrapperFileInfo getFileInfo() throws Exception {
        MinIOWrapperFileInfo fileInfoByUuid = null;
        try {
            MinIOWrapper minIOWrapper = aziendeConnectionManager.getMinIOWrapper();
            fileInfoByUuid = minIOWrapper.getFileInfoByUuid(getMessageUuid());
        } catch (Exception e) {
            throw new Exception("Errore nel recuperare il file da mongo", e);
        }
        return fileInfoByUuid;
    }
    
    @Override
    public void run() {
        try {
            
            log.info("Runno...");
            log.info("Params: " + params.toString());
            
            if (isOutboxSent() && getMessageUuid() != null) {
                Map<String, Object> fascicolo = getFascicolo();
                String nome = getOggettoMail();
                Map<String, Object> gddoc = gddocUtils.createGddoc(params.getIdAzienda(), nome, null);
                log.info("Ora fascicolo il gddoc...");
                MinIOWrapperFileInfo fileInfo = getFileInfo();
                fileInfo.getFileName();
                fascicoloGddocUtils.fascicolaGddoc(params.getIdAzienda(), gddoc, fascicolo);
                sottoDocumentiUtils.createSottoDocumento(params.getIdAzienda(), (String) gddoc.get("id_gddoc"), fileInfo, null);
                
                if (scheduleObject != null) {
                    log.info("Setto cancel true");
                    scheduleObject.cancel(true);
                }
            }
            
        } catch (Exception ex) {
            log.error("Errore imprevisto durante l'esecuzione del mestiere "
                    + "di fascicolazione automatica; params\n:" + params.toString(), ex);
        }
    }
    
}
