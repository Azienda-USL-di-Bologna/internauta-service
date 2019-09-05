package it.bologna.ausl.internauta.service.shpeck.utils;

import it.bologna.ausl.eml.handler.EmlHandler;
import it.bologna.ausl.eml.handler.EmlHandlerAttachment;
import it.bologna.ausl.eml.handler.EmlHandlerException;
import it.bologna.ausl.internauta.service.configuration.utils.MongoConnectionManager;
import it.bologna.ausl.internauta.service.controllers.shpeck.ShpeckCustomController;
import it.bologna.ausl.internauta.service.exceptions.BadParamsException;
import it.bologna.ausl.internauta.service.krint.KrintShpeckService;
import it.bologna.ausl.internauta.service.krint.KrintUtils;
import it.bologna.ausl.internauta.service.repositories.baborg.PecRepository;
import it.bologna.ausl.internauta.service.repositories.configurazione.ApplicazioneRepository;
import it.bologna.ausl.internauta.service.repositories.shpeck.DraftRepository;
import it.bologna.ausl.internauta.service.repositories.shpeck.OutboxRepository;
import it.bologna.ausl.model.entities.baborg.Pec;
import it.bologna.ausl.model.entities.configuration.Applicazione;
import it.bologna.ausl.model.entities.shpeck.Draft;
import it.bologna.ausl.model.entities.shpeck.Draft.MessageRelatedType;
import it.bologna.ausl.model.entities.shpeck.Message;
import it.bologna.ausl.model.entities.shpeck.MessageTag;
import it.bologna.ausl.model.entities.shpeck.Outbox;
import it.bologna.ausl.model.entities.shpeck.Tag;
import it.bologna.ausl.model.entities.shpeck.Tag.SystemTagName;
import it.bologna.ausl.mongowrapper.MongoWrapper;
import it.nextsw.common.utils.CommonUtils;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Optional;
import java.util.Properties;
import java.util.UUID;
import javax.mail.Address;
import javax.mail.MessagingException;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.persistence.EntityNotFoundException;
import org.apache.tomcat.util.http.fileupload.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;
import org.springframework.web.multipart.MultipartFile;
import it.bologna.ausl.internauta.service.repositories.shpeck.TagRepository;
import it.bologna.ausl.internauta.service.repositories.shpeck.MessageTagRepository;
import it.bologna.ausl.internauta.service.repositories.shpeck.MessageRepository;
import it.bologna.ausl.model.entities.logs.OperazioneKrint;
import java.util.List;
import javax.mail.Message.RecipientType;
import javax.servlet.http.HttpServletRequest;

/**
 *
 * @author Giuseppe Russo <g.russo@nsi.it>
 */
@Component
public class ShpeckUtils {

    private static final Logger LOG = LoggerFactory.getLogger(ShpeckCustomController.class);

    @Autowired
    private CommonUtils nextSdrCommonUtils;

    @Autowired
    private MongoConnectionManager mongoConnectionManager;

    @Autowired
    ShpeckCacheableFunctions shpeckCacheableFunctions;

    @Autowired
    private DraftRepository draftRepository;

    @Autowired
    private OutboxRepository outboxRepository;

    @Autowired
    private PecRepository pecRepository;

    @Autowired
    private MessageRepository messageRepository;

    @Autowired
    private MessageTagRepository messageTagRepository;

    @Autowired
    private TagRepository tagRepository;

    @Autowired
    private ApplicazioneRepository applicazioneRepository;
    
    @Autowired
    private KrintShpeckService krintShpeckService;

    /**
     * usato da {@link #downloadEml(EmlSource, Integer)} per reperire l'eml
     * dalla sorgente giusta
     */
    public static enum EmlSource {
        DRAFT,
        OUTBOX,
        MESSAGE
    }

