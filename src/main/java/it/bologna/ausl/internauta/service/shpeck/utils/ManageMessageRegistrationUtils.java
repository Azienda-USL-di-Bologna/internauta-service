package it.bologna.ausl.internauta.service.shpeck.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.bologna.ausl.blackbox.exceptions.BlackBoxPermissionException;
import it.bologna.ausl.internauta.service.authorization.AuthenticatedSessionData;
import it.bologna.ausl.internauta.service.authorization.AuthenticatedSessionDataBuilder;
import it.bologna.ausl.internauta.service.krint.KrintShpeckService;
import it.bologna.ausl.internauta.service.repositories.shpeck.MessageFolderRepository;
import it.bologna.ausl.internauta.service.repositories.shpeck.MessageRepository;
import it.bologna.ausl.internauta.service.repositories.shpeck.MessageTagRepository;
import it.bologna.ausl.internauta.service.utils.InternautaConstants;
import it.bologna.ausl.model.entities.baborg.Azienda;
import it.bologna.ausl.model.entities.logs.OperazioneKrint;
import it.bologna.ausl.model.entities.shpeck.Folder;
import it.bologna.ausl.model.entities.shpeck.Message;
import it.bologna.ausl.model.entities.shpeck.MessageFolder;
import it.bologna.ausl.model.entities.shpeck.MessageTag;
import it.bologna.ausl.model.entities.shpeck.Tag;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 *
 * @author gusgus
 */
@Component
public class ManageMessageRegistrationUtils {
    
    private static final Logger LOG = LoggerFactory.getLogger(ManageMessageRegistrationUtils.class);
    
    @Autowired
    private AuthenticatedSessionDataBuilder authenticatedSessionDataBuilder;
    
    @Autowired
    private MessageFolderRepository messageFolderRespository;
    
    @Autowired
    private MessageRepository messageRepository;
    
    @Autowired
    private MessageTagRepository messageTagRespository;
    
    @Autowired
    ObjectMapper objectMapper;
    
    @Autowired
    private KrintShpeckService krintShpeckService;
    
    @Autowired
    private ShpeckCacheableFunctions shpeckCacheableFunctions;
    
