
package it.bologna.ausl.internauta.service.krint;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.bologna.ausl.model.entities.logs.Krint;
import it.bologna.ausl.model.entities.logs.OperazioneKrint;
import it.bologna.ausl.model.entities.logs.projections.KrintShpeckDraft;
import it.bologna.ausl.model.entities.logs.projections.KrintShpeckFolder;
import it.bologna.ausl.model.entities.logs.projections.KrintShpeckMessage;
import it.bologna.ausl.model.entities.logs.projections.KrintShpeckOutbox;
import it.bologna.ausl.model.entities.logs.projections.KrintShpeckPec;
import it.bologna.ausl.model.entities.logs.projections.KrintShpeckTag;
import it.bologna.ausl.model.entities.shpeck.Draft;
import it.bologna.ausl.model.entities.shpeck.Folder;
import it.bologna.ausl.model.entities.shpeck.Message;
import it.bologna.ausl.model.entities.shpeck.MessageFolder;
import it.bologna.ausl.model.entities.shpeck.Outbox;
import it.bologna.ausl.model.entities.shpeck.Tag;
import it.bologna.ausl.model.entities.shpeck.data.AdditionalDataArchiviation;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.projection.ProjectionFactory;
import org.springframework.stereotype.Service;

/**
 *
 * @author gusgus
 */
@Service
public class KrintShpeckService {
    private static final Logger LOGGER = LoggerFactory.getLogger(KrintShpeckService.class);
    
    @Autowired
    ProjectionFactory factory;
   
    @Autowired
    ObjectMapper objectMapper;
    
    @Autowired
    KrintService krintService;
    
    /**
     * Questa funzione si occupa di preparare il log per il reindirizzamento.
     * @param message è il messaggio che subisce l'operazione
     * @param correlatedMessage è il messaggio collegato all'operazione
     * @param codiceOperazione: PEC_MESSAGE_REINDIRIZZAMENTO_IN - PEC_MESSAGE_REINDIRIZZAMENTO_OUT
     */
    public void writeReaddress(Message message, Message correlatedMessage, OperazioneKrint.CodiceOperazione codiceOperazione) {
        try {
            // Informazioni oggetto
            KrintShpeckMessage krintPecMessage = factory.createProjection(KrintShpeckMessage.class, message);
            KrintShpeckMessage coorelatedkrintPecMessage = factory.createProjection(KrintShpeckMessage.class, correlatedMessage);
            Map<String, Object> map = new HashMap();
            map.put("idMessage", krintPecMessage);
            map.put("idMessageCorrelated", coorelatedkrintPecMessage);
//            String jsonKrintPecMessage = objectMapper.writeValueAsString(map);
            
            // Informazioni oggetto contenitore
            KrintShpeckPec krintPec = factory.createProjection(KrintShpeckPec.class, message.getIdPec());
//            String jsonKrintPec = objectMapper.writeValueAsString(krintPec);
            HashMap<String, Object> krintPecMap = objectMapper.convertValue(krintPec, new TypeReference<HashMap<String, Object>>() {});
            krintService.writeKrintRow(
                message.getId().toString(),
                Krint.TipoOggettoKrint.SHPECK_MESSAGE,
                message.getId().toString(),
                map,
                message.getIdPec().getId().toString(),
                Krint.TipoOggettoKrint.BABORG_PEC,
                message.getIdPec().getIndirizzo(),
                krintPecMap,
                codiceOperazione);
        } catch (Exception ex) {
            LOGGER.error("Errore nella writeReaddress con messaggio" + message.getId().toString(), ex);
            krintService.writeKrintError(message.getId(), "writeReaddress", codiceOperazione);
        }
    }
    
