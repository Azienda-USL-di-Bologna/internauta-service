package it.bologna.ausl.internauta.service.controllers.tools;

import org.json.JSONObject;
import com.mongodb.MongoException;
import it.bologna.ausl.blackbox.exceptions.BlackBoxPermissionException;
import it.bologna.ausl.internauta.service.exceptions.http.ControllerHandledExceptions;
import it.bologna.ausl.internauta.service.exceptions.http.HttpInternautaResponseException;
import it.bologna.ausl.internauta.service.repositories.baborg.AziendaRepository;
import it.bologna.ausl.internauta.service.utils.CachedEntities;
import it.bologna.ausl.model.entities.baborg.Azienda;
import it.bologna.ausl.model.entities.baborg.AziendaParametriJson;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

//import per mandare mail
import com.sun.mail.smtp.SMTPTransport;
import it.bologna.ausl.internauta.service.baborg.utils.BaborgUtils;
import it.bologna.ausl.internauta.service.configuration.utils.ReporitoryConnectionManager;
import it.bologna.ausl.internauta.service.controllers.utils.ToolsUtils;
import it.bologna.ausl.internauta.service.exceptions.SendMailException;
import it.bologna.ausl.internauta.service.repositories.baborg.UtenteRepository;
import it.bologna.ausl.internauta.service.repositories.scrivania.RichiestaSmartWorkingRepository;
import it.bologna.ausl.internauta.service.utils.SimpleMailSenderUtility;
import it.bologna.ausl.internauta.service.utils.redmine.factories.MiddleMineManagerFactory;
import it.bologna.ausl.internauta.service.utils.redmine.middlemine.communications.MiddleMineNewIssueManager;
import it.bologna.ausl.internauta.service.utils.redmine.middlemine.communications.MiddleMineNewIssueResponseManager;
import it.bologna.ausl.internauta.utils.parameters.manager.ParametriAziendeReader;
import it.bologna.ausl.minio.manager.MinIOWrapper;
import it.bologna.ausl.model.entities.baborg.Utente;
import it.bologna.ausl.model.entities.configurazione.ParametroAziende;
import it.bologna.ausl.model.entities.forms.Segnalazione;
import it.bologna.ausl.model.entities.scrivania.RichiestaSmartWorking;
import it.nextsw.common.projections.ProjectionsInterceptorLauncher;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.util.Date;
import java.util.Properties;
import java.util.logging.Level;
import javax.activation.DataHandler;
import javax.mail.Multipart;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMultipart;
import javax.mail.util.ByteArrayDataSource;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import okhttp3.Credentials;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.apache.tomcat.util.http.fileupload.IOUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StreamUtils;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

/**
 *
 * @author gdm
 */
@RestController
@RequestMapping(value = "${tools.mapping.url.root}")
public class ToolsCustomController implements ControllerHandledExceptions {

    private static final Logger LOGGER = LoggerFactory.getLogger(ToolsCustomController.class);

    @Value("${customer.support.email}")
    private String emailCustomerSupport;
    @Value("${customer.support.name}")
    private String nameCustomerSupport;
    //per parametri pubblici?
    @Autowired
    private AziendaRepository aziendaRepository;
    
    @Autowired
    private CachedEntities cachedEntities;
    
    @Autowired
    private ParametriAziendeReader parametriAziendaReader;
    
    @Autowired
    private UtenteRepository utenteRepository;
    
    @Autowired
    private RichiestaSmartWorkingRepository richiestaSmartWorkingRepository;
    
    @Autowired
    private ProjectionsInterceptorLauncher projectionsInterceptorLauncher;

    @Autowired
    private SimpleMailSenderUtility simpleMailSenderUtility;

    @Autowired
    private BaborgUtils baborgUtils;

    @Autowired
    private ParametriAziendeReader parametriAziende;
    
    @Autowired
    private ReporitoryConnectionManager mongoConnectionManager;

    @Value("${redmine-test-mode}")
    boolean redmineTestMode;