    public void manageMessageRegistration(
            String uuidMessage,
            InternautaConstants.Shpeck.MessageRegistrationOperation operation,
            Integer idMessage,
            Map<String, Map<String, Object>> additionalData,
            Boolean doIHaveToKrint,
            Azienda azienda
    ) throws BlackBoxPermissionException, IOException, Throwable {

        LOG.info("Inizio manageMessageRegistration. uuidMessage: " + uuidMessage + " operation: " + operation + " additionalData: " + additionalData.toString());

        try {
            // operation: IN_REGISTRATION, REGISTER, REMOVE_IN_REGISTRATION
            AuthenticatedSessionData authenticatedUserProperties = authenticatedSessionDataBuilder.getAuthenticatedUserProperties();

            if (additionalData != null) {
                makeAdditionalData(additionalData, authenticatedUserProperties, azienda);
            }
            // recupero tutti i messaggi con l'uuid passato
            List<Message> messages = messageRepository.findByUuidMessage(StringUtils.trimWhitespace(uuidMessage));

            // Ciclo sui messaggi trovati e skippo quelli che non sono di tipo MAIL
            for (Message message : messages) {

                if (message.getMessageType() != Message.MessageType.MAIL) {
                    continue;
                }

                LOG.info("processo messaggio con uuidMessage: " + message.getUuidMessage() + " e id: " + message.getId());

                List<Tag> tagList = message.getIdPec().getTagList();
                List<Folder> folderList = message.getIdPec().getFolderList();
                Tag tagInRegistration = tagList.stream().filter(t -> Tag.SystemTagName.in_registration.toString().equals(StringUtils.trimWhitespace(t.getName()))).collect(Collectors.toList()).get(0);
                Tag tagRegistered = tagList.stream().filter(t -> Tag.SystemTagName.registered.toString().equals(StringUtils.trimWhitespace(t.getName()))).collect(Collectors.toList()).get(0);
                Folder folderRegistered = folderList.stream().filter(f -> Folder.FolderType.REGISTERED.equals(f.getType())).collect(Collectors.toList()).get(0);
                MessageTag messageTagInRegistration = null;
                MessageTag messageTagRegistered = null;
                List<MessageTag> messageTagInRegistrationList = messageTagRespository.findByIdMessageAndIdTag(message, tagInRegistration);
                List<MessageTag> messageTagRegisteredList = messageTagRespository.findByIdMessageAndIdTag(message, tagRegistered);
                //vedere se cambiare
                List<Map<String, Map<String, Object>>> initialAdditionalDataArrayInRegistration = new ArrayList<>();
                List<Map<String, Map<String, Object>>> initialAdditionalDataArrayRegistered = new ArrayList<>();

                if (messageTagInRegistrationList != null && (messageTagInRegistrationList.size() == 1)) {
                    messageTagInRegistration = messageTagInRegistrationList.get(0);
                }

                if ((messageTagRegisteredList != null) && (messageTagRegisteredList.size() == 1)) {
                    messageTagRegistered = messageTagRegisteredList.get(0);
                }

                // leggo gli additional data del messaggio in stado di in registrazione
                if (messageTagInRegistration != null && messageTagInRegistration.getAdditionalData() != null) {
                    try {
                        Map<String, Map<String, Object>> initialAdditionalData = objectMapper.readValue(messageTagInRegistration.getAdditionalData(), Map.class);
                        initialAdditionalDataArrayInRegistration.add(initialAdditionalData);
                    } catch (Throwable ex) {
                        initialAdditionalDataArrayInRegistration = objectMapper.readValue(messageTagInRegistration.getAdditionalData(), List.class);
                    }
                }
                // leggo gli additional data del messaggio in stato di registrati
                if (messageTagRegistered != null && messageTagRegistered.getAdditionalData() != null) {
                    try {
                        Map<String, Map<String, Object>> initialAdditionalData = objectMapper.readValue(messageTagRegistered.getAdditionalData(), Map.class);
                        initialAdditionalDataArrayRegistered.add(initialAdditionalData);
                    } catch (Throwable ex) {
                        initialAdditionalDataArrayRegistered = objectMapper.readValue(messageTagRegistered.getAdditionalData(), List.class);
                    }
                }

                // Eseguo l'operazione richiesta
                switch (operation.toString()) {
                    case "ADD_IN_REGISTRATION":
                        addInRegistration(additionalData, authenticatedUserProperties, message, messageTagInRegistration, tagInRegistration, initialAdditionalDataArrayInRegistration);
                        if (doIHaveToKrint) {
                            krintShpeckService.writeRegistration(message, OperazioneKrint.CodiceOperazione.PEC_MESSAGE_IN_PROTOCOLLAZIONE);
                        }
                        break;
                    case "ADD_REGISTERED":
                        addRegistered(additionalData, message, messageTagRegistered, authenticatedUserProperties, tagRegistered, initialAdditionalDataArrayRegistered, messageTagInRegistration, initialAdditionalDataArrayInRegistration, folderRegistered);
                        if (doIHaveToKrint) {
                            krintShpeckService.writeRegistration(message, OperazioneKrint.CodiceOperazione.PEC_MESSAGE_PROTOCOLLAZIONE);
                        }

                        break;
                    case "REMOVE_IN_REGISTRATION":
                        removeInRegistration(messageTagInRegistration, initialAdditionalDataArrayInRegistration, additionalData);
                        if (doIHaveToKrint) {
                            krintShpeckService.writeRegistration(message, OperazioneKrint.CodiceOperazione.PEC_MESSAGE_REMOVE_IN_PROTOCOLLAZIONE);
                        }
                        break;
                    case "REMOVE_REGISTERED":
                        removeRegistered(message, messageTagRegistered, initialAdditionalDataArrayRegistered, authenticatedUserProperties, additionalData);
                        if (doIHaveToKrint) {
                            krintShpeckService.writeRegistration(message, OperazioneKrint.CodiceOperazione.PEC_MESSAGE_REMOVE_PROTOCOLLAZIONE);
                        }
                        break;
                    default:
                        throw new Exception("Operation requested not found");
                }
            }
        } catch (Throwable ex) {
            LOG.error(ex.getMessage(), ex);
            throw ex;
        }
    }
    