    /**
     * Questa funzione si occupa di preparare il log che riguarda la protocollazione
     * @param message
     * @param codiceOperazione: PEC_MESSAGE_IN_PROTOCOLLAZIONE - PEC_MESSAGE_PROTOCOLLAZIONE - PEC_MESSAGE_REMOVE_IN_PROTOCOLLAZIONE
     */    
    public void writeRegistration(Message message, OperazioneKrint.CodiceOperazione codiceOperazione) {
        try {
            // Informazioni oggetto
            KrintShpeckMessage krintPecMessage = factory.createProjection(KrintShpeckMessage.class, message);
//            String jsonKrintPecMessage = objectMapper.writeValueAsString(krintPecMessage);
            HashMap<String, Object> krintPecMessageMap = objectMapper.convertValue(krintPecMessage, new TypeReference<HashMap<String, Object>>() {});
            // Informazioni oggetto contenitore
            KrintShpeckPec krintPec = factory.createProjection(KrintShpeckPec.class, message.getIdPec());
//            String jsonKrintPec = objectMapper.writeValueAsString(krintPec);
            HashMap<String, Object> krintPecMap = objectMapper.convertValue(krintPec, new TypeReference<HashMap<String, Object>>() {});
            krintService.writeKrintRow(
                message.getId().toString(),
                Krint.TipoOggettoKrint.SHPECK_MESSAGE,
                message.getId().toString(),
                krintPecMessageMap,
                message.getIdPec().getId().toString(),
                Krint.TipoOggettoKrint.BABORG_PEC,
                message.getIdPec().getIndirizzo(),
                krintPecMap,
                codiceOperazione);
        } catch (Exception ex) {
            LOGGER.error("Errore nella writeRegistration con messaggio" + message.getId().toString(), ex);
            krintService.writeKrintError(message.getId(), "writeRegistration", codiceOperazione);
        }
    }
    
    /**
     * Questa funzione si occupa di preparare il log per la fascicolazione
     * @param message
     * @param codiceOperazione: PEC_MESSAGE_FASCICOLAZIONE
     * @param additionalDataArchiviation
     */
    public void writeArchiviation(Message message, OperazioneKrint.CodiceOperazione codiceOperazione, AdditionalDataArchiviation additionalDataArchiviation) {
        try {
            // Informazioni oggetto
            KrintShpeckMessage krintPecMessage = factory.createProjection(KrintShpeckMessage.class, message);
            Map<String, Object> map = new HashMap();
            map.put("idMessage", krintPecMessage);
            map.put("tagAdditionalData", additionalDataArchiviation);
//            String jsonKrintPecMessage = objectMapper.writeValueAsString(map);
            
            // Informazioni oggetto contenitore
            KrintShpeckPec krintPec = factory.createProjection(KrintShpeckPec.class, message.getIdPec());
//            String jsonKrintPec = objectMapper.writeValueAsString(krintPec);
            HashMap<String, Object> krintPecMap = objectMapper.convertValue(krintPec, new TypeReference<HashMap<String, Object>>() {});
            krintService.writeKrintRow(
                message.getId().toString(),
                Krint.TipoOggettoKrint.SHPECK_MESSAGE,
                message.getId().toString(),
                map,
                message.getIdPec().getId().toString(),
                Krint.TipoOggettoKrint.BABORG_PEC,
                message.getIdPec().getIndirizzo(),
                krintPecMap,
                codiceOperazione);
        } catch (Exception ex) {
            LOGGER.error("Errore nella writeReaddress con messaggio" + message.getId().toString(), ex);
            krintService.writeKrintError(message.getId(), "writeArchiviation", codiceOperazione);
        }
    }
    
    /**
     * Questa funzione si occupa di preparare il log per il settaggio del visto non visto nel messaggio
     * @param message
     * @param codiceOperazione: PEC_MESSAGE_LETTO - PEC_MESSAGE_DA_LEGGERE
     */    
    public void writeSeenOrNotSeen(Message message, OperazioneKrint.CodiceOperazione codiceOperazione) {
        try {
            // Informazioni oggetto
            KrintShpeckMessage krintPecMessage = factory.createProjection(KrintShpeckMessage.class, message);    
//            String jsonKrintPecMessage = objectMapper.writeValueAsString(krintPecMessage);
            HashMap<String, Object> krintPecMessageMap = objectMapper.convertValue(krintPecMessage, new TypeReference<HashMap<String, Object>>() {});
            // Informazioni oggetto contenitore
            KrintShpeckPec krintPec = factory.createProjection(KrintShpeckPec.class, message.getIdPec());   
//            String jsonKrintPec = objectMapper.writeValueAsString(krintPec);
            HashMap<String, Object> krintPecMap = objectMapper.convertValue(krintPec, new TypeReference<HashMap<String, Object>>() {});
            krintService.writeKrintRow(
                message.getId().toString(),
                Krint.TipoOggettoKrint.SHPECK_MESSAGE,
                message.getId().toString(),
                krintPecMessageMap,
                message.getIdPec().getId().toString(),
                Krint.TipoOggettoKrint.BABORG_PEC,
                message.getIdPec().getIndirizzo(),
                krintPecMap,
                codiceOperazione);                                                    
        } catch (Exception ex) {
            LOGGER.error("Errore nella writeSeenOrNotSeen con messaggio" + message.getId().toString(), ex);
            krintService.writeKrintError(message.getId(), "writeSeenOrNotSeen", codiceOperazione);
        }  
    }
    
