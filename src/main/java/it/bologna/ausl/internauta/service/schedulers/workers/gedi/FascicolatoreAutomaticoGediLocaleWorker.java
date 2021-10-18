package it.bologna.ausl.internauta.service.schedulers.workers.gedi;

import com.fasterxml.jackson.core.JsonProcessingException;
import it.bologna.ausl.internauta.service.argo.utils.gd.FascicoloGddocUtils;
import it.bologna.ausl.internauta.service.argo.utils.gd.FascicoloUtils;
import it.bologna.ausl.internauta.service.argo.utils.gd.GddocUtils;
import it.bologna.ausl.internauta.service.argo.utils.gd.SottoDocumentiUtils;
import it.bologna.ausl.internauta.service.configuration.utils.ReporitoryConnectionManager;
import it.bologna.ausl.internauta.service.exceptions.sai.FascicoloNotFoundException;
import it.bologna.ausl.internauta.service.repositories.shpeck.MessageRepository;
import it.bologna.ausl.internauta.service.repositories.shpeck.OutboxRepository;
import it.bologna.ausl.internauta.service.schedulers.workers.gedi.wrappers.FascicolatoreAutomaticoGediParams;
import it.bologna.ausl.internauta.service.shpeck.utils.ShpeckUtils;
import it.bologna.ausl.model.entities.shpeck.data.AdditionalDataTagComponent;
import it.bologna.ausl.minio.manager.MinIOWrapper;
import it.bologna.ausl.minio.manager.MinIOWrapperFileInfo;
import it.bologna.ausl.model.entities.baborg.Azienda;
import it.bologna.ausl.model.entities.baborg.Utente;
import it.bologna.ausl.model.entities.shpeck.Message;
import it.bologna.ausl.model.entities.shpeck.Outbox;
import it.bologna.ausl.model.entities.shpeck.Tag;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;
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
    private ShpeckUtils shpeckUtils;
    
    @Autowired
    private SottoDocumentiUtils sottoDocumentiUtils;
    
    @Autowired
    private FascicoloGddocUtils fascicoloGddocUtils;
    
    @Autowired
    private ReporitoryConnectionManager aziendeConnectionManager;
    
    private Message message;
    
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
        Map<String, Object> fascicolo = fascicoloUtils.getFascicoloByNumerazioneGerarchica(params.getIdAzienda().getId(), params.getNumerazioneGerarchica());
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
    
    private void loadMessage() throws Exception {
        try {
            this.message = messageRepository.findByIdOutbox(params.getIdOutbox());
        } catch (Exception e) {
            throw new Exception("Errore nel recuperare il Message con id_outbox " + params.getIdOutbox(), e);
        }
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
            fileInfoByUuid = minIOWrapper.getFileInfoByUuid(message.getUuidRepository());
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
            loadMessage();
            if (isOutboxSent() && message.getUuidRepository() != null) {
                Map<String, Object> fascicolo = getFascicolo();
                String nome = getOggettoMail();
                Map<String, Object> gddoc = gddocUtils.createGddoc(params.getIdAzienda().getId(), nome, null);
                log.info("Ora fascicolo il gddoc...");
                MinIOWrapperFileInfo fileInfo = getFileInfo();
                fileInfo.getFileName();
                fascicoloGddocUtils.fascicolaGddoc(params.getIdAzienda().getId(), gddoc, fascicolo);
                sottoDocumentiUtils.createSottoDocumento(params.getIdAzienda().getId(), (String) gddoc.get("id_gddoc"), fileInfo, null);

                log.info("messaggio fascicolato, setto il tag...");
                insertArchiviationTag(fascicolo, gddoc);

                if (scheduleObject != null) {
                    log.info("Setto cancel true");
                    scheduleObject.cancel(true);
                }
            }
            
        } catch (Exception ex) {
            log.error("Errore imprevisto durante l'esecuzione del mestiere di fascicolazione automatica; params\n:" + params.toString(), ex);
        }
    }
    
    private void insertArchiviationTag(Map<String, Object> fascicolo, Map<String, Object> gddoc) throws JsonProcessingException {
        Utente utente = params.getUtente();
        AdditionalDataTagComponent.AdditionalDataArchiviation additionalDataArchiviation = new AdditionalDataTagComponent().new  AdditionalDataArchiviation();

        AdditionalDataTagComponent.idUtente utenteAdditionalData = new AdditionalDataTagComponent().new idUtente(utente.getId(), params.getPersona().getDescrizione());
        additionalDataArchiviation.setIdUtente(utenteAdditionalData);

        Azienda azienda = params.getIdAzienda();
        AdditionalDataTagComponent.idAzienda aziendaAdditionalData = new AdditionalDataTagComponent().new idAzienda(azienda.getId(), azienda.getNome(), azienda.getDescrizione());
        additionalDataArchiviation.setIdAzienda(aziendaAdditionalData);

        String idFascicolo = (String) fascicolo.get("id_fascicolo");
        String oggettoFascicolo = (String) fascicolo.get("nome_fascicolo");
        String numerazioneGerarchicaFascicolo = (String) fascicolo.get("numerazione_gerarchica");
        AdditionalDataTagComponent.idFascicolo fascicoloAdditionalData = new AdditionalDataTagComponent().new idFascicolo(idFascicolo, oggettoFascicolo, numerazioneGerarchicaFascicolo);
        additionalDataArchiviation.setIdFascicolo(fascicoloAdditionalData);

        String idGdDoc = (String) gddoc.get("id_gddoc");
//        String oggettoGdDoc = (String) gddoc.get("oggetto");
        String oggettoGdDoc = (String) gddoc.get("nome_gddoc");
        AdditionalDataTagComponent.IdGdDoc gdDocAdditionalData = new AdditionalDataTagComponent().new IdGdDoc(idGdDoc, oggettoGdDoc);
        additionalDataArchiviation.setIdGdDoc(gdDocAdditionalData);

        additionalDataArchiviation.setDataArchiviazione(LocalDateTime.now());
        
        shpeckUtils.SetArchiviationTag(message.getIdPec(), message, additionalDataArchiviation, utente, true);
    }
}
