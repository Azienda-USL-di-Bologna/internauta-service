/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.bologna.ausl.internauta.service.utils.aggiustatori.messagetaginregistrationfixer.managers;

import it.bologna.ausl.internauta.service.repositories.baborg.PecAziendaRepository;
import it.bologna.ausl.internauta.service.repositories.shpeck.MessageRepository;
import it.bologna.ausl.internauta.service.repositories.shpeck.MessageTagRepository;
import it.bologna.ausl.internauta.service.utils.aggiustatori.messagetaginregistrationfixer.factories.DataHolderFactory;
import it.bologna.ausl.internauta.service.utils.aggiustatori.messagetaginregistrationfixer.handlers.MessagesFoldersHandler;
import it.bologna.ausl.internauta.service.utils.aggiustatori.messagetaginregistrationfixer.holders.MessagesTagsProtocollazioneFixDataHolder;
import it.bologna.ausl.model.entities.baborg.Pec;
import it.bologna.ausl.model.entities.baborg.PecAzienda;
import it.bologna.ausl.model.entities.shpeck.Folder;
import it.bologna.ausl.model.entities.shpeck.Message;
import it.bologna.ausl.model.entities.shpeck.MessageTag;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
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
public class MessagesTagsProtocollazioneFixManager {

    private static final Logger log = LoggerFactory.getLogger(MessagesTagsProtocollazioneFixManager.class);

    private MessagesTagsProtocollazioneFixDataHolder dataHolder;

    @Autowired
    DataHolderFactory dataHolderFactory;

    @Autowired
    AdditionalDataFixManager additionalDataFixManager;

    @Autowired
    MessageTagRepository messageTagRepository;

    @Autowired
    PecAziendaRepository pecAziendaRepository;

    @Autowired
    MessageRepository messageRepository;

    private int getIdAziendaFromAdditionalDataEleemnt(JSONObject element) {
        JSONObject aziendaElement = element.getJSONObject("idAzienda");
        return aziendaElement.getInt("id");
    }

    private boolean isPecAziendaOwned(Integer idPec, Integer idAzienda) {
        boolean appartiene = false;
        Integer idByIdPecAndIdAzienda = pecAziendaRepository.getIdByIdPecAndIdAzienda(idPec, idAzienda);
        if (idByIdPecAndIdAzienda != null && idByIdPecAndIdAzienda != 0) {
            appartiene = true;
        }
        return appartiene;
    }

    private void spostaMessageInProtocollati(Message message) {
        Folder actualFolder = dataHolder.getActualFolder();
        if (!actualFolder.getType().toString().equals(Folder.FolderType.REGISTERED.toString())) {
            MessagesFoldersHandler messagesFoldersHandler = dataHolder.getMessagesFoldersHandler();
            messagesFoldersHandler.moveToRegisteredFolder(message);
        }
    }

    private void verificaAndSpostaMessaggioInProtocollatiSeAppartenenteAdAzienda(Message message, JSONArray fixedElements) {
        log.info("verificaAndSpostaMessaggioInProtocollatiSeAppartenenteAdAzienda....");
        for (int i = 0; i < fixedElements.length(); i++) {
            JSONObject elemento = (JSONObject) fixedElements.get(i);
            JSONObject idAziendaObject = (JSONObject) elemento.get("idAzienda");
            int idAzienda = idAziendaObject.getInt("id");
            if (isPecAziendaOwned(message.getIdPec().getId(), idAzienda)) {
                spostaMessageInProtocollati(message);
                return;
            }
        }
    }

    private JSONArray getAdditionalDataRemovingElementiInutili(MessageTag inRegistrationMessagesTag, JSONArray elementiDaRimuovere) {
        JSONArray additionalData = new JSONArray(inRegistrationMessagesTag.getAdditionalData());
        for (int i = 0; i < additionalData.length(); i++) {
            JSONObject originalElement = additionalData.getJSONObject(i);
            int idAziendaElementoOriginale = getIdAziendaFromAdditionalDataEleemnt(originalElement);
            for (int k = 0; k < elementiDaRimuovere.length(); k++) {
                JSONObject elementToDelete = elementiDaRimuovere.getJSONObject(k);
                int idAziendaElementoDaCancellare = getIdAziendaFromAdditionalDataEleemnt(elementToDelete);
                if (idAziendaElementoOriginale == idAziendaElementoDaCancellare) {
                    additionalData.remove(i);
                }

            }
        }
        return additionalData;
    }

