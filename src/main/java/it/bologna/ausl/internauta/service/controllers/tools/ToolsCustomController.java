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
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.UnknownHostException;
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
import java.util.Enumeration;
import java.util.Properties;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/**
 *
 * @author gdm
 */
@RestController
@RequestMapping(value = "${tools.mapping.url.root}")
public class ToolsCustomController implements ControllerHandledExceptions {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(ToolsCustomController.class);
    //dati per mandare la mail
    private static final String SMTP_SERVER = "smtp server ";
    private static final String USERNAME = "";
    private static final String PASSWORD = "";

    private static final String EMAIL_FROM = "From@gmail.com";

    private static final String EMAIL_SUBJECT = "richiesta SMART WORKING - ";
    private static String EMAIL_TEXT = "Dati del richiedente:\n";

   
    @RequestMapping(value = {"sendSmartWorkingMail"}, method = RequestMethod.POST)
    public void sendSmartWorkingMail( @RequestBody Map<String, Object> jsonRequestSW) throws HttpInternautaResponseException, IOException, BlackBoxPermissionException {
        
        Properties prop = System.getProperties();
        prop.put("mail.smtp.host", SMTP_SERVER); //optional, defined in SMTPTransport
        prop.put("mail.smtp.auth", "true");
        prop.put("mail.smtp.port", "25"); // default port 25
        Session session = Session.getInstance(prop, null);
        Message msg = new MimeMessage(session);
        
        
        
        
        try {
		
			// from
            msg.setFrom(new InternetAddress(EMAIL_FROM));

            // to 
            msg.setRecipients(Message.RecipientType.TO,
                    InternetAddress.parse(jsonRequestSW.get("mailCentroDiGestione").toString(), false));
            
			// cc
            msg.setRecipients(Message.RecipientType.CC,
                    InternetAddress.parse(jsonRequestSW.get("mail").toString(), false));

            // subject
            LocalDateTime today = LocalDateTime.now();
            ZoneId id = ZoneId.of("Europe/Rome");
            ZonedDateTime zonedDateTime = ZonedDateTime.of(today, id);      //That's how you add timezone to date
            String formattedDateTime = DateTimeFormatter
                            .ofPattern("dd/MM/yyyy ore HH:mm")
                            .format(zonedDateTime);             //11/03/2020 ore 10.44
            msg.setSubject(EMAIL_SUBJECT+jsonRequestSW.get("richiedente").toString()+ " "+ formattedDateTime);
 
            EMAIL_TEXT=EMAIL_TEXT+"Richiedente: "+jsonRequestSW.get("richiedente").toString() + "\n";
            EMAIL_TEXT=EMAIL_TEXT+"CF: "+jsonRequestSW.get("CF").toString()+ "\n";
            EMAIL_TEXT=EMAIL_TEXT+"Mail del richiedente: "+jsonRequestSW.get("mail").toString()+ "\n";
            EMAIL_TEXT=EMAIL_TEXT+"Periodo richiesto dal: "+jsonRequestSW.get("periodoDal").toString()+"al: "+jsonRequestSW.get("periodoAl").toString()+ "\n";
            EMAIL_TEXT=EMAIL_TEXT+"Motivazione: "+jsonRequestSW.get("motivazione").toString()+ "\n";
            EMAIL_TEXT=EMAIL_TEXT+"********";
            EMAIL_TEXT=EMAIL_TEXT+"Dati della postazione di lavoro: \n";
            EMAIL_TEXT=EMAIL_TEXT+"IP: "+jsonRequestSW.get("ip").toString()+ "\n";
            EMAIL_TEXT=EMAIL_TEXT+"Ubicazione: "+jsonRequestSW.get("azienda").toString()+ " - "+jsonRequestSW.get("sede")+ "\n";
            EMAIL_TEXT=EMAIL_TEXT+"********";
            EMAIL_TEXT=EMAIL_TEXT+"Dati del richiedente: \n";
            EMAIL_TEXT=EMAIL_TEXT+"Profilo professionale: "+jsonRequestSW.get("profiloProfessionale").toString()+ "\n";
            EMAIL_TEXT=EMAIL_TEXT+"Mansione: "+jsonRequestSW.get("mansione").toString()+ "\n";
            EMAIL_TEXT=EMAIL_TEXT+"Responsabile: "+jsonRequestSW.get("responsabile").toString()+ "\n";
            EMAIL_TEXT=EMAIL_TEXT+"Mail del responsabile: "+jsonRequestSW.get("mailResponsabile").toString()+ "\n";
            EMAIL_TEXT=EMAIL_TEXT+"Centro di gestione: "+jsonRequestSW.get("mailCentroDiGestione").toString()+ "\n";
            EMAIL_TEXT=EMAIL_TEXT+"********";
            EMAIL_TEXT=EMAIL_TEXT+"Dati della postazione smart working: \n";
            EMAIL_TEXT=EMAIL_TEXT+"Pc Personale: "+jsonRequestSW.get("pcPersonale").toString()+ "\n";
            EMAIL_TEXT=EMAIL_TEXT+"Pc Aziendale: "+jsonRequestSW.get("pcAziendale").toString()+ "\n";
            EMAIL_TEXT=EMAIL_TEXT+"Sistema Operativo: "+jsonRequestSW.get("sistemaOperativo").toString()+ "\n";
            EMAIL_TEXT=EMAIL_TEXT+"Connettività Domestica: "+jsonRequestSW.get("connettivitaDomestica").toString()+ "\n";
            EMAIL_TEXT=EMAIL_TEXT+"Numero di telefono di contatto: "+jsonRequestSW.get("numeroTel").toString()+ "\n";
            EMAIL_TEXT=EMAIL_TEXT+"Ho il cellulare aziendale: "+jsonRequestSW.get("hoCellulareAziendale").toString()+ "\n";
            EMAIL_TEXT=EMAIL_TEXT+"Numero del cellulare aziendale: "+jsonRequestSW.get("cellulareAziendale").toString()+ "\n";
            EMAIL_TEXT=EMAIL_TEXT+"Disponibilità: "+jsonRequestSW.get("contattabilita").toString()+ "\n";
            EMAIL_TEXT=EMAIL_TEXT+"VPN attiva: "+jsonRequestSW.get("vpn").toString()+ "\n";
            EMAIL_TEXT=EMAIL_TEXT+"Firma Digitale: "+jsonRequestSW.get("firma").toString()+ "\n";
            EMAIL_TEXT=EMAIL_TEXT+"Lettore smart card: "+jsonRequestSW.get("haLettoreSmartCard").toString()+ "\n";
            EMAIL_TEXT=EMAIL_TEXT+"********";
            EMAIL_TEXT=EMAIL_TEXT+"Dati sull'attività di smart working: \n";
            EMAIL_TEXT=EMAIL_TEXT+"Proposta attività in smart working: "+jsonRequestSW.get("attivitaSW").toString()+ "\n";
            EMAIL_TEXT=EMAIL_TEXT+"Applicativi usati da internet: gru:"+jsonRequestSW.get("gru").toString()+ ", gaac: "+jsonRequestSW.get("gaac").toString()+ ", babel: "+jsonRequestSW.get("babel").toString()+ ", sirer: "+jsonRequestSW.get("sirer").toString()+ ", nextcloud: "+jsonRequestSW.get("nextcloud").toString()+ "\n";
            EMAIL_TEXT=EMAIL_TEXT+"Applicativi e software utilizzati in rete aziendale: "+jsonRequestSW.get("appUsate").toString()+ "\n";
            EMAIL_TEXT=EMAIL_TEXT+"Usa cartelle condivise: "+jsonRequestSW.get("cartelleCondivise").toString()+ "\n";
			
			// content 
            msg.setText(EMAIL_TEXT);
			
            msg.setSentDate(new Date());

			// Get SMTPTransport
            SMTPTransport t = (SMTPTransport) session.getTransport("smtp");
			
			// connect
            t.connect(SMTP_SERVER, USERNAME, PASSWORD);
			
			// send
            t.sendMessage(msg, msg.getAllRecipients());

            System.out.println("Response: " + t.getLastServerResponse());

            t.close();

        } catch (MessagingException e) {
            e.printStackTrace();
        }
        
    }
    
    
    /**
     * Torna un oggetto che contiene varie informazioni relative al client.
     * Attualmente torna indirizzo ip e nome del computer (hostName).
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
}