    public Boolean sendMail(
            Integer idAzienda, String fromName, String Subject, List<String> To, String body,
            List<String> cc, List<String> bcc, MultipartFile[] attachments, List<String> replyTo, boolean... htmlBody) throws IOException {

        Azienda azienda = cachedEntities.getAzienda(idAzienda);
        AziendaParametriJson aziendaParametri = azienda.getParametri();
        AziendaParametriJson.MailParams mailParams = aziendaParametri.getMailParams();
        boolean flag = (htmlBody.length >= 1) ? htmlBody[0] : false;
        if (mailParams != null) {
            String smtpServer = mailParams.getMailServerSmtpUrl();
            Integer port = mailParams.getMailServerSmtpPort();

            String username = null;
            String password = null;

            Properties prop = System.getProperties();
            prop.put("mail.smtp.host", smtpServer);                                 //optional, defined in SMTPTransport

            if (StringUtils.isEmpty(username) && StringUtils.isEmpty(password)) {
                prop.put("mail.smtp.auth", "false");
            } else {
                prop.put("mail.smtp.auth", "true");
            }

            if (port != null && port != -1) {
                prop.put("mail.smtp.port", port.toString());                        // default port 25
            } else {
                prop.put("mail.smtp.port", "25");
            }

            Session session = Session.getInstance(prop, null);
            Message msg = new MimeMessage(session);

            try {
                String mailFrom = mailParams.getMailFrom();
                // from
                msg.setFrom(new InternetAddress(mailFrom, fromName));

                if (replyTo != null) {
                    String addressesReplyTo = "";
                    for (String toElement : replyTo) {
                        addressesReplyTo += toElement + ",";
                    }
                    addressesReplyTo = addressesReplyTo.substring(0, addressesReplyTo.length() - 1);
                    msg.setReplyTo(InternetAddress.parse(addressesReplyTo, false));
                }

                // inserisco lista TO
                if (To != null && !To.isEmpty()) {
                    String addressesTo = "";
                    for (String toElement : To) {
                        addressesTo += toElement + ",";
                    }
                    addressesTo = addressesTo.substring(0, addressesTo.length() - 1);
                    msg.setRecipients(Message.RecipientType.TO, InternetAddress.parse(addressesTo, false));
                }

                //inserico lista CC e BCC
                if (cc != null && !cc.isEmpty()) {
                    String addressesCC = "";
                    for (String ccElement : cc) {
                        addressesCC += ccElement + ",";
                    }
                    if (bcc != null && !bcc.isEmpty()) {
                        for (String bccElement : bcc) {
                            addressesCC += bccElement + ",";
                        }
                    }
                    addressesCC = addressesCC.substring(0, addressesCC.length() - 1);
                    msg.setRecipients(Message.RecipientType.CC, InternetAddress.parse(addressesCC, false));
                }

                //            if (bcc!=null && !bcc.isEmpty()){
                //                for (String bccElement : bcc) {
                //                    msg.setRecipients(Message.RecipientType.CC,
                //                        InternetAddress.parse(bccElement, false));
                //                    }
                //            }
                // subject
                //            LocalDateTime today = LocalDateTime.now();
                //            ZoneId id = ZoneId.of("Europe/Rome");
                //            ZonedDateTime zonedDateTime = ZonedDateTime.of(today, id);      //That's how you add timezone to date
                //            String formattedDateTime = DateTimeFormatter
                //                            .ofPattern("dd/MM/yyyy - HH:mm")
                //                            .format(zonedDateTime);             //  esempio 11/03/2020 - 10.44
                msg.setSubject(Subject);
                // content
                if (attachments != null && attachments.length > 0) {
                    Multipart multipart = new MimeMultipart();

                    // Body
                    MimeBodyPart messageBodyPart = new MimeBodyPart();
                    if (flag) {
                        messageBodyPart.setContent(body, "text/html; charset=UTF-8");
                    } else {
                        messageBodyPart.setText(body);
                    }

                    multipart.addBodyPart(messageBodyPart);

                    // Allegati
                    MimeBodyPart attachmentPart;
                    for (MultipartFile attachment : attachments) {
                        attachmentPart = new MimeBodyPart();
                        byte[] fileBytes = attachment.getBytes();
                        String attachmentName = attachment.getOriginalFilename();
                        ByteArrayDataSource source
                                = new ByteArrayDataSource(fileBytes, attachment.getContentType());
                        attachmentPart.setDataHandler(new DataHandler(source));
                        attachmentPart.setFileName(attachmentName);
                        multipart.addBodyPart(attachmentPart);
                    }
                    msg.setContent(multipart);
                } else {
                    if (flag) {
                        msg.setContent(body, "text/html; charset=UTF-8");
                    } else {
                        msg.setText(body);
                    }
                }

                // msg.setContent(body, "text/html; charset=utf-8");
                msg.setSentDate(new Date());
                // Get SMTPTransport
                SMTPTransport t = (SMTPTransport) session.getTransport("smtp");
                // connect
                t.connect(smtpServer, username, password);
                // send
                t.sendMessage(msg, msg.getAllRecipients());
                LOGGER.info("Invio mail");
                System.out.println("Response: " + t.getLastServerResponse());
                t.close();
            } catch (MessagingException e) {
                e.printStackTrace();
                LOGGER.info("Invio mail fallito", e);
                return false;
            }
            return true;

        }
        return false;
    }

    @RequestMapping(value = {"newdate"}, method = RequestMethod.GET)
    public ResponseEntity<?> newDate() {
        return ResponseEntity.ok(ZonedDateTime.now());
    }

