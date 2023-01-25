package it.bologna.ausl.internauta.utils.masterjobs.workers.jobs.fascicolatoresai;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.querydsl.jpa.impl.JPAQueryFactory;
import it.bologna.ausl.internauta.service.argo.utils.gd.FascicoloGddocUtils;
import it.bologna.ausl.internauta.service.argo.utils.gd.FascicoloUtils;
import it.bologna.ausl.internauta.service.argo.utils.gd.GddocUtils;
import it.bologna.ausl.internauta.service.argo.utils.gd.SottoDocumentiUtils;
import it.bologna.ausl.internauta.service.configuration.utils.ReporitoryConnectionManager;
import it.bologna.ausl.internauta.service.exceptions.sai.FascicoloNotFoundException;
import it.bologna.ausl.internauta.service.shpeck.utils.ShpeckUtils;
import it.bologna.ausl.internauta.service.utils.CachedEntities;
import it.bologna.ausl.internauta.utils.masterjobs.annotations.MasterjobsWorker;
import it.bologna.ausl.internauta.utils.masterjobs.exceptions.MasterjobsWorkerException;
import it.bologna.ausl.internauta.utils.masterjobs.workers.jobs.JobWorker;
import it.bologna.ausl.internauta.utils.masterjobs.workers.jobs.JobWorkerResult;
import it.bologna.ausl.minio.manager.MinIOWrapper;
import it.bologna.ausl.minio.manager.MinIOWrapperFileInfo;
import it.bologna.ausl.model.entities.baborg.Azienda;
import it.bologna.ausl.model.entities.baborg.Persona;
import it.bologna.ausl.model.entities.baborg.Utente;
import it.bologna.ausl.model.entities.shpeck.Message;
import it.bologna.ausl.model.entities.shpeck.QMessage;
import it.bologna.ausl.model.entities.shpeck.QOutbox;
import it.bologna.ausl.model.entities.shpeck.data.AdditionalDataArchiviation;
import it.bologna.ausl.model.entities.shpeck.data.AdditionalDataTagComponent;
import java.time.LocalDateTime;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

/**
 *
 * @author gdm
 */
@MasterjobsWorker
public class FascicolatoreSAIWorker extends JobWorker<FascicolatoreSAIWorkerData, JobWorkerResult> {
    private static final Logger log = LoggerFactory.getLogger(FascicolatoreSAIWorker.class);
    private final String name = FascicolatoreSAIWorker.class.getSimpleName();

    @Autowired
    private CachedEntities cachedEntities;
    
    @Autowired
    private FascicoloUtils fascicoloUtils;
    
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
    
    @Override
    public String getName() {
        return this.name;
    }
    
    @Override
    public JobWorkerResult doRealWork() throws MasterjobsWorkerException {
        log.info(String.format("avvio %s", getName()));
        FascicolatoreSAIWorkerData workerData = getWorkerData();

        try {
            loadMessageByIdOutbox();
            Map<String, Object> fascicolo = getFascicolo();
            String nome = message.getName();

            // Cerco il gddoc. Se non esite lo creo
            log.info("creo il gddoc...");
            Map<String, Object> gddoc = gddocUtils.getGdDocByIdOutbox(workerData.getIdAzienda(), workerData.getIdOutbox());
            if (gddoc == null) {
                gddoc = gddocUtils.createGddoc(workerData.getIdAzienda(), nome, null, workerData.getIdOutbox());
            } else {
                log.info("gddoc già esistente, salto il passaggio");
            }
            log.info("Ora fascicolo il gddoc...");
            MinIOWrapperFileInfo fileInfo = getFileInfo();

            // Se non c'è già la fascicolazione la inserisco. Può succedere se siamo in un rilancio in caso di down del servizio
            log.info("fascicolazione gdDoc...");

            if (fascicoloGddocUtils.getFascicolazione(workerData.getIdAzienda(), gddoc, fascicolo) == null) {
                fascicoloGddocUtils.fascicolaGddoc(workerData.getIdAzienda(), gddoc, fascicolo);
            } else {
                log.info("gddoc già fascicolato, salto il passaggio");
            }
            log.info("creazione il sottodocumento...");
            if (sottoDocumentiUtils.getSottoDocumentoByIdOutbox(workerData.getIdAzienda(), workerData.getIdOutbox()) == null) {
                sottoDocumentiUtils.createSottoDocumento(workerData.getIdAzienda(), (String) gddoc.get("id_gddoc"), fileInfo, null, workerData.getIdOutbox());
            } else {
                log.info("sottodocumento già esistente, salto il passaggio");
            }
            log.info("messaggio fascicolato, setto il tag...");
            // le funzione che tagga ha già il controllo per non inserire il tag se questo è già presente
            insertArchiviationTag(fascicolo, gddoc);
        } catch (Exception ex) {
            String errorMessage = "errore fascicolazione";
            log.error(errorMessage, ex);
            throw new MasterjobsWorkerException(errorMessage, ex);
        }
        return null;
    }

