package it.bologna.ausl.internauta.service.controllers.tip.validations;

import it.bologna.ausl.model.entities.tip.ImportazioneOggetto;
import it.bologna.ausl.model.entities.tip.SessioneImportazione;
import static it.bologna.ausl.model.entities.tip.SessioneImportazione.TipologiaPregresso.DELIBERA;
import static it.bologna.ausl.model.entities.tip.SessioneImportazione.TipologiaPregresso.DETERMINA;
import static it.bologna.ausl.model.entities.tip.SessioneImportazione.TipologiaPregresso.FASCICOLO;
import static it.bologna.ausl.model.entities.tip.SessioneImportazione.TipologiaPregresso.PROTOCOLLO_IN_ENTRATA;
import static it.bologna.ausl.model.entities.tip.SessioneImportazione.TipologiaPregresso.PROTOCOLLO_IN_USCITA;
import it.bologna.ausl.model.entities.tip.data.TipErroriImportazione;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;

/**
 * Classe stratta che si occupa della validazione delle importazioni.
 * Deve esistere una classe concreta per ogni tipologia di importazione
 * @author gdm
 */
public abstract class TipDataValidator {
    public static final String FORMATO_DATA = "dd/MM/yyyy";
    public static final String DEFAULT_STRING_SEPARATOR = "#";

    /**
     * torna la classe concreta in base alla tipologia passata
     * @param tipologia
     * @return 
     */
    public static TipDataValidator getTipDataValidator(SessioneImportazione.TipologiaPregresso tipologia) {
        TipDataValidator res = null;
        switch (tipologia) {
            case FASCICOLO: //TODO: tornare la classe corretta dopo l'implementazione dell'importazione degli archivi
                throw new RuntimeException("non ancora implementato");
            case PROTOCOLLO_IN_ENTRATA:
                res = new ProtocolloEntrataDataValidation();
                break;
            case PROTOCOLLO_IN_USCITA:
                res = new ProtocolloUscitaDataValidator();
                break;
            case DELIBERA:
                res = new DeliberaDataValidator();
                break;
            case DETERMINA:
                res = new DeterminaDataValidator();
                break;
        }
        return res;
    }
    
//    /**
//     * Controlla gli indirizzi email della stringa passata e, se ce ne sono di errati, inserisce in erroriImportazione passato l'errore 
//     * indicante gli indirizzi errati
//     * 
//     * @param erroriImportazione la classe che contiene gli errori relativi alla riga di importazione
//     * @param colonna la colonna che si sta controllando a cui gli indirizzi fanno riferimento
//     * @param indirizziEmail gli indirizzi da controllare separati dal separatore passato come parametro
//     * @param separatore il separatore
//     * @return una stringa contentente l'elenco degli indirizzi errati separato da ,. Se non ce ne sono torna una stringa vuota
//     */
//    protected String validaIndirizziEmail(TipErroriImportazione erroriImportazione, ColonneImportazioneOggetto colonna, String indirizziEmail, String separatore) {
//        String[] indirizziSplitted = indirizziEmail.split(separatore);
//        String indirizziErrati = "";
//        for (String indirizzo : indirizziSplitted) {
//            try {
//                InternetAddress emailAddress = new InternetAddress(indirizzo);
//                emailAddress.validate();
//            } catch (AddressException ex) {
//                indirizziErrati += String.format("%s, ", indirizzo);
//            }
//        }
//        if (StringUtils.hasText(indirizziErrati)) {
//            erroriImportazione.setErrorInfoColonna(colonna, String.format("Gli indirizzi %s non sono validi", indirizziErrati));
//        }
//        return indirizziErrati;
//    }
    
    /**
     * valdida la riga e setta gli errori nel campo errori
     * @param rigaImportazione la riga da controllare
     * @return la classe TipErroriImportazione che rappresenta il json di esito del controllo di validazione
     */
    public abstract TipErroriImportazione validate(ImportazioneOggetto rigaImportazione);
    
    
    /**
     * controlla se l'indirizzo passato è un indirizzo email valido
     * @param indirizzoEmail
     * @return true se è un indirizzo email valido, false altrimenti
     */
    public static boolean validaIndirizzoEmail(String indirizzoEmail) {
        try {
            InternetAddress emailAddress = new InternetAddress(indirizzoEmail);
            emailAddress.validate();
            return true;
        } catch (AddressException ex) {
            return false;
        }
    }
    
