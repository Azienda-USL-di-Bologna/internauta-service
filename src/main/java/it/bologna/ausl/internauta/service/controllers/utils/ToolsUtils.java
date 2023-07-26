package it.bologna.ausl.internauta.service.controllers.utils;

import it.bologna.ausl.internauta.service.baborg.utils.BaborgUtils;
import it.bologna.ausl.model.entities.baborg.Struttura;
import it.bologna.ausl.model.entities.forms.Segnalazione;
import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import static java.util.Arrays.asList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.safety.Whitelist;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import com.atlassian.renderer.wysiwyg.converter.DefaultWysiwygConverter;

/**
 *
 * @author Giuseppe Russo
 */
public class ToolsUtils {

    public ToolsUtils() {
    }

    public String buildMailForCustomerSupport(Segnalazione segnalazioneUtente, Integer numeroSegnalazione, BaborgUtils baborgUtils) {

        String body = "*** Riepilogo Segnalazione Utente ***<br/><br/>";
        body += "Numero: " + (numeroSegnalazione != null ? numeroSegnalazione.toString() : "[DA ELABORARE]") + "<br/>";
        body += "Azienda: " + segnalazioneUtente.getAzienda() + "<br/>";
        if (segnalazioneUtente.getStruttura()!= null) {
            body += "Struttura: "+ segnalazioneUtente.getStruttura().getNome() + "<br/>";
            Struttura a = segnalazioneUtente.getStruttura();
            List<Struttura> struttureReplicate = baborgUtils.getStruttureUnificate(a, null, "REPLICA");
            List<Struttura> struttureFuse = baborgUtils.getStruttureUnificate(a, null, "FUSIONE");
            
            if ((struttureReplicate != null && !struttureReplicate.isEmpty()) || (struttureFuse != null && !struttureFuse.isEmpty())){
                String labelStruttureUnificate = "Altre strutture coinvolte: ";
                for (Struttura s : struttureFuse) {
                    labelStruttureUnificate += getStrutturaNameAndAzienda(s) + ", ";
                }
                for (Struttura s : struttureReplicate) {
                    labelStruttureUnificate += getStrutturaNameAndAzienda(s) + ", ";
                }
                body += labelStruttureUnificate.substring(0, labelStruttureUnificate.length() - 2) + "<br/>";
                System.out.println(body);
            } 
        }
        body += "Cognome: " + segnalazioneUtente.getCognome() + "<br/>";
        body += "Nome: " + segnalazioneUtente.getNome() + "<br/>";
        body += "IdBabel: " + segnalazioneUtente.getUsername() + "<br/>";
        String tipologiaSegnalazione = segnalazioneUtente.getTipologiaSegnalazione();
        if (StringUtils.hasText(tipologiaSegnalazione)) {
            switch (segnalazioneUtente.getTipologiaSegnalazione()) {
                case "FORMAZIONE":
                    body += "Tipo Segnalazione: " + "Formazione" + "<br/>";
                    break;
                case "MALFUNZIONAMENTO":
                    body += "Tipo Segnalazione: " + "Malfunzionamento" + "<br/>";
                    break;
                case "CORREZIONE_DOCUMENTALE":
                    body += "Tipo Segnalazione: " + "Modifica" + "<br/>";

                    String descrizioneAutorizzaotore = segnalazioneUtente.getDescrizioneAutorizzatore() + " (" + segnalazioneUtente.getEmailAutorizzatore()+ ")";
                    body += "Autorizzatore: " + descrizioneAutorizzaotore + "<br/>";
                    break;
                default:
                    throw new AssertionError();
            }
        }
        body += "Telefono: " + segnalazioneUtente.getTelefono() + "<br/>";
        body += "Mail: " + segnalazioneUtente.getMail() + "<br/><br/>";
        
        body += "Oggetto: " + segnalazioneUtente.getOggetto() + "<br/>";
        body += "Data e ora: " + DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss").format(LocalDateTime.now()) + "<br/><br/>";
        
        body += "Descrizione del problema:<br/>" + segnalazioneUtente.getDescrizione() + "<br/><br/>";
        
        if (segnalazioneUtente.getAllegati() != null) {
            body += "Allegati:<br/>";
            MultipartFile[] allegati = segnalazioneUtente.getAllegati();
            for (MultipartFile file : allegati) {
                body += file.getOriginalFilename() + "<br/>";
            }
            body += "<br/><br/>";
        }
        return body;
    }

