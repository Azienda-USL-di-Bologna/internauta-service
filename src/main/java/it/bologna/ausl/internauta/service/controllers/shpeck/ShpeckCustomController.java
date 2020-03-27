package it.bologna.ausl.internauta.service.controllers.shpeck;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.JsonObject;
import com.querydsl.core.types.dsl.BooleanExpression;
import it.bologna.ausl.blackbox.exceptions.BlackBoxPermissionException;
import it.bologna.ausl.eml.handler.EmlHandler;
import it.bologna.ausl.eml.handler.EmlHandlerException;
import it.bologna.ausl.eml.handler.EmlHandlerAttachment;
import it.bologna.ausl.eml.handler.EmlHandlerResult;
import it.bologna.ausl.internauta.service.authorization.AuthenticatedSessionData;
import it.bologna.ausl.internauta.service.authorization.AuthenticatedSessionDataBuilder;
import it.bologna.ausl.internauta.service.exceptions.BadParamsException;
import it.bologna.ausl.internauta.service.exceptions.http.ControllerHandledExceptions;
import it.bologna.ausl.internauta.service.exceptions.http.Http409ResponseException;
import it.bologna.ausl.internauta.service.exceptions.http.Http500ResponseException;
import it.bologna.ausl.internauta.service.interceptors.shpeck.MessageTagInterceptor;
import it.bologna.ausl.internauta.service.krint.KrintShpeckService;
import it.bologna.ausl.internauta.service.krint.KrintUtils;
import it.bologna.ausl.internauta.service.repositories.baborg.PecRepository;
import it.bologna.ausl.internauta.service.repositories.baborg.PersonaRepository;
import it.bologna.ausl.internauta.service.repositories.shpeck.DraftRepository;
import it.bologna.ausl.internauta.service.shpeck.utils.ShpeckCacheableFunctions;
import it.bologna.ausl.internauta.service.shpeck.utils.ShpeckUtils;
import it.bologna.ausl.internauta.service.shpeck.utils.ShpeckUtils.EmlSource;
import it.bologna.ausl.model.entities.baborg.Pec;
import it.bologna.ausl.model.entities.baborg.Persona;
import it.bologna.ausl.model.entities.baborg.Utente;
import it.bologna.ausl.model.entities.shpeck.Draft;
import it.bologna.ausl.model.entities.shpeck.Draft.MessageRelatedType;
import it.bologna.ausl.model.entities.shpeck.Folder;
import it.bologna.ausl.model.entities.shpeck.Message;
import it.bologna.ausl.model.entities.shpeck.MessageFolder;
import it.bologna.ausl.model.entities.shpeck.MessageTag;
import it.bologna.ausl.model.entities.shpeck.QMessageFolder;
import it.bologna.ausl.model.entities.shpeck.QMessageTag;
import it.bologna.ausl.model.entities.shpeck.QTag;
import it.bologna.ausl.model.entities.shpeck.Tag;
import it.nextsw.common.utils.CommonUtils;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipOutputStream;
import javax.mail.MessagingException;
import javax.mail.internet.AddressException;
import javax.mail.internet.MimeMessage;
import javax.persistence.EntityNotFoundException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.tomcat.util.http.fileupload.IOUtils;
import org.jose4j.json.internal.json_simple.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StreamUtils;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import it.bologna.ausl.internauta.service.repositories.shpeck.RecepitRepository;
import it.bologna.ausl.internauta.service.repositories.shpeck.TagRepository;
import it.bologna.ausl.internauta.service.repositories.shpeck.MessageTagRepository;
import it.bologna.ausl.internauta.service.repositories.shpeck.MessageRepository;
import it.bologna.ausl.internauta.service.repositories.shpeck.MessageFolderRepository;
import it.bologna.ausl.internauta.service.repositories.shpeck.MessageCompleteRepository;
import it.bologna.ausl.internauta.service.repositories.shpeck.FolderRepository;
import it.bologna.ausl.internauta.service.repositories.shpeck.OutboxLiteRepository;
import it.bologna.ausl.internauta.service.utils.CachedEntities;
import it.bologna.ausl.internauta.service.utils.InternautaConstants;
import it.bologna.ausl.model.entities.logs.OperazioneKrint;
import it.bologna.ausl.model.entities.shpeck.QDraft;
import it.bologna.ausl.model.entities.shpeck.QMessage;
import it.bologna.ausl.model.entities.shpeck.views.QOutboxLite;
import it.nextsw.common.interceptors.exceptions.AbortSaveInterceptorException;
import it.nextsw.common.interceptors.exceptions.SkipDeleteInterceptorException;
import java.util.Arrays;
import java.util.function.Predicate;
import java.util.logging.Level;
import org.apache.commons.lang3.ArrayUtils;
import org.json.JSONArray;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.util.StringUtils;

/**
 *
 * @author gusgus
 */
@RestController
@RequestMapping(value = "${shpeck.mapping.url.root}")
public class ShpeckCustomController implements ControllerHandledExceptions {

