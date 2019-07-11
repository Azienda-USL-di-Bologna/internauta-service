
package it.bologna.ausl.internauta.service.krint;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.bologna.ausl.model.entities.configuration.Applicazione;
import it.bologna.ausl.model.entities.logs.Krint;
import it.bologna.ausl.model.entities.logs.OperazioneKrint;
import it.bologna.ausl.model.entities.logs.projections.KrintShpeckFolder;
import it.bologna.ausl.model.entities.logs.projections.KrintShpeckMessage;
import it.bologna.ausl.model.entities.logs.projections.KrintShpeckPec;
import it.bologna.ausl.model.entities.shpeck.Folder;
import it.bologna.ausl.model.entities.shpeck.Message;
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
     * Questa funzione si occupa di preparare il log per il reindirizzamento.Che sia esso IN oppure OUT
     * @param message è il messaggio che subisce l'operazione
     * @param correlatedMessage è il messaggio collegato all'operazione
     * @param codiceOperazione il tipo di operazione PEC_MESSAGE_REINDIRIZZAMENTO_IN o PEC_MESSAGE_REINDIRIZZAMENTO_OUT
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
                Applicazione.Applicazioni.shpeck,
                message.getId().toString(),
                Krint.TipoOggettoKrint.PEC_MESSAGE,
                message.getId().toString(),
                jsonKrintPecMessage,
                message.getIdPec().getId().toString(),
                Krint.TipoOggettoKrint.PEC,
                message.getIdPec().getIndirizzo(),
                jsonKrintPec,
                codiceOperazione);
        } catch (Exception ex) {
            //TODO: loggare errore
        }
    }
    
    /**
     * 
     * @param message
     * @param codiceOperazione 
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
                Applicazione.Applicazioni.shpeck,
                message.getId().toString(),
                Krint.TipoOggettoKrint.PEC_MESSAGE,
                message.getId().toString(),
                jsonKrintPecMessage,
                message.getIdPec().getId().toString(),
                Krint.TipoOggettoKrint.PEC,
                message.getIdPec().getIndirizzo(),
                jsonKrintPec,
                codiceOperazione);
        } catch (Exception ex) {
            //TODO: loggare errore
        }
    }
    
    /**
     * 
     * @param message
     * @param codiceOperazione
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
                Applicazione.Applicazioni.shpeck,
                message.getId().toString(),
                Krint.TipoOggettoKrint.PEC_MESSAGE,
                message.getId().toString(),
                jsonKrintPecMessage,
                message.getIdPec().getId().toString(),
                Krint.TipoOggettoKrint.PEC,
                message.getIdPec().getIndirizzo(),
                jsonKrintPec,
                codiceOperazione);
        } catch (Exception ex) {
            //TODO: loggare errore
        }
    }
    
    /**
     * 
     * @param message
     * @param codiceOperazione 
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
                Applicazione.Applicazioni.shpeck,
                message.getId().toString(),
                Krint.TipoOggettoKrint.PEC_MESSAGE,
                message.getId().toString(),
                jsonKrintPecMessage,
                message.getIdPec().getId().toString(),
                Krint.TipoOggettoKrint.PEC,
                message.getIdPec().getIndirizzo(),
                jsonKrintPec,
                codiceOperazione);                                                    

        } catch (Exception ex) {
            //TODO: loggare errore
        }  
    }
    
    /**
     * 
     * @param message
     * @param codiceOperazione
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
                Applicazione.Applicazioni.shpeck,
                message.getId().toString(),
                Krint.TipoOggettoKrint.PEC_MESSAGE,
                message.getId().toString(),
                jsonKrintPecMessage,
                message.getIdPec().getId().toString(),
                Krint.TipoOggettoKrint.PEC,
                message.getIdPec().getIndirizzo(),
                jsonKrintPec,
                codiceOperazione);                                                    

        } catch (Exception ex) {
            //TODO: loggare errore
        }  
    }
    
            
    public void writeReplyToMessage(Message message, OperazioneKrint.CodiceOperazione codiceOperazione) {
        try {
            // Informazioni oggetto
            KrintShpeckMessage krintPecMessage = factory.createProjection(KrintShpeckMessage.class, message);
            String jsonKrintPecMessage = objectMapper.writeValueAsString(krintPecMessage);
            
            // Informazioni oggetto contenitore
            KrintShpeckPec krintPec = factory.createProjection(KrintShpeckPec.class, message.getIdPec());   
            String jsonKrintPec = objectMapper.writeValueAsString(krintPec);

            krintService.writeKrintRow(
                Applicazione.Applicazioni.shpeck,
                message.getId().toString(),
                Krint.TipoOggettoKrint.PEC_MESSAGE,
                message.getId().toString(),
                jsonKrintPecMessage,
                message.getIdPec().getId().toString(),
                Krint.TipoOggettoKrint.PEC,
                message.getIdPec().getIndirizzo(),
                jsonKrintPec,
                codiceOperazione);                                                    

        } catch (Exception ex) {
            //TODO: loggare errore
        }  
    }
}
