package it.bologna.ausl.internauta.service.controllers.tools;

import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.jose4j.json.internal.json_simple.JSONObject;
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
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.util.Date;
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
    
    //per parametri pubblici?
    @Autowired
    private AziendaRepository aziendaRepository;
    @Autowired
    private CachedEntities cachedEntities;
    @Autowired
    private ObjectMapper objectMapper;
   
    public Boolean sendMail(Integer idAzienda, String fromName, String Subject, List<String> To, String body, List<String> cc, List<String> bcc ) throws IOException{
        
        Azienda azienda = cachedEntities.getAzienda(idAzienda);
        AziendaParametriJson aziendaParametri = AziendaParametriJson.parse(objectMapper, azienda.getParametri());
        AziendaParametriJson.MailParams mailParams=aziendaParametri.getMailParams(); 
        
        if (mailParams != null) {
            String smtpServer=mailParams.getMailServerSmtpUrl();
            Integer port=mailParams.getMailServerSmtpPort();
            
            String username=null;
            String password=null; 
        
        Properties prop = System.getProperties();
        prop.put("mail.smtp.host", smtpServer);                                 //optional, defined in SMTPTransport
        
        if (StringUtils.isEmpty(username) && StringUtils.isEmpty(password)){
            prop.put("mail.smtp.auth", "false");
        }else{
            prop.put("mail.smtp.auth", "true");
        }
        
        if (port != null && port != -1 ){
            prop.put("mail.smtp.port", port.toString());                        // default port 25
        }else{
            prop.put("mail.smtp.port", "25");
        }
        
        Session session = Session.getInstance(prop, null);
        Message msg = new MimeMessage(session); 
       
        try {
            String mailFrom = mailParams.getMailFrom();
            // from
            msg.setFrom(new InternetAddress (mailFrom,fromName));

            // inserisco lista TO
            if (To!=null && !To.isEmpty()){
			// cc non so se va bene sicuramente no
                for (String toElement : To) {
                    msg.setRecipients(Message.RecipientType.TO,
                        InternetAddress.parse(toElement, false));
                    }
            }
            
//            msg.setRecipients(Message.RecipientType.TO,
//                    InternetAddress.parse(To, false));
 
            //inserico lista CC
            if (cc!=null && !cc.isEmpty()){
                for (String ccElement : cc) {
                    msg.setRecipients(Message.RecipientType.CC,
                        InternetAddress.parse(ccElement, false));
                    }
            }
            //inserisco lista BCC
            if (bcc!=null && !bcc.isEmpty()){
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
                            .format(zonedDateTime);             //  esempio 11/03/2020 - 10.44
            
            msg.setSubject(Subject+ " "+ formattedDateTime);
            // content 
            msg.setText(body);
            // msg.setContent(body, "text/html; charset=utf-8");
            msg.setSentDate(new Date());
            // Get SMTPTransport
            SMTPTransport t = (SMTPTransport) session.getTransport("smtp");
            // connect
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
    
    
    //funzione di prova per testare sendMail
//     @RequestMapping(value = {"testMailSend"}, method = RequestMethod.GET)
//    public void test() throws IOException{
//        sendMail(2, "prova.smart@ausl.bo.it", "test", "", "prova", new ArrayList<String>(), new ArrayList<String>());
//    }
    

    @RequestMapping(value = {"sendSmartWorkingMail"}, method = RequestMethod.POST)
    public void sendSmartWorkingMail(@RequestBody Map<String, Object> jsonRequestSW) throws HttpInternautaResponseException, IOException, BlackBoxPermissionException {
        
        String accountFrom="smartworking@auslbo.it";
        String Subject="Richiesta SMART WORKING";
        String emailTextBody = "";
        
        List<String> to = new ArrayList();
        to.add(jsonRequestSW.get("mailCentroDiGestione").toString());
        List<String> cc = new ArrayList();
        cc.add(jsonRequestSW.get("mail").toString());
        
        //data e ora odierni
        LocalDateTime today = LocalDateTime.now();
        ZoneId id = ZoneId.of("Europe/Rome");
        ZonedDateTime zonedDateTime = ZonedDateTime.of(today, id);      //That's how you add timezone to date
        String formattedDateTime = DateTimeFormatter
                .ofPattern("dd/MM/yyyy - HH:mm")
                .format(zonedDateTime);             //11/03/2020 - 10.44
        
        // 2020-03-02T23:00:00.000Z
        LocalDate periodoDalDate = LocalDate.parse(jsonRequestSW.get("periodoDal").toString(), DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"));
        LocalDate periodoAlDate = LocalDate.parse(jsonRequestSW.get("periodoAl").toString(), DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"));
        DateTimeFormatter outputFormatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");
        String periodoDal = outputFormatter.format(periodoDalDate);
        String periodoAl = outputFormatter.format(periodoAlDate);
        
        Subject = Subject + jsonRequestSW.get("richiedente").toString() + " " + formattedDateTime;
        
        emailTextBody += "DATI DEL RICHIEDENTE\n";
        emailTextBody += "Richiedente: " + jsonRequestSW.get("richiedente").toString() + "\n";
        emailTextBody += "Codice fiscale: " + jsonRequestSW.get("CF").toString()+ "\n";
        emailTextBody += "Mail del richiedente: " + jsonRequestSW.get("mail").toString()+ "\n";
        emailTextBody += "Profilo professionale: " + jsonRequestSW.get("profiloProfessionale").toString()+ "\n";
        emailTextBody += "Mansione: " + jsonRequestSW.get("mansione").toString()+ "\n";
        emailTextBody += "Responsabile: " + jsonRequestSW.get("responsabile").toString()+ "\n";
        emailTextBody += "Mail del responsabile: " + jsonRequestSW.get("mailResponsabile").toString()+ "\n";
        emailTextBody += "Centro di gestione: " + jsonRequestSW.get("mailCentroDiGestione").toString()+ "\n";
        emailTextBody += "\n********\n";
        
        emailTextBody += "MOTIVAZIONE\n";
        emailTextBody += "Periodo richiesto dal: " + periodoDal + " al: " + periodoAl + "\n";
        emailTextBody += "Motivazione: " + jsonRequestSW.get("motivazione").toString()+ "\n";
        emailTextBody += "\n********\n";
        
        emailTextBody += "DATI DELLA POSTAZIONE DI LAVORO\n";
        emailTextBody += "Utente con postazione esclusiva: " + ((Boolean)jsonRequestSW.get("hoPostazioneEsclusiva") ? "Si" : "No" ) + "\n";
        emailTextBody += "IP: " + jsonRequestSW.get("ip").toString()+ "\n";
        emailTextBody += "Ubicazione: " + jsonRequestSW.get("azienda").toString() + " - " + jsonRequestSW.get("sede") + "\n";
        emailTextBody += "\n********\n";
        
        emailTextBody += "DATI DELLA POSTAZIONE SMART WORKING\n";
        emailTextBody += "Possiede Pc Personale: " + ((Boolean)jsonRequestSW.get("pcPersonale") ? "Si" : "No" ) + "\n";
        emailTextBody += "Possiede Pc Aziendale: " + ((Boolean)jsonRequestSW.get("pcAziendale") ? "Si" : "No" ) + "\n";
        emailTextBody += "Nome Pc Aziendale: " + jsonRequestSW.get("idPcAziendale").toString() + "\n";
        emailTextBody += "Sistema Operativo: " + jsonRequestSW.get("sistemaOperativo").toString()+ "\n";
        emailTextBody += "Dispone di connettività domestica: " + ((Boolean)jsonRequestSW.get("connettivitaDomestica") ? "Si" : "No" ) + "\n";
        emailTextBody += "Numero di telefono di contatto: " + jsonRequestSW.get("numeroTel").toString()+ "\n";
        // emailTextBody += "Ho il cellulare aziendale: " + jsonRequestSW.get("hoCellulareAziendale").toString()+ "\n";
        emailTextBody += "Numero del cellulare aziendale: " + jsonRequestSW.get("cellulareAziendale").toString()+ "\n";
        emailTextBody += "Disponibilità: " + jsonRequestSW.get("contattabilita").toString()+ "\n";
        emailTextBody += "VPN attiva: " + ((Boolean)jsonRequestSW.get("vpn") ? "Si" : "No" ) + "\n";
        emailTextBody += "Firma Digitale: " + ((Boolean)jsonRequestSW.get("firma") ? "Si" : "No" ) + "\n";
        emailTextBody += "Lettore smart card: " + ((Boolean)jsonRequestSW.get("haLettoreSmartCard") ? "Si" : "No" ) + "\n";
        emailTextBody += "\n********\n";
        
        emailTextBody += "DATI SULLA ATTIVITA DI SMART WORKING\n";
        emailTextBody += "Proposta attività in smart working: " + jsonRequestSW.get("attivitaSW").toString()+ "\n";
        emailTextBody += "Applicativi usati da internet: " 
                + "gru: " + ((Boolean)jsonRequestSW.get("gru") ? "Si" : "No" )
                + ", gaac: " + ((Boolean)jsonRequestSW.get("gaac") ? "Si" : "No" ) 
                + ", babel: " + ((Boolean)jsonRequestSW.get("babel") ? "Si" : "No" )
                + ", sirer: " + ((Boolean)jsonRequestSW.get("sirer") ? "Si" : "No" )
                + ", nextcloud: " + ((Boolean)jsonRequestSW.get("nextcloud") ? "Si" : "No" )
                + "\n";
        emailTextBody += "Applicativi e software utilizzati in rete aziendale: " + jsonRequestSW.get("appUsate").toString()+ "\n";
        emailTextBody += "Usa cartelle condivise: " + ((Boolean)jsonRequestSW.get("cartelleCondivise") ? "Si" : "No" ) + "\n";
        emailTextBody += "\n";
        
        Integer idAzienda = (Integer) jsonRequestSW.get("idAzienda");        
        sendMail(idAzienda, accountFrom, Subject, to, emailTextBody, cc, null);
        
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
    
    /**
     * Dato un ip viene tornato, quando possibile, il nome del computer corrispondente.
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
}
