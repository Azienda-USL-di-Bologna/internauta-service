/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.bologna.ausl.internauta.service.utils.aggiustatori.messagetaginregistrationfixer.factories;

import it.bologna.ausl.internauta.service.utils.aggiustatori.messagetaginregistrationfixer.builders.DataHolderBuilder;
import it.bologna.ausl.internauta.service.utils.aggiustatori.messagetaginregistrationfixer.holders.MessagesTagsProtocollazioneFixDataHolder;
import it.bologna.ausl.model.entities.shpeck.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 *
 * @author Salo
 */
@Component
public class DataHolderFactory {
    
    private static final Logger log = LoggerFactory.getLogger(DataHolderFactory.class);
    
    @Autowired
    private DataHolderBuilder dataHolderBuilder;
    
    public MessagesTagsProtocollazioneFixDataHolder createNewMessagesTagsProctocollazioneFixDataHolder(Message message) {
        log.info("Creo un nuovo MTPFixDataHolder");
        MessagesTagsProtocollazioneFixDataHolder dataHolder = new MessagesTagsProtocollazioneFixDataHolder();
        dataHolderBuilder.buildNewMessagesTagsProctocollazioneFixDataHolder(dataHolder, message);
        return dataHolder;
    }
    
}
