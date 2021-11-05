/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.bologna.ausl.internauta.service.test.shpeckcustomcontroller;

import static com.fasterxml.jackson.databind.util.ClassUtil.name;
import static com.fasterxml.jackson.databind.util.ClassUtil.name;
import static com.fasterxml.jackson.databind.util.ClassUtil.name;
import it.bologna.ausl.blackbox.exceptions.BlackBoxPermissionException;
import it.bologna.ausl.internauta.service.authorization.jwt.AuthenticationEndpoint;
import it.bologna.ausl.internauta.service.authorization.jwt.AuthorizationUtils;
import it.bologna.ausl.internauta.service.authorization.jwt.LoginController;
import it.bologna.ausl.internauta.service.controllers.shpeck.ShpeckCustomController;
import it.bologna.ausl.internauta.service.exceptions.ObjectNotFoundException;
import it.bologna.ausl.internauta.service.exceptions.SSOException;
import it.bologna.ausl.internauta.service.repositories.baborg.AziendaRepository;
import it.bologna.ausl.internauta.service.repositories.baborg.PecRepository;
import it.bologna.ausl.internauta.service.repositories.baborg.PersonaRepository;
import it.bologna.ausl.internauta.service.repositories.baborg.UtenteRepository;
import it.bologna.ausl.internauta.service.repositories.shpeck.MessageRepository;
import it.bologna.ausl.internauta.service.repositories.shpeck.MessageTagRepository;
import it.bologna.ausl.internauta.service.repositories.shpeck.TagRepository;
import it.bologna.ausl.internauta.service.utils.InternautaConstants;
import it.bologna.ausl.model.entities.baborg.Azienda;
import it.bologna.ausl.model.entities.baborg.Persona;
import it.bologna.ausl.model.entities.baborg.Utente;
import it.bologna.ausl.model.entities.shpeck.Message;
import it.bologna.ausl.model.entities.shpeck.MessageTag;
import it.bologna.ausl.model.entities.shpeck.Tag;
import java.io.IOException;
import java.net.URLEncoder;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.http.HttpServletRequest;
import static net.bytebuddy.matcher.ElementMatchers.is;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.HttpClientBuilder;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Assert;
import static org.junit.Assume.assumeTrue;
import org.junit.Before;
import org.junit.Test;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.context.junit4.SpringRunner;

/**
 *
 * @author Salo
 */
