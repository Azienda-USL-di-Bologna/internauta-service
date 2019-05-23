package it.bologna.ausl.internauta.service.shpeck.utils;

import it.bologna.ausl.eml.handler.EmlHandler;
import it.bologna.ausl.eml.handler.EmlHandlerAttachment;
import it.bologna.ausl.eml.handler.EmlHandlerException;
import it.bologna.ausl.internauta.service.configuration.utils.MongoConnectionManager;
import it.bologna.ausl.internauta.service.controllers.shpeck.ShpeckCustomController;
import it.bologna.ausl.internauta.service.exceptions.BadParamsException;
import it.bologna.ausl.internauta.service.repositories.baborg.PecRepository;
import it.bologna.ausl.internauta.service.repositories.configurazione.ApplicazioneRepository;
import it.bologna.ausl.internauta.service.repositories.shpeck.DraftRepository;
import it.bologna.ausl.internauta.service.repositories.shpeck.MessageRespository;
import it.bologna.ausl.internauta.service.repositories.shpeck.MessageTagRespository;
import it.bologna.ausl.internauta.service.repositories.shpeck.OutboxRepository;
import it.bologna.ausl.internauta.service.repositories.shpeck.TagRespository;
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
import javax.mail.Address;
import javax.mail.MessagingException;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.persistence.EntityNotFoundException;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

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
    private DraftRepository draftRepository;
        
    @Autowired
    private OutboxRepository outboxRepository;
    
    @Autowired
    private PecRepository pecRepository;
    
    @Autowired 
    private MessageRespository messageRepository;
    
    @Autowired
    private MessageTagRespository messageTagRepository;
    
    @Autowired
    private TagRespository tagRepository;
    
    @Autowired
    private ApplicazioneRepository applicazioneRepository;
    
    /**
     * usato da {@link #downloadEml(EmlSource, Integer)} per reperire l'eml dalla sorgente giusta
     */
    public static enum EmlSource {
        DRAFT,
        OUTBOX,
        MESSAGE
    }
    
    
    public MimeMessage buildMimeMessage(String from, String[] to, String[] cc, String body, String subject,
        ArrayList<EmlHandlerAttachment> listAttachments, Integer idMessageRelated, MessageRelatedType messageRelatedType,
        Integer[] idMessageRelatedAttachments, String hostname) throws AddressException, IOException, MessagingException, EmlHandlerException {
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
        
        if (idMessageRelated != null) {
            if (messageRelatedType != null && 
                messageRelatedType.equals(MessageRelatedType.FORWARDED)) {
                System.out.println("hostanme " + hostname);
                String repositoryTemp = null;
                if (hostname.equals("localhost")) {
                    repositoryTemp = "C:\\Users\\Public\\prova";
                } else {
                    repositoryTemp = "/tmp/emlProveShpeckUI/prova";
                }
                try {
                    ArrayList<EmlHandlerAttachment> emls = 
                            EmlHandler.getListAttachments(repositoryTemp + idMessageRelated + ".eml", idMessageRelatedAttachments);
                    listAttachments.addAll(emls);
                } catch (EmlHandlerException ex) {
                    LOG.error("Error while retrieving the attachments from messaged forwarded. ", ex);
                    throw new EmlHandlerException("Error while retrieving the attachments from messaged forwarded.");
                }
            }
        }
        
        LOG.info("Fields ready, building mime message...");
        Properties props = null;
        if (hostname.equals("localhost")) {
            props = new Properties();
            props.put("mail.host", "localhost");
        }
        MimeMessage mimeMessage = null;
        try {
            mimeMessage = EmlHandler.buildDraftMessage(body, subject, fromAddress, toAddresses, ccAddresses, listAttachments, props);        
        } catch (MessagingException ex) {
            LOG.error("Errore while generating the mimemessage", ex);
            throw new MessagingException("Errore while generating the mimemessage", ex);
        }
        return mimeMessage;
    }
    
    /**
     * Salva il draftMessage
     * @param draftMessage Il draft da salvare
     * @param pec La casella Pec mittente
     * @param subject L'oggetto della mail
     * @param to Array dei destinatari in formato stringa
     * @param cc Array dei cc in formato string
     * @param hideRecipients Booleano per i destinatari privati
     * @param listAttachments Lista degli allegati in formato EmlHandlerAttachment
     * @param body Il body della mail
     * @param mimeMessage Il MimeMessage
     * @param idMessageRelated L'id del messaggio correlato
     * @param messageRelatedType Tipo di relazione
     * @throws MessagingException
     * @throws IOException
     */
    public void saveDraft(Draft draftMessage, Pec pec, String subject, String[] to, String[] cc,
            Boolean hideRecipients, ArrayList<EmlHandlerAttachment> listAttachments, String body,
            MimeMessage mimeMessage, Integer idMessageRelated, MessageRelatedType messageRelatedType) throws MessagingException, IOException {
        
        try {
            draftMessage.setIdPec(pec);
            draftMessage.setSubject(subject);
            draftMessage.setToAddresses(to);
            draftMessage.setCcAddresses(cc);
            draftMessage.setHiddenRecipients(hideRecipients);
//            draftMessage.setCreateTime(LocalDateTime.now());
            draftMessage.setUpdateTime(LocalDateTime.now());
            LOG.info("Write attachments as bytearrayOutputStream...");
            draftMessage.setAttachmentsNumber(listAttachments != null ? listAttachments.size() : 0);
            draftMessage.setAttachmentsName(listAttachments != null ? listAttachments.stream()
                    .map(EmlHandlerAttachment::getFileName).toArray(size -> new String[size]) : new String[0]);
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
     * @param draftMessage La bozza da eliminare
     */
    public void deleteDraft(Draft draftMessage) {
        LOG.info("Deleting draft message with id: {}", draftMessage.getId());
        draftRepository.delete(draftMessage);
        LOG.info("Draft deleted.", draftMessage);
    }
    
    /**
     * Invia il messaggio allo shpeck
     * @param pec La casella Pec mittente
     * @param mimeMessage Il MimeMessage da inviare
     * @throws MessagingException
     * @throws IOException
     */
    public void sendMessage(Pec pec, MimeMessage mimeMessage) throws IOException, MessagingException {
        Outbox outboxMessage = new Outbox();
        Applicazione shpeckApp = applicazioneRepository.getOne("shpeck");
        try {
            outboxMessage.setIdPec(pec);
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            mimeMessage.writeTo(output);
            String rawEmail = output.toString();
            outboxMessage.setRawData(rawEmail);
            outboxMessage.setIdApplicazione(shpeckApp);
            outboxMessage = outboxRepository.save(outboxMessage);   
        } catch (EntityNotFoundException ex) {
            LOG.error("Element not found!", ex);
            throw new EntityNotFoundException("Element not found!");
        } catch (IOException | MessagingException ex) {
            LOG.error("Error while sending message! Trying to save the draf instead", ex);
            throw new IOException("Error while sending message! Trying to save the draf instead");
        }
        finally {
            LOG.info("Message enqueued to outbox : ", outboxMessage);
        } 
    }
    
    static Specification<Tag> hasAuthor(Integer idPec) {
        return (tag, cq, cb) -> cb.equal(tag.get("idPec"), idPec);
    }
    
    /**
     * Converte gli attachments Multipart provenienti dal client in EmlHandlerAttachment
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
                file.setMimeType(attachment.getContentType());
                file.setFileBytes(attachment.getBytes());
                listAttachments.add(file);
            }
        }
        return listAttachments;
    }
    
    /**
     * Inserisce i tag alla mail originale passato in ingresso
     * @param pec La pec che sta inviado la mail
     * @param idMessageRelated Id del messaggio a cui si sta rispondendo o inoltrando
     * @param messageRelatedType Tipo di relazione del messaggio relazionato
     */
    public void setTagsToMessage(Pec pec, Integer idMessageRelated, MessageRelatedType messageRelatedType) {
        LOG.info("Getting message...");
        Message messageRelated = messageRepository.getOne(idMessageRelated);
        Tag tag = new Tag();
        
        LOG.info("Getting tag to apply...");
        switch (messageRelatedType) {
            case REPLIED:
                tag = tagRepository.findByidPecAndName(pec, SystemTagName.replied.toString());
                break;
            case REPLIED_ALL:
                tag = tagRepository.findByidPecAndName(pec, SystemTagName.replied_all.toString());
                break;
            case FORWARDED:
                tag = tagRepository.findByidPecAndName(pec, SystemTagName.forwarded.toString());
                break;
        }
        LOG.info("Applying tag: {} to message with id: {}", tag.getName(), messageRelated.getId());
        MessageTag messageTag = new MessageTag();
        messageTag.setIdMessage(messageRelated);
        messageTag.setIdTag(tag);
        messageTag.setInserted(LocalDateTime.now());
        messageTagRepository.save(messageTag);
        LOG.info("Tag applied!");
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
        String fileName = String.format("%s_%d.eml", emlSource.toString(), id);
        File emlFile = new File(System.getProperty("java.io.tmpdir"), fileName);
        switch (emlSource) {
            case DRAFT:
                Optional<Draft> draftOp = draftRepository.findById( id );
                if (draftOp.isEmpty()){
                    throw new BadParamsException(String.format("bozza %d non trovata", id));
                } else {
                    Draft draft = draftOp.get();
                    try (DataOutputStream dataOs = new DataOutputStream(new FileOutputStream(emlFile))){
                        dataOs.write(draft.getEml());
                    }
                }
                break;
            case OUTBOX:
                Optional<Outbox> outboxOp = outboxRepository.findById(id);
                if (outboxOp.isEmpty()){
                    throw new BadParamsException(String.format("messaggio in Uscita %d non trovato", id));
                } else {
                    Outbox outbox = outboxOp.get();
                    try (DataOutputStream dataOs = new DataOutputStream(new FileOutputStream(emlFile))){
                        dataOs.writeBytes(outbox.getRawData());
                    }
                }
                break;
            case MESSAGE:
                Optional<Message> messageOp = messageRepository.findById(id);
                if (messageOp.isEmpty()){
                    throw new BadParamsException(String.format("messaggio %d non trovato", id));
                } else {
                    Message message = messageOp.get();
                    MongoWrapper mongoWrapper = mongoConnectionManager.getConnection(message.getIdPec().getIdAziendaRepository().getId());
                    InputStream is = null;
                    try (DataOutputStream dataOs = new DataOutputStream(new FileOutputStream(emlFile))){
                        is = mongoWrapper.get(message.getUuidRepository());
                        IOUtils.copy(is, dataOs);
                    }
                    finally {
                        IOUtils.closeQuietly(is);
                    }
                }
                break;
        }
        return emlFile;
    }
}
