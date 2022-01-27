package it.bologna.ausl.internauta.service.controllers.shpeck;

import com.querydsl.core.types.dsl.BooleanExpression;
import it.bologna.ausl.blackbox.exceptions.BlackBoxPermissionException;
import it.bologna.ausl.eml.handler.EmlHandler;
import it.bologna.ausl.eml.handler.EmlHandlerAttachment;
import it.bologna.ausl.eml.handler.EmlHandlerException;
import it.bologna.ausl.eml.handler.EmlHandlerResult;
import it.bologna.ausl.internauta.service.authorization.AuthenticatedSessionData;
import it.bologna.ausl.internauta.service.authorization.AuthenticatedSessionDataBuilder;
import it.bologna.ausl.internauta.service.exceptions.BadParamsException;
import it.bologna.ausl.internauta.service.exceptions.http.ControllerHandledExceptions;
import it.bologna.ausl.internauta.service.exceptions.http.Http400ResponseException;
import it.bologna.ausl.internauta.service.exceptions.http.Http403ResponseException;
import it.bologna.ausl.internauta.service.exceptions.http.Http500ResponseException;
import it.bologna.ausl.internauta.service.exceptions.sai.FascicolazioneGddocException;
import it.bologna.ausl.internauta.service.exceptions.sai.FascicoloNotFoundException;
import it.bologna.ausl.internauta.service.exceptions.sai.FascicoloPadreNotDefinedException;
import it.bologna.ausl.internauta.service.exceptions.sai.FascicoloPermissionSettingException;
import it.bologna.ausl.internauta.service.exceptions.sai.GddocCreationException;
import it.bologna.ausl.internauta.service.exceptions.sai.SubFascicoloCreationException;
import it.bologna.ausl.internauta.service.gedi.utils.SAIUtils;
import it.bologna.ausl.internauta.service.repositories.baborg.PecRepository;
import it.bologna.ausl.internauta.service.repositories.shpeck.DraftRepository;
import it.bologna.ausl.internauta.service.repositories.shpeck.MessageRepository;
import it.bologna.ausl.internauta.service.schedulers.FascicolatoreOutboxGediLocaleManager;
import it.bologna.ausl.internauta.service.shpeck.utils.ShpeckCacheableFunctions;
import it.bologna.ausl.internauta.service.shpeck.utils.ShpeckUtils;
import it.bologna.ausl.model.entities.baborg.Pec;
import it.bologna.ausl.model.entities.baborg.Persona;
import it.bologna.ausl.model.entities.baborg.Utente;
import it.bologna.ausl.model.entities.shpeck.Draft;
import it.nextsw.common.utils.CommonUtils;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import java.util.Optional;
import javax.mail.MessagingException;
import javax.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import it.bologna.ausl.internauta.service.utils.CachedEntities;
import it.bologna.ausl.internauta.service.utils.FileUtilities;
import it.bologna.ausl.model.entities.baborg.Azienda;
import it.bologna.ausl.model.entities.baborg.QPec;
import it.bologna.ausl.model.entities.shpeck.Message;
import it.bologna.ausl.model.entities.shpeck.QMessage;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.stream.Stream;
import org.apache.commons.codec.digest.MessageDigestAlgorithms;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.http.MediaType;
import org.springframework.util.StreamUtils;

/**
 *
 * @author gusgus
 */
@RestController
@RequestMapping(value = "${shpeck.mapping.url.root}")
public class SAIController implements ControllerHandledExceptions {

    private static final Logger LOG = LoggerFactory.getLogger(SAIController.class);

    @Autowired
    private ShpeckUtils shpeckUtils;

    @Autowired
    private CommonUtils nextSdrCommonUtils;

    @Autowired
    private PecRepository pecRepository;
    
    @Autowired
    private MessageRepository messageRepository;

    @Autowired
    private DraftRepository draftRepository;

    @Autowired
    private SAIUtils saiUtils;
    
    @Autowired
    private ShpeckCacheableFunctions shpeckCacheableFunctions;

    @Autowired
    private AuthenticatedSessionDataBuilder authenticatedSessionDataBuilder;

    @Autowired
    private CachedEntities cachedEntities;
    
    @Autowired
    private FascicolatoreOutboxGediLocaleManager fascicolatoreOutboxGediLocaleManager;

    @RequestMapping(value = {"reschedule-fascicolatore-sai-jobs", "rescheduleFascicolatoreSaiJobs"}, method = RequestMethod.GET)
    public void sendAndArchiveMail() throws Exception {
        fascicolatoreOutboxGediLocaleManager.scheduleAutoFascicolazioneOutboxAtBoot();
    }
    