    /**
     * Questa funzione si occupa di preparare il log per lo spostamento di un message tra folder
     * @param message
     * @param codiceOperazione: PEC_MESSAGE_SPOSTAMENTO
     * @param folder 
     * @param idPreviousFolder 
     */
    public void writeFolderChanged(Message message, OperazioneKrint.CodiceOperazione codiceOperazione, Folder folder, Folder idPreviousFolder) {
        try {
            // Informazioni oggetto
            KrintShpeckMessage krintPecMessage = factory.createProjection(KrintShpeckMessage.class, message);
            KrintShpeckFolder krintPecFolder = factory.createProjection(KrintShpeckFolder.class, folder);
            KrintShpeckFolder krintPecPreviousFolder = null;
            LOGGER.info("idPreviousFolder: " + idPreviousFolder);
            if (idPreviousFolder != null) {
                krintPecPreviousFolder = factory.createProjection(KrintShpeckFolder.class, idPreviousFolder);
            }
            Map<String, Object> map = new HashMap();
            map.put("idMessage", krintPecMessage);
            map.put("idFolder", krintPecFolder);
            map.put("idPreviousFolder", krintPecPreviousFolder);
//            String jsonKrintPecMessage = objectMapper.writeValueAsString(map);
            
            // Informazioni oggetto contenitore
            KrintShpeckPec krintPec = factory.createProjection(KrintShpeckPec.class, message.getIdPec());   
//            String jsonKrintPec = objectMapper.writeValueAsString(krintPec);
            HashMap<String, Object> krintPecMap = objectMapper.convertValue(krintPec, new TypeReference<HashMap<String, Object>>() {});
            krintService.writeKrintRow(
                message.getId().toString(),
                Krint.TipoOggettoKrint.SHPECK_MESSAGE,
                message.getId().toString(),
                map,
                message.getIdPec().getId().toString(),
                Krint.TipoOggettoKrint.BABORG_PEC,
                message.getIdPec().getIndirizzo(),
                krintPecMap,
                codiceOperazione);                                                    
        } catch (Exception ex) {
            LOGGER.error("Errore nella writeFolderChanged con messaggio" + message.getId().toString(), ex);
            krintService.writeKrintError(message.getId(), "writeFolderChanged", codiceOperazione);
        }  
    }
    
    /**
     * Questa funzione si occupa di preparare il log per la risposta/inoltro ad un messaggio.
     * @param message
     * @param codiceOperazione: PEC_MESSAGE_RISPOSTA - PEC_MESSAGE_RISPOSTA_A_TUTTI - PEC_MESSAGE_INOLTRO
     */
    public void writeReplyToMessage(Message message, OperazioneKrint.CodiceOperazione codiceOperazione) {
        try {
            // Informazioni oggetto
            KrintShpeckMessage krintPecMessage = factory.createProjection(KrintShpeckMessage.class, message);
//            String jsonKrintPecMessage = objectMapper.writeValueAsString(krintPecMessage);
            HashMap<String, Object> krintPecMessageMap = objectMapper.convertValue(krintPecMessage, new TypeReference<HashMap<String, Object>>() {});
            // Informazioni oggetto contenitore
            KrintShpeckPec krintPec = factory.createProjection(KrintShpeckPec.class, message.getIdPec());   
//            String jsonKrintPec = objectMapper.writeValueAsString(krintPec);
            HashMap<String, Object> krintPecMap = objectMapper.convertValue(krintPec, new TypeReference<HashMap<String, Object>>() {});
            
            krintService.writeKrintRow(
                message.getId().toString(),
                Krint.TipoOggettoKrint.SHPECK_MESSAGE,
                message.getId().toString(),
                krintPecMessageMap,
                message.getIdPec().getId().toString(),
                Krint.TipoOggettoKrint.BABORG_PEC,
                message.getIdPec().getIndirizzo(),
                krintPecMap,
                codiceOperazione);                                                    
        } catch (Exception ex) {
            LOGGER.error("Errore nella writeReplyToMessage con messaggio" + message.getId().toString(), ex);
            krintService.writeKrintError(message.getId(), "writeReplyToMessage", codiceOperazione);
        }  
    }
    
