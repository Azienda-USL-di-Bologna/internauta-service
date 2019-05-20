package it.bologna.ausl.internauta.service.controllers.shpeck;

import it.bologna.ausl.eml.handler.EmlHandler;
import it.bologna.ausl.eml.handler.EmlHandlerException;
import it.bologna.ausl.eml.handler.EmlHandlerAttachment;
import it.bologna.ausl.internauta.service.exceptions.ControllerHandledExceptions;
import it.bologna.ausl.internauta.service.exceptions.Http500ResponseException;
import it.bologna.ausl.internauta.service.repositories.baborg.PecRepository;
import it.bologna.ausl.internauta.service.repositories.shpeck.DraftRepository;
import it.bologna.ausl.internauta.service.shpeck.utils.ShpeckCacheableFunctions;
import it.bologna.ausl.internauta.service.shpeck.utils.ShpeckUtils;
import it.bologna.ausl.model.entities.baborg.Pec;
import it.bologna.ausl.model.entities.shpeck.Draft;
import it.bologna.ausl.model.entities.shpeck.Draft.MessageRelatedType;
import it.nextsw.common.utils.CommonUtils;
import java.io.BufferedOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.util.ArrayList;
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
import java.util.Objects;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipOutputStream;
import javax.mail.MessagingException;
import javax.mail.internet.AddressException;
import javax.mail.internet.MimeMessage;
import javax.persistence.EntityNotFoundException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

/**
 *
 * @author gusgus
 */
@RestController
@RequestMapping(value = "${shpeck.mapping.url.root}")
public class ShpeckCustomController implements ControllerHandledExceptions{

    private static final Logger LOG = LoggerFactory.getLogger(ShpeckCustomController.class);
    
    @Autowired
    private ShpeckUtils shpeckUtils;
    
    @Autowired
    private CommonUtils nextSdrCommonUtils;
    
    @Autowired
    private PecRepository pecRepository;
    
    @Autowired
    private DraftRepository draftRepository;
    /**
     *
     * @param idMessage
     * @param request
     * @return
     * @throws EmlHandlerException 
     * @throws java.io.UnsupportedEncodingException 
     */
    @RequestMapping(value = "extractEmlData/{idMessage}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> extractEmlData(
            @PathVariable(required = true) Integer idMessage,
            HttpServletRequest request
        ) throws EmlHandlerException, UnsupportedEncodingException {
        LOG.info("extractMessageData", idMessage);
        String hostname = nextSdrCommonUtils.getHostname(request);
        System.out.println("hostanme " + hostname);
        String repositoryTemp = null;
        if (hostname.equals("localhost")) {
            repositoryTemp = "C:\\Users\\Public\\prova";
        } else {
            repositoryTemp = "/tmp/emlProveShpeckUI/prova";
        }
        return new ResponseEntity(ShpeckCacheableFunctions.getInfoEml(idMessage, repositoryTemp), HttpStatus.OK);
    }
    
    /**
     * 
     * @param idMessage
     * @param response
     * @param request
     * @throws EmlHandlerException
     * @throws FileNotFoundException
     * @throws MalformedURLException
     * @throws IOException
     * @throws MessagingException 
     */
    @RequestMapping(value = "downloadEml/{idMessage}", method = RequestMethod.GET)
    public void downloadEml(
            @PathVariable(required = true) Integer idMessage,
            HttpServletResponse response,
            HttpServletRequest request
        ) throws EmlHandlerException, FileNotFoundException, MalformedURLException, IOException, MessagingException {
        LOG.info("getEml", idMessage);
        // TODO: Usare repository reale
        String hostname = nextSdrCommonUtils.getHostname(request);
        System.out.println("hostanme " + hostname);
        String repositoryTemp = null;
        if (hostname.equals("localhost")) {
            repositoryTemp = "C:\\Users\\Public\\prova";
        } else {
            repositoryTemp = "/tmp/emlProveShpeckUI/prova";
        }
        
        IOUtils.copy(new FileInputStream(repositoryTemp + idMessage + ".eml"), response.getOutputStream());
        response.flushBuffer();
    }
    
