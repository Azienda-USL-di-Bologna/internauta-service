/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.bologna.ausl.internauta.service.test.aggiustatori;

import it.bologna.ausl.internauta.service.repositories.baborg.AziendaRepository;
import it.bologna.ausl.internauta.service.repositories.baborg.PecRepository;
import it.bologna.ausl.internauta.service.repositories.baborg.PersonaRepository;
import it.bologna.ausl.internauta.service.repositories.baborg.UtenteRepository;
import it.bologna.ausl.internauta.service.repositories.shpeck.MessageRepository;
import it.bologna.ausl.internauta.service.repositories.shpeck.MessageTagRepository;
import it.bologna.ausl.internauta.service.repositories.shpeck.TagRepository;
import it.bologna.ausl.internauta.service.utils.aggiustatori.messagetaginregistrationfixer.factories.DataHolderFactory;
import it.bologna.ausl.internauta.service.utils.aggiustatori.messagetaginregistrationfixer.holders.MessagesTagsProtocollazioneFixDataHolder;
import it.bologna.ausl.internauta.service.utils.aggiustatori.messagetaginregistrationfixer.managers.MessagesTagsProtocollazioneFixManager;
import it.bologna.ausl.internauta.service.utils.aggiustatori.messagetaginregistrationfixer.managers.ProctonWebApiCallManager;
import it.bologna.ausl.model.entities.baborg.Azienda;
import it.bologna.ausl.model.entities.baborg.Pec;
import it.bologna.ausl.model.entities.baborg.Persona;
import it.bologna.ausl.model.entities.baborg.Utente;
import it.bologna.ausl.model.entities.shpeck.Message;
import it.bologna.ausl.model.entities.shpeck.MessageTag;
import it.bologna.ausl.model.entities.shpeck.Tag;
import java.io.IOException;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import org.json.JSONObject;

/**
 *
 * @author Salo
 */
//@RunWith(SpringRunner.class)
//@SpringBootTest
public class MessagesTagsProtocollazioneFixManagerTest {

    public final static int ID_MESSAGE = 189573;  //  TEST
    //public final static int ID_MESSAGE = 189280;  //  in protocollazione
    //public final static int ID_MESSAGE = 188770;    //  protocollato

    @Autowired
    private ProctonWebApiCallManager proctonWebApiCallManager;

    @Autowired
    private MessageRepository messageRepository;

    @Autowired
    private AziendaRepository aziendaRepository;
    @Autowired
    private MessageTagRepository messageTagRepository;

    @Autowired
    private PecRepository pecRepository;

    @Autowired
    private TagRepository tagRepository;

    @Autowired
    private UtenteRepository utenteRepository;

