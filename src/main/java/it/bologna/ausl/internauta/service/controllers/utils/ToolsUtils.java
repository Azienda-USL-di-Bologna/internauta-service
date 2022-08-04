package it.bologna.ausl.internauta.service.controllers.utils;

import it.bologna.ausl.model.entities.forms.Segnalazione;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

/**
 *
 * @author Giuseppe Russo
 */
public class ToolsUtils {

    public ToolsUtils() {
    }

    public String buildMailForCustomerSupport(Segnalazione segnalazioneUtente, Integer numeroSegnalazione) {

        String body = "*** Riepilogo Segnalazione Utente ***\n\n";
        body += "Numero: " + (numeroSegnalazione != null ? numeroSegnalazione.toString() : "[DA ELABORARE]") + "\n";
        body += "Azienda: " + segnalazioneUtente.getAzienda() + "\n";
        if (segnalazioneUtente.getStruttura()!= null) {
            body += "Struttura: "+ segnalazioneUtente.getStruttura() + "\n";
        }
        body += "Cognome: " + segnalazioneUtente.getCognome() + "\n";
        body += "Nome: " + segnalazioneUtente.getNome() + "\n";
        body += "IdBabel: " + segnalazioneUtente.getUsername() + "\n";
        String tipologiaSegnalazione = segnalazioneUtente.getTipologiaSegnalazione();
        if (StringUtils.hasText(tipologiaSegnalazione)) {
            switch (segnalazioneUtente.getTipologiaSegnalazione()) {
                case "FORMAZIONE":
                    body += "Tipo Segnalazione: " + "Formazione" + "\n";
                    break;
                case "MALFUNZIONAMENTO":
                    body += "Tipo Segnalazione: " + "Malfunzionamento" + "\n";
                    break;
                case "CORREZIONE_DOCUMENTALE":
                    body += "Tipo Segnalazione: " + "Modifica" + "\n";

                    String descrizioneAutorizzaotore = segnalazioneUtente.getDescrizioneAutorizzatore() + " (" + segnalazioneUtente.getEmailAutorizzatore()+ ")";
                    body += "Autorizzatore: " + descrizioneAutorizzaotore + "\n";
                    break;
                default:
                    throw new AssertionError();
            }
        }
        body += "Telefono: " + segnalazioneUtente.getTelefono() + "\n";
        body += "Mail: " + segnalazioneUtente.getMail() + "\n\n";
        
        body += "Oggetto: " + segnalazioneUtente.getOggetto() + "\n";
        body += "Data e ora: " + DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss").format(LocalDateTime.now()) + "\n\n";
        
        body += "Descrizione del problema:\n" + segnalazioneUtente.getDescrizione() + "\n\n";
        
        if (segnalazioneUtente.getAllegati() != null) {
            body += "Allegati:\n";
            MultipartFile[] allegati = segnalazioneUtente.getAllegati();
            for (MultipartFile file : allegati) {
                body += file.getOriginalFilename() + "\n";
            }
            body += "\n\n";
        }
        return body;
    }

    public String buildMailForUser(String bodyCustomerSupport, Integer numeroSegnalazione) {

        String body = "";
        body += "Gent.le Utente\n";
        if (numeroSegnalazione != null) {
            body += "E' stata aperta una nuova segnalazione al servizio Babelcare con numero "
                    + numeroSegnalazione.toString() + ".\n";
        } else {

            body += "La tua segnalazione è stata inviata al servizio Babelcare. \n";
        }
        body += "Al più presto sarai contattato da un operatore dedicato per approfondire il problema oppure "
                + "riceverai direttamente un riscontro sull’eventuale risoluzione del caso.\n\n";
        // IdBabel non va inviato all'utente in quanto se è il codice fiscale è un problema di privacy
        body += bodyCustomerSupport.replaceAll("IdBabel.*\\n", "");
        body
                += "\nRicordiamo che sono in atto misure straordinarie di contenimento dell’emergenza COVID-19 "
                + "pertanto il personale del servizio Babelcare e di sviluppo Babel "
                + "sta gestendo le normali attività adottando la modalità di smartworking.\n"
                + "In questo periodo per aiutarci a gestire al meglio il servizio e per garantire tempi di risposta rapidi, "
                + "preghiamo di utilizzare l’apposito FORM di Invio Segnalazione.\n"
                + "Ringraziamo anticipatamente per la collaborazione.\n"
                + "Saluti e buon lavoro.\n\n"
                + "Help Desk Babelcare\n"
                + "Email: babel.care@ausl.bologna.it\n"
                + "Orario di servizio: 9.00-17.00\n"
                + "(Il telefono in ingresso è disattivato, si possono aprire ticket solo via Mail o Form Invio Segnalazione)\n";

        return body;
    }

}
