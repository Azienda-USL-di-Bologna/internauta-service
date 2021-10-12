package it.bologna.ausl.internauta.service.controllers.shpeck;

import it.bologna.ausl.blackbox.exceptions.BlackBoxPermissionException;
import it.bologna.ausl.eml.handler.EmlHandlerException;
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
import it.bologna.ausl.model.entities.baborg.Azienda;
import it.bologna.ausl.model.entities.baborg.QPec;

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
    private DraftRepository draftRepository;

    @Autowired
    private SAIUtils saiUtils;

    @Autowired
    private AuthenticatedSessionDataBuilder authenticatedSessionDataBuilder;

    @Autowired
    private CachedEntities cachedEntities;

    @Transactional(rollbackFor = Throwable.class, noRollbackFor = Http500ResponseException.class)
    @RequestMapping(value = {"send-and-archive-pec", "sendAndArchivePec"}, method = RequestMethod.POST)
    public String sendAndArchiveMail(
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
    ) throws Http500ResponseException, Http400ResponseException, Http403ResponseException {

        Boolean doIHaveToKrint = false;
        String hostname;
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

        Draft draft = new Draft();
        try {
            draft.setIdPec(pec);
            draft = draftRepository.save(draft);
        } catch (Exception e) {
            throw new Http500ResponseException("500-003", "errore nella creazione della bozza per l'invio della mail");
        }

        Integer idOutBox;
        try {
            idOutBox = shpeckUtils.BuildAndSendMailMessage(
                    ShpeckUtils.MailMessageOperation.SEND_MESSAGE,
                    null,
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
            throw new Http403ResponseException("403-001", String.format("l'utente %s non ha il permesso di invio sulla casella %s", person.getDescrizione(), senderAddress), ex);
        } catch (Http403ResponseException ex) {
            throw new Http500ResponseException("500-005", "Errore nel calcolo dei permessi", ex);
        } catch (Exception ex) {
            throw new Http500ResponseException("500-006", "Errore non previsto nell'invio della mail", ex);
        }

        String numerazioneGerarchica = null;
        try {
            numerazioneGerarchica = saiUtils.fascicolaPec(idOutBox, aziendaObj.getId(), userCF, senderAddress, fascicolo);
        } catch (FascicolazioneGddocException ex) {
            throw new Http500ResponseException("500-007", "Il sottofacicolo è stato creato, ma c'è stato un errore nella fascicolazione della mail", ex);
        } catch (FascicoloNotFoundException ex) {
            throw new Http400ResponseException("400-003", "Il fascicolo padre in cui fascicolare non è stato trovato", ex);
        } catch (FascicoloPadreNotDefinedException ex) {
            throw new Http500ResponseException("500-006", "Il fascicolo padre in cui fascicolare non è definito in Babel", ex);
        } catch (FascicoloPermissionSettingException ex) {
            throw new Http500ResponseException("500-007", "Errore nell'attribuzione dei permessi al fascicolo", ex);
        } catch (GddocCreationException ex) {
            throw new Http500ResponseException("500-008", "Il sottofacicolo è stato creato, ma c'è stato un errore nella creazione del documento da fascicolare", ex);
        } catch (SubFascicoloCreationException ex) {
            throw new Http500ResponseException("500-009", "Errore nella creazione del sottofascicolo", ex);
        } catch (Exception ex) {
            throw new Http500ResponseException("500-010", "Errore non previsto nella fascicolazione della mail", ex);
        }

        return numerazioneGerarchica;
    }

}
