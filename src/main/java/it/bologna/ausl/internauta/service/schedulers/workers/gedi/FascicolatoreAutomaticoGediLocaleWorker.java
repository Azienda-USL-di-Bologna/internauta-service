package it.bologna.ausl.internauta.service.schedulers.workers.gedi;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import it.bologna.ausl.internauta.service.repositories.tools.PendingJobRepository;
import it.bologna.ausl.internauta.service.utils.CachedEntities;
import it.bologna.ausl.model.entities.baborg.Persona;
import it.bologna.ausl.model.entities.shpeck.data.AdditionalDataArchiviation;
import it.bologna.ausl.model.entities.tools.PendingJob;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
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
    
    @Autowired
    private PendingJobRepository pendingJobRepository;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @Autowired
    private CachedEntities cachedEntities;
    
    private Message message;
    
    private ScheduledFuture<?> scheduleObject;
    
    private PendingJob pendingJob;
    
    private FascicolatoreAutomaticoGediParams params;
    
    public void setScheduleObject(ScheduledFuture<?> schedule) {
        this.scheduleObject = schedule;
    }

    public PendingJob getPendingJob() {
        return pendingJob;
    }

    public void setPendingJob(PendingJob pendingJob) {
        this.pendingJob = pendingJob;
    }
    
    public FascicolatoreAutomaticoGediParams getParams() {
        return params;
    }