    public MimeMessage buildMimeMessage(String from, String[] to, String[] cc, String body, String subject,
            ArrayList<EmlHandlerAttachment> listAttachments, ArrayList<EmlHandlerAttachment> emlAttachments,
            String hostname, Draft draftMessage) throws AddressException, IOException, MessagingException, EmlHandlerException, BadParamsException {
        LOG.info("Creating the sender address...");
        Address fromAddress = new InternetAddress(from);

        LOG.info("Creating destination's addresses array");
        Address toAddresses[] = null;
        if (to != null) {
            toAddresses = new Address[to.length];
            for (int i = 0; i < to.length; i++) {
                toAddresses[i] = new InternetAddress(to[i]);
            }
        }
        LOG.info("Creating carbon copy's addresses array");
        Address ccAddresses[] = null;
        if (cc != null) {
            ccAddresses = new Address[cc.length];
            for (int i = 0; i < cc.length; i++) {
                ccAddresses[i] = new InternetAddress(cc[i]);
            }
        }
        // Copia degli allegati per evitare che vengano duplicati in caso di DestinatariPrivati
        ArrayList<EmlHandlerAttachment> listAttachmentsTemp = new ArrayList<>(listAttachments);
        if (!emlAttachments.isEmpty()) {
            listAttachmentsTemp.addAll(emlAttachments);
        }

        LOG.info("Fields ready, building mime message...");
        Properties props = null;
        if (hostname.equals("localhost")) {
            props = new Properties();
            props.put("mail.host", "localhost");
        }
        MimeMessage mimeMessage = null;
        try {
            mimeMessage = EmlHandler.buildDraftMessage(body, subject, fromAddress, toAddresses, ccAddresses, listAttachmentsTemp, props);
        } catch (MessagingException ex) {
            LOG.error("Errore while generating the mimemessage", ex);
            throw new MessagingException("Errore while generating the mimemessage", ex);
        }
        return mimeMessage;
    }

    /**
     * Salva il draftMessage
     *
     * @param draftMessage Il draft da salvare
     * @param pec La casella Pec mittente
     * @param subject L'oggetto della mail
     * @param to Array dei destinatari in formato stringa
     * @param cc Array dei cc in formato string
     * @param hideRecipients Booleano per i destinatari privati
     * @param listAttachments Lista degli allegati in formato
     * EmlHandlerAttachment
     * @param body Il body della mail
     * @param mimeMessage Il MimeMessage
     * @param idMessageRelated L'id del messaggio correlato
     * @param messageRelatedType Tipo di relazione
     * @param emlAttachments
     * @throws MessagingException
     * @throws IOException
     */
    public void saveDraft(Draft draftMessage, Pec pec, String subject, String[] to, String[] cc,
            Boolean hideRecipients, ArrayList<EmlHandlerAttachment> listAttachments, String body,
            MimeMessage mimeMessage, Integer idMessageRelated, MessageRelatedType messageRelatedType,
            ArrayList<EmlHandlerAttachment> emlAttachments, HttpServletRequest request) throws MessagingException, IOException {

        try {
            draftMessage.setIdPec(pec);
            draftMessage.setSubject(subject);
            draftMessage.setToAddresses(to);
            draftMessage.setCcAddresses(cc);
            draftMessage.setHiddenRecipients(hideRecipients);
//            draftMessage.setCreateTime(LocalDateTime.now());
            draftMessage.setUpdateTime(LocalDateTime.now());
            LOG.info("Write attachments as bytearrayOutputStream...");
            ArrayList<EmlHandlerAttachment> listTemp = new ArrayList<>(listAttachments);
            if (!emlAttachments.isEmpty()) {
                listTemp.addAll(emlAttachments);
            }
            draftMessage.setAttachmentsNumber(listTemp.size());
            draftMessage.setAttachmentsName(listTemp.stream()
                    .map(EmlHandlerAttachment::getFileName).toArray(size -> new String[size]));
            LOG.info("Attachments converted!");
            LOG.info("Write body...");
            draftMessage.setBody(body);
            LOG.info("Body wrote!");
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            LOG.info("Write mimemessage to baos...");
            mimeMessage.writeTo(baos);
            baos.close();
            LOG.info("Message baos complete!");
            draftMessage.setEml(baos.toByteArray());
            LOG.info("Message setted!");
            if (idMessageRelated != null) {
                LOG.info("Find Message...");
                Message messageRelated = messageRepository.getOne(idMessageRelated);
                LOG.info("Message found!");
                draftMessage.setIdMessageRelated(messageRelated);
                draftMessage.setMessageRelatedType(messageRelatedType);
            }
            LOG.info("Message ready. Saving...");
            draftMessage = draftRepository.save(draftMessage);
            if (KrintUtils.doIHaveToKrint(request)) {
                krintShpeckService.writeDraft(draftMessage, OperazioneKrint.CodiceOperazione.PEC_DRAFT_MODIFICA);
            }
        } catch (IOException ex) {
            LOG.error("Error while saving message");
            throw new IOException("Error while saving message", ex);
        } catch (EntityNotFoundException ex) {
            LOG.error("Element not found!", ex);
            throw new EntityNotFoundException("Element not found!");
        } finally {
            LOG.info("Draft message saved: {}", draftMessage);
        }
    }

