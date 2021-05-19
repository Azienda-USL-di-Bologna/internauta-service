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
import it.bologna.ausl.internauta.service.repositories.shpeck.FolderRepository;
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
import it.bologna.ausl.internauta.service.repositories.shpeck.TagRepository;
import it.bologna.ausl.internauta.service.repositories.shpeck.MessageTagRepository;
import it.bologna.ausl.internauta.service.repositories.shpeck.MessageRepository;
import it.bologna.ausl.internauta.service.repositories.shpeck.MessageFolderRepository;
import it.bologna.ausl.internauta.service.repositories.shpeck.OutboxLiteRepository;
import it.bologna.ausl.internauta.service.shpeck.utils.ManageMessageRegistrationUtils;
import it.bologna.ausl.internauta.service.utils.CachedEntities;
import it.bologna.ausl.internauta.service.utils.InternautaConstants;
import it.bologna.ausl.internauta.service.utils.aggiustatori.messagetaginregistrationfixer.managers.MessagesTagsProtocollazioneFixManager;
import it.bologna.ausl.model.entities.logs.OperazioneKrint;
import it.bologna.ausl.model.entities.shpeck.QDraft;
import it.bologna.ausl.model.entities.shpeck.QMessage;
import it.bologna.ausl.model.entities.shpeck.views.QOutboxLite;
import it.nextsw.common.interceptors.exceptions.AbortSaveInterceptorException;
import it.nextsw.common.interceptors.exceptions.SkipDeleteInterceptorException;
import java.util.Arrays;
import java.util.function.Predicate;
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
    private TagRepository tagRepository;

    @Autowired
    private MessageTagRepository messageTagRespository;

    @Autowired
    private MessageFolderRepository messageFolderRespository;

    @Autowired
    private FolderRepository folderRepository;
    
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

    @Autowired
    MessagesTagsProtocollazioneFixManager messagesTagsProtocollazioneFixManager;
    
    @Autowired
    ManageMessageRegistrationUtils manageMessageRegistrationUtils;

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
//            httpSessionData.putData(InternautaConstants.HttpSessionData.Keys.test, "gdml");
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
            } catch (Exception ex) {
                LOG.error("errore nello scaricamento del file eml", ex);
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
            @RequestParam(name = "idDraftMessage", required = false) Integer idDraftMessage,
            @RequestParam(name = "idPec", required = false) Integer idPec,
            @RequestParam(name = "body", required = false) String body,
            @RequestParam(name = "hideRecipients", required = false) Boolean hideRecipients,
            @RequestParam(name = "subject", required = false) String subject,
            @RequestParam(name = "to", required = false) String[] to,
            @RequestParam(name = "cc", required = false) String[] cc,
            @RequestParam(name = "attachments", required = false) MultipartFile[] attachments,
            @RequestParam(name = "idMessageRelated", required = false) Integer idMessageRelated,
            @RequestParam(name = "messageRelatedType", required = false) MessageRelatedType messageRelatedType,
            @RequestParam(name = "idMessageRelatedAttachments", required = false) Integer[] idMessageRelatedAttachments,
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
            HttpServletRequest request) throws CloneNotSupportedException, Http409ResponseException, JsonProcessingException {
        // la funzione è disponibile solo se il messaggio non è stato già reindirizzato
        // recupero message sorgente
        Message messageSource = messageRepository.getOne(idMessageSource);
        List<MessageTag> messageTagListSource = messageSource.getMessageTagList();
        List<Integer> idAziendePec = messageSource.getIdPec().getPecAziendaList().stream().map(pa -> pa.getIdAzienda().getId()).collect(Collectors.toList());

        for (MessageTag mt : messageTagListSource) {
            if (mt.getIdTag().getName().equals(Tag.SystemTagName.readdressed_out.toString())) {
                throw new Http409ResponseException("1", "il messaggio è gia stato reindirizzato.");
            } else if (mt.getIdTag().getName().equals(Tag.SystemTagName.registered.toString())) {
                ArrayList<HashMap> additionalData = objectMapper.readValue(mt.getAdditionalData(), ArrayList.class);
                for (HashMap hashMap : additionalData) {
                    HashMap obj = (HashMap) hashMap.get("idAzienda");
                    int idAzienda = (int) obj.get("id");
                    if (idAziendePec.contains(idAzienda)) {
                        throw new Http409ResponseException("2", "il messaggio è stato protocollato.");
                    }
                }
            } else if (mt.getIdTag().getName().equals(Tag.SystemTagName.in_registration.toString())) {
                ArrayList<HashMap> additionalData = objectMapper.readValue(mt.getAdditionalData(), ArrayList.class);
                for (HashMap hashMap : additionalData) {
                    HashMap obj = (HashMap) hashMap.get("idAzienda");
                    int idAzienda = (int) obj.get("id");
                    if (idAziendePec.contains(idAzienda)) {
                        throw new Http409ResponseException("3", "il messaggio è in protocollazione.");
                    }
                }
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
     * @param folderType
     * @param idPec
     * @return
     */
    @RequestMapping(value = "countMessageInFolder/{idFolder}", method = RequestMethod.GET)
    public Long countMessageInFolder(
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
                    if (unSeen) {
                        // Il campo unread messages continene i non letti e non delated
                        return new Long(folderRepository.getOne(idFolder).getUnreadMessages());
                    } else {
                        filter = QMessageFolder.messageFolder.idFolder.id.eq(idFolder).and(QMessageFolder.messageFolder.deleted.eq(false));
                        return messageFolderRespository.count(filter);
                    }
                    
            }
        }

        if (unSeen) {
            // Il campo unread messages continene i non letti e non delated
            return new Long(folderRepository.getOne(idFolder).getUnreadMessages());
        } else {
            filter = QMessageFolder.messageFolder.idFolder.id.eq(idFolder).and(QMessageFolder.messageFolder.deleted.eq(false));
            return messageFolderRespository.count(filter);
        }
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

    /**
     * Chiama il metodo per il fix degli additionalData del MessagesTag
     * "InRegistration".
     *
     * @param idMessage L'id del messaggio di cui recuperare il MessageTag
     * @return String Un json di risposta
     */
    @RequestMapping(value = "fixMessageTagInRegistration/{idMessage}", method = RequestMethod.GET)
    public String fixMessageTagInRegistration(@PathVariable(required = true) Integer idMessage) throws Throwable {
        LOG.info("Ho chiamato la funzione per aggiustare il MessageTag di "
                + "message con id {} ...", idMessage);
        JSONObject risposta = new JSONObject();
        try {
            Message message = messageRepository.findById(idMessage).get();
            LOG.info("Trovato messagggio: uuidMessage {}", message.getUuidMessage());
            JSONArray fixedData = messagesTagsProtocollazioneFixManager.fixDatiProtocollazioneMessaggio(message);

            String fixedDataString = "Fixed Data: ";
            if (fixedData != null) {
                fixedDataString += fixedData.toString(4);
            } else {
                fixedDataString += "NO DATA FIXED";
            }
            String responseString = "Tutto ok - " + fixedDataString;
            risposta.put("Response", responseString);
        } catch (Throwable t) {
            t.printStackTrace();
            risposta.put("Response", "PROBLEMI: " + t.getMessage());
        }
        return risposta.toString();
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

        LOG.info("Dentro controller manageMessageRegistration");

        Boolean doIHaveToKrint = KrintUtils.doIHaveToKrint(request);
        
        manageMessageRegistrationUtils.manageMessageRegistration(
            uuidMessage, operation, idMessage, additionalData, doIHaveToKrint, null
        );
    }

    /**
     * Gestisco il dopo archiviazione di un messaggio.La funzione nasce per
     * essere chiamata da Babel. Aggiunge il tag archiviazione con le
     * informazioni su chi e fascicolo.
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

    
}