    public void makeAdditionalData(Map<String, Map<String, Object>> additionalData, AuthenticatedSessionData authenticatedUserProperties, Azienda azienda) {
        Map<String, Object> idUtenteMap = new HashMap<>();
        idUtenteMap.put("id", authenticatedUserProperties.getUser().getId());
        idUtenteMap.put("descrizione", authenticatedUserProperties.getPerson().getDescrizione());
        additionalData.put("idUtente", idUtenteMap);
        if (azienda == null) {
            azienda = authenticatedUserProperties.getUser().getIdAzienda();
        }
        Map<String, Object> idAziendaMap = new HashMap<>();
        idAziendaMap.put("id", azienda.getId());
        idAziendaMap.put("nome", azienda.getNome());
        idAziendaMap.put("descrizione", azienda.getDescrizione());
        additionalData.put("idAzienda", idAziendaMap);
    }
    
    public void addInRegistration(
            Map<String, Map<String, Object>> additionalData,
            AuthenticatedSessionData authenticatedUserProperties,
            Message message,
            MessageTag messageTagInRegistration,
            Tag tagInRegistration,
            List<Map<String, Map<String, Object>>> initialAdditionalDataArrayInRegistration
    ) throws Exception {
        LOG.info("dentro ADD_IN_REGISTRATION per il messaggio con id: " + message.getId());
        if (additionalData == null) {
            throw new Exception("add tag ADD_IN_REGISTRATION no additional data");
        } else {
            // Controllo che initialAdditionalDataArrayInRegistration non contenga già l'azienda che sto aggiungendo su additionalData
            // Se fosse così allora starei rimettendo in registrazione un messaggio che è già in registrazione in quella azienda.
            // Ciò non è accettabile.
            checkIfAziendaAlreadyHasThisTag(additionalData, initialAdditionalDataArrayInRegistration);
        }
        try {
            MessageTag messageTagToAdd = null;
            if (messageTagInRegistration != null) {
                messageTagToAdd = messageTagInRegistration;
            } else {
                messageTagToAdd = new MessageTag();
                messageTagToAdd.setIdUtente(authenticatedUserProperties.getUser());
                messageTagToAdd.setIdMessage(message);
                messageTagToAdd.setIdTag(tagInRegistration);
            }
            initialAdditionalDataArrayInRegistration.add(additionalData);
            messageTagToAdd.setAdditionalData(objectMapper.writeValueAsString(initialAdditionalDataArrayInRegistration));
            messageTagRespository.save(messageTagToAdd);
        } catch (Exception ex) {
            throw new Exception("errore nella funzione--> addInRegistration " + ex.getMessage());
        }
    }
    
    /**
     * A questa funzione viene passato l'additionalData che si vuole aggiungere ad un tag (nello specifico in_registration oppure registered)
     * E viene passato anche initialAdditionalDataArray che contiene gli altri già presenti additionalData per quel tag.
     * Se la funzione rileva che l'azienda di additionalData è già presente dentro initialAdditionalDataArray allora lancerà errore.
     * @param additionalData
     * @param initialAdditionalDataArray
     * @throws Exception 
     */
    public void checkIfAziendaAlreadyHasThisTag(
            Map<String, Map<String, Object>> additionalData,
            List<Map<String, Map<String, Object>>> initialAdditionalDataArray
    ) throws Exception {
        Integer idAziendaAdditionalData = (Integer)additionalData.get("idAzienda").get("id");
        for (Map<String, Map<String, Object>> initialData : initialAdditionalDataArray) {
            Integer idAziendaInitialData = (Integer)initialData.get("idAzienda").get("id");
            if (idAziendaInitialData.equals(idAziendaAdditionalData)) {
                LOG.info("errore, tag su azienda " + idAziendaAdditionalData + " gia presente");
                throw new Exception("errore, tag su azienda " + idAziendaAdditionalData + " gia presente");
            }
        }
    }