    private static final Logger LOG = LoggerFactory.getLogger(ShpeckCustomController.class);

    @Autowired
    private ShpeckUtils shpeckUtils;

    @Autowired
    private CommonUtils nextSdrCommonUtils;

    @Autowired
    ShpeckCacheableFunctions shpeckCacheableFunctions;

    @Autowired
    private PecRepository pecRepository;

    @Autowired
    private DraftRepository draftRepository;

    @Autowired
    private MessageRepository messageRepository;

    @Autowired
    private RecepitRepository recepitRepository;

    @Autowired
    private TagRepository tagRepository;

    @Autowired
    private FolderRepository folderRepository;

    @Autowired
    private MessageTagRepository messageTagRespository;

    @Autowired
    private MessageFolderRepository messageFolderRespository;

    @Autowired
    private MessageCompleteRepository messageCompleteRespository;

    @Autowired
    private OutboxLiteRepository outboxLiteRepository;

    @Autowired
    private AuthenticatedSessionDataBuilder authenticatedSessionDataBuilder;

    @Autowired
    private PersonaRepository personaRepository;

    @Autowired
    private KrintShpeckService krintShpeckService;

    @Autowired
    private MessageTagInterceptor messageTagInterceptor;
    
    @Autowired
    private ApplicationEventPublisher applicationEventPublisher;
    
    @Autowired
    private CachedEntities cachedEntities;
            
    @Autowired
    ObjectMapper objectMapper;

