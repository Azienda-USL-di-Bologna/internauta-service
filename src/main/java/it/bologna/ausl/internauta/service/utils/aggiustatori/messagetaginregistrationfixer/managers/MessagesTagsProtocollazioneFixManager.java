/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.bologna.ausl.internauta.service.utils.aggiustatori.messagetaginregistrationfixer.managers;

import it.bologna.ausl.internauta.service.repositories.shpeck.MessageTagRepository;
import it.bologna.ausl.internauta.service.utils.aggiustatori.messagetaginregistrationfixer.factories.DataHolderFactory;
import it.bologna.ausl.internauta.service.utils.aggiustatori.messagetaginregistrationfixer.holders.MessagesTagsProtocollazioneFixDataHolder;
import it.bologna.ausl.model.entities.shpeck.Message;
import it.bologna.ausl.model.entities.shpeck.MessageTag;
import java.util.Iterator;
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

    private int getIdAziendaFromAdditionalDataEleemnt(JSONObject element) {
        JSONObject aziendaElement = element.getJSONObject("idAzienda");
        return aziendaElement.getInt("id");
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

    public void fixDatiProtocollazioneMessaggio(Message message) {
        log.debug("Entrato in fixDatiProtocollazioneMessaggio(" + message.getId() + ")");
        dataHolder = dataHolderFactory.createNewMessagesTagsProctocollazioneFixDataHolder(message);

        MessageTag inRegistrationMessagesTag = dataHolder.getInRegistrationMessagesTag();
        if (inRegistrationMessagesTag == null) {
            log.info("Beh, NON ESISTE un tag IN_REGISTRATION per il messaggio " + message.getId());
            return;
        }
        MessageTag registeredTag = dataHolder.getRegisteredTag();
        JSONArray elementiDaRimuovere = additionalDataFixManager.verifyAndFixInRegistrationAdditionalData(inRegistrationMessagesTag, registeredTag);
        JSONArray purifiedAdditionalData = getAdditionalDataRemovingElementiInutili(inRegistrationMessagesTag, elementiDaRimuovere);
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
    }
}