    /**
     * Elimina una bozza
     *
     * @param draftMessage La bozza da eliminare
     */
    public void deleteDraft(Draft draftMessage) {
        LOG.info("Deleting draft message with id: {}", draftMessage.getId());
        draftRepository.delete(draftMessage);
        LOG.info("Draft deleted.", draftMessage);
    }

    /**
     * Invia il messaggio allo shpeck
     *
     * @param pec La casella Pec mittente
     * @param subject
     * @param hiddenRecipients
     * @param mimeMessage Il MimeMessage da inviare
     * @param listAttachments
     * @param emlAttachments
     * @param body
     * @throws MessagingException
     * @throws IOException
     */
    public void sendMessage(Pec pec, String subject, Boolean hiddenRecipients, String body,
            ArrayList<EmlHandlerAttachment> listAttachments, ArrayList<EmlHandlerAttachment> emlAttachments,
            MimeMessage mimeMessage, HttpServletRequest request) throws IOException, MessagingException {
        Outbox outboxMessage = new Outbox();
        Applicazione shpeckApp = applicazioneRepository.getOne("shpeck");
        try {
            outboxMessage.setIdPec(pec);
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            mimeMessage.writeTo(output);
            String rawEmail = output.toString();
            outboxMessage.setRawData(rawEmail);
            outboxMessage.setIdApplicazione(shpeckApp);

            outboxMessage.setSubject(subject);
            outboxMessage.setHiddenRecipients(hiddenRecipients);
            outboxMessage.setUpdateTime(LocalDateTime.now());
            ArrayList<EmlHandlerAttachment> listTemp = new ArrayList<>(listAttachments);
            if (!emlAttachments.isEmpty()) {
                listTemp.addAll(emlAttachments);
            }
            outboxMessage.setAttachmentsName(listTemp.stream().map(EmlHandlerAttachment::getFileName).toArray(size -> new String[size]));
            outboxMessage.setAttachmentsNumber(listTemp.size());
            outboxMessage.setBody(body);

            Address[] toRecipients = mimeMessage.getRecipients(RecipientType.TO);
            String[] toAddress = new String[toRecipients.length];
            for (int i = 0; i < toRecipients.length; i++) {
                toAddress[i] = toRecipients[i].toString();
            }
            outboxMessage.setToAddresses(toAddress);

            Address[] ccRecipients = mimeMessage.getRecipients(RecipientType.CC);
            if (ccRecipients != null && ccRecipients.length > 0) {
                String[] ccAddress = new String[ccRecipients.length];
                for (int c = 0; c < ccRecipients.length; c++) {
                    ccAddress[c] = ccRecipients[c].toString();
                }
                outboxMessage.setCcAddresses(ccAddress);
            }

            outboxMessage = outboxRepository.save(outboxMessage);
            if (KrintUtils.doIHaveToKrint(request)) {
                krintShpeckService.writeOutboxMessage(outboxMessage, OperazioneKrint.CodiceOperazione.PEC_MESSAGE_INVIO_NUOVA_MAIL);
            }
        } catch (EntityNotFoundException ex) {
            LOG.error("Element not found!", ex);
            throw new EntityNotFoundException("Element not found!");
        } catch (IOException | MessagingException ex) {
            LOG.error("Error while sending message! Trying to save the draf instead", ex);
            throw new IOException("Error while sending message! Trying to save the draf instead");
        } finally {
            LOG.info("Message enqueued to outbox : ", outboxMessage);
        }
    }

