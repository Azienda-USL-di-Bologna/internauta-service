package it.bologna.ausl.internauta.service.controllers.tools;

import it.bologna.ausl.internauta.service.controllers.scrivania.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.querydsl.core.types.dsl.BooleanExpression;
import it.bologna.ausl.blackbox.exceptions.BlackBoxPermissionException;
import it.bologna.ausl.internauta.service.authorization.AuthenticatedSessionData;
import it.bologna.ausl.internauta.service.authorization.AuthenticatedSessionDataBuilder;
import it.bologna.ausl.internauta.utils.bds.types.CategoriaPermessiStoredProcedure;
import it.bologna.ausl.internauta.utils.bds.types.PermessoEntitaStoredProcedure;
import it.bologna.ausl.internauta.utils.bds.types.PermessoStoredProcedure;
import it.bologna.ausl.internauta.service.authorization.UserInfoService;
import it.bologna.ausl.internauta.service.configuration.nextsdr.RestControllerEngineImpl;
import it.bologna.ausl.internauta.service.exceptions.http.ControllerHandledExceptions;
import it.bologna.ausl.internauta.service.exceptions.http.Http400ResponseException;
import it.bologna.ausl.internauta.service.exceptions.http.Http403ResponseException;
import it.bologna.ausl.internauta.service.exceptions.http.Http404ResponseException;
import it.bologna.ausl.internauta.service.exceptions.http.Http500ResponseException;
import it.bologna.ausl.internauta.service.exceptions.http.HttpInternautaResponseException;
import it.bologna.ausl.internauta.service.repositories.baborg.AziendaRepository;
import it.bologna.ausl.internauta.service.repositories.baborg.PersonaRepository;
import it.bologna.ausl.internauta.service.repositories.baborg.StrutturaRepository;
import it.bologna.ausl.internauta.service.repositories.configurazione.ApplicazioneRepository;
import it.bologna.ausl.internauta.service.repositories.scrivania.AttivitaRepository;
import it.bologna.ausl.internauta.service.scrivania.anteprima.BabelDownloader;
import it.bologna.ausl.internauta.service.scrivania.anteprima.BabelDownloaderResponseBody;
import it.bologna.ausl.internauta.service.utils.CachedEntities;
import it.bologna.ausl.internauta.service.utils.InternautaUtils;
import it.bologna.ausl.model.entities.baborg.Azienda;
import it.bologna.ausl.model.entities.baborg.AziendaParametriJson;
import it.bologna.ausl.model.entities.baborg.Persona;
import it.bologna.ausl.model.entities.baborg.Struttura;
import it.bologna.ausl.model.entities.baborg.Utente;
import it.bologna.ausl.model.entities.configuration.Applicazione;
import it.bologna.ausl.model.entities.scrivania.Attivita;
import it.bologna.ausl.model.entities.scrivania.Attivita.TipoAttivita;
import it.bologna.ausl.model.entities.scrivania.QAttivita;
import it.nextsw.common.annotations.NextSdrRepository;
import it.nextsw.common.controller.exceptions.NotFoundResourceException;
import it.nextsw.common.controller.exceptions.RestControllerEngineException;
import it.nextsw.common.interceptors.exceptions.AbortSaveInterceptorException;
import it.nextsw.common.utils.CommonUtils;
import it.nextsw.common.utils.EntityReflectionUtils;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import okhttp3.Response;
import org.jose4j.json.internal.json_simple.JSONArray;
import org.jose4j.json.internal.json_simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StreamUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;


//import per mandare mail 
import com.sun.mail.smtp.SMTPTransport;
import static com.sun.tools.internal.xjc.reader.Ring.add;
import java.net.InetAddress;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.util.Date;
import java.util.Objects;
import java.util.Properties;

/**
 *
 * @author gdm
 */



@RestController
@RequestMapping(value = "${tools.mapping.url.root}")
public class ToolsCustomController implements ControllerHandledExceptions {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(ToolsCustomController.class);
    //dati per mandare la mail

    
    
    //per parametri pubblici?
    @Autowired
    private AziendaRepository aziendaRepository;
    @Autowired
    private CachedEntities cachedEntities;
    @Autowired
    private ObjectMapper objectMapper;
   