    @RequestMapping(value = {"sendSmartWorkingMail"}, method = RequestMethod.POST)
    public void sendSmartWorkingMail(@RequestBody Map<String, Object> jsonRequestSW) throws HttpInternautaResponseException, IOException, BlackBoxPermissionException, SendMailException {

        String accountFrom = "babelform.smartworking@ausl.bologna.it";
        String subject = "Richiesta SMART WORKING";
        String emailTextBody = "";
        List<String> to = new ArrayList();
        List<String> cc = new ArrayList();

        Boolean inviaRichiestaSmartWorkingAUfficioPersonale = (Boolean) jsonRequestSW.get("inviaRichiestaSmartWorkingAUfficioPersonale");

        if (inviaRichiestaSmartWorkingAUfficioPersonale) {
            to.add(jsonRequestSW.get("mailUfficioPersonale").toString());
            cc.add(jsonRequestSW.get("mailResponsabile").toString());
            cc.add(jsonRequestSW.get("mailRichiedente").toString());
        } else {
            to.add(jsonRequestSW.get("mailResponsabile").toString());
            cc.add(jsonRequestSW.get("mailRichiedente").toString());
        }

        //data e ora odierni
        LocalDateTime today = LocalDateTime.now();
        ZoneId id = ZoneId.of("Europe/Rome");
        ZonedDateTime zonedDateTime = ZonedDateTime.of(today, id);      //That's how you add timezone to date
        String dataRichiesta = DateTimeFormatter
                .ofPattern("dd/MM/yyyy - HH:mm")
                .format(zonedDateTime);             //11/03/2020 - 10.44

        // 2020-02-29T23:00:00.000Z
        LocalDate periodoDalDate = LocalDate.parse(jsonRequestSW.get("periodoDal").toString(), DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        LocalDate periodoAlDate = LocalDate.parse(jsonRequestSW.get("periodoAl").toString(), DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        DateTimeFormatter outputFormatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");
        String periodoDal = outputFormatter.format(periodoDalDate);
        String periodoAl = outputFormatter.format(periodoAlDate);

        Integer idRichiesta = salvaRichiestaNelDB(jsonRequestSW);

        subject = subject + " id " + idRichiesta + ", " + jsonRequestSW.get("richiedente").toString() + " " + dataRichiesta;

        emailTextBody += "Richiesta di autorizzazione allo smart working di " + jsonRequestSW.get("richiedente").toString() + " del " + dataRichiesta + "\n\n";

        if (!inviaRichiestaSmartWorkingAUfficioPersonale) {
            emailTextBody += "RISERVATO AL RESPONSABILE" + "\n";
            emailTextBody += "Da inoltrare solo in caso di autorizzazione positiva a:" + "\n";
            emailTextBody += jsonRequestSW.get("mailUfficioPersonale").toString() + "\n";
            emailTextBody += jsonRequestSW.get("mailICT").toString() + "\n";
            emailTextBody += jsonRequestSW.get("mailRichiedente").toString() + "\n\n";
            emailTextBody += "non modificare in alcun modo i dati al di sotto di questi asterischi" + "\n";
            emailTextBody += "\n********************************************************************************" + "\n\n";
        }
        emailTextBody += "Dati della richiesta" + "\n";
        emailTextBody += "Richiedente: " + jsonRequestSW.get("richiedente").toString() + "\n";
        // emailTextBody += "Username: " + jsonRequestSW.get("username").toString()+ "\n";
        emailTextBody += "Codice fiscale: " + jsonRequestSW.get("codiceFiscale").toString() + "\n";
        emailTextBody += "Mail del richiedente: " + jsonRequestSW.get("mailRichiedente").toString() + "\n";
        emailTextBody += "Data richiesta: " + dataRichiesta + "\n";
        emailTextBody += "Periodo richiesto dal: " + periodoDal + " al: " + periodoAl + "\n";
        emailTextBody += "Motivazione: " + jsonRequestSW.get("motivazione").toString() + "\n";
        emailTextBody += "\n********\n";

        emailTextBody += "DATI DELLA POSTAZIONE DI LAVORO\n";
        emailTextBody += "Utente con postazione esclusiva: " + ((Boolean) jsonRequestSW.get("hoPostazioneEsclusiva") ? "Si" : "No") + "\n";
        // emailTextBody += "Nome computer: " + jsonRequestSW.get("nomePc").toString()+ "\n";
        emailTextBody += "IP: " + jsonRequestSW.get("ip").toString() + "\n";
        emailTextBody += "Ubicazione: " + jsonRequestSW.get("azienda").toString() + " - " + jsonRequestSW.get("sede") + "\n";
        emailTextBody += "\n********\n";

        emailTextBody += "DATI DEL RICHIEDENTE\n";
        // emailTextBody += "Profilo professionale: " + jsonRequestSW.get("profiloProfessionale").toString()+ "\n";
        // emailTextBody += "Mansione: " + jsonRequestSW.get("mansione").toString()+ "\n";
        emailTextBody += "Responsabile: " + jsonRequestSW.get("responsabile").toString() + "\n";
        emailTextBody += "Mail del responsabile: " + jsonRequestSW.get("mailResponsabile").toString() + "\n";
        emailTextBody += "Ufficio personale: " + jsonRequestSW.get("mailUfficioPersonale").toString() + "\n";
        String mailICT = jsonRequestSW.get("mailICT").toString();
        if (mailICT != null && !mailICT.equals("")) {
            emailTextBody += "ICT: " + jsonRequestSW.get("mailICT").toString() + "\n";
        }

        emailTextBody += "\n********\n";

        emailTextBody += "DATI DELLA POSTAZIONE SMART WORKING\n";
        emailTextBody += "Possiede Pc Personale: " + ((Boolean) jsonRequestSW.get("pc").equals("personale") ? "Si" : "No") + "\n";
        Boolean haPcAziendale = (Boolean) jsonRequestSW.get("pc").equals("aziendale");
        emailTextBody += "Possiede Pc Aziendale: " + (haPcAziendale ? "Si" : "No") + "\n";
        if (haPcAziendale) {
            emailTextBody += "Nome Pc Aziendale: " + jsonRequestSW.get("inventario").toString() + "\n";
        }
//        emailTextBody += "Sistema Operativo: " + jsonRequestSW.get("sistemaOperativo").toString()+ "\n";
        emailTextBody += "Dispone di connettività domestica: " + ((Boolean) jsonRequestSW.get("connettivitaDomestica") ? "Si" : "No") + "\n";
        emailTextBody += "Numero di telefono di contatto: " + jsonRequestSW.get("numeroTel").toString() + "\n";
        Boolean hoCellulareAziendale = (Boolean) jsonRequestSW.get("hoCellulareAziendale");
        if (hoCellulareAziendale) {
            emailTextBody += "Numero del cellulare aziendale: " + jsonRequestSW.get("numeroCellAziendale").toString() + "\n";
        }
        emailTextBody += "Disponibilità: " + jsonRequestSW.get("contattabilita").toString() + "\n";
        emailTextBody += "VPN attiva e funzionante: " + ((Boolean) jsonRequestSW.get("vpn") ? "Si" : "No") + "\n";
        emailTextBody += "Utilizza la Firma Digitale: " + ((Boolean) jsonRequestSW.get("firma") ? "Si" : "No") + "\n";
        emailTextBody += "Lettore smart card: " + ((Boolean) jsonRequestSW.get("haLettoreSmartCard") ? "Si" : "No") + "\n";
        emailTextBody += "\n********\n";

        emailTextBody += "DATI SULLA ATTIVITA DI SMART WORKING\n";
        emailTextBody += "Proposta attività in smart working: " + jsonRequestSW.get("attivitaSmartWorking").toString() + "\n";
        emailTextBody += "\n********\n";

        emailTextBody += "Applicativi usati da internet: "
                + "gru: " + ((Boolean) jsonRequestSW.get("gru") ? "Si" : "No")
                + ", gaac: " + ((Boolean) jsonRequestSW.get("gaac") ? "Si" : "No")
                + ", babel: " + ((Boolean) jsonRequestSW.get("babel") ? "Si" : "No")
                + ", sirer: " + ((Boolean) jsonRequestSW.get("sirer") ? "Si" : "No")
                //                + ", nextcloud: " + ((Boolean)jsonRequestSW.get("nextcloud") ? "Si" : "No" )
                + "\n";
        emailTextBody += "Applicativi e software utilizzati in rete aziendale: " + jsonRequestSW.get("appUsate").toString() + "\n";
        emailTextBody += "Usa MySanità: " + ((Boolean) jsonRequestSW.get("mySanita") ? "Si" : "No") + "\n";
        emailTextBody += "Usa cartelle condivise: " + ((Boolean) jsonRequestSW.get("cartelleCondivise") ? "Si" : "No") + "\n";
        emailTextBody += "\n********\n";

//        emailTextBody += "Il richiedente dichiara inoltre:" + "\n";
//        emailTextBody += "- di essere disponibile a verificare con il servizio ICT la compatibilità tecnica del pc personale con gli applicativi aziendali" + "\n";
//        emailTextBody += "- di essere consapevole che non tutti gli applicativi potranno essere resi compatibili con tale PC" + "\n";
//        emailTextBody += "- di essere disponibile a configurare il PC personale secondo le policy aziendali" + "\n";
//        emailTextBody += "- di aver preso visione dell'informativa sulla gestione della salute e sicurezza per i lavoratori in smartworking e di impegnarmi a seguire le indicazioni in essa contenute, nonché di rispettare le disposizioni aziendali in mateira di privacy" + "\n";
//        emailTextBody += "- di avere un sistema operativo pari o superiore a Windows 7 aggiornato con gli ultimi windows update, oppure un sistema operativo Linux o MAC (IOS) con il client RDP correttamente configurato" + "\n";
        List<String> dichiarazioniFinali = (List<String>) jsonRequestSW.get("dichiarazioniFinali");
//        String[] dichiarazioniFinali = new String[get.size()];
//        dichiarazioniFinali = get.toArray(dichiarazioniFinali);
        for (String d : dichiarazioniFinali) {
            emailTextBody += d + "\n";
        }
        emailTextBody += "\n********************************************************************************" + "\n\n";

        emailTextBody += idRichiesta + ";"
                + jsonRequestSW.get("codiceFiscale").toString() + ";"
                + jsonRequestSW.get("mailRichiedente").toString() + ";"
                + jsonRequestSW.get("numeroTel").toString() + ";"
                + jsonRequestSW.get("ip").toString() + " (" + jsonRequestSW.get("ip").toString().replace(".", " ") + ")" + ";"
                + ((Boolean) jsonRequestSW.get("mySanita") ? "Si" : "No");

        emailTextBody += "\n";

        Integer idAzienda = (Integer) jsonRequestSW.get("idAzienda");

        if (!sendMail(idAzienda, accountFrom, subject, to, emailTextBody, cc, null, null, null)) {
            throw new SendMailException("Fallito invio mail");
        }
    }

    private Integer salvaRichiestaNelDB(Map<String, Object> jsonRequestSW) {
        Utente utenteRichiedente = utenteRepository.getOne((Integer) jsonRequestSW.get("idUtenteRichiedente"));
        Object idUtenteResponsabile = jsonRequestSW.get("idUtenteResponsabile");
        Utente utenteResponsabile = null;
        if (idUtenteResponsabile != null) {
            utenteResponsabile = utenteRepository.getOne((Integer) idUtenteResponsabile);
        }

        Azienda a = aziendaRepository.getOne((Integer) jsonRequestSW.get("idAzienda"));
        LocalDate periodoDalDate = LocalDate.parse(jsonRequestSW.get("periodoDal").toString(), DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        LocalDate periodoAlDate = LocalDate.parse(jsonRequestSW.get("periodoAl").toString(), DateTimeFormatter.ofPattern("dd/MM/yyyy"));

        RichiestaSmartWorking r = new RichiestaSmartWorking();
        r.setIdUtenteRichiedente(utenteRichiedente);
        r.setRichiedente((String) jsonRequestSW.get("richiedente"));
        r.setCodiceFiscale((String) jsonRequestSW.get("codiceFiscale"));
        r.setMailRichiedente((String) jsonRequestSW.get("mailRichiedente"));
        r.setResponsabile((String) jsonRequestSW.get("responsabile"));
        r.setIdUtenteResponsabile(utenteResponsabile);
        r.setMailResponsabile((String) jsonRequestSW.get("mailResponsabile"));
        r.setMailUfficioPersonale((String) jsonRequestSW.get("mailUfficioPersonale"));
        r.setPeriodoDal(periodoDalDate);
        r.setPeriodoAl(periodoAlDate);
        r.setMotivazione((String) jsonRequestSW.get("motivazione"));
        r.setHoPostazioneEsclusiva((Boolean) jsonRequestSW.get("hoPostazioneEsclusiva"));
        r.setPostazioneIp((Boolean) jsonRequestSW.get("postazioneIp"));
        r.setIp((String) jsonRequestSW.get("ip"));
        r.setIdAzienda(a);
        r.setAzienda((String) jsonRequestSW.get("azienda"));
        r.setSede((String) jsonRequestSW.get("sede"));
        r.setPc((String) jsonRequestSW.get("pc"));
        r.setInventario((String) jsonRequestSW.get("inventario"));
        r.setNomePc((String) jsonRequestSW.get("nomePc"));
//        r.setSistemaOperativo((String)jsonRequestSW.get("sistemaOperativo"));
        r.setConnettivitaDomestica((Boolean) jsonRequestSW.get("connettivitaDomestica"));
        r.setNumeroTel(jsonRequestSW.get("numeroTel").toString());
        Boolean hoCellulareAziendale = (Boolean) jsonRequestSW.get("hoCellulareAziendale");
        r.setHoCellulareAziendale(hoCellulareAziendale);
        if (hoCellulareAziendale) {
            r.setNumeroCellAziendale(jsonRequestSW.get("numeroCellAziendale").toString());
        }
        r.setContattabilita((String) jsonRequestSW.get("contattabilita"));
        r.setVpn((Boolean) jsonRequestSW.get("vpn"));
        r.setFirma((Boolean) jsonRequestSW.get("firma"));
        r.setHaLettoreSmartCard((Boolean) jsonRequestSW.get("haLettoreSmartCard"));
        r.setAttivitaSmartWorking((String) jsonRequestSW.get("attivitaSmartWorking"));
        r.setGru((Boolean) jsonRequestSW.get("gru"));
        r.setGaac((Boolean) jsonRequestSW.get("gaac"));
        r.setBabel((Boolean) jsonRequestSW.get("babel"));
        r.setSirer((Boolean) jsonRequestSW.get("sirer"));
//        r.setNextcloud((Boolean)jsonRequestSW.get("nextcloud"));
        r.setAppUsate((String) jsonRequestSW.get("appUsate"));
        r.setCartelleCondivise((Boolean) jsonRequestSW.get("cartelleCondivise"));
        r.setIctInvolved((Boolean) jsonRequestSW.get("ictInvolved"));
        r.setNonTuttoAndra((Boolean) jsonRequestSW.get("nonTuttoAndra"));
        r.setPolicy((Boolean) jsonRequestSW.get("policy"));
        r.setVisionatoInformativa((Boolean) jsonRequestSW.get("visionatoInformativa"));
        r.setMailict((String) jsonRequestSW.get("mailICT"));

        List<String> get = (List<String>) jsonRequestSW.get("dichiarazioniFinali");
        String[] dichiarazioniFinali = new String[get.size()];
        dichiarazioniFinali = get.toArray(dichiarazioniFinali);
        r.setDichiarazioniFinali(dichiarazioniFinali);

        r.setMySanita((Boolean) jsonRequestSW.get("mySanita"));

        RichiestaSmartWorking saveAndFlush = richiestaSmartWorkingRepository.saveAndFlush(r);
        return saveAndFlush.getId();
    }

    /**
     * Torna un oggetto che contiene varie informazioni relative al client.
     * Attualmente torna indirizzo ip e nome del computer (hostName).
     *
     * @param request
     * @return
     */
    @RequestMapping(value = {"getClientInfo"}, method = RequestMethod.GET)
    public ResponseEntity<JSONObject> getClientInfo(HttpServletRequest request) {
        String ipAddress = request.getHeader("X-FORWARDED-FOR");
        if (ipAddress == null) {
            ipAddress = request.getRemoteAddr();
        }
        if (ipAddress.equals("0:0:0:0:0:0:0:1")) {
            try {
                ipAddress = java.net.InetAddress.getLocalHost().getCanonicalHostName();
            } catch (UnknownHostException e) {
                ipAddress = null;
            }
        }
        String hostName = null;
        if (ipAddress != null) {
            hostName = getHostName(ipAddress);
        }

        JSONObject o = new JSONObject();
        o.put("ip", ipAddress);
        o.put("hostName", hostName);

        // Esempio di HostName che riesco a prendere
//        String hostName = getHostName("172.23.100.225");
//        LOGGER.info("172.23.100.225" + hostName);
        return new ResponseEntity(o, HttpStatus.OK);
    }

    /**
     * Dato un ip viene tornato, quando possibile, il nome del computer
     * corrispondente.
     *
     * @param ip
     * @return
     */
    private String getHostName(String ip) {
        String computerName = null;
        try {
            InetAddress inetAddress = InetAddress.getByName(ip);
            computerName = inetAddress.getHostName();
            if (computerName.equalsIgnoreCase("localhost")) {
                computerName = java.net.InetAddress.getLocalHost().getCanonicalHostName();
            }
        } catch (UnknownHostException e) {
            LOGGER.error("UnknownHostException detected in StartAction. ", e);
        }
        return computerName;
    }

    /**
     * Api per inviare una segnalazione utente via mail al servizio di
     * assistenza
     *
     * @param segnalazioneUtente La form da inviare
     * @param result Risultato del mapping dei dati arrivati con la classe
     * @return Http response
     */
    @RequestMapping(value = "/inviaSegnalazione", method = RequestMethod.POST)
    public ResponseEntity<?> inviaSegnalazione(@Valid @ModelAttribute() Segnalazione segnalazioneUtente,
            BindingResult result,
            HttpServletRequest request) throws IOException {
        projectionsInterceptorLauncher.setRequestParams(null, request);
        if (result.hasErrors()) {
            return new ResponseEntity(HttpStatus.BAD_REQUEST);
        }
        
        // variabili che conterranno i dati relativi alle due segnalazioni (JIRA,Redmine) dopo averle create
        JSONObject jiraResponse = null;
        Integer numeroNuovaSegnalazioneRedmine = null;
        
        // ottengo il parametro che mi dirà se dovro creare il ticket su jira
        boolean useJiraForCustomerSupportParamiterValue = false;
        List<ParametroAziende> parametersUseJiraForCustomerSupport = parametriAziendaReader.getParameters("useJiraForCustomerSupport");
        if (!parametersUseJiraForCustomerSupport.isEmpty()){
            useJiraForCustomerSupportParamiterValue = parametersUseJiraForCustomerSupport.get(0).getValore().equals("true");
        }
        
        // ottengo il parametro che mi dirà se dovro creare il ticket su redmine
        boolean useRedmineForCustomerSupportParamiterValue = false;
        List<ParametroAziende> parametersUseRedmineForCustomerSupport = parametriAziendaReader.getParameters("useRedmineForCustomerSupport");
        if (!parametersUseRedmineForCustomerSupport.isEmpty()){
            useRedmineForCustomerSupportParamiterValue = parametersUseRedmineForCustomerSupport.get(0).getValore().equals("true");
        }
        
        
        if (useJiraForCustomerSupportParamiterValue){
            // voglio usare jira
            JSONObject jsonForJira = null;
            JSONObject jiraApiToken = null;
            
            // ottengo il nome dell'azienda e genero il JSON per jira
            Azienda azienda = aziendaRepository.getByDescrizione(segnalazioneUtente.getAzienda());
            List<ParametroAziende> parametersCodiciJiraProject = parametriAziendaReader.getParameters("keyBabelFormJiraProject", new Integer[]{azienda.getId()});
            List<ParametroAziende> parametersJiraAccountID = parametriAziendaReader.getParameters("jiraAccountID");
            if (!parametersCodiciJiraProject.isEmpty()){
                ToolsUtils toolsUtils = new ToolsUtils();
                String jiraAccountID = parametersJiraAccountID.get(0).getValore().replaceAll("\"", "");
                String jiraCodiceProgetto = parametersCodiciJiraProject.get(0).getValore().replaceAll("\"", "");
                jsonForJira = toolsUtils.getJSONForJira(segnalazioneUtente, jiraAccountID, jiraCodiceProgetto);
            }
            
            // ottengo il token per creare su jira
            List<ParametroAziende> parametersJiraApiToken = parametriAziendaReader.getParameters("jiraApiToken");
            if (!parametersJiraApiToken.isEmpty()){
                jiraApiToken =  new JSONObject(parametersJiraApiToken.get(0).getValore());
            }
            
            // creo il ticket
            if (jsonForJira != null){
                OkHttpClient client = new OkHttpClient();
                String credential = Credentials.basic(jiraApiToken.getString("user"), jiraApiToken.getString("token"));
                String jiraTicketCreateUrl = null;
                // ottengo il url per creare il ticket su jira
                List<ParametroAziende> parametersJiraTicketCreateUrl = parametriAziendaReader.getParameters("jiraTicketCreateUrl");
                if (!parametersJiraTicketCreateUrl.isEmpty()){
                    jiraTicketCreateUrl = parametersJiraTicketCreateUrl.get(0).getValore().replaceAll("\"", "");
                }
                if (jiraTicketCreateUrl != null){
                    
                    okhttp3.RequestBody requestBody = okhttp3.RequestBody.create(okhttp3.MediaType.parse("application/json; charset=utf-8"), jsonForJira.toString());
                    Request requestTicket = new Request.Builder()
                        .url(jiraTicketCreateUrl)
                        .addHeader("Authorization", credential)
                        .addHeader("Accept", "application/json")
                        .addHeader("Content-Type", "application/json")
                        .post(requestBody)
                        .build();
                    Response response = client.newCall(requestTicket).execute();
                    if (!response.isSuccessful()){
                        response.body().close();
                        return new ResponseEntity("Errore durante la creazione del ticket di supporto.", HttpStatus.INTERNAL_SERVER_ERROR);
                    }
                    jiraResponse = new JSONObject(response.body().string());
                    response.body().close();
                } else{
                    return new ResponseEntity("Errore durante la creazione del ticket di supporto.", HttpStatus.INTERNAL_SERVER_ERROR);
                    
                }
                
                // allego gli allegati
                if (segnalazioneUtente.getAllegati() != null){
                    String jiraTicketAttachmentsUrl = null;
                    // ottengo il url per creare il ticket su jira
                    List<ParametroAziende> parametersJiraTicketAttachmentsUrl = parametriAziendaReader.getParameters("jiraTicketAttachmentsUrl");
                    if (!parametersJiraTicketAttachmentsUrl.isEmpty()){
                        jiraTicketAttachmentsUrl = parametersJiraTicketAttachmentsUrl.get(0).getValore().replaceAll("\"", "");
                    }
                    if (jiraTicketAttachmentsUrl != null){
                        MultipartBody.Builder requestAttachmentsBody = new MultipartBody.Builder();
                        requestAttachmentsBody.setType(MultipartBody.FORM);
                        for (MultipartFile multipartFile : segnalazioneUtente.getAllegati()) {
                            File convFile = new File(System.getProperty("java.io.tmpdir")+"/"+ multipartFile.getOriginalFilename());
                            multipartFile.transferTo(convFile);
                            requestAttachmentsBody.addFormDataPart("file", convFile.getName(), okhttp3.RequestBody.create(okhttp3.MediaType.parse("text/plain"), convFile));
                        }
                        okhttp3.RequestBody requestAttachmentsBodyBuilded = requestAttachmentsBody.build();
                        Request requestAttachments = new Request.Builder()
                                .url(jiraTicketAttachmentsUrl.replace("$TICKET_KEY$", jiraResponse.getString("key")))
                                .addHeader("Authorization", credential)
                                .addHeader("X-Atlassian-Token", "no-check")
                                .post(requestAttachmentsBodyBuilded)
                                .build();
                        Response responseAttachments = client.newCall(requestAttachments).execute();
                        if (!responseAttachments.isSuccessful()){
                            responseAttachments.body().close();
                            return new ResponseEntity("Errore relativo agli allegati del ticket di supporto.", HttpStatus.INTERNAL_SERVER_ERROR);
                        }
                        responseAttachments.body().close();
                    }else{
                        return new ResponseEntity("Errore relativo agli allegati del ticket di supporto.", HttpStatus.INTERNAL_SERVER_ERROR);
                    }
                }
            }else{
                return new ResponseEntity("Errore durante la creazione del ticket di supporto.", HttpStatus.INTERNAL_SERVER_ERROR);
            }
        } 
        if (useRedmineForCustomerSupportParamiterValue){
            // voglio usare redmine
//            Integer numeroNuovaSegnalazione = null;
            if (!redmineTestMode) {
                try {
                    MiddleMineNewIssueManager middleMineNewIssueManager = MiddleMineManagerFactory.getAndBuildMiddleMineNewIssueManager();
                    ResponseEntity<String> res = middleMineNewIssueManager.postNewIssue(segnalazioneUtente);
                    MiddleMineNewIssueResponseManager resManager = new MiddleMineNewIssueResponseManager();
                    numeroNuovaSegnalazioneRedmine = resManager.getNewIssueIdByResponse(res);
                    LOGGER.info("Numero segnalazione: " + numeroNuovaSegnalazioneRedmine);
                    if (numeroNuovaSegnalazioneRedmine == null) {
                        LOGGER.error("Errore nella creazione dell'id della segnalazione");
                    }
                } catch (Exception e) {
                    LOGGER.error("Errore nella creazione della nuova segnlazione: ", e);
                }
            }
            
            // Se sono attive le segnalazioni jira, le mail ci pensa jira.
            // qui devo mandare mail solo se jira è spento.
            if (!useJiraForCustomerSupportParamiterValue){
                // Prendo l'utente loggato
                Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
                Utente utente = (Utente) authentication.getPrincipal();

                // Build dei campi della mail da inviare
                String fromName = segnalazioneUtente.getMail();
                String subject;
                if (numeroNuovaSegnalazioneRedmine != null) {
                    subject = "(Segnalazione " + numeroNuovaSegnalazioneRedmine + ") " + segnalazioneUtente.getOggetto();
                } else {
                    subject = segnalazioneUtente.getOggetto();
                }
                List<String> to = Arrays.asList(emailCustomerSupport);

                ToolsUtils toolsUtils = new ToolsUtils();
                // Build body mail da inviare al servizio assistenza
                String bodyCustomerSupport = toolsUtils.buildMailForCustomerSupport(segnalazioneUtente, numeroNuovaSegnalazioneRedmine, baborgUtils);
                // Build body mail da inviare all'utente
                String bodyUser = toolsUtils.buildMailForUser(bodyCustomerSupport, numeroNuovaSegnalazioneRedmine);
                List<String> replyToUsers = Arrays.asList(fromName);

                List<MultipartFile> allegatiList = null;
                if (segnalazioneUtente.getAllegati() != null) {
                    allegatiList = Arrays.asList(segnalazioneUtente.getAllegati());
                }
                try {
                    simpleMailSenderUtility.sendMail(
                            utente.getIdAzienda().getId(),
                            nameCustomerSupport,
                            subject,
                            to,
                            bodyCustomerSupport,
                            null,
                            null,
                            allegatiList,
                            replyToUsers,
                            true);
                } catch (IOException ex) {
                    return new ResponseEntity("Errore durante l'invio della mail al servizio assistenza.", HttpStatus.INTERNAL_SERVER_ERROR);
                }

                try {
                    List<String> toUser = Arrays.asList(fromName);

                    simpleMailSenderUtility.sendMail(
                            utente.getIdAzienda().getId(),
                            nameCustomerSupport,
                            subject,
                            toUser,
                            bodyUser,
                            null,
                            null,
                            null,
                            null,
                            true);
                } catch (IOException ex) {
                    return new ResponseEntity("Errore durante l'invio della mail all'utente.", HttpStatus.INTERNAL_SERVER_ERROR);
                }

                String tipologiaSegnalazione = segnalazioneUtente.getTipologiaSegnalazione();
                if (StringUtils.hasText(tipologiaSegnalazione) && tipologiaSegnalazione.equals("CORREZIONE_DOCUMENTALE")) {
                    try {
                        List<String> toAutorizzatore = Arrays.asList(segnalazioneUtente.getEmailAutorizzatore());
                        String introPerAutorizzatore = "Questa è una segnalazione di richiesta modifica da parte dell'utente "
                                + utente.getIdPersona().getDescrizione() + ".\nVedi sotto il dettaglio e rispondi alla mail per autorizzare babelcare a procedere\n\n";
                        bodyCustomerSupport = introPerAutorizzatore + bodyCustomerSupport;
                        List<String> replyToBabelcare = Arrays.asList("babel.care@ausl.bologna.it");

                        simpleMailSenderUtility.sendMail(
                                utente.getIdAzienda().getId(),
                                nameCustomerSupport,
                                subject,
                                toAutorizzatore,
                                bodyCustomerSupport,
                                null,
                                null,
                                allegatiList,
                                replyToBabelcare,
                                true);
                    } catch (IOException ex) {
                        return new ResponseEntity("Errore durante l'invio della mail all'autorizzatore.", HttpStatus.INTERNAL_SERVER_ERROR);
                    }
                }
            }
        }

        return new ResponseEntity("Successfully sent!", HttpStatus.OK);
    }

    @RequestMapping(value = "downloadCSVModel", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public void downloadCSVModel(@RequestParam(required = true) Integer idAzienda, @RequestParam(required = true) String idApplicazione, @RequestParam(required = true) String filename, HttpServletResponse response, HttpServletRequest request) throws FileNotFoundException {

        String modelloCSV = "";
        List<ParametroAziende> parameters = parametriAziende.getParameters(filename, new Integer[]{idAzienda}, new String[]{idApplicazione});
        if (parameters != null && !parameters.isEmpty()) {
            modelloCSV = parametriAziende.getValue(parameters.get(0), String.class);
        } else {
            LOGGER.error("manca il parametro pubblico");
        }

        MinIOWrapper minIOWrapper = mongoConnectionManager.getMinIOWrapper();
        InputStream is = null;

        try {
            try {
                is = minIOWrapper.getByFileId(modelloCSV);

                if (is == null) {
                    throw new MongoException("File non trovato!!");
                }
            } catch (Exception e) {
                throw new MongoException("qualcosa è andato storto in downloadCSVModel", e);
            }
            response.setHeader("Content-Type", "text/csv");
            StreamUtils.copy(is, response.getOutputStream());
        } catch (IOException ex) {
            java.util.logging.Logger.getLogger(BaborgUtils.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            IOUtils.closeQuietly(is);
        }

    }
}