@RunWith(SpringRunner.class)
@SpringBootTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class ManageMessageRegistrationTests {

    private final String uuidMessageTest = "<DB8PR10MB354862B77BFA75286EA6D612E78D9@DB8PR10MB3548.EURPRD10.PROD.OUTLOOK.COM>";

    @Value("${internauta.mode}")
    String serviceMode;

    @Value("${shpeck.mapping.url.root}")
    String shpeckMappingUrl;

    @Value("${jwt.secret}")
    private String secretKey;

    @Autowired
    MessageTagRepository messageTagRepository;

    @Autowired
    MessageRepository messageRepository;

    @Autowired
    AziendaRepository aziendaRepository;

    @Autowired
    PecRepository pecRepository;

    @Autowired
    TagRepository tagRepository;

    @Autowired
    UtenteRepository utenteRepository;

    @Autowired
    PersonaRepository personaRepository;

    @Autowired
    ShpeckCustomController shpeckController;

    @Autowired
    AuthorizationUtils authorizationUtils;

    Message testMessage;

    Utente utenteAgente;

    Tag inRegistrationTag;

    String token;

    private String getToken() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        String cf = "SLMLNZ85C13A944M";
        String token = null;
        Azienda azienda = aziendaRepository.findById(2).get();
        try {
            LoginController.LoginResponse generateTestLoginResponse = authorizationUtils.
                    generateTestLoginResponse(utenteAgente, utenteAgente,
                            azienda, Persona.class, cf, "codiceFiscale",
                            secretKey, "procton", false);
            token = generateTestLoginResponse.token;
            System.out.println(generateTestLoginResponse.toString());
        } catch (Throwable ex) {
            ex.printStackTrace();
            throw ex;
        }
        return token;
    }

    private String getMessageTagAdditionalDataString() {
        return "[{\n"
                + "	\"idUtente\": {\n"
                + "		\"id\": 294712,\n"
                + "		\"descrizione\": \"Salomone Lorenzo\"\n"
                + "	},\n"
                + "	\"idAzienda\": {\n"
                + "		\"id\": 2,\n"
                + "		\"nome\": \"AUSLBO\",\n"
                + "		\"descrizione\": \"Azienda USL Bologna\"\n"
                + "	},\n"
                + "	\"idDocumento\": {\n"
                + "		\"oggetto\": \"Test\",\n"
                + "		\"codiceRegistro\": \"PG\",\n"
                + "		\"numeroProposta\": \"2021-1815\"\n"
                + "	}\n"
                + "},\n"
                + "{\n"
                + "	\"idUtente\": {\n"
                + "		\"id\": 1054352,\n"
                + "		\"descrizione\": \"Salomone Lorenzo\"\n"
                + "	},\n"
                + "	\"idAzienda\": {\n"
                + "		\"id\": 12,\n"
                + "		\"nome\": \"AUSLIM\",\n"
                + "		\"descrizione\": \"Azienda USL Imola\"\n"
                + "	},\n"
                + "	\"idDocumento\": {\n"
                + "		\"oggetto\": \"Test\",\n"
                + "		\"codiceRegistro\": \"PG\",\n"
                + "		\"numeroProposta\": \"2021-1815\"\n"
                + "	}\n"
                + "}]";
    }

    private String getBolognaAdditionalRegistrationData() {
        return "{\n"
                + "	\"idUtente\": {\n"
                + "		\"id\": 294712,\n"
                + "		\"descrizione\": \"Salomone Lorenzo\"\n"
                + "	},\n"
                + "	\"idAzienda\": {\n"
                + "		\"id\": 2,\n"
                + "		\"nome\": \"AUSLBO\",\n"
                + "		\"descrizione\": \"Azienda USL Bologna\"\n"
                + "	},\n"
                + "	\"idDocumento\": {\n"
                + "		\"oggetto\": \"Test\",\n"
                + "		\"codiceRegistro\": \"PG\",\n"
                + "		\"numeroProposta\": \"2021-1815\"\n"
                + "	}\n"
                + "}";
    }

    private String getImolaAdditionalRegistrationData() {
        return "{\n"
                + "	\"idUtente\": {\n"
                + "		\"id\": 1054352,\n"
                + "		\"descrizione\": \"Salomone Lorenzo\"\n"
                + "	},\n"
                + "	\"idAzienda\": {\n"
                + "		\"id\": 12,\n"
                + "		\"nome\": \"AUSLIM\",\n"
                + "		\"descrizione\": \"Azienda USL Imola\"\n"
                + "	},\n"
                + "	\"idDocumento\": {\n"
                + "		\"oggetto\": \"Test\",\n"
                + "		\"codiceRegistro\": \"PG\",\n"
                + "		\"numeroProposta\": \"2021-1815\"\n"
                + "	}\n"
                + "}";
    }

    private boolean isTestMode() {
        return serviceMode.equals("test");
    }

    private boolean chiamaShpeckCustomController(InternautaConstants.Shpeck.MessageRegistrationOperation operation) {
        boolean tuttoOK = false;
        try {
            System.out.println("chiamaShpeckCustomController(" + operation.toString() + ")");
//            String encodedUUID = URLEncoder.encode(uuidMessageTest);
            String encodedUUID = uuidMessageTest;
            Base64.Encoder encoder = Base64.getEncoder();
            System.out.println("TOKEN " + token);
            String params = String.format("uuidMessage=%s&operation=%s&idMessage=%s", encodedUUID, operation.toString(), testMessage.getId());
            //HttpUriRequest request = new HttpPost("http://localhost:10005" + shpeckMappingUrl + "/manageMessageRegistration" + "?" + params);
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.setServerName("localhost");
            request.setRequestURI("/" + shpeckMappingUrl + "/manageMessageRegistration");
            request.setQueryString(params);
            request.addHeader("Authorization", "Bearer " + token);
            request.addHeader("Content-Type", "application/json");
            request.addHeader("application", "procton");
            JSONObject krintHeader = new JSONObject();
            krintHeader.put("logga", false);
            request.addHeader("krint", encoder.encode(krintHeader.toString().getBytes()));
            HashMap<String, Map<String, Object>> additionalData = new HashMap<String, Map<String, Object>>();
            shpeckController.manageMessageRegistration(encodedUUID,
                    operation,
                    Integer.SIZE, additionalData, (HttpServletRequest) request);
            System.out.println("TUTTO OK");
            tuttoOK = true;
        } catch (Throwable ex) {
            ex.printStackTrace();
        }
        return tuttoOK;
    }

    private Utente loadUtente() {
        return utenteRepository.findById(294711).get();
    }

    private Tag loadInRegistrationTag() {
        return tagRepository.findByidPecAndName(testMessage.getIdPec(), Tag.SystemTagName.in_registration.toString());
    }

    private Message loadTestMessage() {
        List<Message> messages = messageRepository.findByUuidMessage(uuidMessageTest);
        // prendo il messaggio di tipo mail
        Message message = messages.stream().filter(
                m -> m.getMessageType().toString().equals(
                        Message.MessageType.MAIL.toString()))
                .findFirst().get();
        return message;
    }

    private MessageTag getMessageTagInRegistrationPerCasoBase() {
        System.out.println("Preparo il caso Base");
        String messageTagAdditionalData = getMessageTagAdditionalDataString();
        MessageTag messageTag = new MessageTag();
        messageTag.setIdMessage(testMessage);
        messageTag.setIdTag(inRegistrationTag);
        messageTag.setAdditionalData(messageTagAdditionalData);
        messageTag.setIdUtente(utenteAgente);
        return messageTag;
    }

    public List<MessageTag> loadSavedMessagesTag() {
        System.out.println("Cerco il messagetag di message " + testMessage.getId());
        return messageTagRepository.findByIdMessage(testMessage);
    }

    public boolean isMessageTagIsAlreadyPresent() {
        System.out.println("isMessageTagIsAlreadyPresent()");
        List<MessageTag> loadSavedMessagesTag = loadSavedMessagesTag();
        return loadSavedMessagesTag != null && loadSavedMessagesTag.size() > 0;
    }

    @Before
    public void preparaDatiDiBase() {
        assumeTrue(isTestMode());
        System.out.println("@Before: preparaDatiDiBase()");
        testMessage = loadTestMessage();
        utenteAgente = loadUtente();
        inRegistrationTag = loadInRegistrationTag();
        token = getToken();
    }

    @Test
    @Order(1)
    public void verifyMessagesTagIsNotPresent() {
        assumeTrue(isTestMode());
        System.out.println("@Test: verifyMessagesTagIsNotPresent()");
        Assert.assertFalse("Il messaggio e' già taggato", isMessageTagIsAlreadyPresent());
    }

    @Test
    @Order(2)
    public void saveMessageTag() throws JSONException {
        assumeTrue(isTestMode());
        System.out.println("@Test: saveMessageTag()");
        MessageTag mt = getMessageTagInRegistrationPerCasoBase();
        MessageTag saved = messageTagRepository.save(mt);
        System.out.println("Saved: " + saved.toString());
        Assert.assertTrue("Il salvataggio di MessageTag non e' andato a buon fine",
                saved != null && isMessageTagIsAlreadyPresent());
    }

    @Test
    @Order(3)
    public void manageRemoveInRegistrationTag() {
        System.out.println("@Test manageRemoveInRegistrationTag()");
        System.out.println("Ho gia' salvato una riga messagesTag? " + isMessageTagIsAlreadyPresent());
        boolean res = chiamaShpeckCustomController(InternautaConstants.Shpeck.MessageRegistrationOperation.REMOVE_IN_REGISTRATION);
        Assert.assertTrue("Rimozione InRegistration Tag fallita", res);
        System.out.println("E' rimasta una riga messagesTag? " + isMessageTagIsAlreadyPresent());

    }

//    @Test
//    @Order(4)
    public void removeMessageTagAlreadyPresent() {
        assumeTrue(isTestMode());
        System.out.println("@Test removeMessageTagAlreadyPresent()");
        List<MessageTag> loadSavedMessagesTag = loadSavedMessagesTag();
        loadSavedMessagesTag.forEach((messageTag) -> {
            messageTagRepository.delete(messageTag);
        });
        Assert.assertFalse("Il messaggio e' ancora taggato", isMessageTagIsAlreadyPresent());
    }

}
