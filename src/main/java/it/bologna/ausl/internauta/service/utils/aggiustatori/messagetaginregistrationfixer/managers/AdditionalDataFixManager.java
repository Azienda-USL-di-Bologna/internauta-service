/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.bologna.ausl.internauta.service.utils.aggiustatori.messagetaginregistrationfixer.managers;

import it.bologna.ausl.internauta.service.repositories.baborg.AziendaRepository;
import it.bologna.ausl.internauta.service.utils.aggiustatori.messagetaginregistrationfixer.holders.AdditionalDataOjectHolder;
import it.bologna.ausl.model.entities.baborg.Azienda;
import it.bologna.ausl.model.entities.shpeck.MessageTag;
import java.io.IOException;
import java.util.logging.Level;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 *
 * @author Salo
 */
@Component
public class AdditionalDataFixManager {
    
    @Autowired
    AziendaRepository aziendaRepository;
    
    @Autowired
    ProctonWebApiCallManager proctonWebApiCallManager;
    
    private static enum AdditionalDataKey {
        
        ID_UTENTE {
            public String toString() {
                return "idUtente";
            }
        },
        ID_AZIENDA {
            public String toString() {
                return "idAzienda";
            }
        },
        ID_DOCUMENTO {
            public String toString() {
                return "idDocumento";
            }
        },
    }
    
    private static final Logger log = LoggerFactory.getLogger(AdditionalDataFixManager.class);
    
    private void getDatiDocumentiDaPico(JSONObject aziendaObject, JSONObject documentObject) {
        Azienda azienda = aziendaRepository.findById(aziendaObject.getInt("id")).get();
        log.info("Azienda ->\t[" + azienda.getId() + "\t" + azienda.getCodice()
                + "\t" + azienda.getDescrizione()
                + "\n" + new JSONObject(azienda.getParametri()).toString(4) + "]");
        
        try {
            proctonWebApiCallManager.getDatiProtocollazioneDocumento(azienda, documentObject.getString("numeroProposta"));
        } catch (IOException ex) {
            log.error("ERRORE", ex.getMessage());
        }
    }
    
    private boolean isDocumentoAlreadyRegisteredInPico(JSONObject aziendaObject, JSONObject documentObject) {
        boolean isAlreadyRegistered = false;
        return isAlreadyRegistered;
    }
    
    private boolean isDocumentoAlreadyPresenteInRegisteredTag(JSONObject aziendaObject, MessageTag registeredMessageTag) {
        boolean isAlreadyProtocollato = false;
        JSONArray registeredAddData = new JSONArray(registeredMessageTag.getAdditionalData());
        for (int i = 0; i < registeredAddData.length(); i++) {
            JSONObject registeredAziendaObject = new AdditionalDataOjectHolder((JSONObject) registeredAddData.get(i)).getAziendaObject();
            if (registeredAziendaObject.get("id") == aziendaObject.get("id")) {
                isAlreadyProtocollato = true;
                break;
            }
        }
        return isAlreadyProtocollato;
    }
    
    public void fixInRegistrationAdditionalData(MessageTag inRegistrationMessageTag, MessageTag registeredMessageTag) {
        JSONArray inRegistrationAdditionalData = new JSONArray(inRegistrationMessageTag.getAdditionalData());
        
        JSONArray elementiDaRimuovere = new JSONArray();
        for (int i = 0; i < inRegistrationAdditionalData.length(); i++) {
            JSONObject inRegistrationAdditionalDataElement = inRegistrationAdditionalData.getJSONObject(i);
            AdditionalDataOjectHolder inRegistrationAdditionalDataHolder = new AdditionalDataOjectHolder(inRegistrationAdditionalDataElement);
            JSONObject aziendaObject = inRegistrationAdditionalDataHolder.getAziendaObject();
            log.info("AziendaObject:\n" + aziendaObject.toString(4));
            JSONObject documentObject = inRegistrationAdditionalDataHolder.getDocumentoObject();
            log.info("DocumentObject:\n" + documentObject.toString(4));
            if (isDocumentoAlreadyPresenteInRegisteredTag(aziendaObject, registeredMessageTag)) {
                log.info("Nel MessageTag REGISTERED è già presente il dato di protocollazione");
                elementiDaRimuovere.put(inRegistrationAdditionalDataElement);
            } else {
                log.info("Occorre verificare se il documento è stato protocollato");
                getDatiDocumentiDaPico(aziendaObject, documentObject);
                //boolean eraProtocollato = isDocumentoAlreadyRegisteredInPico(aziendaObject, documentObject);
            }
        }
        log.info("Elementi da eliminare -> " + elementiDaRimuovere.toString(4));
    }
}
