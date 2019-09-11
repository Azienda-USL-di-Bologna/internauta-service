
package it.bologna.ausl.internauta.service.krint;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.bologna.ausl.model.entities.configuration.Applicazione;
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
import it.bologna.ausl.model.entities.shpeck.Outbox;
import it.bologna.ausl.model.entities.shpeck.Tag;
import java.util.HashMap;
import java.util.Map;
import org.jose4j.json.internal.json_simple.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.projection.ProjectionFactory;
import org.springframework.stereotype.Service;

/**
 *
 * @author gusgus
 */
@Service
public class KrintShpeckService {
    
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
            Map<String, KrintShpeckMessage> map = new HashMap();
            map.put("idMessage", krintPecMessage);
            map.put("idMessageCorrelated", coorelatedkrintPecMessage);
            String jsonKrintPecMessage = objectMapper.writeValueAsString(map);
            
            // Informazioni oggetto contenitore
            KrintShpeckPec krintPec = factory.createProjection(KrintShpeckPec.class, message.getIdPec());
            String jsonKrintPec = objectMapper.writeValueAsString(krintPec);
            
            krintService.writeKrintRow(
                message.getId().toString(),
                Krint.TipoOggettoKrint.SHPECK_MESSAGE,
                message.getId().toString(),
                jsonKrintPecMessage,
                message.getIdPec().getId().toString(),
                Krint.TipoOggettoKrint.BABORG_PEC,
                message.getIdPec().getIndirizzo(),
                jsonKrintPec,
                codiceOperazione);
        } catch (Exception ex) {
            Integer idOggetto = null;
            try {
                idOggetto = message.getId();
            } catch (Exception exa) {}
            krintService.writeKrintError(idOggetto, "writeReaddress", codiceOperazione);
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
            String jsonKrintPecMessage = objectMapper.writeValueAsString(krintPecMessage);
            
            // Informazioni oggetto contenitore
            KrintShpeckPec krintPec = factory.createProjection(KrintShpeckPec.class, message.getIdPec());
            String jsonKrintPec = objectMapper.writeValueAsString(krintPec);
            
            krintService.writeKrintRow(
                message.getId().toString(),
                Krint.TipoOggettoKrint.SHPECK_MESSAGE,
                message.getId().toString(),
                jsonKrintPecMessage,
                message.getIdPec().getId().toString(),
                Krint.TipoOggettoKrint.BABORG_PEC,
                message.getIdPec().getIndirizzo(),
                jsonKrintPec,
                codiceOperazione);
        } catch (Exception ex) {
            Integer idOggetto = null;
            try {
                idOggetto = message.getId();
            } catch (Exception exa) {}
            krintService.writeKrintError(idOggetto, "writeRegistration", codiceOperazione);
        }
    }
    
    /**
     * Questa funzione si occupa di preparare il log per la fascicolazione
     * @param message
     * @param codiceOperazione: PEC_MESSAGE_FASCICOLAZIONE
     * @param jsonAdditionalData
     */
    public void writeArchiviation(Message message, OperazioneKrint.CodiceOperazione codiceOperazione, JSONObject jsonAdditionalData) {
        try {
            // Informazioni oggetto
            KrintShpeckMessage krintPecMessage = factory.createProjection(KrintShpeckMessage.class, message);
            Map<String, Object> map = new HashMap();
            map.put("idMessage", krintPecMessage);
            map.put("tagAdditionalData", jsonAdditionalData);
            String jsonKrintPecMessage = objectMapper.writeValueAsString(map);
            
            // Informazioni oggetto contenitore
            KrintShpeckPec krintPec = factory.createProjection(KrintShpeckPec.class, message.getIdPec());
            String jsonKrintPec = objectMapper.writeValueAsString(krintPec);
            
            krintService.writeKrintRow(
                message.getId().toString(),
                Krint.TipoOggettoKrint.SHPECK_MESSAGE,
                message.getId().toString(),
                jsonKrintPecMessage,
                message.getIdPec().getId().toString(),
                Krint.TipoOggettoKrint.BABORG_PEC,
                message.getIdPec().getIndirizzo(),
                jsonKrintPec,
                codiceOperazione);
        } catch (Exception ex) {
            Integer idOggetto = null;
            try {
                idOggetto = message.getId();
            } catch (Exception exa) {}
            krintService.writeKrintError(idOggetto, "writeArchiviation", codiceOperazione);
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
            String jsonKrintPecMessage = objectMapper.writeValueAsString(krintPecMessage);
            
            // Informazioni oggetto contenitore
            KrintShpeckPec krintPec = factory.createProjection(KrintShpeckPec.class, message.getIdPec());   
            String jsonKrintPec = objectMapper.writeValueAsString(krintPec);

            krintService.writeKrintRow(
                message.getId().toString(),
                Krint.TipoOggettoKrint.SHPECK_MESSAGE,
                message.getId().toString(),
                jsonKrintPecMessage,
                message.getIdPec().getId().toString(),
                Krint.TipoOggettoKrint.BABORG_PEC,
                message.getIdPec().getIndirizzo(),
                jsonKrintPec,
                codiceOperazione);                                                    
        } catch (Exception ex) {
            Integer idOggetto = null;
            try {
                idOggetto = message.getId();
            } catch (Exception exa) {}
            krintService.writeKrintError(idOggetto, "writeSeenOrNotSeen", codiceOperazione);
        }  
    }
    
    /**
     * Questa funzione si occupa di preparare il log per lo spostamento di un message tra folder
     * @param message
     * @param codiceOperazione: PEC_MESSAGE_SPOSTAMENTO
     * @param folder 
     */
    public void writeFolderChanged(Message message, OperazioneKrint.CodiceOperazione codiceOperazione, Folder folder, Folder idPreviousFolder) {
        try {
            // Informazioni oggetto
            KrintShpeckMessage krintPecMessage = factory.createProjection(KrintShpeckMessage.class, message);
            KrintShpeckFolder krintPecFolder = factory.createProjection(KrintShpeckFolder.class, folder);
            KrintShpeckFolder krintPecPreviousFolder = factory.createProjection(KrintShpeckFolder.class, idPreviousFolder);    
            Map<String, Object> map = new HashMap();
            map.put("idMessage", krintPecMessage);
            map.put("idFolder", krintPecFolder);
            map.put("idPreviousFolder", krintPecPreviousFolder);
            String jsonKrintPecMessage = objectMapper.writeValueAsString(map);
            
            // Informazioni oggetto contenitore
            KrintShpeckPec krintPec = factory.createProjection(KrintShpeckPec.class, message.getIdPec());   
            String jsonKrintPec = objectMapper.writeValueAsString(krintPec);

            krintService.writeKrintRow(
                message.getId().toString(),
                Krint.TipoOggettoKrint.SHPECK_MESSAGE,
                message.getId().toString(),
                jsonKrintPecMessage,
                message.getIdPec().getId().toString(),
                Krint.TipoOggettoKrint.BABORG_PEC,
                message.getIdPec().getIndirizzo(),
                jsonKrintPec,
                codiceOperazione);                                                    
        } catch (Exception ex) {
            Integer idOggetto = null;
            try {
                idOggetto = message.getId();
            } catch (Exception exa) {}
            krintService.writeKrintError(idOggetto, "writeFolderChanged", codiceOperazione);
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
            String jsonKrintPecMessage = objectMapper.writeValueAsString(krintPecMessage);
            
            // Informazioni oggetto contenitore
            KrintShpeckPec krintPec = factory.createProjection(KrintShpeckPec.class, message.getIdPec());   
            String jsonKrintPec = objectMapper.writeValueAsString(krintPec);

            krintService.writeKrintRow(
                message.getId().toString(),
                Krint.TipoOggettoKrint.SHPECK_MESSAGE,
                message.getId().toString(),
                jsonKrintPecMessage,
                message.getIdPec().getId().toString(),
                Krint.TipoOggettoKrint.BABORG_PEC,
                message.getIdPec().getIndirizzo(),
                jsonKrintPec,
                codiceOperazione);                                                    
        } catch (Exception ex) {
            Integer idOggetto = null;
            try {
                idOggetto = message.getId();
            } catch (Exception exa) {}
            krintService.writeKrintError(idOggetto, "writeReplyToMessage", codiceOperazione);
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
            Map<String, Object> map = new HashMap();
            map.put("idMessage", krintPecMessage);
            map.put("idTag", krintPecTag);
            String jsonKrintPecMessage = objectMapper.writeValueAsString(map);
            
            // Informazioni oggetto contenitore
            KrintShpeckPec krintPec = factory.createProjection(KrintShpeckPec.class, message.getIdPec());   
            String jsonKrintPec = objectMapper.writeValueAsString(krintPec);

            krintService.writeKrintRow(
                message.getId().toString(),
                Krint.TipoOggettoKrint.SHPECK_MESSAGE,
                message.getId().toString(),
                jsonKrintPecMessage,
                message.getIdPec().getId().toString(),
                Krint.TipoOggettoKrint.BABORG_PEC,
                message.getIdPec().getIndirizzo(),
                jsonKrintPec,
                codiceOperazione);                                                    
        } catch (Exception ex) {
            Integer idOggetto = null;
            try {
                idOggetto = message.getId();
            } catch (Exception exa) {}
            krintService.writeKrintError(idOggetto, "writeMessageTag", codiceOperazione);
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
            String jsonKrintPecDraft = objectMapper.writeValueAsString(krintPecMessage);
            
            // Informazioni oggetto contenitore
            KrintShpeckPec krintPec = factory.createProjection(KrintShpeckPec.class, draft.getIdPec());   
            String jsonKrintPec = objectMapper.writeValueAsString(krintPec);

            krintService.writeKrintRow(
                draft.getId().toString(),
                Krint.TipoOggettoKrint.SHPECK_DRAFT,
                draft.getId().toString(),
                jsonKrintPecDraft,
                draft.getIdPec().getId().toString(),
                Krint.TipoOggettoKrint.BABORG_PEC,
                draft.getIdPec().getIndirizzo(),
                jsonKrintPec,
                codiceOperazione);                                                    
        } catch (Exception ex) {
            Integer idOggetto = null;
            try {
                idOggetto = draft.getId();
            } catch (Exception exa) {}
            krintService.writeKrintError(idOggetto, "writeDraft", codiceOperazione);
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
            String jsonKrintPecOutbox = objectMapper.writeValueAsString(krintPecOutbox);
            
            // Informazioni oggetto contenitore
            KrintShpeckPec krintPec = factory.createProjection(KrintShpeckPec.class, outbox.getIdPec());   
            String jsonKrintPec = objectMapper.writeValueAsString(krintPec);

            krintService.writeKrintRow(
                outbox.getId().toString(),
                Krint.TipoOggettoKrint.SHPECK_OUTBOX,
                outbox.getId().toString(),
                jsonKrintPecOutbox,
                outbox.getIdPec().getId().toString(),
                Krint.TipoOggettoKrint.BABORG_PEC,
                outbox.getIdPec().getIndirizzo(),
                jsonKrintPec,
                codiceOperazione);                                                    
        } catch (Exception ex) {
            Integer idOggetto = null;
            try {
                idOggetto = outbox.getId();
            } catch (Exception exa) {}
            krintService.writeKrintError(idOggetto, "writeOutboxMessage", codiceOperazione);
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
            String jsonKrintPecOutbox = objectMapper.writeValueAsString(krintPecFolder);
            
            // Informazioni oggetto contenitore
            KrintShpeckPec krintPec = factory.createProjection(KrintShpeckPec.class, folder.getIdPec());   
            String jsonKrintPec = objectMapper.writeValueAsString(krintPec);

            krintService.writeKrintRow(
                folder.getId().toString(),
                Krint.TipoOggettoKrint.SHPECK_FOLDER,
                folder.getId().toString(),
                jsonKrintPecOutbox,
                folder.getIdPec().getId().toString(),
                Krint.TipoOggettoKrint.BABORG_PEC,
                folder.getIdPec().getIndirizzo(),
                jsonKrintPec,
                codiceOperazione);                                                    
        } catch (Exception ex) {
            Integer idOggetto = null;
            try {
                idOggetto = folder.getId();
            } catch (Exception exa) {}
            krintService.writeKrintError(idOggetto, "writeFolder", codiceOperazione);
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
            String jsonKrintPecOutbox = objectMapper.writeValueAsString(krintPecTag);
            
            // Informazioni oggetto contenitore
            KrintShpeckPec krintPec = factory.createProjection(KrintShpeckPec.class, tag.getIdPec());   
            String jsonKrintPec = objectMapper.writeValueAsString(krintPec);

            krintService.writeKrintRow(
                tag.getId().toString(),
                Krint.TipoOggettoKrint.SHPECK_TAG,
                tag.getId().toString(),
                jsonKrintPecOutbox,
                tag.getIdPec().getId().toString(),
                Krint.TipoOggettoKrint.BABORG_PEC,
                tag.getIdPec().getIndirizzo(),
                jsonKrintPec,
                codiceOperazione);                                                    
        } catch (Exception ex) {
            Integer idOggetto = null;
            try {
                idOggetto = tag.getId();
            } catch (Exception exa) {}
            krintService.writeKrintError(idOggetto, "writeTag", codiceOperazione);
        }  
    }
}