    /**
     * Questa funzione si occupa di preparare il log per l'aggiunta o rimozione di un tag ad un messaggio.
     * Si occupa di tutti i tag gestibili dall'utente cioè tag CUSTOM e i tag "assigned" e "in_error"
     * @param message
     * @param tag
     * @param codiceOperazione: PEC_MESSAGE_AGGIUNTA_TAG - PEC_MESSAGE_ELIMINAZIONE_TAG - PEC_MESSAGE_ERRORE_VISTO - PEC_MESSAGE_ERRORE_NON_VISTO
     */
    public void writeMessageTag(Message message, Tag tag, OperazioneKrint.CodiceOperazione codiceOperazione) {
        try {
            // Informazioni oggetto
            KrintShpeckMessage krintPecMessage = factory.createProjection(KrintShpeckMessage.class, message);
            KrintShpeckTag krintPecTag = factory.createProjection(KrintShpeckTag.class, tag);             
            Map<String, Object> krintPecMessageMap = new HashMap();
            krintPecMessageMap.put("idMessage", krintPecMessage);
            krintPecMessageMap.put("idTag", krintPecTag);
//            String jsonKrintPecMessage = objectMapper.writeValueAsString(map);
            
            // Informazioni oggetto contenitore
            KrintShpeckPec krintPec = factory.createProjection(KrintShpeckPec.class, message.getIdPec());   
//            String jsonKrintPec = objectMapper.writeValueAsString(krintPec);
            HashMap<String, Object> krintPecMap = objectMapper.convertValue(krintPec, new TypeReference<HashMap<String, Object>>() {});
            krintService.writeKrintRow(
                message.getId().toString(),
                Krint.TipoOggettoKrint.SHPECK_MESSAGE,
                message.getId().toString(),
                krintPecMessageMap,
                message.getIdPec().getId().toString(),
                Krint.TipoOggettoKrint.BABORG_PEC,
                message.getIdPec().getIndirizzo(),
                krintPecMap,
                codiceOperazione);                                                    
        } catch (Exception ex) {
            LOGGER.error("Errore nella writeMessageTag con messaggio" + message.getId().toString(), ex);
            krintService.writeKrintError(message.getId(), "writeMessageTag", codiceOperazione);
        }  
    }
    
    /**
     * Questa funzione si occupa di preparare il log per le bozze
     * @param draft
     * @param codiceOperazione: PEC_DRAFT_CREAZIONE - PEC_DRAFT_MODIFICA - PEC_DRAFT_CANCELLAZIONE
     */
    public void writeDraft(Draft draft, OperazioneKrint.CodiceOperazione codiceOperazione) {
        try {
            // Informazioni oggetto
            KrintShpeckDraft krintPecMessage = factory.createProjection(KrintShpeckDraft.class, draft);
//            String jsonKrintPecDraft = objectMapper.writeValueAsString(krintPecMessage);
            HashMap<String, Object> krintPecDraft = objectMapper.convertValue(krintPecMessage, new TypeReference<HashMap<String, Object>>() {});
            // Informazioni oggetto contenitore
            KrintShpeckPec krintPec = factory.createProjection(KrintShpeckPec.class, draft.getIdPec());   
//            String jsonKrintPec = objectMapper.writeValueAsString(krintPec);
            HashMap<String, Object> krintPecMap = objectMapper.convertValue(krintPec, new TypeReference<HashMap<String, Object>>() {});
            krintService.writeKrintRow(
                draft.getId().toString(),
                Krint.TipoOggettoKrint.SHPECK_DRAFT,
                draft.getId().toString(),
                krintPecDraft,
                draft.getIdPec().getId().toString(),
                Krint.TipoOggettoKrint.BABORG_PEC,
                draft.getIdPec().getIndirizzo(),
                krintPecMap,
                codiceOperazione);                                                    
        } catch (Exception ex) {
            LOGGER.error("Errore nella writeDraft con messaggio" + draft.getId().toString(), ex);
            krintService.writeKrintError(draft.getId(), "writeDraft", codiceOperazione);
        }  
    }
    