    /**
     * 
     * @param idMessage
     * @param idAllegato
     * @param response
     * @param request
     * @throws EmlHandlerException
     * @throws FileNotFoundException
     * @throws MalformedURLException
     * @throws IOException
     * @throws MessagingException 
     */
    @RequestMapping(value = "downloadEmlAttachment/{idMessage}/{idAllegato}", method = RequestMethod.GET)
    public void downloadEmlAttachment(
            @PathVariable(required = true) Integer idMessage,
            @PathVariable(required = true) Integer idAllegato,
            HttpServletResponse response,
            HttpServletRequest request
        ) throws EmlHandlerException, FileNotFoundException, MalformedURLException, IOException, MessagingException {
        LOG.info("getEmlAttachment", idMessage, idAllegato);
        String hostname = nextSdrCommonUtils.getHostname(request);
        System.out.println("hostanme " + hostname);
        String repositoryTemp = null;
        if (hostname.equals("localhost")) {
            repositoryTemp = "C:\\Users\\Public\\prova";
        } else {
            repositoryTemp = "/tmp/emlProveShpeckUI/prova";
        }
        InputStream attachment = EmlHandler.getAttachment(repositoryTemp + idMessage + ".eml", idAllegato);
        IOUtils.copy(attachment, response.getOutputStream());
        response.flushBuffer();
    }
    
    /**
     * 
     * @param idMessage
     * @param response
     * @param request
     * @throws EmlHandlerException
     * @throws FileNotFoundException
     * @throws MalformedURLException
     * @throws IOException
     * @throws MessagingException 
     */
    @RequestMapping(value = "downloadAllEmlAttachment/{idMessage}", method = RequestMethod.GET,  produces = "application/zip")
    public void downloadAllEmlAttachment(
            @PathVariable(required = true) Integer idMessage,
            HttpServletResponse response,
            HttpServletRequest request
        ) throws EmlHandlerException, FileNotFoundException, MalformedURLException, IOException, MessagingException {
        LOG.info("get_all_eml_attachment", idMessage);
        String hostname = nextSdrCommonUtils.getHostname(request);
        System.out.println("hostanme " + hostname);
        String repositoryTemp = null;
        if (hostname.equals("localhost")) {
            repositoryTemp = "C:\\Users\\Public\\prova";
        } else {
            repositoryTemp = "/tmp/emlProveShpeckUI/prova";
        }
        
        response.addHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=allegati.zip");
        List<Pair> attachments = EmlHandler.getAttachments(repositoryTemp + idMessage + ".eml");
        ZipOutputStream zos = new ZipOutputStream(new BufferedOutputStream(response.getOutputStream()));
        Integer i;
        for(Pair p : attachments) {
            i = 0;
            Boolean in_error = true;
            while(in_error) {
                try {
                    String s = "";
                    if (i > 0) {
                        s = "_" + Integer.toString(i); 
                    }
                    zos.putNextEntry(new ZipEntry((String) p.getLeft() + s));
                    in_error = false;
                } catch(ZipException ex) {
                    i++;
                }
            }
            IOUtils.copy((InputStream) p.getRight(), zos);
        }
        
        zos.close();
        response.flushBuffer();
    }
    
