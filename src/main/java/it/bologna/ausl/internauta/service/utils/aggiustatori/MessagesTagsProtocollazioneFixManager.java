/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.bologna.ausl.internauta.service.utils.aggiustatori;

import it.bologna.ausl.internauta.service.repositories.baborg.AziendaRepository;
import it.bologna.ausl.internauta.service.repositories.baborg.PecRepository;
import it.bologna.ausl.internauta.service.repositories.baborg.PecAziendaRepository;
import it.bologna.ausl.internauta.service.repositories.shpeck.FolderRepository;
import it.bologna.ausl.internauta.service.repositories.shpeck.MessageFolderRepository;
import it.bologna.ausl.internauta.service.repositories.shpeck.MessageRepository;
import it.bologna.ausl.internauta.service.repositories.shpeck.MessageTagRepository;
import it.bologna.ausl.internauta.service.repositories.shpeck.TagRepository;
import it.bologna.ausl.internauta.service.shpeck.utils.MessagesFoldersHandler;
import it.bologna.ausl.internauta.service.shpeck.utils.MessagesTagsHandler;
import it.bologna.ausl.model.entities.shpeck.Folder;
import it.bologna.ausl.model.entities.shpeck.Message;
import it.bologna.ausl.model.entities.shpeck.MessageFolder;
import it.bologna.ausl.model.entities.shpeck.MessageTag;
import it.bologna.ausl.model.entities.shpeck.Tag;
import it.bologna.ausl.model.entities.baborg.Pec;
import it.bologna.ausl.model.entities.baborg.PecAzienda;
import java.util.List;
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

    private Folder actualFolder;

    @Autowired
    private MessageRepository messageRepository;

    @Autowired
    private TagRepository tagRepository;

    @Autowired
    private MessageTagRepository messageTagRepository;

    @Autowired
    private MessageFolderRepository messageFolderRepository;

    @Autowired
    private FolderRepository folderRepository;

    @Autowired
    private MessagesFoldersHandler messagesFoldersHandler;

    @Autowired
    private MessagesTagsHandler messagesTagsHandler;

    public void fixDatiProtocollazioneMessaggio(Message message) {
        log.debug("Entrato in fixDatiProtocollazioneMessaggio(" + message.getId() + ")");
        actualFolder = messagesFoldersHandler.getActualFolder(message);

        MessageTag inRegistrationMessagesTag = messagesTagsHandler.getInRegistrationMessagesTag(message);
        if (inRegistrationMessagesTag != null) {
            log.debug("Trovato in_registration mt.id: " + inRegistrationMessagesTag.getId());
        } else {
            log.debug("InRegistration non trovato");
        }

        MessageTag registeredTag = messagesTagsHandler.getRegisteredMessagesTag(message);
        if (registeredTag != null) {
            log.debug("Trovato registered mt.id: " + registeredTag.getId());
        } else {
            log.debug("Registered non trovato");
        }
    }
}