    @Override
    public boolean isExecutable() {
        try {
            loadMessageByIdOutbox();
            return (message != null && isOutboxSent() && message.getUuidRepository() != null);
        } catch (MasterjobsWorkerException ex) {
            String errorMessage = "errore nel isExecutable(), lo considero false";
            log.error(errorMessage, ex);
            return false;
        }
    }
    
    
    private void loadMessageByIdOutbox() {
        QMessage qMessage = QMessage.message;
        JPAQueryFactory jPAQueryFactory = new JPAQueryFactory(entityManager);
        this.message = jPAQueryFactory.selectFrom(qMessage).where(qMessage.idOutbox.eq(getWorkerData().getIdOutbox())).fetchOne();;
    }
    
     private boolean isOutboxSent() throws MasterjobsWorkerException {
        try {
            QOutbox qOutbox = QOutbox.outbox;
            JPAQueryFactory jPAQueryFactory = new JPAQueryFactory(entityManager);
            Boolean ignore = jPAQueryFactory
                    .select(qOutbox.ignore)
                    .from(qOutbox)
                    .where(qOutbox.id.eq(getWorkerData().getIdOutbox()))
                    .fetchOne();
            /* 
            Se non lo trovo in outbox (per cui la qury torna null), potrebbe essere che il job sia vecchio e il messaggio sia stato tolto, 
            perché l'outbox viene svuotato ogni tanto.  In ogni caso se viene tolto allora vuol dire che il messaggio è stato spedito per cui
            torno true.
            */
            return ignore == null || ignore;
        } catch (Exception ex) {
            String errorMessage = String.format("Errore nel recuperare l'outbox con id %s", getWorkerData().getIdOutbox());
            log.error(errorMessage, ex);
            throw new MasterjobsWorkerException(errorMessage, ex);
        }
    }
     
    private Map<String, Object> getFascicolo() throws FascicoloNotFoundException {
        FascicolatoreSAIWorkerData data = getWorkerData();
        Map<String, Object> fascicolo = fascicoloUtils.getFascicoloByNumerazioneGerarchica(data.getIdAzienda(), data.getNumerazioneGerarchica());
        if (fascicolo != null) {
            log.info("Id found " + fascicolo);
        } else {
            throw new FascicoloNotFoundException("Fascicolo destinazione non trovato: " + data.getNumerazioneGerarchica());
        }
        log.info("Fascicolo found " + fascicolo.toString());
        return fascicolo;
    }
    
    public MinIOWrapperFileInfo getFileInfo() throws MasterjobsWorkerException {
        MinIOWrapperFileInfo fileInfoByUuid = null;
        try {
            MinIOWrapper minIOWrapper = aziendeConnectionManager.getMinIOWrapper();
            fileInfoByUuid = minIOWrapper.getFileInfoByUuid(message.getUuidRepository());
        } catch (Exception ex) {
            String errorMessage = String.format("Errore nel recuperare il file con id %s dal repository", message.getUuidRepository());
            log.error(errorMessage, ex);
            throw new MasterjobsWorkerException(errorMessage, ex);
        }
        return fileInfoByUuid;
    }
    
    private void insertArchiviationTag(Map<String, Object> fascicolo, Map<String, Object> gddoc) throws JsonProcessingException {
        FascicolatoreSAIWorkerData workerData = getWorkerData();
        Utente utente = cachedEntities.getUtente(workerData.getIdUtente());
        Persona persona = cachedEntities.getPersona(workerData.getIdPersona());
        Azienda azienda = cachedEntities.getAzienda(workerData.getIdAzienda());
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