    static Specification<Tag> hasAuthor(Integer idPec) {
        return (tag, cq, cb) -> cb.equal(tag.get("idPec"), idPec);
    }

    /**
     * Converte gli attachments Multipart provenienti dal client in
     * EmlHandlerAttachment
     *
     * @param attachments Array degli attachments
     * @return Lista di EmlHandlerAttachment
     * @throws IOException
     */
    public ArrayList<EmlHandlerAttachment> convertAttachments(MultipartFile[] attachments) throws IOException {
        ArrayList<EmlHandlerAttachment> listAttachments = new ArrayList<>();
        if (attachments != null) {
            for (MultipartFile attachment : attachments) {
                EmlHandlerAttachment file = new EmlHandlerAttachment();
                file.setFileName(attachment.getOriginalFilename());
                String contentType = attachment.getContentType();
                // Se il content type ha le virgolette a destra e sinistra gliele tolgo.
                if (contentType != null && contentType.substring(0, 1).equals("\"") && contentType.substring(contentType.length() - 1).equals("\"")) {
                    contentType = contentType.substring(1, contentType.length() - 1);
                }
                file.setMimeType(contentType);
                file.setFileBytes(attachment.getBytes());
                listAttachments.add(file);
            }
        }
        return listAttachments;
    }

    /**
     * Inserisce i tag alla mail originale passato in ingresso
     *
     * @param pec La pec che sta inviado la mail
     * @param idMessageRelated Id del messaggio a cui si sta rispondendo o
     * inoltrando
     * @param messageRelatedType Tipo di relazione del messaggio relazionato
     */
    public void setTagsToMessage(Pec pec, Integer idMessageRelated, MessageRelatedType messageRelatedType, HttpServletRequest request) {
        LOG.info("Getting message...");
        Message messageRelated = messageRepository.getOne(idMessageRelated);
        Tag tag = new Tag();
        
        OperazioneKrint.CodiceOperazione operazione = null;
        
        LOG.info("Getting tag to apply...");
        switch (messageRelatedType) {
            case REPLIED:
                tag = tagRepository.findByidPecAndName(pec, SystemTagName.replied.toString());
                operazione = OperazioneKrint.CodiceOperazione.PEC_MESSAGE_RISPOSTA;
                break;
            case REPLIED_ALL:
                tag = tagRepository.findByidPecAndName(pec, SystemTagName.replied_all.toString());
                operazione = OperazioneKrint.CodiceOperazione.PEC_MESSAGE_RISPOSTA_A_TUTTI;
                break;
            case FORWARDED:
                tag = tagRepository.findByidPecAndName(pec, SystemTagName.forwarded.toString());
                operazione = OperazioneKrint.CodiceOperazione.PEC_MESSAGE_INOLTRO;
                break;
        }
        LOG.info("Check if tag is already present");
        List<MessageTag> findByIdMessageAndIdTag = messageTagRepository.findByIdMessageAndIdTag(messageRelated, tag);
        if (findByIdMessageAndIdTag.isEmpty()) {
            LOG.info("Applying tag: {} to message with id: {}", tag.getName(), messageRelated.getId());
            MessageTag messageTag = new MessageTag();
            messageTag.setIdMessage(messageRelated);
            messageTag.setIdTag(tag);
            messageTag.setInserted(LocalDateTime.now());
            messageTagRepository.save(messageTag);
            if (KrintUtils.doIHaveToKrint(request)) {
                krintShpeckService.writeReplyToMessage(messageRelated, operazione);
            }
            LOG.info("Tag applied!");
        } else {
            LOG.info("Tag already present, skip applying!");
        }
    }

