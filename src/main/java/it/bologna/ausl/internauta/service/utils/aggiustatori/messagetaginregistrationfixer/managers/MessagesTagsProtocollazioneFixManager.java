/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.bologna.ausl.internauta.service.utils.aggiustatori.messagetaginregistrationfixer.managers;

import it.bologna.ausl.internauta.service.utils.aggiustatori.messagetaginregistrationfixer.factories.DataHolderFactory;
import it.bologna.ausl.internauta.service.utils.aggiustatori.messagetaginregistrationfixer.holders.MessagesTagsProtocollazioneFixDataHolder;
import it.bologna.ausl.model.entities.shpeck.Message;
import it.bologna.ausl.model.entities.shpeck.MessageTag;
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

    public void fixDatiProtocollazioneMessaggio(Message message) {
        log.debug("Entrato in fixDatiProtocollazioneMessaggio(" + message.getId() + ")");
        dataHolder = dataHolderFactory.createNewMessagesTagsProctocollazioneFixDataHolder(message);

        MessageTag inRegistrationMessagesTag = dataHolder.getInRegistrationMessagesTag();
        MessageTag registeredTag = dataHolder.getRegisteredTag();
        additionalDataFixManager.fixInRegistrationAdditionalData(inRegistrationMessagesTag, registeredTag);

    }
}
