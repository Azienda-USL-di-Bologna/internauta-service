/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.bologna.ausl.internauta.service.utils.aggiustatori.messagetaginregistrationfixer.holders;

import it.bologna.ausl.internauta.service.utils.aggiustatori.messagetaginregistrationfixer.handlers.MessagesFoldersHandler;
import it.bologna.ausl.model.entities.shpeck.Folder;
import it.bologna.ausl.model.entities.shpeck.Message;
import it.bologna.ausl.model.entities.shpeck.MessageTag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 *
 * @author Salo
 */
@Component
public class MessagesTagsProtocollazioneFixDataHolder {

    private Folder actualFolder;

    MessagesFoldersHandler messagesFoldersHandler;

    private Message message;

    private MessageTag inRegistrationMessagesTag;

    private MessageTag registeredMessageTag;

    public Folder getActualFolder() {
        return actualFolder;
    }

    public MessagesFoldersHandler getMessagesFoldersHandler() {
        return messagesFoldersHandler;
    }

    public void setMessagesFoldersHandler(MessagesFoldersHandler messagesFoldersHandler) {
        this.messagesFoldersHandler = messagesFoldersHandler;
    }

    public MessageTag getRegisteredMessageTag() {
        return registeredMessageTag;
    }

    public void setRegisteredMessageTag(MessageTag registeredMessageTag) {
        this.registeredMessageTag = registeredMessageTag;
    }

    public void setActualFolder(Folder actualFolder) {
        this.actualFolder = actualFolder;
    }

    public Message getMessage() {
        return message;
    }

    public void setMessage(Message message) {
        this.message = message;
    }

    public MessageTag getInRegistrationMessagesTag() {
        return inRegistrationMessagesTag;
    }

    public void setInRegistrationMessagesTag(MessageTag inRegistrationMessagesTag) {
        this.inRegistrationMessagesTag = inRegistrationMessagesTag;
    }

    public MessageTag getRegisteredTag() {
        return registeredMessageTag;
    }

    public void setRegisteredTag(MessageTag registeredTag) {
        this.registeredMessageTag = registeredTag;
    }
}