    /**
     *
     * @param emlSource
     * @param id
     * @return
     * @throws BadParamsException
     */
    public File downloadEml(EmlSource emlSource, Integer id) throws BadParamsException, FileNotFoundException, IOException {

        if (emlSource == null) {
            throw new BadParamsException("emlSource non definito");
        }

        String fileName = String.format("%s_%d_%s.eml", emlSource.toString(), id, UUID.randomUUID().toString());
        File emlFile = new File(System.getProperty("java.io.tmpdir"), fileName);
        System.out.println(emlFile.getAbsolutePath());
        switch (emlSource) {
            case DRAFT:
                Optional<Draft> draftOp = draftRepository.findById(id);
                if (!draftOp.isPresent()) {
                    throw new BadParamsException(String.format("bozza %d non trovata", id));
                } else {
                    Draft draft = draftOp.get();
                    if (draft.getEml() == null) {
                        emlFile = null;
                    } else {
                        try (DataOutputStream dataOs = new DataOutputStream(new FileOutputStream(emlFile))) {
                            dataOs.write(draft.getEml());
                        }
                    }
                }
                break;
            case OUTBOX:
                Optional<Outbox> outboxOp = outboxRepository.findById(id);
                if (!outboxOp.isPresent()) {
                    throw new BadParamsException(String.format("messaggio in Uscita %d non trovato", id));
                } else {
                    Outbox outbox = outboxOp.get();
                    try (DataOutputStream dataOs = new DataOutputStream(new FileOutputStream(emlFile))) {
                        dataOs.writeBytes(outbox.getRawData());
                    }
                }
                break;
            case MESSAGE:
                Optional<Message> messageOp = messageRepository.findById(id);
                if (!messageOp.isPresent()) {
                    throw new BadParamsException(String.format("messaggio %d non trovato", id));
                } else {
                    Message message = messageOp.get();
                    MongoWrapper mongoWrapper = mongoConnectionManager.getConnection(this.getIdAziendaRepository(message));
                    InputStream is = null;
                    try (DataOutputStream dataOs = new DataOutputStream(new FileOutputStream(emlFile))) {
                        is = mongoWrapper.get(message.getUuidRepository());
                        StreamUtils.copy(is, dataOs);
                    } finally {
                        IOUtils.closeQuietly(is);
                    }
                }
                break;
        }
        return emlFile;
    }

    /**
     * Estra gli allegati dall'eml di una bozza oppure da un messaggio sul
     * repository se la relazione è Inoltra
     *
     * @param draftMessage La bozza dal quale estrarre gli allegati
     * @param idMessageRelated L'id del messaggio correlato salvato sul
     * repository
     * @param messageRelatedType Tipo di relazione del messaggio
     * @param idMessageRelatedAttachments Array con gli id degli allegati da
     * estrarre dall'eml del repository
     * @return La lista degli allegati estratti
     * @throws BadParamsException
     * @throws MessagingException
     * @throws EmlHandlerException
     * @throws IOException
     */
    public ArrayList<EmlHandlerAttachment> getEmlAttachments(Draft draftMessage, Integer idMessageRelated, MessageRelatedType messageRelatedType,
            Integer[] idMessageRelatedAttachments) throws BadParamsException, MessagingException, EmlHandlerException, IOException {
        ArrayList<EmlHandlerAttachment> listAttachments = new ArrayList<>();
        if (idMessageRelated != null) {
            if (messageRelatedType != null
                    && messageRelatedType.equals(MessageRelatedType.FORWARDED)) {

                File downloadEml = this.downloadEml(EmlSource.MESSAGE, idMessageRelated);
                try {
                    listAttachments = EmlHandler.getListAttachments(downloadEml.getAbsolutePath(), null, idMessageRelatedAttachments);
                } catch (EmlHandlerException ex) {
                    LOG.error("Error while retrieving the attachments from messaged forwarded. ", ex);
                    throw new EmlHandlerException("Error while retrieving the attachments from messaged forwarded.");
                }
            }
        } else if (idMessageRelatedAttachments != null && idMessageRelatedAttachments.length > 0) {
            byte[] eml = draftMessage.getEml();
            listAttachments = EmlHandler.getListAttachments(null, eml, idMessageRelatedAttachments);
        }
        return listAttachments;
    }

    private Integer getIdAziendaRepository(Message message) {
        boolean isMessageReaddressed = message.getMessageTagList().stream().anyMatch(messageTag -> messageTag.getIdTag().getName().equals(Tag.SystemTagName.readdressed_in.toString()));
        if (!isMessageReaddressed) {
            return message.getIdPec().getIdAziendaRepository().getId();
        } else {
            return this.messageRepository.getIdAziendaRepository(message.getId());
        }
    }
}