//    
//    public void setParams(FascicolatoreAutomaticoGediParams params) {
//        this.params = params;
//    }
//
//    public BigInteger getPendingJobId() {
//        return pendingJobId;
//    }
//
//    public void setPendingJobId(BigInteger pendingJobId) {
//        this.pendingJobId = pendingJobId;
//    }
    
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
    
    private void loadMessage() throws Exception {
        try {
            this.message = messageRepository.findByIdOutbox(params.getIdOutbox());
        } catch (Exception e) {
            throw new Exception("Errore nel recuperare il Message con id_outbox " + params.getIdOutbox(), e);
        }
    }
    
    private boolean isOutboxSent() throws Exception {
        try {
            Optional<Outbox> outboxOp = outboxRepository.findById(params.getIdOutbox());
            if (outboxOp.isPresent()) {
                return outboxOp.get().getIgnore();
            } else { 
                /* 
                  Se non lo trovo in outbox, potrebbe essere che il job sia vecchio e il messaggio sia stato tolto, perché l'outbox viene svuotato ogni tanto. 
                  In ogni caso se viene tolto allora vuol dire che il messaggio è stato spedito per cui torno true.
                */
                return true;
            }
        } catch (Exception e) {
            throw new Exception("Errore nel recuperare l'outbox con id " + params.getIdOutbox(), e);
        }
        
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
    
    private void loadParams() {
//        Optional<PendingJob> pendingJobOp = pendingJobRepository.findById(params.getPendingJobId());
        
        if (pendingJob.getData() != null) {
            this.params = objectMapper.convertValue(pendingJob.getData(), FascicolatoreAutomaticoGediParams.class);
        } else {
            log.error(String.format("PendingJob with id %s has empty data", pendingJob.getId()));
        }
    }
    
    private void setPendingJobState(PendingJob.PendigJobsState state) {
        log.info(String.format("set state %s on job %s", state.toString(), this.pendingJob.getId()));
        this.pendingJob.setState(state);
        this.pendingJob = pendingJobRepository.save(this.pendingJob);
    }
    
    private void reloadPendingJob() {
        log.info(String.format("reloading job %s...", pendingJob.getId()));
        Optional<PendingJob> pendingJobOp = pendingJobRepository.findById(pendingJob.getId());
        if (pendingJobOp.isPresent()) {
            this.pendingJob = pendingJobOp.get();
        } else {
            log.error("il job %s non esiste più, lo salto");
            scheduleObject.cancel(true);
        }
    }
    
    @Override
    public void run() {
        try {
            log.info(String.format("avvio thread sul job %s con stato %s...", this.pendingJob.getId(), this.pendingJob.getState().toString()));
            this.reloadPendingJob();
            this.setPendingJobState(PendingJob.PendigJobsState.RUNNING);
            
            loadParams();
            if (params == null) {
                return;
            }
            log.info("params: " + params.toString());
            
            loadMessage();
            if (message != null && isOutboxSent() && message.getUuidRepository() != null) {
                Map<String, Object> fascicolo = getFascicolo();
                String nome = getOggettoMail();
                
                // Cerco il gddoc. Se non esite lo creo
                log.info("creo il gddoc...");
                Map<String, Object> gddoc = gddocUtils.getGdDocByIdOutbox(params.getIdAzienda(), params.getIdOutbox());
                if (gddoc == null) {
                    gddoc = gddocUtils.createGddoc(params.getIdAzienda(), nome, null, params.getIdOutbox());
                } else {
                    log.info("gddoc già esistente, salto il passaggio");
                }
                log.info("Ora fascicolo il gddoc...");
                MinIOWrapperFileInfo fileInfo = getFileInfo();
                fileInfo.getFileName();
                // Se non c'è già la fascicolazione la inserisco. Può succedere se siamo in un retry in caso di down del servizio
                log.info("fascicolazione gdDoc...");
                if (fascicoloGddocUtils.getFascicolazione(params.getIdAzienda(), gddoc, fascicolo) == null) {
                    fascicoloGddocUtils.fascicolaGddoc(params.getIdAzienda(), gddoc, fascicolo);
                } else {
                    log.info("gddoc già fascicolato, salto il passaggio");
                }
                log.info("creazione il sottodocumento...");
                if (sottoDocumentiUtils.getSottoDocumentoByIdOutbox(params.getIdAzienda(), params.getIdOutbox()) == null) {
                    sottoDocumentiUtils.createSottoDocumento(params.getIdAzienda(), (String) gddoc.get("id_gddoc"), fileInfo, null, params.getIdOutbox());
                } else {
                    log.info("sottodocumento già esistente, salto il passaggio");
                }

                log.info("messaggio fascicolato, setto il tag...");
                // le funzione che tagga ha già il controllo per non inserire il tag se questo è già presente
                insertArchiviationTag(fascicolo, gddoc);

                if (scheduleObject != null) {
                    log.info("Il lavoro è finito, terminazione del thread");
                    setPendingJobState(PendingJob.PendigJobsState.DONE);
                    this.pendingJobRepository.delete(this.pendingJob);
                    scheduleObject.cancel(true);
                }
            }
        } catch (Exception ex) {
            log.error(String.format("Errore imprevisto durante l'esecuzione del job di fascicolazione automatica %s. Params %s", this.pendingJob.getId(),params.toString()), ex);
            try {
                this.setPendingJobState(PendingJob.PendigJobsState.ERROR);
            } catch (Exception exception) {
                log.error(String.format("Errore nel settare lo stato di ERROR del job %s", this.pendingJob.getId()), ex);
            }
            log.warn(String.format("cancello il job %s", this.pendingJob.getId()));
            scheduleObject.cancel(true);
        }
    }
    
    private void insertArchiviationTag(Map<String, Object> fascicolo, Map<String, Object> gddoc) throws JsonProcessingException {
        Utente utente = cachedEntities.getUtente(this.params.getUtente());
        Persona persona = cachedEntities.getPersona(this.params.getPersona());
        Azienda azienda = cachedEntities.getAzienda(this.params.getIdAzienda());
        AdditionalDataArchiviation additionalDataArchiviation = new AdditionalDataArchiviation();

        AdditionalDataTagComponent.idUtente utenteAdditionalData = new AdditionalDataTagComponent.idUtente(utente.getId(), persona.getDescrizione());
        additionalDataArchiviation.setIdUtente(utenteAdditionalData);

        
        AdditionalDataTagComponent.idAzienda aziendaAdditionalData = new AdditionalDataTagComponent.idAzienda(azienda.getId(), azienda.getNome(), azienda.getDescrizione());
        additionalDataArchiviation.setIdAzienda(aziendaAdditionalData);

        String idFascicolo = (String) fascicolo.get("id_fascicolo");
        String oggettoFascicolo = (String) fascicolo.get("nome_fascicolo");
        String numerazioneGerarchicaFascicolo = (String) fascicolo.get("numerazione_gerarchica");
        AdditionalDataTagComponent.idFascicolo fascicoloAdditionalData = new AdditionalDataTagComponent.idFascicolo(idFascicolo, oggettoFascicolo, numerazioneGerarchicaFascicolo);
        additionalDataArchiviation.setIdFascicolo(fascicoloAdditionalData);

        String idGdDoc = (String) gddoc.get("id_gddoc");
//        String oggettoGdDoc = (String) gddoc.get("oggetto");
        String oggettoGdDoc = (String) gddoc.get("nome_gddoc");
        AdditionalDataTagComponent.IdGdDoc gdDocAdditionalData = new AdditionalDataTagComponent.IdGdDoc(idGdDoc, oggettoGdDoc);
        additionalDataArchiviation.setIdGdDoc(gdDocAdditionalData);

        additionalDataArchiviation.setDataArchiviazione(LocalDateTime.now());
        
        shpeckUtils.SetArchiviationTag(message.getIdPec(), message, additionalDataArchiviation, utente, true);
    }
}