    public Boolean sendMail(Integer idAzienda, String fromName, String Subject, String To, String body, List<String> cc, List<String> bcc ) throws IOException{
        
        Azienda azienda = cachedEntities.getAzienda(idAzienda);
        AziendaParametriJson aziendaParametri = AziendaParametriJson.parse(objectMapper, azienda.getParametri());
        AziendaParametriJson.MailParams mailParams=aziendaParametri.getMailParams(); 
        
        if (mailParams != null) {
            String smtpServer=mailParams.getMailServerSmtpUrl();
            Integer port=mailParams.getMailServerSmtpPort();
            
            String username=null;
            String password=null; 
        
        
        Properties prop = System.getProperties();
        prop.put("mail.smtp.host", smtpServer); //optional, defined in SMTPTransport
        
        if (StringUtils.isEmpty(username) && StringUtils.isEmpty(password)){
            prop.put("mail.smtp.auth", "false");
        }else{
            prop.put("mail.smtp.auth", "true");
        }
        
        if (port != null && port != -1 ){
            prop.put("mail.smtp.port", port.toString()); // default port 25
        }else{
            prop.put("mail.smtp.port", "25");
        }
        
        Session session = Session.getInstance(prop, null);
        Message msg = new MimeMessage(session); 
       
        try {
            String mailFrom = mailParams.getMailFrom();
            // from
            msg.setFrom(new InternetAddress (mailFrom,fromName));
            

            // to 
            msg.setRecipients(Message.RecipientType.TO,
                    InternetAddress.parse(To, false));
            //
            
            if (cc!=null && !cc.isEmpty()){
			// cc non so se va bene sicuramente no
                for (String ccElement : cc) {
                    msg.setRecipients(Message.RecipientType.CC,
                        InternetAddress.parse(ccElement, false));
                    }
            }
            
            if (bcc!=null && !bcc.isEmpty()){
			// cc non so se va bene sicuramente no
                for (String bccElement : bcc) {
                    msg.setRecipients(Message.RecipientType.CC,
                        InternetAddress.parse(bccElement, false));
                    }
            }
            
            // subject
            LocalDateTime today = LocalDateTime.now();
            ZoneId id = ZoneId.of("Europe/Rome");
            ZonedDateTime zonedDateTime = ZonedDateTime.of(today, id);      //That's how you add timezone to date
            String formattedDateTime = DateTimeFormatter
                            .ofPattern("dd/MM/yyyy - HH:mm")
                            .format(zonedDateTime);             //  esempio 11/03/2020 ore 10.44
            
            msg.setSubject(Subject+ " "+ formattedDateTime);
            // content 
            msg.setText(body);
            msg.setSentDate(new Date());
            // Get SMTPTransport
            SMTPTransport t = (SMTPTransport) session.getTransport("smtp");
            // connect
            
            //attenzione verificare come funziona
            
            t.connect(smtpServer, username, password);
            

        // send
            t.sendMessage(msg, msg.getAllRecipients());
            System.out.println("Response: " + t.getLastServerResponse());
            t.close();
        } catch (MessagingException e) {
            e.printStackTrace();
            return false;
        }
        return true;
        
        }
        return false;
    }
    
//     @RequestMapping(value = {"testMailSend"}, method = RequestMethod.GET)
//    public void test() throws IOException{
//        sendMail(2, "prova.smart@ausl.bo.it", "test", "", "prova", new ArrayList<String>(), new ArrayList<String>());
//    }
    
    
    
