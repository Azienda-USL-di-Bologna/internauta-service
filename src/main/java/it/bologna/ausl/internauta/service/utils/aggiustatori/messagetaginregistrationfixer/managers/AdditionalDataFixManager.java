/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.bologna.ausl.internauta.service.utils.aggiustatori.messagetaginregistrationfixer.managers;

import it.bologna.ausl.internauta.service.repositories.baborg.AziendaRepository;
import it.bologna.ausl.internauta.service.repositories.baborg.PersonaRepository;
import it.bologna.ausl.internauta.service.repositories.baborg.UtenteRepository;
import it.bologna.ausl.internauta.service.utils.aggiustatori.messagetaginregistrationfixer.holders.AdditionalDataOjectHolder;
import it.bologna.ausl.model.entities.baborg.Azienda;
import it.bologna.ausl.model.entities.baborg.Persona;
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
    PersonaRepository personaRepository;

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

    private JSONObject getDatiDocumentiDaPico(JSONObject aziendaObject, JSONObject documentObject) throws IOException {
        log.info("Andiamo a recuperare i dati del documento da pico...");
        JSONObject PicoDocumentDataJSONObjectResponse = null;
        Azienda azienda = aziendaRepository.findById(aziendaObject.getInt("id")).get();
        log.info("Azienda ->\t[" + azienda.getId() + "\t" + azienda.getCodice()
                + "\t" + azienda.getDescrizione()
                + "\n" + new JSONObject(azienda.getParametri()).toString(4) + "]");

        try {
            log.info("Chiamo pico e gli chiedo i dati della proposta {}", documentObject.getString("numeroProposta"));
            PicoDocumentDataJSONObjectResponse = proctonWebApiCallManager
                    .getDatiProtocollazioneDocumento(azienda, documentObject.getString("numeroProposta"));

            log.info("Dati protocollazione documento:\n"
                    + PicoDocumentDataJSONObjectResponse.toString(4));
        } catch (IOException ex) {
            log.error("ERRORE", ex.getMessage());
            throw ex;
        }
        return PicoDocumentDataJSONObjectResponse;
    }

    private boolean isDocumentoAlreadyRegisteredInPico(JSONObject documentData) {
        log.info("isDocumentoAlreadyRegisteredInPico ? {}\n", documentData.toString(4));
        boolean isAlreadyRegistered = false;
        String documentString = (String) documentData.get(AdditionalDataKey.ID_DOCUMENTO.toString());
        JSONObject document = new JSONObject(documentString);
        if (document.has(IdDocumentoKey.NUMERO_PROTOCOLLO.toString())
                && document.get(IdDocumentoKey.NUMERO_PROTOCOLLO.toString()) != null
                && (!document.get(IdDocumentoKey.NUMERO_PROTOCOLLO.toString()).equals(""))) {
            isAlreadyRegistered = true;
        }
        return isAlreadyRegistered;
    }

    public boolean isDocumentoAlreadyPresenteInRegisteredTag(JSONObject aziendaObject, MessageTag registeredMessageTag) {
        log.info("Verifico se ho già un elemento dell'azienda {} sul registeredMessageTag...", aziendaObject.get("id"));
        boolean isAlreadyProtocollato = false;
        JSONArray registeredAddData = new JSONArray(registeredMessageTag.getAdditionalData());
        log.info("JSONArray registeredAddData:\n{}", registeredAddData.toString(4));
        for (int i = 0; i < registeredAddData.length(); i++) {
            JSONObject registeredAziendaObject = new AdditionalDataOjectHolder((JSONObject) registeredAddData.get(i)).getAziendaObject();
            if (registeredAziendaObject.get("id") == aziendaObject.get("id")) {
                isAlreadyProtocollato = true;
                break;
            }
        }
        if (isAlreadyProtocollato) {
            log.info("... sì, c'è");
        } else {
            log.info("... no, non c'è");
        }
        return isAlreadyProtocollato;
    }

    private Utente getUtente(Integer idAzienda, String codiceFiscaleUtenteProtocollante) {
        log.info("Recupero i dati utente da baborg: cf {}, azienda {}", codiceFiscaleUtenteProtocollante, idAzienda);
        Persona persona = personaRepository.findByCodiceFiscale(codiceFiscaleUtenteProtocollante);
        Azienda azienda = aziendaRepository.findById(idAzienda).get();
        return utenteRepository.findByIdAziendaAndIdPersona(azienda, persona);
    }

    private JSONObject getNewRegisteredAdditionalDataJSONObject(JSONObject datiDocumentiDaPico, JSONObject aziendaObject) {
        JSONObject newRegisteredAdditionalDataJSONObject = new JSONObject();
        log.info("Recupero la descrizione dell'utente protocollante");
        String descrizioneUtenteProtocollante = (String) datiDocumentiDaPico.get("descrizioneUtenteProtocollante");
        log.info("Recupero il codice fiscale dell'utente protocollante");
        String codiceFiscaleUtenteProtocollante = (String) datiDocumentiDaPico.get("codiceFiscaleUtenteProtocollante");
        log.info("Carico l'utente by azienda e codice fiscale...");
        Utente utente = getUtente((Integer) aziendaObject.get("id"), codiceFiscaleUtenteProtocollante);
        log.info("Utente protocollante: {}  idUtente: {} ", descrizioneUtenteProtocollante, utente.getId());
        JSONObject datiUtenteJSONObject = new JSONObject();
        datiUtenteJSONObject.put("id", utente.getId());
        datiUtenteJSONObject.put("descrizione", (String) descrizioneUtenteProtocollante);

        // UTENTE
        log.info("Setto i dati dell'utente nel json...");
        newRegisteredAdditionalDataJSONObject.put(AdditionalDataKey.ID_UTENTE.toString(),
                datiUtenteJSONObject);

        //AZIENDA
        log.info("Setto i dati dell'azienda nel json...");
        newRegisteredAdditionalDataJSONObject.put(AdditionalDataKey.ID_AZIENDA.toString(),
                aziendaObject);

        //DOCUMENTO
        log.info("Setto i dati del documento nel json...");
        JSONObject jsonObjectDocumento = new JSONObject((String) datiDocumentiDaPico.get(AdditionalDataKey.ID_DOCUMENTO.toString()));
        newRegisteredAdditionalDataJSONObject.put(AdditionalDataKey.ID_DOCUMENTO.toString(),
                jsonObjectDocumento);
        return newRegisteredAdditionalDataJSONObject;
    }

    private void fixRegisteredAdditionalData(MessageTag registeredMessageTag,
            JSONObject datiDocumentiDaPico, JSONObject aziendaObject) {
        log.info("Mi creo un'oggetto dall'additionalData del REGISTERED MessageTag");
        JSONArray oldAdditionalDataJSONObject = new JSONArray(registeredMessageTag.getAdditionalData());
        log.info("oldAdditionalDataJSONObject:\n{}", oldAdditionalDataJSONObject.toString(4));
        JSONObject newRegisteredAdditionalDataJSONObject = getNewRegisteredAdditionalDataJSONObject(datiDocumentiDaPico, aziendaObject);
        log.info("Inserisco nell'array in nuovo oggetto...");
        oldAdditionalDataJSONObject.put(newRegisteredAdditionalDataJSONObject);
        registeredMessageTag.setAdditionalData(oldAdditionalDataJSONObject.toString());

    }

    public JSONArray verifyAndFixInRegistrationAdditionalData(MessageTag inRegistrationMessageTag,
            MessageTag registeredMessageTag) throws IOException {
        log.info("Verfico e fixo gli additionalData");
        log.info("MessageTag inRegistrationMessageTag {}", inRegistrationMessageTag.getId());
        log.info("MessageTag registeredMessageTag  {}", registeredMessageTag.getId());
        JSONArray inRegistrationAdditionalData = new JSONArray(inRegistrationMessageTag.getAdditionalData());
        log.info("JSONArray inRegistrationAdditionalData:\n{}", inRegistrationAdditionalData.toString(0));
        JSONArray elementiDaRimuovere = new JSONArray();
        for (int i = 0; i < inRegistrationAdditionalData.length(); i++) {
            log.info("Ciclo sull'array e mi prendo l'elemento {}", i);
            JSONObject inRegistrationAdditionalDataElement = inRegistrationAdditionalData.getJSONObject(i);
            log.info("InRegistration additionalData element:\n" + inRegistrationAdditionalDataElement.toString(4));
            AdditionalDataOjectHolder inRegistrationAdditionalDataHolder = new AdditionalDataOjectHolder(inRegistrationAdditionalDataElement);
            JSONObject aziendaObject = inRegistrationAdditionalDataHolder.getAziendaObject();
            log.info("AziendaObject:\n" + aziendaObject.toString(4));
            JSONObject documentObject = inRegistrationAdditionalDataHolder.getDocumentoObject();
            log.info("DocumentObject:\n" + documentObject.toString(4));
            if (isDocumentoAlreadyPresenteInRegisteredTag(aziendaObject, registeredMessageTag)) {
                log.info("Nel MessageTag REGISTERED è già presente il dato di protocollazione");
                log.info("Quindi è da rimuovere poi dal MessageTag IN_REGISTRATION");
                elementiDaRimuovere.put(inRegistrationAdditionalDataElement);
            } else {
                log.info("Occorre verificare se il documento è stato protocollato in Pico");
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