    /**
     * Questa funzione si occupa di preparare il log per il salvataggio dentro outbox. Cioè l'invio di una mail
     * @param outbox
     * @param codiceOperazione: PEC_MESSAGE_INVIO_NUOVA_MAIL
     */
    public void writeOutboxMessage(Outbox outbox, OperazioneKrint.CodiceOperazione codiceOperazione) {
       try {
            // Informazioni oggetto
            KrintShpeckOutbox krintPecOutbox = factory.createProjection(KrintShpeckOutbox.class, outbox);
//            String jsonKrintPecOutbox = objectMapper.writeValueAsString(krintPecOutbox);
            HashMap<String, Object> krintPecOutboxMap = objectMapper.convertValue(krintPecOutbox, new TypeReference<HashMap<String, Object>>() {});
            // Informazioni oggetto contenitore
            KrintShpeckPec krintPec = factory.createProjection(KrintShpeckPec.class, outbox.getIdPec());   
//            String jsonKrintPec = objectMapper.writeValueAsString(krintPec);
            HashMap<String, Object> krintPecMap = objectMapper.convertValue(krintPec, new TypeReference<HashMap<String, Object>>() {});
            krintService.writeKrintRow(
                outbox.getId().toString(),
                Krint.TipoOggettoKrint.SHPECK_OUTBOX,
                outbox.getId().toString(),
                krintPecOutboxMap,
                outbox.getIdPec().getId().toString(),
                Krint.TipoOggettoKrint.BABORG_PEC,
                outbox.getIdPec().getIndirizzo(),
                krintPecMap,
                codiceOperazione);                                                    
        } catch (Exception ex) {
            LOGGER.error("Errore nella writeOutboxMessage con messaggio" + outbox.getId().toString(), ex);
            krintService.writeKrintError(outbox.getId(), "writeOutboxMessage", codiceOperazione);
        }  
    }
    
    /**
     * Questa funzione si occupa di preparare il log per quanto riguarda i Folder
     * @param folder
     * @param codiceOperazione: PEC_FOLDER_CREAZIONE - PEC_FOLDER_RINOMINA - PEC_FOLDER_ELIMINAZIONE
     */
    public void writeFolder(Folder folder, OperazioneKrint.CodiceOperazione codiceOperazione) {
       try {
            // Informazioni oggetto
            KrintShpeckFolder krintPecFolder = factory.createProjection(KrintShpeckFolder.class, folder);
//            String jsonKrintPecOutbox = objectMapper.writeValueAsString(krintPecFolder);
            HashMap<String, Object> krintPecFolderMap = objectMapper.convertValue(krintPecFolder, new TypeReference<HashMap<String, Object>>() {});
            // Informazioni oggetto contenitore
            KrintShpeckPec krintPec = factory.createProjection(KrintShpeckPec.class, folder.getIdPec());   
//            String jsonKrintPec = objectMapper.writeValueAsString(krintPec);
            HashMap<String, Object> krintPecMap = objectMapper.convertValue(krintPec, new TypeReference<HashMap<String, Object>>() {});
            krintService.writeKrintRow(
                folder.getId().toString(),
                Krint.TipoOggettoKrint.SHPECK_FOLDER,
                folder.getId().toString(),
                krintPecFolderMap,
                folder.getIdPec().getId().toString(),
                Krint.TipoOggettoKrint.BABORG_PEC,
                folder.getIdPec().getIndirizzo(),
                krintPecMap,
                codiceOperazione);                                                    
        } catch (Exception ex) {
            LOGGER.error("Errore nella writeFolder con folder" + folder.getId().toString(), ex);
            krintService.writeKrintError(folder.getId(), "writeFolder", codiceOperazione);
        }  
    }
    