    public void addRegistered(Map<String, Map<String, Object>> additionalData,
            Message message,
            MessageTag messageTagRegistered,
            AuthenticatedSessionData authenticatedUserProperties,
            Tag tagRegistered,
            List<Map<String, Map<String, Object>>> initialAdditionalDataArrayRegistered,
            MessageTag messageTagInRegistration,
            List<Map<String, Map<String, Object>>> initialAdditionalDataArrayInRegistration,
            Folder folderRegistered) throws Exception {

        LOG.info("dentro ADD_REGISTERED per il messaggio con id: " + message.getId());
        if (additionalData == null) {
            throw new Exception("add tag ADD_REGISTERED no additional data");
        } else {
            checkIfAziendaAlreadyHasThisTag(additionalData, initialAdditionalDataArrayRegistered);
        }
        try {
            MessageTag messageTagToAdd = null;
            if (messageTagRegistered != null) {
                messageTagToAdd = messageTagRegistered;
            } else {
                messageTagToAdd = new MessageTag();
                messageTagToAdd.setIdUtente(authenticatedUserProperties.getUser());
                messageTagToAdd.setIdMessage(message);
                messageTagToAdd.setIdTag(tagRegistered);
            }
            List<Integer> aziendePrecedentementeProtocollate = initialAdditionalDataArrayRegistered.stream()
                    .map(ad -> (Integer) ad.get("idAzienda").get("id")).collect(Collectors.toList());

            initialAdditionalDataArrayRegistered.add(additionalData);
            messageTagToAdd.setAdditionalData(objectMapper.writeValueAsString(initialAdditionalDataArrayRegistered));
            messageTagRespository.save(messageTagToAdd);

            removeInRegistration(messageTagInRegistration, initialAdditionalDataArrayInRegistration, additionalData);


            /* Spostamento folder.
             * Un messaggio viene spostato nella cartella protocollati qualora la sua PEC abbia nella sua aziendaList
             * l'azienda su cui si è appena protocollato, cioè l'azienda ricavibile dall'utente connesso.
             * Allo stesso tempo se tra le aziende della PEC c'è un azienda facente parte della lista
             * aziendePrecedentementeProtocollate allora non sposterò il messaggio (in quanto già fatto in precedenza).
             * in altre parole:
             * Se l'azienda dell'utente è presente tra le aziende della pec e la pec non è associata anche ad un azienda
             * facente parte delle aziendePrecedentementeProtocollate allora lo sposto altrimenti non faccio nulla.
             */
            Boolean aziendaUtenteInAziendePec = message.getIdPec().getPecAziendaList().stream()
                    .anyMatch(pecazienda
                            -> pecazienda.getIdAzienda().getId().equals(authenticatedUserProperties.getUser().getIdAzienda().getId()));

            Boolean aziendaPecInPrecedentementeProtocollate = message.getIdPec().getPecAziendaList().stream()
                    .anyMatch(pecazienda
                            -> aziendePrecedentementeProtocollate.contains(pecazienda.getIdAzienda().getId()));

            if (aziendaUtenteInAziendePec && !aziendaPecInPrecedentementeProtocollate) {
                // Lo elimino da quella in cui era e lo metto nella cartella registered
                List<MessageFolder> messageFolder = messageFolderRespository.findByIdMessage(message);
                if (!messageFolder.isEmpty()) {
                    MessageFolder mfCurrentMessage = messageFolder.get(0);
                    mfCurrentMessage.setIdUtente(authenticatedUserProperties.getUser());
                    mfCurrentMessage.setIdFolder(folderRegistered);
                    if (mfCurrentMessage.getIdFolder().getType() != Folder.FolderType.REGISTERED) {
                        messageFolderRespository.save(mfCurrentMessage);
                    }
                } else {
                    MessageFolder mfRegistered = new MessageFolder();
                    mfRegistered.setIdUtente(authenticatedUserProperties.getUser());
                    mfRegistered.setIdMessage(message);
                    mfRegistered.setIdFolder(folderRegistered);
                    messageFolderRespository.save(mfRegistered);
                }
            }
        } catch (Exception ex) {
            throw new Exception("errore nella funzione--> addRegistered " + ex.getMessage());
        }
    }