    @Autowired
    private PersonaRepository personaRepository;

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
//    @Test
//    public void caricaUtenteTest() {
//        Persona p = personaRepository.findByCodiceFiscale("SLMLNZ85C13A944M");
//        Utente u = utenteRepository.findByIdAziendaAndIdPersona(2, p.getId());
//        System.out.println(u.toString());
//    }
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
//        Message mess = messageRepository.findById(189573).get();
//        try {
//            mtpfm.fixDatiProtocollazioneMessaggio(mess);
//        } catch (Throwable t) {
//            t.printStackTrace();
//        }
//    }
//    @Test
//    public void testaLaFaccendaDelleMailSimili() throws IOException {
//        Message mess = messageRepository.findById(ID_MESSAGE).get();
//        List<Message> geminiMails = mtpfm.getGeminiMails(mess);
//        System.out.println(geminiMails.size());
//    }
//    private JSONObject getNuovoElementoAdditionalDataFerrara() throws Throwable {
//        JSONObject addDataElement = new JSONObject();
//
//        JSONObject idAzienda = new JSONObject();
//        idAzienda.put("id", 11);
//        idAzienda.put("nome", "AOSPFE");
//        idAzienda.put("descrizione", "Azienda Ospedaliera di Ferrara");
//
//        JSONObject idUtente = new JSONObject();
//        idUtente.put("id", 395122);
//        idUtente.put("descrizione", "Tascone Marianna");
//
//        JSONObject idDocumento = new JSONObject();
//        idDocumento.put("oggetto", "Nota protocollo PG0000774/2020");
//        idDocumento.put("dataProposta", "22/06/2020 15:04");
//        idDocumento.put("codiceRegistro", "PG");
//        idDocumento.put("numeroProposta", "2020-776");
//
//        addDataElement.put("idUtente", idUtente);
//        addDataElement.put("idAzienda", idAzienda);
//        addDataElement.put("idDocumento", idDocumento);
//
//        return addDataElement;
//    }
    //@Test
//    public void testConDoppiaPecProtocollataInDueAziende() throws JSONException, Throwable {
//        System.out.println("******\ntestConDoppiaPecProtocollataInDueAziende\n*******");
//        Message message = messageRepository.findById(189573).get();
//        Pec pec = pecRepository.findById(message.getIdPec().getId()).get();
//        Tag tagRegistered = tagRepository.findByidPecAndName(message.getIdPec(), Tag.SystemTagName.registered.toString());
//        Tag tagInRegistretion = tagRepository.findByidPecAndName(message.getIdPec(), Tag.SystemTagName.in_registration.toString());
//        List<MessageTag> findByIdMessage = messageTagRepository.findByIdMessage(message);
//        for (MessageTag messageTag : findByIdMessage) {
//            // registered
//            if (messageTag.getIdTag().getId().equals(tagRegistered.getId())) {
//                messageTag.setIdTag(tagInRegistretion);
//                String additionalDataString = messageTag.getAdditionalData();
//                JSONArray additionalData = new JSONArray(additionalDataString);
//                System.out.println("AdditionalData " + additionalData.toString(4));
//                additionalData.put(getNuovoElementoAdditionalDataFerrara());
//                messageTag.setAdditionalData(additionalData.toString());
//                messageTag = messageTagRepository.save(messageTag);
//            }
//        }
//        List<Message> geminiMails = mtpfm.getGeminiMails(message);
//        for (Message geminiMail : geminiMails) {
//            Pec pecTwinMail = pecRepository.findById(message.getIdPec().getId()).get();
//            Tag tagRegisteredTwinMail = tagRepository.findByidPecAndName(pecTwinMail, Tag.SystemTagName.registered.toString());
//            Tag tagInRegistretionTwinMail = tagRepository.findByidPecAndName(pecTwinMail, Tag.SystemTagName.in_registration.toString());
//            List<MessageTag> mtGemini = messageTagRepository.findByIdMessage(geminiMail);
//            for (MessageTag messageTag : mtGemini) {
//                if (messageTag.getIdTag().getId().equals(tagRegisteredTwinMail.getId())) {
//                    messageTag.setIdTag(tagInRegistretionTwinMail);
//                    String additionalDataString = messageTag.getAdditionalData();
//                    JSONArray additionalData = new JSONArray(additionalDataString);
//                    System.out.println("AdditionalData " + additionalData.toString(4));
//                    additionalData.put(getNuovoElementoAdditionalDataFerrara());
//                    messageTag.setAdditionalData(additionalData.toString());
//                    messageTag = messageTagRepository.save(messageTag);
//                }
//            }
//        }
//        System.out.println("ORa vedo che cazzo di casino combinare...");
//        try {
//            mtpfm.fixDatiProtocollazioneMessaggio(message);
//        } catch (Throwable t) {
//            t.printStackTrace();
//        }
//    }
//    @Test
//    public void testaSoloChiamataPico() throws IOException {
//        Azienda azienda = aziendaRepository.findById(2).get();
//        try {
//            proctonWebApiCallManager.getDatiProtocollazioneDocumento(azienda, "2020-320");
//        } catch (Throwable t) {
//            System.out.println("PERFETTO! " + t.getMessage());
//        }
//    }
}
