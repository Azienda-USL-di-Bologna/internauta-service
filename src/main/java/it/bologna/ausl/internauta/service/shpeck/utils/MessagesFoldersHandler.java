/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.bologna.ausl.internauta.service.shpeck.utils;

import it.bologna.ausl.internauta.service.repositories.shpeck.FolderRepository;
import it.bologna.ausl.internauta.service.repositories.shpeck.MessageFolderRepository;
import it.bologna.ausl.model.entities.shpeck.Folder;
import it.bologna.ausl.model.entities.shpeck.Message;
import it.bologna.ausl.model.entities.shpeck.MessageFolder;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 *
 * @author Salo
 */
@Component
public class MessagesFoldersHandler {

    @Autowired
    private FolderRepository folderRepository;

    @Autowired
    MessageFolderRepository messageFolderRepository;

    /**
     * Ritorna l'attuale Folder del Message passato come parametro.Ritorna null
     * se il message non è in nessuna folder
     *
     * @param message Messaggio di cui cercare la Folder
     * @return Folder
     */
    public Folder getActualFolder(Message message) {
        System.out.println("getActualFolder()!");
        Folder folderToReturn = null;
        List<MessageFolder> messagesFolders = messageFolderRepository.findByIdMessage(message);
        if (messagesFolders != null && messagesFolders.size() > 0) {
            MessageFolder messageFolder = messagesFolders.get(0);
            folderToReturn = folderRepository.getOne(messageFolder.getIdFolder().getId());
        }
        return folderToReturn;
    }
}
