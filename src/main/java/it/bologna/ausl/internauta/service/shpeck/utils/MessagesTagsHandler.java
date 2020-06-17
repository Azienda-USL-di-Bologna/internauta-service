/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.bologna.ausl.internauta.service.shpeck.utils;

import it.bologna.ausl.internauta.service.repositories.shpeck.MessageTagRepository;
import it.bologna.ausl.internauta.service.repositories.shpeck.TagRepository;
import it.bologna.ausl.model.entities.baborg.Pec;
import it.bologna.ausl.model.entities.shpeck.Message;
import it.bologna.ausl.model.entities.shpeck.MessageTag;
import it.bologna.ausl.model.entities.shpeck.Tag;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 *
 * @author Salo
 */
@Component
public class MessagesTagsHandler {

    @Autowired
    MessageTagRepository messageTagRepository;

    @Autowired
    TagRepository tagRepository;

    /**
     * Ritorna il Tag con il nome richiesto del Message passato come parametro.
     * Ritorna null se il message non viene trovato.
     *
     * @param message Messagge di cui cercare il tag
     * @param tagName String del nome del tag
     * @return MessageTag, null se non trovato
     */
    public MessageTag getMessageTagByName(Message message, String tagName) {
        MessageTag mtInRegistration = null;
        Tag tag = tagRepository.findByidPecAndName(message.getIdPec(), tagName);
        List<MessageTag> messTagsList = messageTagRepository.findByIdMessageAndIdTag(message, tag);
        if (messTagsList != null && messTagsList.size() > 0) {
            mtInRegistration = messTagsList.get(0);
        }
        return mtInRegistration;
    }

    /**
     * Ritorna il Tag "in_registration" del Message passato come parametro.
     * Ritorna null se il message non ha alcun tag "in_registration".
     *
     * @param message Messagge di cui cercare il tag
     * @return MessageTag, null se non trovato
     */
    public MessageTag getInRegistrationMessagesTag(Message message) {
        MessageTag mtInRegistration = getMessageTagByName(message, Tag.SystemTagName.in_registration.toString());
        return mtInRegistration;
    }

    /**
     * Ritorna il Tag "registerd" del Message passato come parametro. Ritorna
     * null se il message non ha alcun tag "registerd".
     *
     * @param message Messaggio di cui cercare il tag
     * @return MessageTag, null se non trovato
     */
    public MessageTag getRegisteredMessagesTag(Message message) {
        MessageTag mtRegistered = getMessageTagByName(message, Tag.SystemTagName.registered.toString());
        return mtRegistered;
    }
}