    public String buildMailForUser(String bodyCustomerSupport, Integer numeroSegnalazione) {

        String body = "";
        body += "Gent.le Utente<br/>";
        if (numeroSegnalazione != null) {
            body += "E' stata aperta una nuova segnalazione al servizio Babelcare con numero "
                    + numeroSegnalazione.toString() + ".<br/>";
        } else {

            body += "La tua segnalazione è stata inviata al servizio Babelcare. <br/>";
        }
        body += "Al più presto sarai contattato da un operatore dedicato per approfondire il problema oppure "
                + "riceverai direttamente un riscontro sull’eventuale risoluzione del caso.<br/><br/>";
        // IdBabel non va inviato all'utente in quanto se è il codice fiscale è un problema di privacy
        body += bodyCustomerSupport.replaceAll("IdBabel.*?\\<br\\/>", "");
        body
                += "<br/>Ricordiamo che sono in atto misure straordinarie di contenimento dell’emergenza COVID-19 "
                + "pertanto il personale del servizio Babelcare e di sviluppo Babel "
                + "sta gestendo le normali attività adottando la modalità di smartworking.<br/>"
                + "In questo periodo per aiutarci a gestire al meglio il servizio e per garantire tempi di risposta rapidi, "
                + "preghiamo di utilizzare l’apposito FORM di Invio Segnalazione.<br/>"
                + "Ringraziamo anticipatamente per la collaborazione.<br/>"
                + "Saluti e buon lavoro.<br/><br/>"
                + "Help Desk Babelcare<br/>"
                + "Email: babel.care@ausl.bologna.it<br/>"
                + "Orario di servizio: 9.00-17.00<br/>"
                + "(Il telefono in ingresso è disattivato, si possono aprire ticket solo via Mail o Form Invio Segnalazione)<br/>";

        return body;
    }
    
    public String getStrutturaNameAndAzienda(Struttura s){
        return s.getNome() + "(" + s.getIdAzienda().getNome() + ")";
    }
    
    public JSONObject getJSONForJira(Segnalazione segnalazioneUtente, String accountId, String codiceProgetto){
        Map<String, String> reporter = new HashMap<>();
        reporter.put("accountId", accountId);
        
        Map<String, String> issuetype = new HashMap<>();
        issuetype.put("name", "Support");
        
        Map<String, String> project = new HashMap<>();
        project.put("key", codiceProgetto);
       
        Map<String, Object> contentText = new HashMap<>();
        DefaultWysiwygConverter wysiwysConverter = new DefaultWysiwygConverter();
        String descrizioneFormatoWiki = wysiwysConverter.convertXHtmlToWikiMarkup(segnalazioneUtente.getDescrizione());
        contentText.put("text", descrizioneFormatoWiki);
        contentText.put("type", "text");
        
        List<Map<String, Object>> listParagraphContent = new ArrayList<>(asList(contentText));   
        
        Map<String, Object> contentParagraph = new HashMap<>();
        contentParagraph.put("content", listParagraphContent);
        contentParagraph.put("type", "paragraph");
        
        List<Map<String, Object>> listDescriptionContent = new ArrayList<>(asList(contentParagraph));      
        
        Map<String, Object> description = new HashMap<>();
        description.put("content", listDescriptionContent);
        description.put("type", "doc");
        description.put("version", 1);
        
        
        
        
        Map<String, Object> fields = new HashMap<>();
        // CAMPI JIRA
        fields.put("project", project);
        fields.put("issuetype", issuetype);
        fields.put("reporter", reporter);
        // CAMPI SEGNALAZIONE
        fields.put("summary", segnalazioneUtente.getOggetto());
        fields.put("description", description);
        fields.put("customfield_10074", segnalazioneUtente.getTelefono());
        fields.put("customfield_10088", segnalazioneUtente.getStruttura().getNome());
        fields.put("customfield_10086", segnalazioneUtente.getMail());
        fields.put("customfield_10087", segnalazioneUtente.getNome() + " " + segnalazioneUtente.getCognome());

        Map<String, Object> mapJson = new HashMap<>();
        mapJson.put("fields", fields);
        
        return new JSONObject(mapJson);
    }
}
