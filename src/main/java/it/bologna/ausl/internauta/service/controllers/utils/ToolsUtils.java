package it.bologna.ausl.internauta.service.controllers.utils;

import it.bologna.ausl.internauta.service.baborg.utils.BaborgUtils;
import it.bologna.ausl.model.entities.baborg.Struttura;
import it.bologna.ausl.model.entities.forms.Segnalazione;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

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
}
