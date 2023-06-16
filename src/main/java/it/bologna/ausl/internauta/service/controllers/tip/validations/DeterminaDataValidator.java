package it.bologna.ausl.internauta.service.controllers.tip.validations;

import it.bologna.ausl.model.entities.scripta.Registro;
import it.bologna.ausl.model.entities.tip.ImportazioneDocumento;
import it.bologna.ausl.model.entities.tip.ImportazioneOggetto;
import it.bologna.ausl.model.entities.tip.SessioneImportazione;
import it.bologna.ausl.model.entities.tip.data.ColonneImportazioneOggetto;
import it.bologna.ausl.model.entities.tip.data.ColonneImportazioneOggettoEnums.ColonneDetermina;
import it.bologna.ausl.model.entities.tip.data.ColonneImportazioneOggettoEnums.MezziConsentiti;
import it.bologna.ausl.model.entities.tip.data.KeyValueEnum;
import it.bologna.ausl.model.entities.tip.data.TipErroriImportazione;
import java.util.Arrays;
import java.util.List;
import org.apache.commons.lang3.EnumUtils;
import org.springframework.util.StringUtils;

/**
 *
 * @author gdm
 */
public class DeterminaDataValidator extends TipDataValidator {
    
    @Override
    public TipErroriImportazione validate(ImportazioneOggetto rigaImportazione) {
        TipErroriImportazione erroriImportazione = new TipErroriImportazione();
        ImportazioneDocumento riga = (ImportazioneDocumento) rigaImportazione;
        if(!StringUtils.hasText(riga.getRegistro()) && !EnumUtils.isValidEnumIgnoreCase(Registro.CodiceRegistro.class, riga.getRegistro())) {
            erroriImportazione.setError(
                    ColonneDetermina.registro, 
                    TipErroriImportazione.Flusso.TipoFlusso.VALIDAZIONE, 
                    String.format("valore non valido, i valori validi sono: %s", Arrays.asList(Registro.CodiceRegistro.values())));
            riga.setErrori(erroriImportazione);
        }
        if (!StringUtils.hasText(riga.getNumero())) {
            erroriImportazione.setError(ColonneDetermina.numero, TipErroriImportazione.Flusso.TipoFlusso.VALIDAZIONE, "Il campo è obbligatorio.");
            riga.setErrori(erroriImportazione);
        } else if (!TipDataValidator.validateNumber(riga.getNumero())) {
            erroriImportazione.setError(ColonneDetermina.numero, TipErroriImportazione.Flusso.TipoFlusso.VALIDAZIONE, "Il campo deve essere un numero intero potivo.");
            riga.setErrori(erroriImportazione);
        }
        if (!StringUtils.hasText(riga.getAnno())) {
            erroriImportazione.setError(ColonneDetermina.anno, TipErroriImportazione.Flusso.TipoFlusso.VALIDAZIONE, "Il campo è obbligatorio.");
            riga.setErrori(erroriImportazione);
        } else if (!validateAnno(riga.getAnno())) {
            erroriImportazione.setError(ColonneDetermina.anno, TipErroriImportazione.Flusso.TipoFlusso.VALIDAZIONE, "Formato errato, il formato corretto è: yyyy");
            riga.setErrori(erroriImportazione);
        }
        if (!StringUtils.hasText(riga.getOggetto())) {
            erroriImportazione.setError(ColonneDetermina.oggetto, TipErroriImportazione.Flusso.TipoFlusso.VALIDAZIONE, "Il campo è obbligatorio.");
            riga.setErrori(erroriImportazione);
        }
        if (!StringUtils.hasText(riga.getDataAdozione())) {
            erroriImportazione.setError(ColonneDetermina.dataAdozione, TipErroriImportazione.Flusso.TipoFlusso.VALIDAZIONE, "Il campo è obbligatorio.");
            riga.setErrori(erroriImportazione);
        } else if (!validateData(riga.getDataAdozione())) {
            erroriImportazione.setError(ColonneDetermina.dataAdozione, TipErroriImportazione.Flusso.TipoFlusso.VALIDAZIONE, "Formato errato. Il formato corretto è: dd/MM/yyyy.");
            riga.setErrori(erroriImportazione);
        }
        if (StringUtils.hasText(riga.getNomiDestinatariEsterni()) && StringUtils.hasText(riga.getIndirizziDestinatariEsterni())) {
            if (!validateNotazioniPosizionali(riga.getNomiDestinatariEsterni(), riga.getIndirizziDestinatariEsterni(), TipDataValidator.DEFAULT_STRING_SEPARATOR)) {
                erroriImportazione.setError(ColonneDetermina.nomiDestinatariEsterni, TipErroriImportazione.Flusso.TipoFlusso.VALIDAZIONE, 
                        String.format ("Il campo deve avere lo stesso numero di elementi di %s", ColonneDetermina.indirizziDestinatariEsterni.toString()));
                erroriImportazione.setError(ColonneDetermina.indirizziDestinatariEsterni, TipErroriImportazione.Flusso.TipoFlusso.VALIDAZIONE, 
                        String.format ("Il campo deve avere lo stesso numero di elementi di %s", ColonneDetermina.nomiDestinatariEsterni.toString()));
            }
        }
        if ( (!StringUtils.hasText(riga.getIdFascicoloPregresso()) && !StringUtils.hasText(riga.getFascicolazione()))) {
            erroriImportazione.setWarning(ColonneDetermina.idFascicoloPregresso, TipErroriImportazione.Flusso.TipoFlusso.VALIDAZIONE, "Non è stata inserita la fascicolazione, sarà inserita quella di default.");
            erroriImportazione.setWarning(ColonneDetermina.fascicolazione, TipErroriImportazione.Flusso.TipoFlusso.VALIDAZIONE, "Non è stata inserita la fascicolazione, sarà inserita quella di default.");
            riga.setErrori(erroriImportazione);
        } else if ( (StringUtils.hasText(riga.getIdFascicoloPregresso()) && StringUtils.hasText(riga.getFascicolazione()))) {
            erroriImportazione.setError(ColonneDetermina.idFascicoloPregresso, TipErroriImportazione.Flusso.TipoFlusso.VALIDAZIONE, 
                    String.format("Un (e un solo) campo tra %s e %s è obbligatorio.", ColonneDetermina.idFascicoloPregresso, ColonneDetermina.fascicolazione));
            erroriImportazione.setError(ColonneDetermina.fascicolazione, TipErroriImportazione.Flusso.TipoFlusso.VALIDAZIONE, 
                    String.format("Un (e un solo) campo tra %s e %s è obbligatorio.", ColonneDetermina.fascicolazione, ColonneDetermina.idFascicoloPregresso));
            riga.setErrori(erroriImportazione);
        } else if (StringUtils.hasText(riga.getFascicolazione()) && !validateFascicolazioni(riga.getFascicolazione())) {
            erroriImportazione.setError(ColonneDetermina.fascicolazione, TipErroriImportazione.Flusso.TipoFlusso.VALIDAZIONE, "Formato errato. Il formato corretto è: num_fasc[-num_sotto_fascicolo](opzionale)[-num_insert](opzionale)/anno[#num_fasc[-num_sotto_fascicolo](opzionale)[-num_insert](opzionale)/anno](opzionale).");
            riga.setErrori(erroriImportazione);
        }
        if (StringUtils.hasText(riga.getClassificazione()) && !validateClassificazione(riga.getClassificazione())) {
            erroriImportazione.setError(ColonneDetermina.classificazione, TipErroriImportazione.Flusso.TipoFlusso.VALIDAZIONE, "Formato errato. Il formato corretto è: CAT1/CLASS1/SOTTOCLASS2#CAT2#CAT1/CLAS3.");
            riga.setErrori(erroriImportazione);
        } 
        if (StringUtils.hasText(riga.getDataInizio()) && !validateData(riga.getDataInizio())) {
            erroriImportazione.setError(ColonneDetermina.dataInizio, TipErroriImportazione.Flusso.TipoFlusso.VALIDAZIONE, "Formato errato. Il formato corretto è: dd/MM/yyyy.");
            riga.setErrori(erroriImportazione);
        }
        if (StringUtils.hasText(riga.getDataFine()) && !validateData(riga.getDataFine())) {
            erroriImportazione.setError(ColonneDetermina.dataFine, TipErroriImportazione.Flusso.TipoFlusso.VALIDAZIONE, "Formato errato. Il formato corretto è: dd/MM/yyyy.");
            riga.setErrori(erroriImportazione);
        }
        if (StringUtils.hasText(riga.getAnnoPubblicazione()) && !validateAnno(riga.getAnnoPubblicazione())) {
            erroriImportazione.setError(ColonneDetermina.annoPubblicazione, TipErroriImportazione.Flusso.TipoFlusso.VALIDAZIONE, "Formato errato, il formato corretto è: yyyy");
            riga.setErrori(erroriImportazione);
        }
        if (StringUtils.hasText(riga.getConservato()) && !validateBoolean(riga.getConservato())) {
            erroriImportazione.setError(ColonneDetermina.conservato, TipErroriImportazione.Flusso.TipoFlusso.VALIDAZIONE, "Formato errato, il formato corretto è: true/false");
            riga.setErrori(erroriImportazione);
        }
        if (StringUtils.hasText(riga.getCollegamentoPrecedente()) && !validateNumeroDocumentoPrecedente(riga.getCollegamentoPrecedente())) {
            erroriImportazione.setError(ColonneDetermina.collegamentoPrecedente, TipErroriImportazione.Flusso.TipoFlusso.VALIDAZIONE, "Formato errato. Il formato corretto è: numero/yyyy");
            riga.setErrori(erroriImportazione);
        }
        if (StringUtils.hasText(riga.getAnnullato()) && !validateBoolean(riga.getAnnullato())) {
            erroriImportazione.setError(ColonneDetermina.annullato, TipErroriImportazione.Flusso.TipoFlusso.VALIDAZIONE, "Formato errato, il formato corretto è: true/false");
            riga.setErrori(erroriImportazione);
        }
        if (StringUtils.hasText(riga.getDataAnnullamento()) && !validateData(riga.getDataAnnullamento())) {
            erroriImportazione.setError(ColonneDetermina.dataAnnullamento, TipErroriImportazione.Flusso.TipoFlusso.VALIDAZIONE, "Formato errato. Il formato corretto è: dd/MM/yyyy.");
            riga.setErrori(erroriImportazione);
        }
        if (StringUtils.hasText(riga.getDataInvioConservazione()) && !validateData(riga.getDataInvioConservazione())) {
            erroriImportazione.setError(ColonneDetermina.dataInvioConservazione, TipErroriImportazione.Flusso.TipoFlusso.VALIDAZIONE, "Formato errato. Il formato corretto è: dd/MM/yyyy.");
            riga.setErrori(erroriImportazione);
        }
        return erroriImportazione;
    }
    
//    public static void main(String[] args) {
//        System.out.println(MezziConsentiti.POSTA_SEMPLICE.getCodiceMezzoScripta());
//    }
}