    /**
     * Salva la bozza della mail sul database
     * @param request La Request Http
     * @param idDraftMessage L'id del messaggio bozza che stiamo salvando
     * @param idPec L'id dell'indirizzo PEC a cui appartiene la bozza
     * @param body Il testo html della mail
     * @param hideRecipients Destinatari nascosti
     * @param subject L'oggetto della mail
     * @param from L'indirizzo mittente
     * @param to Array degli indirizzi di destinazione
     * @param cc Array degli indirizzi in copia carbone
     * @param attachments Array degli allegati
     * @param idMessageRelated Id del messaggio risposto opzionale
     * @param messageRelatedType Il tipo della relazione del messaggio related
     * @param idMessageRelatedAttachments
     * @throws AddressException Errore nella creazione degli indirizzi
     * @throws IOException Errore di salvataggio
     * @throws MessagingException Errore nella creazione del mimemessage
     * @throws EntityNotFoundException Elemento non trovato nel repository
     * @throws it.bologna.ausl.eml.handler.EmlHandlerException
     * @throws it.bologna.ausl.internauta.service.exceptions.Http500ResponseException
     */
    @Transactional(rollbackFor = Throwable.class)
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
        @RequestParam("idMessageRelatedAttachments") Integer[] idMessageRelatedAttachments
        ) throws AddressException, IOException, MessagingException, EntityNotFoundException, EmlHandlerException, Http500ResponseException {
        
        LOG.info("Shpeck controller -> Message received from PEC with id: " + idPec);
        String hostname = nextSdrCommonUtils.getHostname(request);
        
        ArrayList<EmlHandlerAttachment> listAttachments = shpeckUtils.convertAttachments(attachments);
        
        ArrayList<MimeMessage> mimeMessagesList = null;
        MimeMessage mimeMessage = null;  
        
        LOG.info("Getting draft with idDraft: ", idDraftMessage);
        Draft draftMessage = draftRepository.getOne(idDraftMessage);
        
        LOG.info("Getting PEC from repository...");
        Pec pec = pecRepository.getOne(idPec);
        String from = pec.getIndirizzo();
        LOG.info("Start building mime message...");
        
        if (request.getServletPath().endsWith("saveDraftMessage")) {
            mimeMessage = shpeckUtils.buildMimeMessage(from, to, cc, body, subject, listAttachments, 
                idMessageRelated, null, idMessageRelatedAttachments, hostname);
            LOG.info("Mime message generated correctly!");
            LOG.info("Preparing the message for saving...");
            shpeckUtils.saveDraft(draftMessage, pec, subject, to, cc, hideRecipients, 
                    listAttachments, body, mimeMessage, idMessageRelated, messageRelatedType);
        } else if (request.getServletPath().endsWith("sendMessage")) {
            if (Objects.equals(hideRecipients, Boolean.TRUE)) {
                LOG.info("Hide recipients is true, building mime message for each recipient.");
                mimeMessagesList = new ArrayList<>();
                for (String address: to) {
                    mimeMessage = shpeckUtils.buildMimeMessage(from, new String[] {address}, cc, body, subject, listAttachments, 
                    idMessageRelated, null, idMessageRelatedAttachments, hostname);
                    mimeMessagesList.add(mimeMessage);
                }
                LOG.info("Mime messages generated correctly!");
            } else {
                mimeMessage = shpeckUtils.buildMimeMessage(from, to, cc, body, subject, listAttachments, 
                    idMessageRelated, null, idMessageRelatedAttachments, hostname);
                LOG.info("Mime message generated correctly!");
            }
            
            LOG.info("Preparing the message for sending...");
            try {
                if (Objects.equals(hideRecipients, Boolean.TRUE) && mimeMessagesList != null) {
                    for (MimeMessage mime: mimeMessagesList)
                        shpeckUtils.sendMessage(pec, mime);
                } else 
                    shpeckUtils.sendMessage(pec, mimeMessage);
                
                if (idMessageRelated != null) {
                    shpeckUtils.setTagsToMessage(pec, idMessageRelated, messageRelatedType);
                }
                
                shpeckUtils.deleteDraft(draftMessage);   
            } catch (IOException | MessagingException | EntityNotFoundException ex) {
                LOG.error("Handling error on send! Trying to save...", ex);
                mimeMessage = shpeckUtils.buildMimeMessage(from, to, cc, body, subject, listAttachments, 
                    idMessageRelated, null, idMessageRelatedAttachments, hostname);
                shpeckUtils.saveDraft(draftMessage, pec, subject, to, cc, hideRecipients,
                            listAttachments, body, mimeMessage, idMessageRelated, messageRelatedType);
                throw new Http500ResponseException("007","Errore durante l'invio. La mail è stata salvata nelle bozze.", ex);
            }
        }
    }
}