    @RequestMapping(value = {"sendSmartWorkingMail"}, method = RequestMethod.POST)
    public void sendSmartWorkingMail( @RequestBody Map<String, Object> jsonRequestSW) throws HttpInternautaResponseException, IOException, BlackBoxPermissionException {
 
        
        String smtpServer;
        String username =null;
        String password=null;
        
        String accountFrom="smartworking@auslbo.it";
        
        String Subject="Richiesta SMART WORKING";
        
        String emailTextBody = "Dati del richiedente:\n";
        
        String to=jsonRequestSW.get("mailCentroDiGestione").toString();
        
        List<String> cc = null;
        cc.add(jsonRequestSW.get("mail").toString());
        //data e ora odierni
        
        LocalDateTime today = LocalDateTime.now();
        ZoneId id = ZoneId.of("Europe/Rome");
        ZonedDateTime zonedDateTime = ZonedDateTime.of(today, id);      //That's how you add timezone to date
        String formattedDateTime = DateTimeFormatter
                .ofPattern("dd/MM/yyyy ore HH:mm")
                .format(zonedDateTime);             //11/03/2020 ore 10.44
         
        Subject=Subject+jsonRequestSW.get("richiedente").toString()+ " "+ formattedDateTime;
        emailTextBody=emailTextBody+"Richiedente: "+jsonRequestSW.get("richiedente").toString() + "\n";
        emailTextBody=emailTextBody+"CF: "+jsonRequestSW.get("CF").toString()+ "\n";
        emailTextBody=emailTextBody+"Mail del richiedente: "+jsonRequestSW.get("mail").toString()+ "\n";
        emailTextBody=emailTextBody+"Periodo richiesto dal: "+jsonRequestSW.get("periodoDal").toString()+"al: "+jsonRequestSW.get("periodoAl").toString()+ "\n";
        emailTextBody=emailTextBody+"Motivazione: "+jsonRequestSW.get("motivazione").toString()+ "\n";
        emailTextBody=emailTextBody+"********";
        emailTextBody=emailTextBody+"Dati della postazione di lavoro: \n";
        emailTextBody=emailTextBody+"IP: "+jsonRequestSW.get("ip").toString()+ "\n";
        emailTextBody=emailTextBody+"Ubicazione: "+jsonRequestSW.get("azienda").toString()+ " - "+jsonRequestSW.get("sede")+ "\n";
        emailTextBody=emailTextBody+"********";
        emailTextBody=emailTextBody+"Dati del richiedente: \n";
        emailTextBody=emailTextBody+"Profilo professionale: "+jsonRequestSW.get("profiloProfessionale").toString()+ "\n";
        emailTextBody=emailTextBody+"Mansione: "+jsonRequestSW.get("mansione").toString()+ "\n";
        emailTextBody=emailTextBody+"Responsabile: "+jsonRequestSW.get("responsabile").toString()+ "\n";
        emailTextBody=emailTextBody+"Mail del responsabile: "+jsonRequestSW.get("mailResponsabile").toString()+ "\n";
        emailTextBody=emailTextBody+"Centro di gestione: "+jsonRequestSW.get("mailCentroDiGestione").toString()+ "\n";
        emailTextBody=emailTextBody+"********";
        emailTextBody=emailTextBody+"Dati della postazione smart working: \n";
        emailTextBody=emailTextBody+"Pc Personale: "+jsonRequestSW.get("pcPersonale").toString()+ "\n";
        emailTextBody=emailTextBody+"Pc Aziendale: "+jsonRequestSW.get("pcAziendale").toString()+ "\n";
        emailTextBody=emailTextBody+"Sistema Operativo: "+jsonRequestSW.get("sistemaOperativo").toString()+ "\n";
        emailTextBody=emailTextBody+"Connettività Domestica: "+jsonRequestSW.get("connettivitaDomestica").toString()+ "\n";
        emailTextBody=emailTextBody+"Numero di telefono di contatto: "+jsonRequestSW.get("numeroTel").toString()+ "\n";
        emailTextBody=emailTextBody+"Ho il cellulare aziendale: "+jsonRequestSW.get("hoCellulareAziendale").toString()+ "\n";
        emailTextBody=emailTextBody+"Numero del cellulare aziendale: "+jsonRequestSW.get("cellulareAziendale").toString()+ "\n";
        emailTextBody=emailTextBody+"Disponibilità: "+jsonRequestSW.get("contattabilita").toString()+ "\n";
        emailTextBody=emailTextBody+"VPN attiva: "+jsonRequestSW.get("vpn").toString()+ "\n";
        emailTextBody=emailTextBody+"Firma Digitale: "+jsonRequestSW.get("firma").toString()+ "\n";
        emailTextBody=emailTextBody+"Lettore smart card: "+jsonRequestSW.get("haLettoreSmartCard").toString()+ "\n";
        emailTextBody=emailTextBody+"********";
        emailTextBody=emailTextBody+"Dati sull'attività di smart working: \n";
        emailTextBody=emailTextBody+"Proposta attività in smart working: "+jsonRequestSW.get("attivitaSW").toString()+ "\n";
        emailTextBody=emailTextBody+"Applicativi usati da internet: gru:"+jsonRequestSW.get("gru").toString()+ ", gaac: "+jsonRequestSW.get("gaac").toString()+ ", babel: "+jsonRequestSW.get("babel").toString()+ ", sirer: "+jsonRequestSW.get("sirer").toString()+ ", nextcloud: "+jsonRequestSW.get("nextcloud").toString()+ "\n";
        emailTextBody=emailTextBody+"Applicativi e software utilizzati in rete aziendale: "+jsonRequestSW.get("appUsate").toString()+ "\n";
        emailTextBody=emailTextBody+"Usa cartelle condivise: "+jsonRequestSW.get("cartelleCondivise").toString()+ "\n";
              
        Integer idAzienda = (Integer) jsonRequestSW.get("idAzienda");        
        sendMail(idAzienda, accountFrom, Subject, to, emailTextBody, cc, null);
        
    }
    
    
}
