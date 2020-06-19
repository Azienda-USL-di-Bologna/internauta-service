/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.bologna.ausl.internauta.service.utils.aggiustatori.messagetaginregistrationfixer.managers;

import it.bologna.ausl.internauta.service.repositories.baborg.AziendaRepository;
import it.bologna.ausl.internauta.service.repositories.baborg.UtenteRepository;
import it.bologna.ausl.internauta.service.utils.aggiustatori.messagetaginregistrationfixer.holders.AdditionalDataOjectHolder;
import it.bologna.ausl.model.entities.baborg.Azienda;
import it.bologna.ausl.model.entities.baborg.Utente;
import it.bologna.ausl.model.entities.shpeck.MessageTag;
import java.io.IOException;
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
    UtenteRepository utenteRepository;

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

    private static enum IdDocumentoKey {

        ANNO {
            public String toString() {
                return "anno";
            }
        },
        CODICE_REGISTRO {
            public String toString() {
                return "codiceRegistro";
            }
        },
        DATA_PROTOCOLLO {
            public String toString() {
                return "dataProtocollo";
            }
        },
        OGGETTO {
            public String toString() {
                return "oggetto";
            }
        },
        NUMERO_PROPOSTA {
            public String toString() {
                return "numeroProposta";
            }
        },
        NUMERO_PROTOCOLLO {
            public String toString() {
                return "numeroProtocollo";
            }
        },
    }

    private static final Logger log = LoggerFactory.getLogger(AdditionalDataFixManager.class);

    private JSONObject getDatiDocumentiDaPico(JSONObject aziendaObject, JSONObject documentObject) {
        JSONObject PicoDocumentDataJSONObjectResponse = null;
        Azienda azienda = aziendaRepository.findById(aziendaObject.getInt("id")).get();
        log.info("Azienda ->\t[" + azienda.getId() + "\t" + azienda.getCodice()
                + "\t" + azienda.getDescrizione()
                + "\n" + new JSONObject(azienda.getParametri()).toString(4) + "]");

        try {
            PicoDocumentDataJSONObjectResponse = proctonWebApiCallManager
                    .getDatiProtocollazioneDocumento(azienda, documentObject.getString("numeroProposta"));

            log.info("Dati protocollazione documento:\n"
                    + PicoDocumentDataJSONObjectResponse.toString(4));
        } catch (IOException ex) {
            log.error("ERRORE", ex.getMessage());
        }
        return PicoDocumentDataJSONObjectResponse;
    }

    private boolean isDocumentoAlreadyRegisteredInPico(JSONObject documentData) {
        boolean isAlreadyRegistered = false;
        JSONObject document = new JSONObject((String) documentData.get(AdditionalDataKey.ID_DOCUMENTO.toString()));
        if (document.has(IdDocumentoKey.NUMERO_PROTOCOLLO.toString())
                && document.get(IdDocumentoKey.NUMERO_PROTOCOLLO.toString()) != null
                && (!document.get(IdDocumentoKey.NUMERO_PROTOCOLLO.toString()).equals(""))) {
            isAlreadyRegistered = true;
        }
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

    private Utente getUtente(Integer idAzienda, String descrizioneUtente) {
        return utenteRepository.getIdUtenteByIdAziendaAndPersonaDescrizione(idAzienda, descrizioneUtente);
    }

    private JSONObject getNewRegisteredAdditionalDataJSONObject(JSONObject datiDocumentiDaPico, JSONObject aziendaObject) {
        JSONObject newRegisteredAdditionalDataJSONObject = new JSONObject();

        String descrizioneUtenteProtocollante = (String) datiDocumentiDaPico.get("descrizioneUtenteProtocollante");
        Utente utente = getUtente((Integer) aziendaObject.get("id"), descrizioneUtenteProtocollante);
        log.info("Utente protocollante: {}  idUtnte {} ", descrizioneUtenteProtocollante, utente.getId());
        JSONObject datiUtenteJSONObject = new JSONObject();
        datiUtenteJSONObject.put("id", utente.getId());
        datiUtenteJSONObject.put("descrizione", (String) descrizioneUtenteProtocollante);

        // UTENTE
        newRegisteredAdditionalDataJSONObject.put(AdditionalDataKey.ID_UTENTE.toString(),
                datiUtenteJSONObject);

        //AZIENDA
        newRegisteredAdditionalDataJSONObject.put(AdditionalDataKey.ID_AZIENDA.toString(),
                aziendaObject);

        //DOCUMENTO
        newRegisteredAdditionalDataJSONObject.put(AdditionalDataKey.ID_DOCUMENTO.toString(),
                datiDocumentiDaPico.get(AdditionalDataKey.ID_DOCUMENTO.toString()));
        return newRegisteredAdditionalDataJSONObject;
    }

    private void fixRegisteredAdditionalData(MessageTag registeredMessageTag,
            JSONObject datiDocumentiDaPico, JSONObject aziendaObject) {

        JSONArray oldAdditionalDataJSONObject = new JSONArray(registeredMessageTag.getAdditionalData());
        JSONObject newRegisteredAdditionalDataJSONObject = getNewRegisteredAdditionalDataJSONObject(datiDocumentiDaPico, aziendaObject);
        oldAdditionalDataJSONObject.put(newRegisteredAdditionalDataJSONObject);
        registeredMessageTag.setAdditionalData(oldAdditionalDataJSONObject.toString());

    }

    public JSONArray verifyAndFixInRegistrationAdditionalData(MessageTag inRegistrationMessageTag, MessageTag registeredMessageTag) {
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
                JSONObject datiDocumentiDaPico = getDatiDocumentiDaPico(aziendaObject, documentObject);
                boolean eraProtocollato = isDocumentoAlreadyRegisteredInPico(datiDocumentiDaPico);
                if (!eraProtocollato) {
                    String numeroProposta = documentObject.getString("numeroProposta");
                    log.info("La proposta {} non è ancora protocollata.", numeroProposta);
                } else {
                    fixRegisteredAdditionalData(registeredMessageTag, datiDocumentiDaPico, aziendaObject);
                    log.info("Ho sistemato gli additional data del MessageTag REGISTERED\n{}", registeredMessageTag.getAdditionalData());
                    elementiDaRimuovere.put(inRegistrationAdditionalDataElement);
                }
            }
        }
        log.info("Elementi da eliminare da tag InRegistration -> " + elementiDaRimuovere.toString(4));
        return elementiDaRimuovere;
    }
}
