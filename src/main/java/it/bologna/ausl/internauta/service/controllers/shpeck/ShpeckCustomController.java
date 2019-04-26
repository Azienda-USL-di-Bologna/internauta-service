package it.bologna.ausl.internauta.service.controllers.shpeck;

import it.bologna.ausl.eml.handler.EmlHandler;
import it.bologna.ausl.eml.handler.EmlHandlerException;
import it.bologna.ausl.eml.handler.EmlHandlerUtils;
import it.bologna.ausl.eml.handler.EmlHandlerAttachment;
import it.bologna.ausl.internauta.service.repositories.baborg.PecRepository;
import it.bologna.ausl.internauta.service.repositories.shpeck.DraftRepository;
import it.bologna.ausl.internauta.service.shpeck.utils.ShpeckCacheableFunctions;
import it.bologna.ausl.model.entities.baborg.Pec;
import it.bologna.ausl.model.entities.shpeck.Draft;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.time.LocalDateTime;
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
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipOutputStream;
import javax.mail.Address;
import javax.mail.MessagingException;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

/**
 *
 * @author gusgus
 */
@RestController
@RequestMapping(value = "${shpeck.mapping.url.root}")
public class ShpeckCustomController {

    private static final Logger LOG = LoggerFactory.getLogger(ShpeckCustomController.class);
    
    @Autowired
    private DraftRepository draftRepository;
    
    @Autowired
    private PecRepository pecRepository;
    
    /**
     * 
     * @param idMessage
     * @return
     * @throws EmlHandlerException 
     */
    @RequestMapping(value = "extractMessageData/{idMessage}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> extractMessageData(
            @PathVariable(required = true) Integer idMessage,
            HttpServletRequest request
        ) throws EmlHandlerException, UnsupportedEncodingException {
        // TODO: Gestire idMessage.
        String hostname = getHostname(request);
        System.out.println("hostanme " + hostname);
        String repositoryTemp = null;
        if (hostname.equals("localhost")) {
            repositoryTemp = "C:\\Users\\Public\\prova";
        } else {
            repositoryTemp = "/tmp/emlProveShpeckUI/prova";
        }
        LOG.info("extractMessageData", idMessage);
        return new ResponseEntity(ShpeckCacheableFunctions.getInfoEml(idMessage, repositoryTemp), HttpStatus.OK);
    }
    