    public void removeInRegistration(MessageTag messageTagInRegistration,
            List<Map<String, Map<String, Object>>> initialAdditionalDataArrayInRegistration,
            Map<String, Map<String, Object>> additionalData) throws JsonProcessingException {
        removeAdditionalDataByIdAziendaFromTag(messageTagInRegistration, initialAdditionalDataArrayInRegistration, additionalData);

    }
    
    
    /**
     * TODO
     *
     * @param messageTag
     * @param initialAdditionalDataArrayOfTag
     * @param additionalData
     * @throws JsonProcessingException
     */
    public void removeAdditionalDataByIdAziendaFromTag(MessageTag messageTag,
            List<Map<String, Map<String, Object>>> initialAdditionalDataArrayOfTag,
            Map<String, Map<String, Object>> additionalData) throws JsonProcessingException {
        if (messageTag != null) {
            // devo togliere dal tag in_registration l'azienda passata
            Predicate<Map<String, Map<String, Object>>> isQualified
                    = item
                    -> item.get("idAzienda").get("id").equals(additionalData.get("idAzienda").get("id"));

            initialAdditionalDataArrayOfTag.removeIf(isQualified);

            if (initialAdditionalDataArrayOfTag.size() > 0) {
                messageTag.setAdditionalData(objectMapper.writeValueAsString(initialAdditionalDataArrayOfTag));
                messageTagRespository.save(messageTag);
            } else {
                messageTagRespository.delete(messageTag);
            }
        }
    }
    
    
    public void removeRegistered(Message message,
            MessageTag messageTagRegistered,
            List<Map<String, Map<String, Object>>> initialAdditionalDataArrayRegistered,
            AuthenticatedSessionData authenticatedUserProperties,
            Map<String, Map<String, Object>> additionalData) throws JsonProcessingException {
        LOG.info("dentro REMOVE_REGISTERED per il messaggio con id: " + message.getId());

        removeAdditionalDataByIdAziendaFromTag(messageTagRegistered, initialAdditionalDataArrayRegistered, additionalData);

        /*
            Il tag l'ho tolto ma devo controllare se spostare il messaggio da posta protocollata a altra cartella
            Devo spostare il messaggio se esso non è protocollato in altre aziende della aziendaPecList
         */
        List<Integer> aziendeInCuiRimaneProtocollato = initialAdditionalDataArrayRegistered.stream()
                .map(ad -> (Integer) ad.get("idAzienda").get("id")).collect(Collectors.toList());

        Boolean daNonSpostare = message.getIdPec().getPecAziendaList().stream()
                .anyMatch(pecazienda
                        -> aziendeInCuiRimaneProtocollato.contains(pecazienda.getIdAzienda().getId()));

        if (!daNonSpostare) {
            moveInPreviousFolder(message, authenticatedUserProperties);
        }

    }
    
    private void moveInPreviousFolder(Message message, AuthenticatedSessionData authenticatedUserProperties) {
        List<MessageFolder> messageFolder = messageFolderRespository.findByIdMessage(message);

        if (messageFolder != null && !messageFolder.isEmpty()) {
            MessageFolder currentMessageFolder = messageFolder.get(0);
            // se il messaggio si trova nella folder REGISTERED lo sposto nella previousFolder, se no lo lascio nella cartella in cui si trova
            if (currentMessageFolder.getIdPreviousFolder() != null && currentMessageFolder.getIdFolder().getType().equals(Folder.FolderType.REGISTERED)) {
                currentMessageFolder.setIdUtente(authenticatedUserProperties.getUser());
                currentMessageFolder.setIdFolder(currentMessageFolder.getIdPreviousFolder());
                messageFolderRespository.save(currentMessageFolder);
            }
        }
    }
}