    // @Transactional(rollbackFor = Throwable.class, noRollbackFor = Http500ResponseException.class)
    @RequestMapping(value = {"send-and-archive-pec", "sendAndArchivePec"}, method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, Object> sendAndArchiveMail(
            HttpServletRequest request,
            @RequestParam(name = "senderAddress", required = true) String senderAddress,
            @RequestParam(name = "body", required = false) String body,
            @RequestParam(name = "hideRecipients", defaultValue = "false") Boolean hideRecipients,
            @RequestParam(name = "subject", required = true) String subject,
            @RequestParam(name = "to", required = true) String[] to,
            @RequestParam(name = "cc", required = false) String[] cc,
            @RequestParam(name = "attachments", required = false) MultipartFile[] attachments,
            @RequestParam(name = "userCF", required = true) String userCF,
            @RequestParam(name = "fascicolo", required = false) String fascicolo,
            @RequestParam(name = "azienda", required = true) String azienda
    ) throws Http500ResponseException, Http400ResponseException, Http403ResponseException, IOException, NoSuchAlgorithmException {

        Boolean doIHaveToKrint = true;
        
        
//        String hashFromBytes = FileUtilities.getHashFromBytes(MessageDigestAlgorithms.SHA_256, attachments[0].getBytes());
//        System.out.println(hashFromBytes);
//        
//        String hashFromBytes2 = FileUtilities.getHashFromBytes(MessageDigestAlgorithms.SHA_256, attachments[0].getBytes());
//        System.out.println(hashFromBytes2);
        
        String hostname = null;
        try {
            hostname = nextSdrCommonUtils.getHostname(request);
        } catch (Exception e) {
            LOG.warn("errore nel reperimento dell'hostname");
        }

        AuthenticatedSessionData authenticatedUserProperties = null;
        Utente user;
        Persona person;
        try {
            authenticatedUserProperties = authenticatedSessionDataBuilder.getAuthenticatedUserProperties();
            user = authenticatedUserProperties.getUser();
            person = authenticatedUserProperties.getPerson();
        } catch (BlackBoxPermissionException ex) {
            throw new Http500ResponseException("500-001", String.format("errore nel reperimento dell'utente applicativo dalla sessione", senderAddress));
        }

        Azienda aziendaObj;
        try {
            aziendaObj = cachedEntities.getAziendaFromNome(azienda);
        } catch (Exception e) {
            throw new Http500ResponseException("500-002", "errore nel reperimento dell'azienda");
        }
        if (aziendaObj == null) {
            throw new Http400ResponseException("400-001", String.format("l'azienda %s non è presente in babel", azienda));
        }

        Optional<Pec> pecOp = pecRepository.findOne(QPec.pec.indirizzo.eq(senderAddress).and(QPec.pec.attiva.eq(true)));
        if (!pecOp.isPresent()) {
            throw new Http400ResponseException("400-002", String.format("la casella con indirizzo %s non è presente in babel oppure non è attiva", senderAddress));
        }
        Pec pec = pecOp.get();

        Integer idOutBox = null;
//        try {
//            idOutBox = checkIfMailIsSentAndGetIdOutbox(pec, senderAddress, to, cc, subject, body, attachments);
//        } catch (Exception ex) {
//            LOG.error("errore ricerca mail inviata 500-012", ex);
//            throw new Http500ResponseException("500-012", "errore nella ricerca della mail inviata", ex);
//        }
        
        if (idOutBox == null) {
            Draft draft = new Draft();
            try {
                draft.setIdPec(pec);
                draft = draftRepository.save(draft);
            } catch (Exception e) {
                throw new Http500ResponseException("500-003", "errore nella creazione della bozza per l'invio della mail");
            }

            try {
                    idOutBox = shpeckUtils.BuildAndSendMailMessage(
                        ShpeckUtils.MailMessageOperation.SEND_MESSAGE,
                        hostname,
                        draft.getId(),
                        pec.getId(),
                        body,
                        hideRecipients,
                        subject,
                        to,
                        cc,
                        attachments,
                        null,
                        null,
                        null,
                        user.getId(),
                        doIHaveToKrint);

            } catch (IOException | EmlHandlerException | BadParamsException | MessagingException ex) {
                throw new Http500ResponseException("500-004", "Errore nella creazione del messaggio mail", ex);
            } catch (Http500ResponseException ex) {
                throw ex;
            } catch (BlackBoxPermissionException ex) {
                throw new Http500ResponseException("500-005", "Errore nel calcolo dei permessi", ex);
            } catch (Http403ResponseException ex) {
                throw new Http403ResponseException("403-001", String.format("l'utente %s non ha il permesso di invio sulla casella %s", person.getDescrizione(), senderAddress), ex);
            } catch (Exception ex) {
                throw new Http500ResponseException("500-006", "Errore non previsto nell'invio della mail", ex);
            }
        }

        String numerazioneGerarchica = null;
        try {
            numerazioneGerarchica = saiUtils.fascicolaPec(idOutBox, aziendaObj, userCF, senderAddress, fascicolo, user, person);
        } catch (FascicolazioneGddocException ex) {
            LOG.error("errore fascicolazione 500-007", ex);
            throw new Http500ResponseException("500-007", "Il sottofacicolo è stato creato, ma c'è stato un errore nella fascicolazione della mail", ex);
        } catch (FascicoloNotFoundException ex) {
            LOG.error("errore fascicolazione 400-003", ex);
            throw new Http400ResponseException("400-003", "Il fascicolo padre in cui fascicolare non è stato trovato", ex);
        } catch (FascicoloPadreNotDefinedException ex) {
            LOG.error("errore fascicolazione 500-006", ex);
            throw new Http500ResponseException("500-006", "Il fascicolo padre in cui fascicolare non è definito in Babel", ex);
        } catch (FascicoloPermissionSettingException ex) {
            LOG.error("errore fascicolazione 500-011", ex);
            throw new Http500ResponseException("500-011", "Errore nell'attribuzione dei permessi al fascicolo", ex);
        } catch (GddocCreationException ex) {
            LOG.error("errore fascicolazione 500-008", ex);
            throw new Http500ResponseException("500-008", "Il sottofacicolo è stato creato, ma c'è stato un errore nella creazione del documento da fascicolare", ex);
        } catch (SubFascicoloCreationException ex) {
            LOG.error("errore fascicolazione 500-009", ex);
            throw new Http500ResponseException("500-009", "Errore nella creazione del sottofascicolo", ex);
        } catch (Exception ex) {
            LOG.error("errore fascicolazione 500-010", ex);
            throw new Http500ResponseException("500-010", "Errore non previsto nella fascicolazione della mail", ex);
        }

        Map<String, Object> res = new HashMap();
        res.put("mail-id", idOutBox);
        res.put("fascicolo-id", numerazioneGerarchica);
        
        return res;
    }
    
    private Integer checkIfMailIsSentAndGetIdOutbox(Pec pec, String from, String[] to, String[] cc, String subject, String body, MultipartFile[] multipartAttachments) throws FileNotFoundException, EmlHandlerException, MessagingException, IOException, NoSuchAlgorithmException, BadParamsException {
        BooleanExpression filter = QMessage.message.idPec.id.eq(pec.getId()).and(
                QMessage.message.subject.eq(subject).and(
                QMessage.message.attachmentsNumber.eq(multipartAttachments.length).and(
                QMessage.message.inOut.eq(Message.InOut.OUT.toString())))
        );
        Iterable<Message> candidateMessages = messageRepository.findAll(filter);
        for (Message m: candidateMessages) {
            boolean found = true;
//            File downloadEml = null;SD
//                downloadEml = shpeckUtils.downloadEml(ShpeckUtils.EmlSource.MESSAGE, m.getId());

            EmlHandlerResult emlHandlerResult = shpeckCacheableFunctions.getInfoEml(ShpeckUtils.EmlSource.MESSAGE, m.getId());
            int attNumber = (int) Arrays.stream(emlHandlerResult.getAttachments()).filter(a -> {
                        LOG.info(a.toString());
                        return a.getForHtmlAttribute() == false;
            }).count();
            emlHandlerResult.setRealAttachmentNumber(attNumber);
            if (attNumber == multipartAttachments.length) {
                Arrays.sort(emlHandlerResult.getTo());
                Arrays.sort(to);
                Arrays.sort(emlHandlerResult.getCc());
                Arrays.sort(cc);
                if (emlHandlerResult.getFrom().equals(from) && 
                        Arrays.equals(emlHandlerResult.getTo(), to) &&
                        Arrays.equals(emlHandlerResult.getCc(), cc) &&
                        emlHandlerResult.getSubject().equalsIgnoreCase(subject)
                    ) {
                    for (EmlHandlerAttachment attachment : emlHandlerResult.getAttachments()) {
                        String hashFromFile = FileUtilities.getHashFromFile(attachment.getInputStream(), MessageDigestAlgorithms.SHA_256);
                        boolean attachementFound = Stream.of(multipartAttachments).anyMatch(att -> {
                            String hash = null;
                            try {
                                hash = FileUtilities.getHashFromBytes(MessageDigestAlgorithms.SHA_256, att.getBytes());
                            } catch (IOException | NoSuchAlgorithmException ex) {
                                LOG.error("errore nel calcolo dell'hash dell'allegato multipart", ex);
                            }
                            return hash != null && hashFromFile.equalsIgnoreCase(hash);
                        });
                        if (!attachementFound) {
                            found = false;
                            break;
                        }
                    }
                }
            } else {
                found = false;
            }
            if (found) {
                return m.getIdOutbox();
            }
        }
        return null;
    }
    
    

}
