/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.bologna.ausl.internauta.service.test.aggiustatori;

import it.bologna.ausl.internauta.service.repositories.baborg.UtenteRepository;
import it.bologna.ausl.internauta.service.repositories.shpeck.MessageRepository;
import it.bologna.ausl.internauta.service.utils.aggiustatori.messagetaginregistrationfixer.factories.DataHolderFactory;
import it.bologna.ausl.internauta.service.utils.aggiustatori.messagetaginregistrationfixer.holders.MessagesTagsProtocollazioneFixDataHolder;
import it.bologna.ausl.internauta.service.utils.aggiustatori.messagetaginregistrationfixer.managers.MessagesTagsProtocollazioneFixManager;
import it.bologna.ausl.model.entities.baborg.Utente;
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

    public final static int ID_MESSAGE = 189280;  //  in protocollazione
    //public final static int ID_MESSAGE = 188770;    //  protocollato

    @Autowired
    private MessageRepository messageRepository;

    @Autowired
    private UtenteRepository utenteRepository;

    @Autowired
    MessagesTagsProtocollazioneFixManager mtpfm;

    @Autowired
    private MessagesTagsProtocollazioneFixDataHolder dataHolder;
    @Autowired
    private DataHolderFactory dataHolderFactory;

    //private static final Logger log = LoggerFactory.getLogger(MessagesTagsProtocollazioneFixManagerTest.class);
//    @Test
//    public void testaSpecifico() {
//        System.out.println("testaSpecifico...");
//        Message mess = messageRepository.findById(ID_MESSAGE).get();
//        dataHolderFactory.createNewMessagesTagsProctocollazioneFixDataHolder(mess);
//    }
    @Test
    public void caricaUtenteTest() {
        Utente u = utenteRepository.getIdUtenteByIdAziendaAndPersonaDescrizione(2, "Gusella Francesco");
        System.out.println(u.toString());
    }

//    @Test
//    public void doubleTest() {
//        System.out.println("doubleTest!!");
//        Message mess = messageRepository.findById(189280).get();
//        mtpfm.fixDatiProtocollazioneMessaggio(mess);
//        mess = messageRepository.findById(188770).get();
//        mtpfm.fixDatiProtocollazioneMessaggio(mess);
//    }
//    @Test
//    public void xxxTestaIlMain() {
//        System.out.println("SUPERFIKO DELUXXXE!!");
//        Message mess = messageRepository.findById(ID_MESSAGE).get();
//        mtpfm.fixDatiProtocollazioneMessaggio(mess);
//    }
}