    /**
     *
     * @param idMessage
     * @param emlSource
     * @param request
     * @return
     * @throws EmlHandlerException
     * @throws java.io.UnsupportedEncodingException
     * @throws
     * it.bologna.ausl.internauta.service.exceptions.http.Http500ResponseException
     */
    @RequestMapping(value = "extractEmlData/{idMessage}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @Transactional(rollbackFor = Throwable.class)
    public ResponseEntity<String> extractEmlData(
            @PathVariable(required = true) Integer idMessage,
            @RequestParam("emlSource") EmlSource emlSource,
            HttpServletRequest request
    ) throws EmlHandlerException, UnsupportedEncodingException, Http500ResponseException {
        try {
            EmlHandlerResult res = shpeckCacheableFunctions.getInfoEml(emlSource, idMessage);
            if (emlSource != EmlSource.DRAFT && emlSource != EmlSource.OUTBOX) {
                int attNumber = (int) Arrays.stream(res.getAttachments())
                        .filter(a -> {
                            LOG.info(a.toString());
                            return a.getForHtmlAttribute() == false;
                        }).count();
                res.setRealAttachmentNumber(attNumber);
                Message m = messageRepository.getOne(idMessage);
                if (m != null) {
                    if (m.getAttachmentsNumber() != attNumber) {
                        m.setAttachmentsNumber(attNumber);
                        messageRepository.save(m);
                    }
                }
            } else {
                res.setRealAttachmentNumber(res.getAttachments().length);
            }
            return new ResponseEntity(res, HttpStatus.OK);
        } catch (Exception ex) {
            LOG.error("errore nella creazione del file eml", ex);
            throw new Http500ResponseException("1", "errore nella creazione del file eml", ex);
        }
    }

    /**
     *
     * @param idMessage
     * @param emlSource
     * @param recepit
     * @param response
     * @param request
     * @throws EmlHandlerException
     * @throws FileNotFoundException
     * @throws MalformedURLException
     * @throws IOException
     * @throws MessagingException
     * @throws java.io.UnsupportedEncodingException
     * @throws it.bologna.ausl.internauta.service.exceptions.BadParamsException
     */
    @RequestMapping(value = "downloadEml/{idMessage}", method = RequestMethod.GET)
    @Transactional(rollbackFor = Throwable.class)
    public void downloadEml(
            @PathVariable(required = true) Integer idMessage,
            @RequestParam("emlSource") EmlSource emlSource,
            HttpServletResponse response,
            HttpServletRequest request
    ) throws EmlHandlerException, FileNotFoundException, MalformedURLException, IOException, MessagingException, UnsupportedEncodingException, BadParamsException {
        LOG.info("getEml", idMessage);
        // TODO: Usare repository reale
//        String hostname = nextSdrCommonUtils.getHostname(request);
//        System.out.println("hostanme " + hostname);
//        String repositoryTemp = null;
//        if (hostname.equals("localhost")) {
//            repositoryTemp = "C:\\Users\\Public\\prova";
//        } else {
//            repositoryTemp = "/tmp/emlProveShpeckUI/prova";
//        }
        File downloadEml = null;
        try {
            downloadEml = shpeckUtils.downloadEml(emlSource, idMessage);
            try (FileInputStream is = new FileInputStream(downloadEml.getAbsolutePath());) {
                StreamUtils.copy(is, response.getOutputStream());
                response.flushBuffer();
            }
        } finally {
            if (downloadEml != null) {
                downloadEml.delete();
            }
        }
    }

    @RequestMapping(value = "downloadEmlByUuid", method = RequestMethod.GET)
    public void downloadEmlByUuid(
            @RequestParam(required = true) String uuidRepository,
            HttpServletResponse response,
            HttpServletRequest request
    ) throws EmlHandlerException, FileNotFoundException, MalformedURLException, IOException, MessagingException, UnsupportedEncodingException, BadParamsException {
        LOG.info("downloadEmlByUuid");
        BooleanExpression filter = QMessage.message.uuidRepository.eq(uuidRepository);
        Message ricevuta = messageRepository.findOne(filter).get();
        LOG.info("Trovata ricevuta  " + ricevuta.toString());
        File downloadEml = null;
        try {
            downloadEml = shpeckUtils.downloadEml(EmlSource.MESSAGE, ricevuta.getId());
            try (FileInputStream is = new FileInputStream(downloadEml.getAbsolutePath());) {
                response.setHeader("filename", ricevuta.getName());
                StreamUtils.copy(is, response.getOutputStream());
                response.flushBuffer();
            }
        } finally {
            if (downloadEml != null) {
                downloadEml.delete();
            }
        }
    }

    /**
     *
     * @param idMessage
     * @param idAllegato
     * @param emlSource
     * @param response
     * @param request
     * @throws EmlHandlerException
     * @throws FileNotFoundException
     * @throws MalformedURLException
     * @throws IOException
     * @throws MessagingException
     * @throws java.io.UnsupportedEncodingException
     * @throws it.bologna.ausl.internauta.service.exceptions.BadParamsException
     */
    @RequestMapping(value = "downloadEmlAttachment/{idMessage}/{idAllegato}", method = RequestMethod.GET)
    public void downloadEmlAttachment(
            @PathVariable(required = true) Integer idMessage,
            @PathVariable(required = true) Integer idAllegato,
            @RequestParam("emlSource") EmlSource emlSource,
            HttpServletResponse response,
            HttpServletRequest request
    ) throws EmlHandlerException, FileNotFoundException, MalformedURLException, IOException, MessagingException, UnsupportedEncodingException, BadParamsException {
        LOG.info("getEmlAttachment", idMessage, idAllegato);
//        String hostname = nextSdrCommonUtils.getHostname(request);
//        System.out.println("hostanme " + hostname);
//        String repositoryTemp = null;
//        if (hostname.equals("localhost")) {
//            repositoryTemp = "C:\\Users\\Public\\prova";
//        } else {
//            repositoryTemp = "/tmp/emlProveShpeckUI/prova";
//        }
        File downloadEml = null;
        try {
            downloadEml = shpeckUtils.downloadEml(emlSource, idMessage);
            try (InputStream attachment = EmlHandler.getAttachment(new FileInputStream(downloadEml.getAbsolutePath()), idAllegato)) {
                StreamUtils.copy(attachment, response.getOutputStream());
                response.flushBuffer();
            }
        } finally {
            if (downloadEml != null) {
                downloadEml.delete();
            }
        }
    }

    public void makeAdditionalData(Map<String, Map<String, Object>> additionalData, AuthenticatedSessionData authenticatedUserProperties) {
        Map<String, Object> idUtenteMap = new HashMap<>();
        idUtenteMap.put("id", authenticatedUserProperties.getUser().getId());
        idUtenteMap.put("descrizione", authenticatedUserProperties.getPerson().getDescrizione());
        additionalData.put("idUtente", idUtenteMap);
        Map<String, Object> idAziendaMap = new HashMap<>();
        idAziendaMap.put("id", authenticatedUserProperties.getUser().getIdAzienda().getId());
        idAziendaMap.put("nome", authenticatedUserProperties.getUser().getIdAzienda().getNome());
        idAziendaMap.put("descrizione", authenticatedUserProperties.getUser().getIdAzienda().getDescrizione());
        additionalData.put("idAzienda", idAziendaMap);
    }

    public void addInRegistration(Map<String, Map<String, Object>> additionalData,
            AuthenticatedSessionData authenticatedUserProperties,
            Message message,
            MessageTag messageTagInRegistration,
            Tag tagInRegistration,
            List<Map<String, Map<String, Object>>> initialAdditionalDataArrayInRegistration
    ) throws Exception {
        LOG.info("dentro ADD_IN_REGISTRATION per il messaggio con id: " + message.getId());
        if (additionalData == null) {
            throw new Exception("add tag ADD_IN_REGISTRATION no additional data");
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

    /**
     *
     * @param idMessage
     * @param emlSource
     * @param response
     * @param request
     * @throws EmlHandlerException
     * @throws FileNotFoundException
     * @throws MalformedURLException
     * @throws IOException
     * @throws MessagingException
     * @throws java.io.UnsupportedEncodingException
     * @throws it.bologna.ausl.internauta.service.exceptions.BadParamsException
     */
    @RequestMapping(value = "downloadAllEmlAttachment/{idMessage}", method = RequestMethod.GET, produces = "application/zip")
    public void downloadAllEmlAttachment(
            @PathVariable(required = true) Integer idMessage,
            @RequestParam("emlSource") EmlSource emlSource,
            HttpServletResponse response,
            HttpServletRequest request
    ) throws EmlHandlerException, FileNotFoundException, MalformedURLException, IOException, MessagingException, UnsupportedEncodingException, BadParamsException {
        LOG.info("downloadAllEmlAttachment", idMessage);
//        String hostname = nextSdrCommonUtils.getHostname(request);
//        System.out.println("hostanme " + hostname);
//        String repositoryTemp = null;
//        if (hostname.equals("localhost")) {
//            repositoryTemp = "C:\\Users\\Public\\prova";
//        } else {
//            repositoryTemp = "/tmp/emlProveShpeckUI/prova";
//        }
        File downloadEml = null;
        ZipOutputStream zos = null;
        try {
            downloadEml = shpeckUtils.downloadEml(emlSource, idMessage);
            response.addHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=allegati.zip");
            List<Pair> attachments = EmlHandler.getAttachments(new FileInputStream(downloadEml.getAbsolutePath()));
            zos = new ZipOutputStream(new BufferedOutputStream(response.getOutputStream()));
            Integer i;
            for (Pair p : attachments) {
                i = 0;
                Boolean in_error = true;
                while (in_error) {
                    try {
                        String s = "";
                        if (i > 0) {
                            s = "_" + Integer.toString(i);
                        }
                        zos.putNextEntry(new ZipEntry((String) p.getLeft() + s));
                        in_error = false;
                    } catch (ZipException ex) {
                        i++;
                    }
                }
                StreamUtils.copy((InputStream) p.getRight(), zos);
            }
            response.flushBuffer();
        } finally {
            IOUtils.closeQuietly(zos);
            if (downloadEml != null) {
                downloadEml.delete();
            }
        }
    }

    /**
     * Salva la bozza della mail sul database
     *
     * @param request La Request Http
     * @param idDraftMessage L'id del messaggio bozza che stiamo salvando
     * @param idPec L'id dell'indirizzo PEC a cui appartiene la bozza
     * @param body Il testo html della mail
     * @param hideRecipients Destinatari nascosti
     * @param subject L'oggetto della mail
     * @param to Array degli indirizzi di destinazione
     * @param cc Array degli indirizzi in copia carbone
     * @param attachments Array degli allegati
     * @param idMessageRelated Id del messaggio risposto opzionale
     * @param messageRelatedType Il tipo della relazione del messaggio related
     * @param idMessageRelatedAttachments
     * @param idUtente // TODO: non usato ancora
     * @throws AddressException Errore nella creazione degli indirizzi
     * @throws IOException Errore di salvataggio
     * @throws MessagingException Errore nella creazione del mimemessage
     * @throws EntityNotFoundException Elemento non trovato nel repository
     * @throws it.bologna.ausl.eml.handler.EmlHandlerException
     * @throws
     * it.bologna.ausl.internauta.service.exceptions.http.Http500ResponseException
     * @throws it.bologna.ausl.internauta.service.exceptions.BadParamsException
     */
    @Transactional(rollbackFor = Throwable.class, noRollbackFor = Http500ResponseException.class)
    @RequestMapping(value = {"saveDraftMessage", "sendMessage"}, method = RequestMethod.POST)
    public void saveDraftMessage(
            HttpServletRequest request,
            @RequestParam("idDraftMessage") Integer idDraftMessage,
            @RequestParam("idPec") Integer idPec,
            @RequestParam("body") String body,
            @RequestParam("hideRecipients") Boolean hideRecipients,
            @RequestParam("subject") String subject,
            @RequestParam("to") String[] to,
            @RequestParam("cc") String[] cc,
            @RequestParam("attachments") MultipartFile[] attachments,
            @RequestParam("idMessageRelated") Integer idMessageRelated,
            @RequestParam("messageRelatedType") MessageRelatedType messageRelatedType,
            @RequestParam("idMessageRelatedAttachments") Integer[] idMessageRelatedAttachments,
            @RequestParam(name = "idUtente", required = false) Integer idUtente
    ) throws AddressException, IOException, MessagingException, EntityNotFoundException, EmlHandlerException, Http500ResponseException, BadParamsException {

        LOG.info("Shpeck controller -> Message received from PEC with id: " + idPec);
        String hostname = nextSdrCommonUtils.getHostname(request);

        ArrayList<EmlHandlerAttachment> listAttachments = shpeckUtils.convertAttachments(attachments);

        ArrayList<MimeMessage> mimeMessagesList = new ArrayList<>();
        MimeMessage mimeMessage = null;

        LOG.info("Getting draft with idDraft: ", idDraftMessage);
        Draft draftMessage = draftRepository.getOne(idDraftMessage);
        if (idUtente != null) {
            Utente utente = this.cachedEntities.getUtente(idUtente);
            draftMessage.setIdUtente(utente);
        }
        
        LOG.info("Getting PEC from repository...");
        Pec pec = pecRepository.getOne(idPec);
        String from = pec.getIndirizzo();
        LOG.info("Start building mime message...");
        // Prende gli allegati dall'eml della draft o dal messaggio che si sta inoltrando
        ArrayList<EmlHandlerAttachment> emlAttachments = shpeckUtils.getEmlAttachments(draftMessage, idMessageRelated, messageRelatedType, idMessageRelatedAttachments);
        if (request.getServletPath().endsWith("saveDraftMessage")) {
            mimeMessage = shpeckUtils.buildMimeMessage(from, to, cc, body, subject, listAttachments, emlAttachments,
                    hostname, draftMessage);
            LOG.info("Mime message generated correctly!");
            LOG.info("Preparing the message for saving...");
            shpeckUtils.saveDraft(draftMessage, pec, subject, to, cc, hideRecipients,
                    listAttachments, body, mimeMessage, idMessageRelated, messageRelatedType, emlAttachments, request);
        } else if (request.getServletPath().endsWith("sendMessage")) {
            if (Objects.equals(hideRecipients, Boolean.TRUE)) {
                LOG.info("Hide recipients is true, building mime message for each recipient.");
                for (String address : to) {
                    mimeMessage = shpeckUtils.buildMimeMessage(from, new String[]{address}, cc, body, subject, listAttachments,
                            emlAttachments, hostname, draftMessage);
                    mimeMessagesList.add(mimeMessage);
                }
                LOG.info("Mime messages generated correctly!");
            } else {
                mimeMessage = shpeckUtils.buildMimeMessage(from, to, cc, body, subject, listAttachments,
                        emlAttachments, hostname, draftMessage);
                mimeMessagesList.add(mimeMessage);
                LOG.info("Mime message generated correctly!");
            }

            LOG.info("Preparing the message for sending...");
            try {
                for (MimeMessage mime : mimeMessagesList) {
                    shpeckUtils.sendMessage(pec, subject, idMessageRelated, hideRecipients, body, listAttachments, emlAttachments, mime, request);
                }

                if (idMessageRelated != null) {
                    shpeckUtils.setTagsToMessage(pec, idMessageRelated, messageRelatedType, request);
                }

                shpeckUtils.deleteDraft(draftMessage);
            } catch (IOException | MessagingException | EntityNotFoundException ex) {
                LOG.error("Handling error on send! Trying to save...", ex);
                mimeMessage = shpeckUtils.buildMimeMessage(from, to, cc, body, subject, listAttachments,
                        emlAttachments, hostname, draftMessage);
                shpeckUtils.saveDraft(draftMessage, pec, subject, to, cc, hideRecipients,
                        listAttachments, body, mimeMessage, idMessageRelated, messageRelatedType, emlAttachments, request);
                throw new Http500ResponseException("007", "Errore durante l'invio. La mail è stata salvata nelle bozze.", ex);
            }
        }
    }

    /**
     * La funzione si occupa di reindirizzare il messaggio messageSource alla
     * casella idPecDestination.Viene quindi copiato il messaggio sostituiendo
     * l'idPec e altri campi.Viene poi attaccato il tag di readdressed_out al
     * messageSource e readdressed_in al messaggio appena creato
     *
     * @param idMessageSource
     * @param idPecDestination
     * @param request
     * @return
     * @throws CloneNotSupportedException
     * @throws Http409ResponseException
     */
    @Transactional
    @RequestMapping(value = {"readdressMessage"}, method = RequestMethod.POST)
    public String readdressMessage(
            @RequestParam("idMessageSource") Integer idMessageSource,
            @RequestParam("idPecDestination") Integer idPecDestination,
            HttpServletRequest request) throws CloneNotSupportedException, Http409ResponseException {
        // la funzione è disponibile solo se il messaggio non è stato già reindirizzato
        // recupero message sorgente
        Message messageSource = messageRepository.getOne(idMessageSource);
        List<MessageTag> messageTagListSource = messageSource.getMessageTagList();
        for (MessageTag mt : messageTagListSource) {
            if (mt.getIdTag().getName().equals(Tag.SystemTagName.readdressed_out.toString())) {
                throw new Http409ResponseException("1", "il messaggio è gia stato reindirizzato.");
            } else if (mt.getIdTag().getName().equals(Tag.SystemTagName.registered.toString())) {
                throw new Http409ResponseException("2", "il messaggio è stato protocollato.");
            } else if (mt.getIdTag().getName().equals(Tag.SystemTagName.in_registration.toString())) {
                throw new Http409ResponseException("3", "il messaggio è in protocollazione.");
            }
        }
        if (messageSource.getInOut().equals(Message.InOut.OUT.toString())) {
            throw new Http409ResponseException("4", "un messaggio in uscita non può essere reindirizzato.");
        }
        // recupero PEC destinazione e source
        Pec pecDestination = pecRepository.getOne(idPecDestination);

        // Prendo l'utente loggato
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Utente utente = (Utente) authentication.getPrincipal();
        Persona persona = personaRepository.getOne(utente.getIdPersona().getId());

        // creo il nuovo messaggio reindirizzato
        Message messageDestination = messageSource.clone();

        messageDestination.setIdPec(pecDestination);
        messageDestination.setInOut(Message.InOut.IN);
        messageDestination.setSeen(Boolean.FALSE);
        messageDestination.setIdRelated(messageSource);

        MessageTag messageTag = new MessageTag();
        List<MessageTag> messageTagList = new ArrayList();

        messageTag.setIdMessage(messageDestination);
        Optional<Tag> tagOp = tagRepository.findOne(
                QTag.tag.name.eq(Tag.SystemTagName.readdressed_in.toString())
                        .and(QTag.tag.idPec.id.eq(pecDestination.getId())));
        messageTag.setIdTag(tagOp.get());
        messageTag.setInserted(LocalDateTime.now());
        messageTag.setIdUtente(utente);
        JsonObject idPecSource = new JsonObject();
        idPecSource.addProperty("id", messageSource.getIdPec().getId());
        idPecSource.addProperty("indirizzo", messageSource.getIdPec().getIndirizzo());
        JsonObject idUtente = new JsonObject();
        idUtente.addProperty("id", utente.getId());
        idUtente.addProperty("descrizione", utente.getIdPersona().getDescrizione());
        JsonObject additionalData = new JsonObject();
        additionalData.add("idUtente", idUtente);
        additionalData.add("idPec", idPecSource);
        messageTag.setAdditionalData(additionalData.toString());
        messageTagList.add(messageTag);
        messageDestination.setMessageTagList(messageTagList);

        /* gdm: commentato perché con lo spostamento del trigger che sposta nella posta in arrivo all'inserimento di ogni messaggio in messages, 
        *  questa parte non serve più perché viene fatta in auomatico dal trigger
        MessageFolder messageFolder = new MessageFolder();
        List<MessageFolder> mfList = new ArrayList();

        messageFolder.setIdMessage(messageDestination);
        Optional<Folder> folderOp = folderRepository.findOne(
                QFolder.folder.type.eq(Folder.FolderType.INBOX.toString())
                        .and(QFolder.folder.idPec.id.eq(pecDestination.getId()))
        );
        messageFolder.setIdFolder(folderOp.get());
        messageFolder.setInserted(LocalDateTime.now());
        mfList.add(messageFolder);
        messageDestination.setMessageFolderList(mfList);
         */
        messageRepository.save(messageDestination);
        // assegno il tag reindirizzato out a il message source

        MessageTag messageTagSource = new MessageTag();

        messageTagSource.setIdMessage(messageSource);
        Optional<Tag> tagOpSource = tagRepository.findOne(
                QTag.tag.name.eq(Tag.SystemTagName.readdressed_out.toString())
                        .and(QTag.tag.idPec.id.eq(messageSource.getIdPec().getId())));
        messageTagSource.setIdTag(tagOpSource.get());
        messageTagSource.setInserted(LocalDateTime.now());
        messageTagSource.setIdUtente(utente);
        JsonObject idPecDestinationJson = new JsonObject();
        idPecDestinationJson.addProperty("id", pecDestination.getId());
        idPecDestinationJson.addProperty("indirizzo", pecDestination.getIndirizzo());
        JsonObject additionalDataSource = new JsonObject();
        additionalDataSource.add("idUtente", idUtente);
        additionalDataSource.add("idPec", idPecDestinationJson);
        messageTagSource.setAdditionalData(additionalDataSource.toString());
        // List<MessageFolder> messageFolderList = messageSource.getMessageFolderList();

        messageTagListSource.add(messageTagSource);
        messageRepository.save(messageSource);

        System.out.println(messageSource.toString());
        System.out.println("-----------------------");
        System.out.println(messageDestination.toString());

        messageRepository.updateTscol(messageDestination.getId());

        // Loggo il reindirizzamento
        if (KrintUtils.doIHaveToKrint(request)) {
            krintShpeckService.writeReaddress(messageSource, messageDestination, OperazioneKrint.CodiceOperazione.PEC_MESSAGE_REINDIRIZZAMENTO_OUT);
            krintShpeckService.writeReaddress(messageDestination, messageSource, OperazioneKrint.CodiceOperazione.PEC_MESSAGE_REINDIRIZZAMENTO_IN);
        }

        return additionalDataSource.toString();
    }

    /**
     * Conta il numero di messaggi presenti nella folder passata
     *
     * @param idFolder l'id della folder
     * @param unSeen conta solo i non letti, default false (conta solo i non
     * letti)
     * @param draft
     * @param outbox
     * @return
     */
    @RequestMapping(value = "countMessageInFolder/{idFolder}", method = RequestMethod.GET)
    public Long countMesisageInFolder(
            @PathVariable(required = true) Integer idFolder,
            @RequestParam(name = "folderType", required = false) Folder.FolderType folderType,
            @RequestParam(name = "idPec", required = false) Integer idPec,
            @RequestParam(name = "unSeen", required = false, defaultValue = "false") Boolean unSeen
    ) {
        BooleanExpression filter;
        if (folderType != null) {
            switch (folderType) {
                case OUTBOX:

                    filter = QOutboxLite.outboxLite.idPec.id.eq(idPec).and(QOutboxLite.outboxLite.ignore.eq(false));
                    return outboxLiteRepository.count(filter);

                case DRAFT:

                    filter = QDraft.draft.idPec.id.eq(idPec);
                    return draftRepository.count(filter);

                default:
                    filter = QMessageFolder.messageFolder.idFolder.id.eq(idFolder).and(QMessageFolder.messageFolder.deleted.eq(false));
                    if (unSeen) {
                        filter = filter.and(QMessageFolder.messageFolder.idMessage.seen.eq(false));
                    }
                    return messageFolderRespository.count(filter);
            }

        }

        filter = QMessageFolder.messageFolder.idFolder.id.eq(idFolder).and(QMessageFolder.messageFolder.deleted.eq(false));
        if (unSeen) {
            filter = filter.and(QMessageFolder.messageFolder.idMessage.seen.eq(false));
        }
        return messageFolderRespository.count(filter);
    }
//        BooleanExpression filter = QMessageComplete.messageComplete.idFolder.id.eq(idFolder);
//        if (unSeen) {
//            filter = filter.and(QMessageComplete.messageComplete.seen.eq(false));
//        }
//        return messageCompleteRespository.count(filter);
    

    /**
     * La funzione conta quanti messaggi hanno l'idTag passato
     *
     * @param idTag
     * @return
     */
    @RequestMapping(value = "countMessageInTag/{idTag}", method = RequestMethod.GET)
    public Long countMessageInTag(@PathVariable(required = true) Integer idTag
    ) {
        return messageTagRespository.count(QMessageTag.messageTag.idTag.id.eq(idTag)
                .and(QMessageTag.messageTag.idMessage.messageFolderList.any().deleted.eq(false)));
    }

    @Transactional(rollbackFor = Throwable.class)
    @RequestMapping(value = "manageMessageRegistration", method = RequestMethod.POST)
    public void manageMessageRegistration(
            @RequestParam(name = "uuidMessage", required = true) String uuidMessage,
            @RequestParam(name = "operation", required = true) InternautaConstants.Shpeck.MessageRegistrationOperation operation,
            @RequestParam(name = "idMessage", required = true) Integer idMessage,
            @RequestBody Map<String, Map<String, Object>> additionalData,
            HttpServletRequest request
    ) throws BlackBoxPermissionException, IOException, Throwable {

        LOG.info("Inizio manageMessageRegistration. uuidMessage: " + uuidMessage + " operation: " + operation + " additionalData: " + additionalData.toString());

        try {
            // operation: IN_REGISTRATION, REGISTER, REMOVE_IN_REGISTRATION
            AuthenticatedSessionData authenticatedUserProperties = authenticatedSessionDataBuilder.getAuthenticatedUserProperties();

            if (additionalData != null) {
                makeAdditionalData(additionalData, authenticatedUserProperties);
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
                        if (KrintUtils.doIHaveToKrint(request)) {
                            krintShpeckService.writeRegistration(message, OperazioneKrint.CodiceOperazione.PEC_MESSAGE_IN_PROTOCOLLAZIONE);
                        }
                        break;
                    case "ADD_REGISTERED":
                        addRegistered(additionalData, message, messageTagRegistered, authenticatedUserProperties, tagRegistered, initialAdditionalDataArrayRegistered, messageTagInRegistration, initialAdditionalDataArrayInRegistration, folderRegistered);
                        if (KrintUtils.doIHaveToKrint(request)) {
                            krintShpeckService.writeRegistration(message, OperazioneKrint.CodiceOperazione.PEC_MESSAGE_PROTOCOLLAZIONE);
                        }

                        break;
                    case "REMOVE_IN_REGISTRATION":
                        removeInRegistration(messageTagInRegistration, initialAdditionalDataArrayInRegistration, additionalData);
                        if (KrintUtils.doIHaveToKrint(request)) {
                            krintShpeckService.writeRegistration(message, OperazioneKrint.CodiceOperazione.PEC_MESSAGE_REMOVE_IN_PROTOCOLLAZIONE);
                        }
                        break;
                    case "REMOVE_REGISTERED":
                        removeRegistered(message, messageTagRegistered, initialAdditionalDataArrayRegistered, authenticatedUserProperties, additionalData);
                        if (KrintUtils.doIHaveToKrint(request)) {
                            krintShpeckService.writeRegistration(message, OperazioneKrint.CodiceOperazione.PEC_MESSAGE_REMOVE_PROTOCOLLAZIONE);
                        }
                    default:
                        throw new Exception("Operation requested not found");
                }
            }
        } catch (Throwable ex) {
            LOG.error(ex.getMessage(), ex);
            throw ex;
        }
    }

    /**
     * Gestisco il dopo archiviazione di un messaggio.La funzione nasce per
 essere chiamata da Babel. Aggiunge il tag archiviazione con le
 informazioni su chi e fascicolo.
     *
     * @param idMessage
     * @param additionalData
     * @param request
     * @throws BlackBoxPermissionException
     */
    @Transactional
    @RequestMapping(value = "manageMessageArchiviation", method = RequestMethod.POST)
    public void manageMessageArchiviation(
            @RequestParam(name = "idMessage", required = true) Integer idMessage,
            @RequestBody Map<String, Object> additionalData,
            HttpServletRequest request) throws BlackBoxPermissionException {

        Message message = messageRepository.getOne(idMessage);
        List<Tag> pecTagList = message.getIdPec().getTagList();
        Tag pecTagArchived = pecTagList.stream().filter(t -> "archived".equals(t.getName())).collect(Collectors.toList()).get(0);

        AuthenticatedSessionData authenticatedUserProperties = authenticatedSessionDataBuilder.getAuthenticatedUserProperties();

        JSONObject jsonAdditionalData = null;

        if (additionalData != null) {
            // Inserisco l'utente fascicolatore
            Map<String, Object> idUtenteMap = new HashMap<>();
            idUtenteMap.put("id", authenticatedUserProperties.getUser().getId());
            idUtenteMap.put("descrizione", authenticatedUserProperties.getPerson().getDescrizione());
            additionalData.put("idUtente", idUtenteMap);
            // Inserisco l'azienda su cui il message è stato fascicolato
            Map<String, Object> idAziendaMap = new HashMap<>();
            idAziendaMap.put("id", authenticatedUserProperties.getUser().getIdAzienda().getId());
            idAziendaMap.put("nome", authenticatedUserProperties.getUser().getIdAzienda().getNome());
            idAziendaMap.put("descrizione", authenticatedUserProperties.getUser().getIdAzienda().getDescrizione());
            additionalData.put("idAzienda", idAziendaMap);
            // Inserisco la data di arichiviazione
            additionalData.put("dataArchiviazione", LocalDateTime.now());
            jsonAdditionalData = new JSONObject(additionalData);
        }

        MessageTag messageTag = null;

        // Cerco se il messageTag esiste già
        List<MessageTag> findByIdMessageAndIdTag = messageTagRespository.findByIdMessageAndIdTag(message, pecTagArchived);

        if (!findByIdMessageAndIdTag.isEmpty()) {   // Il message era già stato archiviato in passato
            messageTag = findByIdMessageAndIdTag.get(0);
            JSONArray jsonArr = new JSONArray(messageTag.getAdditionalData());
            jsonArr.put(jsonAdditionalData);
            messageTag.setAdditionalData(jsonArr.toString());
        } else {
            // Devo creare il message tag e mettere dentro all'additional data il jsonAdditionalData dentro un array
            messageTag = new MessageTag();
            messageTag.setIdUtente(authenticatedUserProperties.getUser());
            messageTag.setIdMessage(message);
            messageTag.setIdTag(pecTagArchived);
            JSONArray jsonArr = new JSONArray();
            jsonArr.put(jsonAdditionalData);
            messageTag.setAdditionalData(jsonArr.toString());
        }

        messageTagRespository.save(messageTag);
        if (KrintUtils.doIHaveToKrint(request)) {
            krintShpeckService.writeArchiviation(message, OperazioneKrint.CodiceOperazione.PEC_MESSAGE_FASCICOLAZIONE, jsonAdditionalData);
        }
    }
    
    @Transactional(rollbackFor = Throwable.class)
    @RequestMapping(value = "deleteMessageTagCustom", method = RequestMethod.POST)
    public void deleteMessageTagCustom(
//            @RequestParam(required = false, name = "additionalData") String additionalData, 
            @RequestBody(required = true) List<Integer> idMessageTagList,
            HttpServletRequest request) throws AbortSaveInterceptorException {

        for (Integer idMessageTag : idMessageTagList) {
            MessageTag messageTagToDelete = messageTagRespository.getOne(idMessageTag);
            Integer idTag = messageTagToDelete.getIdTag().getId();
            Integer idMessage = messageTagToDelete.getIdMessage().getId();
            try {
                messageTagInterceptor.beforeDeleteEntityInterceptor(messageTagToDelete, null, request, true, null);
            } catch (SkipDeleteInterceptorException ex) {
                LOG.warn("delete skipped", ex);
            }
            messageTagRespository.deleteById(idMessageTag);
            Map<String, Integer> data = new HashMap();
            data.put("idTag", idTag);
            data.put("idMessage", idMessage);
            applicationEventPublisher.publishEvent(new ShpeckEvent(ShpeckEvent.Phase.AFTER_DELETE, ShpeckEvent.Operation.SEND_CUSTOM_DELETE_INTIMUS_COMMAND, data));
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