    @RequestMapping(value = "getEmlAttachment/{idMessage}/{idAllegato}", method = RequestMethod.GET)
    public void getEmlAttachment(
            @PathVariable(required = true) Integer idMessage,
            @PathVariable(required = true) Integer idAllegato,
            HttpServletResponse response,
            HttpServletRequest request
        ) throws EmlHandlerException, FileNotFoundException, MalformedURLException, IOException, MessagingException {
        // TODO: Gestire idMessage e idAllegato.
        String hostname = getHostname(request);
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
    
    @RequestMapping(value = "get_all_eml_attachment/{idMessage}", method = RequestMethod.GET,  produces = "application/zip")
    public void getAllEmlAttachment(
            @PathVariable(required = true) Integer idMessage,
            HttpServletResponse response,
            HttpServletRequest request
        ) throws EmlHandlerException, FileNotFoundException, MalformedURLException, IOException, MessagingException {
        // TODO: Gestire idMessage
        String hostname = getHostname(request);
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
     * @param idPec L'id dell'indirizzo PEC a cui appartiene la bozza
     * @param body Il testo html della mail
     * @param hideRecipients Destinatari nascosti
     * @param subject L'oggetto della mail
     * @param from L'indirizzo mittente
     * @param to Array degli indirizzi di destinazione
     * @param cc Array degli indirizzi in copia carbone
     * @param attachments Array degli allegati
     * @throws AddressException Errore nella creazione degli indirizzi
     * @throws IOException Errore di salvataggio
     * @throws MessagingException Errore nella creazione del mimemessage
     */
    @Transactional
    @RequestMapping(value = "saveDraftMessage", method = RequestMethod.POST)
    public void saveDraftMessage(
        @RequestParam("idPec") Integer idPec,
        @RequestParam("body") String body,
        @RequestParam("hideRecipients") Boolean hideRecipients,
        @RequestParam("subject") String subject,
        @RequestParam("from") String from,
        @RequestParam("to") String[] to,
        @RequestParam("cc") String[] cc,
        @RequestParam("attachments") MultipartFile[] attachments
        ) throws AddressException, IOException, MessagingException {
        
        LOG.info("Saving draft message received from PEC with id: " + idPec);
        LOG.info("Creating the sender address");
        Address fromAddress = new InternetAddress(from);
        
        LOG.info("Creating destination's addresses array");
        Address toAddress[] = null;
        if (to != null) {
            toAddress = new Address[to.length];
            for (int i = 0; i < to.length; i++) {
                toAddress[i] = new InternetAddress(to[i]);
            }
        }
        LOG.info("Creating carbon copy's addresses array");
        Address ccAddress[] = null;
        if (cc != null) {
            ccAddress = new Address[cc.length];
            for (int i = 0; i < cc.length; i++) {
                ccAddress[i] = new InternetAddress(cc[i]);
            }
        }
        LOG.info("Creating the attachments array");
        ArrayList<EmlHandlerAttachment> listAttachments = null;
        if (attachments != null) {
            listAttachments = new ArrayList<>();
            for (MultipartFile attachment : attachments) {
                EmlHandlerAttachment file = new EmlHandlerAttachment();
                file.setFileName(attachment.getOriginalFilename());
                file.setMimeType(attachment.getContentType());
                file.setFileBytes(attachment.getBytes());
                listAttachments.add(file);
            }
        }
        
        LOG.info("Building mime message...");
        EmlHandlerUtils emlHandlerUtilis = new EmlHandlerUtils();
        MimeMessage mimeMessage = null;
        try {
            mimeMessage = emlHandlerUtilis.buildDraftMessage(body, subject, fromAddress, toAddress, ccAddress, listAttachments);        
        } catch (MessagingException ex) {
            LOG.error("Errore while generating the mimemessage", ex);
            throw new MessagingException("Errore while generating the mimemessage", ex);
        }
        LOG.info("Mime message generated correctly!");
        LOG.info("Preparing the message for saving...");
        Draft draftMessage = new Draft();
        try {
            Pec pec = pecRepository.findById(idPec).orElseThrow();
            draftMessage.setIdPec(pec);
            draftMessage.setSubject(subject);
            draftMessage.setToAddresses(to);
            draftMessage.setCcAddresses(cc);
            draftMessage.setHiddenRecipients(Boolean.FALSE);
            draftMessage.setCreateTime(LocalDateTime.now());
            draftMessage.setUpdateTime(LocalDateTime.now());
            draftMessage.setAttachmentsNumber(listAttachments != null ? listAttachments.size() : 0);
            draftMessage.setAttachmentsName(listAttachments != null ? listAttachments.stream()
                    .map(EmlHandlerAttachment::getFileName).toArray(size -> new String[size]) : new String[0]);
            draftMessage.setBody(body);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            mimeMessage.writeTo(baos);
            draftMessage.setEml(baos.toByteArray());
            LOG.info("Message ready. Saving...");
            draftMessage = draftRepository.save(draftMessage);
        } catch (IOException ex) {
            LOG.error("Error while saving message");
            throw new IOException("Error while saving message", ex);
        } finally {
            LOG.info("Draft message saved: {}", draftMessage);
        }
    }
    
    // Metodo temporano. rimane qui finché non abbiamo il vero repository
    public String getHostname(HttpServletRequest request) {
        String res;
        String header = request.getHeader("X-Forwarded-Host");
        // TODO: non è detto che vada bene tornare sempre il primo elemento, bisognerebbe controllare che il Path dell'azienda matchi con uno qualsiasi degli elementi
        if (StringUtils.hasText(header)) {
            String[] headerToken = header.split(",");
            res = headerToken[0];
        } else {
            res = request.getServerName();
        }
        return res;
    }
}
