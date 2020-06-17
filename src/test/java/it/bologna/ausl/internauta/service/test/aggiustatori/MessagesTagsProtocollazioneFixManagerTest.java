/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.bologna.ausl.internauta.service.test.aggiustatori;

import it.bologna.ausl.internauta.service.repositories.shpeck.MessageRepository;
import it.bologna.ausl.internauta.service.utils.aggiustatori.MessagesTagsProtocollazioneFixManager;
import it.bologna.ausl.model.entities.shpeck.Message;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

/**
 *
 * @author Salo
 */
@RunWith(SpringRunner.class)
@SpringBootTest
public class MessagesTagsProtocollazioneFixManagerTest {

    @Autowired
    private MessageRepository messageRepository;

    @Autowired
    MessagesTagsProtocollazioneFixManager mtpfm;

    //private static final Logger log = LoggerFactory.getLogger(MessagesTagsProtocollazioneFixManagerTest.class);
    @Test
    public void testaSeFunzia() {
        System.out.println("SUPERFIKO DELUXXXE!!");
        Message mess = messageRepository.findById(188766).get();
        mtpfm.fixDatiProtocollazioneMessaggio(mess);
    }
}
