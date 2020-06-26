/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.bologna.ausl.internauta.service.utils.aggiustatori.messagetaginregistrationfixer.builders;

import it.bologna.ausl.internauta.service.utils.aggiustatori.messagetaginregistrationfixer.holders.MessagesTagsProtocollazioneFixDataHolder;
import it.bologna.ausl.internauta.service.utils.aggiustatori.messagetaginregistrationfixer.handlers.MessagesFoldersHandler;
import it.bologna.ausl.internauta.service.utils.aggiustatori.messagetaginregistrationfixer.handlers.MessagesTagsHandler;
import it.bologna.ausl.model.entities.shpeck.Folder;
import it.bologna.ausl.model.entities.shpeck.Message;
import it.bologna.ausl.model.entities.shpeck.MessageTag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 *
 * @author Salo
 */
@Component
public class DataHolderBuilder {
    
    private static final Logger log = LoggerFactory.getLogger(DataHolderBuilder.class);
    
    @Autowired
    private MessagesFoldersHandler messagesFoldersHandler;
    
    @Autowired
    private MessagesTagsHandler messagesTagsHandler;
    
    public void buildNewMessagesTagsProctocollazioneFixDataHolder(MessagesTagsProtocollazioneFixDataHolder dataHolder, Message message) {
        log.info("Buildo MessagesTagsProctocollazioneFixDataHolder");
        dataHolder.setMessage(message);
        
        dataHolder.setMessagesFoldersHandler(messagesFoldersHandler);

        //setto l'actualFolder
        Folder actualFolder = messagesFoldersHandler.getActualFolder(message);
        dataHolder.setActualFolder(actualFolder);

        //setto il MessageTag InRegistration
        MessageTag inRegistrationMessagesTag = messagesTagsHandler.getInRegistrationMessagesTag(message);
        if (inRegistrationMessagesTag != null) {
            log.info("Trovato in_registration mt.id: " + inRegistrationMessagesTag.getId());
            dataHolder.setInRegistrationMessagesTag(inRegistrationMessagesTag);
        } else {
            log.info("InRegistration non trovato");
        }

        //setto il MessageTag Reigstered
        MessageTag registeredTag = messagesTagsHandler.getRegisteredMessagesTag(message);
        if (registeredTag != null) {
            log.info("Trovato registered mt.id: " + registeredTag.getId());
        } else {
            log.info("Registered non trovato, lo creo");
            registeredTag = messagesTagsHandler.createNewRegisteredMessageTag(message);
        }
        dataHolder.setRegisteredTag(registeredTag);
    }
    
}