    /**
     * Valida la stringa rappresentante delle numerazioni gerarchiche separate da cancelletto
     * @param stringaFascicolazioni la stringa rappresentante delle numerazioni gerarchiche separate da cancelletto (se più di una)
     * @return true se la stringa è nel formato corretto, false altrimenti
     */
    public static boolean validateFascicolazioni(String stringaFascicolazioni) {
        String regex = "^(?:\\d+-\\d+-\\d+/\\d{4}|\\d+-\\d+/\\d{4}|\\d+/\\d{4})(?:#(?:\\d+-\\d+-\\d+/\\d{4}|\\d+-\\d+/\\d{4}|\\d+/\\d{4}))*$";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(stringaFascicolazioni);
        return matcher.matches();
    }
    
    /**
     * Valida la stringa rappresentante delle classificazioni separate da cancelletto
     * @param stringaClassificazioni la stringa rappresentante delle classificazioni separate da cancelletto (se più di una)
     * @return true se la stringa è nel formato corretto, false altrimenti
     */
    public static boolean validateClassificazione(String stringaClassificazioni) {
        String regex = "^(\\d+)(\\/\\d+){0,2}(#(\\d+)(\\/\\d+){0,2})*$";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(stringaClassificazioni);
        return matcher.matches();
    }
    
    /**
     * Valida una data nel formato dd/MM/aaaa
     * @param stringaData la data da validare
     * @return true se la stringa è nel formato corretto, false altrimenti
     */
    public static boolean validateData(String stringaData) {
        try {
           LocalDate data = parseData(stringaData);
           return true;
        }  catch (DateTimeParseException ex) {
            return false;
        }
    }
    
    /**
     * Parsa una stringa rappresentante una data nel formato dd/MM/aaaa
     * @param stringaData la data da parsare
     * @return un oggetto LocalDate rappresentante la data passata
     */
    public static LocalDate parseData(String stringaData) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(FORMATO_DATA);
        return LocalDate.parse(stringaData, formatter);
    }
    
    /**
     * Valida la stringa rappresentante un anno nel formato yyyy
     * @param stringaAnno la stringa da validare
     * @return true se la stringa è nel formato corretto, false altrimenti
     */
    public static boolean validateAnno(String stringaAnno) {
        String regex = "^\\d{4}$";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(stringaAnno);
        return matcher.matches();
    }
    
    /**
     * Valida la stringa rappresentante un numero di registrazione, cioè che sia un numero intero positivo
     * @param StringaNumero la stringa da validare
     * @return true se la stringa è nel formato corretto, false altrimenti
     */
    public static boolean validateNumber(String StringaNumero) {
        try {
            Long.parseUnsignedLong(StringaNumero);
        } catch (NumberFormatException ex) {
            return false;
        }
        return true;
    }
    
    /**
     * Valida la stringa rappresentante un numero di documento precedente, cioè nel formato numero/yyyy
     * @param stringaNumeroDocumento la stringa da validare
     * @return true se la stringa è nel formato corretto, false altrimenti
     */
    public static boolean validateNumeroDocumentoPrecedente(String stringaNumeroDocumento) {
        String regex = "^\\d+\\/\\d{4}$";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(stringaNumeroDocumento);
        return matcher.matches();
    }
    
    /**
     * Valida la stringa rappresentante un boolean
     * @param stringaBoolean la stringa da validare
     * @return true se la stringa è nel formato corretto, false altrimenti
     */
    public static boolean validateBoolean(String stringaBoolean) {
        return stringaBoolean.equalsIgnoreCase("true") || stringaBoolean.equalsIgnoreCase("false");
    }
    
    /**
     * Controlla che le 2 stringhe contenenti elementi separati de un separatore, ne contengano lo stesso numero
     * @param stringa1 la prima stringa da validare
     * @param stringa2 la seconda stringa da validare
     * @param separatore il separatore che separa gli elementi nelle stringe
     * @return true se le due stringhe hanno lo stesso numero di elementi, false altrimenti
     */
    public static boolean validateNotazioniPosizionali(String stringa1, String stringa2, String separatore) {
        return stringa2.split(separatore).length == stringa2.split(separatore).length;
    }
}
