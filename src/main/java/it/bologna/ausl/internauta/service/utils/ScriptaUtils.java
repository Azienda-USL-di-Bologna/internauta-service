/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.bologna.ausl.internauta.service.utils;

import it.bologna.ausl.internauta.service.repositories.baborg.PecRepository;
import it.bologna.ausl.internauta.service.repositories.shpeck.MessageRepository;
import it.bologna.ausl.model.entities.baborg.Pec;
import it.bologna.ausl.model.entities.scripta.Allegato;
import it.bologna.ausl.model.entities.scripta.Doc;
import it.bologna.ausl.model.entities.scripta.Related;
import it.bologna.ausl.model.entities.scripta.Spedizione;
import it.bologna.ausl.model.entities.shpeck.Message;
import java.util.List;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 *
 * @author Salo
 */
@Component
public class ScriptaUtils {

    @Autowired
    private MessageRepository messageRepository;
    @Autowired
    private PecRepository pecRepository;

    public Message getPecMittenteMessage(Doc doc) {
        Spedizione spedizioneMittenteFromDoc = getSpedizioneMittenteFromDoc(doc);
        Integer idMessage = spedizioneMittenteFromDoc.getIdMessage().getId();
        return messageRepository.getOne(idMessage);
    }

    public JSONObject getPecMittenteMessageJSONObjectByDoc(Doc doc) {
        JSONObject messageJsonObject = new JSONObject();
        Message message = getPecMittenteMessage(doc);
        messageJsonObject.put("idSorgentePec", message.getId());
        Related mittente = getMittentePE(doc);
        messageJsonObject.put("mittente", mittente.getDescrizione());
        messageJsonObject.put("subject", message.getSubject());
        messageJsonObject.put("dataArrivo", message.getReceiveTime());
        messageJsonObject.put("messageID", message.getUuidMessage());
        Pec pecDaCuiProtocollo = message.getIdPec();
        pecDaCuiProtocollo = pecRepository.getOne(pecDaCuiProtocollo.getId());
        messageJsonObject.put("indirizzoPecOrigine", pecDaCuiProtocollo.getIndirizzo());

        return messageJsonObject;
    }

    public Related getMittentePE(Doc doc) {
        Related mittente = null;
        List<Related> mittenti = doc.getMittenti();
        if (mittenti != null && !mittenti.isEmpty()) {
            mittente = mittenti.get(0);
        }
        return mittente;
    }

    public Spedizione getSpedizioneMittente(Related mittente) {
        Spedizione spedizioneMittente = new Spedizione();
        if (mittente != null) {
            List<Spedizione> spedizioneList = mittente.getSpedizioneList();
            if (spedizioneList != null && !spedizioneList.isEmpty()) {
                spedizioneMittente = spedizioneList.get(0);
            }
        }
        return spedizioneMittente;
    }

    public Spedizione getSpedizioneMittenteFromDoc(Doc doc) {
        Spedizione spedizioneMittente = new Spedizione();
        Related mittentePE = getMittentePE(doc);
        if (mittentePE != null) {
            spedizioneMittente = getSpedizioneMittente(mittentePE);
        }
        return spedizioneMittente;
    }

    public Allegato getAllegatoPrincipale(Doc doc) {
        Allegato allegatoDaTornare = null;
        List<Allegato> allegati = doc.getAllegati();
        for (Allegato allegato : allegati) {
            if (allegato.getPrincipale()) {
                allegatoDaTornare = allegato;
                break;
            }
        }
        return allegatoDaTornare;
    }

    public void getDettaglioAllegatoByTipo() {

    }

}