    /**
     * Questa funzione si occupa di preparare il log per quanto riguarda i Tag
     * @param tag
     * @param codiceOperazione: PEC_TAG_CREAZIONE - PEC_TAG_RINOMINA - PEC_TAG_ELIMINAZIONE
     */
    public void writeTag(Tag tag, OperazioneKrint.CodiceOperazione codiceOperazione) {
       try {
            // Informazioni oggetto
            KrintShpeckTag krintPecTag = factory.createProjection(KrintShpeckTag.class, tag);
//            String jsonKrintPecOutbox = objectMapper.writeValueAsString(krintPecTag);
            HashMap<String, Object> krintPecOutboxMap = objectMapper.convertValue(krintPecTag, new TypeReference<HashMap<String, Object>>() {});
            
            // Informazioni oggetto contenitore
            KrintShpeckPec krintPec = factory.createProjection(KrintShpeckPec.class, tag.getIdPec());   
//            String jsonKrintPec = objectMapper.writeValueAsString(krintPec);
            HashMap<String, Object> krintPecMap = objectMapper.convertValue(krintPec, new TypeReference<HashMap<String, Object>>() {});
            
            krintService.writeKrintRow(
                tag.getId().toString(),
                Krint.TipoOggettoKrint.SHPECK_TAG,
                tag.getId().toString(),
                krintPecOutboxMap,
                tag.getIdPec().getId().toString(),
                Krint.TipoOggettoKrint.BABORG_PEC,
                tag.getIdPec().getIndirizzo(),
                krintPecMap,
                codiceOperazione);                                                    
        } catch (Exception ex) {
            LOGGER.error("Errore nella writeTag con tag" + tag.getId().toString(), ex);
            krintService.writeKrintError(tag.getId(), "writeTag", codiceOperazione);
        }  
    }
    
    /**
     * Questa procedura logga l'eliminazione di un messaggio dal cestino
     * @param message Il messaggio eliminato
     * @param codiceOperazione Il codice dell'operazione
     */
    public void writeDeletedFromTrash(MessageFolder messageFolder, OperazioneKrint.CodiceOperazione codiceOperazione) {
        try {
            // Informazioni oggetto
            KrintShpeckMessage krintPecMessage = factory.createProjection(KrintShpeckMessage.class, messageFolder.getIdMessage());    
            String jsonKrintPecMessage = objectMapper.writeValueAsString(krintPecMessage);
            HashMap<String, Object> krintPecMessageMap = objectMapper.convertValue(krintPecMessage, new TypeReference<HashMap<String, Object>>() {});
            // Informazioni oggetto contenitore
            KrintShpeckPec krintPec = factory.createProjection(KrintShpeckPec.class, messageFolder.getIdMessage().getIdPec());   
//            String jsonKrintPec = objectMapper.writeValueAsString(krintPec);
            HashMap<String, Object> krintPecMap = objectMapper.convertValue(krintPec, new TypeReference<HashMap<String, Object>>() {});
            krintService.writeKrintRow(
                messageFolder.getIdMessage().getId().toString(),
                Krint.TipoOggettoKrint.SHPECK_MESSAGE,
                messageFolder.getIdMessage().getId().toString(),
                krintPecMessageMap,
                messageFolder.getIdMessage().getIdPec().getId().toString(),
                Krint.TipoOggettoKrint.BABORG_PEC,
                messageFolder.getIdMessage().getIdPec().getIndirizzo(),
                krintPecMap,
                codiceOperazione);                                                    
        } catch (Exception ex) {
            LOGGER.error("Errore nella writeDeletedFromTrash con tag" + messageFolder.getId().toString(), ex);
            krintService.writeKrintError(messageFolder.getId(), "writeDeletedFromTrash", codiceOperazione);
        }  
    }
}