    public JSONArray fixDatiProtocollazioneMessaggio(Message message) throws IOException {
        log.debug("Entrato in fixDatiProtocollazioneMessaggio(" + message.getId() + ")");
        dataHolder = dataHolderFactory.createNewMessagesTagsProctocollazioneFixDataHolder(message);
        JSONArray fixedElements = null;
        MessageTag inRegistrationMessagesTag = dataHolder.getInRegistrationMessagesTag();
        if (inRegistrationMessagesTag == null) {
            log.info("Beh, NON ESISTE un tag IN_REGISTRATION per il messaggio " + message.getId());
            return fixedElements;
        }
        MessageTag registeredTag = dataHolder.getRegisteredTag();
        fixedElements = additionalDataFixManager.verifyAndFixInRegistrationAdditionalData(inRegistrationMessagesTag, registeredTag);
        if (fixedElements.length() > 0) {
            log.info("Andiamo ad aggiustare gli additional_data dei vari MessageTag...");
            JSONArray purifiedAdditionalData = getAdditionalDataRemovingElementiInutili(inRegistrationMessagesTag, fixedElements);
            log.info("Ora additionalData di MT InRegistration è\n{} ", purifiedAdditionalData.toString(4));
            inRegistrationMessagesTag.setAdditionalData(purifiedAdditionalData.toString());
            if (purifiedAdditionalData.length() == 0) {
                log.info("AdditionalData di MT InRegistration è vuoto, quindi va cancellato");
                messageTagRepository.delete(inRegistrationMessagesTag);
            } else {
                log.info("Aggiorno MT InRegistration {}...", inRegistrationMessagesTag.getId());
                inRegistrationMessagesTag = messageTagRepository.save(inRegistrationMessagesTag);
            }
            if (registeredTag.getAdditionalData() != null && !registeredTag.getAdditionalData().equals("")) {
                log.info("Salvo MT Registered...");
                registeredTag = messageTagRepository.save(registeredTag);
            }

            // decidiamo di spostare la pec in protocollati
            verificaAndSpostaMessaggioInProtocollatiSeAppartenenteAdAzienda(message, fixedElements);

            // ORA SI SISTEMANO LE PEC CON UUID_MESSAGE UGUALE
            geminiMailFixing(message, fixedElements);

        } else {
            log.info("Sembra tutto normale... Quindi niente");
        }
        return fixedElements;
    }

    public List<Message> getGeminiMails(Message message) {
        List<Message> geminiMails = new ArrayList<>();
        List<Message> similarMessages = messageRepository.findByUuidMessage(message.getUuidMessage());
        for (Message similarMessage : similarMessages) {
            if (!similarMessage.getId().equals(message.getId())
                    && similarMessage.getMessageType().toString().equals(Message.MessageType.MAIL.toString())) {
                log.info("Trovato mail gemella {}", similarMessage.getId());
                geminiMails.add(similarMessage);
            }
        }
        return geminiMails;
    }

    private int getMessageTagAdditionalDataArraySize(MessageTag messageTag) {
        JSONArray jsonArray = new JSONArray((String) messageTag.getAdditionalData());
        return jsonArray.length();
    }

    private void verifyAndFixRegisteredMessageTagWithVerifiedElements(MessageTag registeredTag, JSONArray verifiedElements) {
        JSONArray registererAdditionalData = new JSONArray((String) registeredTag.getAdditionalData());
        for (int i = 0; i < verifiedElements.length(); i++) {
            JSONObject additionalDataVerifiedElement = (JSONObject) verifiedElements.get(i);
            if (!additionalDataFixManager.isDocumentoAlreadyPresenteInRegisteredTag(additionalDataVerifiedElement.getJSONObject("idAzienda"), registeredTag)) {
                log.info("Devo aggiungere l'elemento al messageTag");
                registererAdditionalData.put(additionalDataVerifiedElement);
            }
        }
        registeredTag.setAdditionalData(registererAdditionalData.toString());
    }

    private void geminiMailFixing(Message message, JSONArray verifiedElements) {

        List<Message> geminiMails = getGeminiMails(message);
        if (geminiMails.size() > 0) {
            log.info("Mettiamo a posto i messaggi gemelli...");
            for (Message geminiMail : geminiMails) {
                log.info("Vediamo di fissare questo: id = {}", geminiMail.getId());

                dataHolder = dataHolderFactory.createNewMessagesTagsProctocollazioneFixDataHolder(geminiMail);

                MessageTag registeredTag = dataHolder.getRegisteredTag();

                verifyAndFixRegisteredMessageTagWithVerifiedElements(registeredTag, verifiedElements);

                MessageTag inRegistrationMessageTag = dataHolder.getInRegistrationMessagesTag();

                if (inRegistrationMessageTag != null) {
                    JSONArray purifiedAdditionalData = getAdditionalDataRemovingElementiInutili(inRegistrationMessageTag, verifiedElements);
                    inRegistrationMessageTag.setAdditionalData(purifiedAdditionalData.toString());
                    if (getMessageTagAdditionalDataArraySize(inRegistrationMessageTag) > 0) {
                        log.info("Salvo i dati aggiornati di InRegistration MessageTag");
                        inRegistrationMessageTag = messageTagRepository.save(inRegistrationMessageTag);
                    } else {
                        log.info("InRegistration MessageTag {} è vuoto, quindi lo cancello", inRegistrationMessageTag.getId());
                    }
                }
                if (getMessageTagAdditionalDataArraySize(registeredTag) > 0) {
                    log.info("Salvo i dati aggiornati di Registered MessageTag");
                    registeredTag = messageTagRepository.save(registeredTag);
                }
                verificaAndSpostaMessaggioInProtocollatiSeAppartenenteAdAzienda(message, verifiedElements);
            }
        } else {
            log.info("Nessun altro messaggio da sistemare.");
        }
    }
}
