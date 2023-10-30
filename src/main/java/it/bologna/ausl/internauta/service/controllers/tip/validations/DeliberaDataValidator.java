package it.bologna.ausl.internauta.service.controllers.tip.validations;

import it.bologna.ausl.model.entities.tip.ImportazioneDocumento;
import it.bologna.ausl.model.entities.tip.ImportazioneOggetto;
import it.bologna.ausl.model.entities.tip.data.ColonneImportazioneOggettoEnums.ColonneDelibera;
import it.bologna.ausl.model.entities.tip.data.TipErroriImportazione;
import org.springframework.util.StringUtils;

/**
 *
 * @author gdm
 */
public class DeliberaDataValidator extends TipDataValidator {

    @Override
    public TipErroriImportazione validate(ImportazioneOggetto rigaImportazione) {
        TipErroriImportazione erroriImportazione = new TipErroriImportazione();
        ImportazioneDocumento riga = (ImportazioneDocumento) rigaImportazione;
//        if(StringUtils.hasText(riga.getRegistro()) && !EnumUtils.isValidEnumIgnoreCase(Registro.CodiceRegistro.class, riga.getRegistro())) {
//            erroriImportazione.setError(
//                    ColonneDelibera.registro, 
//                    TipErroriImportazione.Flusso.TipoFlusso.VALIDAZIONE, 
//                    String.format("valore non valido, i valori validi sono: %s", Arrays.asList(Registro.CodiceRegistro.values())));
//            riga.setErrori(erroriImportazione);
//        }
        if (!StringUtils.hasText(riga.getNumero())) {
            erroriImportazione.setError(ColonneDelibera.numero, TipErroriImportazione.Flusso.TipoFlusso.VALIDAZIONE, "Il campo è obbligatorio.");
            riga.setErrori(erroriImportazione);
        } else if (!TipDataValidator.validateNumber(riga.getNumero())) {
            erroriImportazione.setError(ColonneDelibera.numero, TipErroriImportazione.Flusso.TipoFlusso.VALIDAZIONE, "Il campo deve essere un numero intero potivo.");
            riga.setErrori(erroriImportazione);
        }
        if (!StringUtils.hasText(riga.getAnno())) {
            erroriImportazione.setError(ColonneDelibera.anno, TipErroriImportazione.Flusso.TipoFlusso.VALIDAZIONE, "Il campo è obbligatorio.");
            riga.setErrori(erroriImportazione);
        } else if (!validateAnno(riga.getAnno())) {
            erroriImportazione.setError(ColonneDelibera.anno, TipErroriImportazione.Flusso.TipoFlusso.VALIDAZIONE, "Formato errato, il formato corretto è: yyyy");
            riga.setErrori(erroriImportazione);
        }
        if (!StringUtils.hasText(riga.getOggetto())) {
            erroriImportazione.setError(ColonneDelibera.oggetto, TipErroriImportazione.Flusso.TipoFlusso.VALIDAZIONE, "Il campo è obbligatorio.");
            riga.setErrori(erroriImportazione);
        }
        if (!StringUtils.hasText(riga.getDataAdozione())) {
            erroriImportazione.setError(ColonneDelibera.dataAdozione, TipErroriImportazione.Flusso.TipoFlusso.VALIDAZIONE, "Il campo è obbligatorio.");
            riga.setErrori(erroriImportazione);
        } else if (!validateData(riga.getDataAdozione())) {
            erroriImportazione.setError(ColonneDelibera.dataAdozione, TipErroriImportazione.Flusso.TipoFlusso.VALIDAZIONE, "Formato errato. Il formato corretto è: dd/MM/yyyy.");
            riga.setErrori(erroriImportazione);
        }
        if (!StringUtils.hasText(riga.getPropostoDa())) {
            erroriImportazione.setWarning(ColonneDelibera.propostoDa, TipErroriImportazione.Flusso.TipoFlusso.VALIDAZIONE, "Non è stata specificata la struttura, sarà usata quella di default");
            riga.setErrori(erroriImportazione);
        }
        if (StringUtils.hasText(riga.getNomiDestinatariEsterni()) && StringUtils.hasText(riga.getIndirizziDestinatariEsterni())) {
            if (!validateNotazioniPosizionali(riga.getNomiDestinatariEsterni(), riga.getIndirizziDestinatariEsterni(), ImportazioneDocumento.DEFAULT_STRING_SEPARATOR)) {
                erroriImportazione.setError(ColonneDelibera.nomiDestinatariEsterni, TipErroriImportazione.Flusso.TipoFlusso.VALIDAZIONE, 
                        String.format ("Il campo deve avere lo stesso numero di elementi di %s", ColonneDelibera.indirizziDestinatariEsterni.toString()));
                erroriImportazione.setError(ColonneDelibera.indirizziDestinatariEsterni, TipErroriImportazione.Flusso.TipoFlusso.VALIDAZIONE, 
                        String.format ("Il campo deve avere lo stesso numero di elementi di %s", ColonneDelibera.nomiDestinatariEsterni.toString()));
            }
        }
        if ( (!StringUtils.hasText(riga.getIdFascicoloPregresso()) && !StringUtils.hasText(riga.getFascicolazione()))) {
            erroriImportazione.setWarning(ColonneDelibera.idFascicoloPregresso, TipErroriImportazione.Flusso.TipoFlusso.VALIDAZIONE, "Non è stata inserita la fascicolazione, sarà inserita quella di default.");
            erroriImportazione.setWarning(ColonneDelibera.fascicolazione, TipErroriImportazione.Flusso.TipoFlusso.VALIDAZIONE, "Non è stata inserita la fascicolazione, sarà inserita quella di default.");
            riga.setErrori(erroriImportazione);
        } else if ( (StringUtils.hasText(riga.getIdFascicoloPregresso()) && StringUtils.hasText(riga.getFascicolazione()))) {
            erroriImportazione.setError(ColonneDelibera.idFascicoloPregresso, TipErroriImportazione.Flusso.TipoFlusso.VALIDAZIONE, 
                    String.format("Un (e un solo) campo tra %s e %s è obbligatorio.", ColonneDelibera.idFascicoloPregresso, ColonneDelibera.fascicolazione));
            erroriImportazione.setError(ColonneDelibera.fascicolazione, TipErroriImportazione.Flusso.TipoFlusso.VALIDAZIONE, 
                    String.format("Un (e un solo) campo tra %s e %s è obbligatorio.", ColonneDelibera.fascicolazione, ColonneDelibera.idFascicoloPregresso));
            riga.setErrori(erroriImportazione);
        } else if (StringUtils.hasText(riga.getFascicolazione()) && !validateFascicolazioni(riga.getFascicolazione())) {
            erroriImportazione.setError(ColonneDelibera.fascicolazione, TipErroriImportazione.Flusso.TipoFlusso.VALIDAZIONE, "Formato errato. Il formato corretto è: num_fasc[-num_sotto_fascicolo](opzionale)[-num_insert](opzionale)/anno[#num_fasc[-num_sotto_fascicolo](opzionale)[-num_insert](opzionale)/anno](opzionale).");
            riga.setErrori(erroriImportazione);
        }
        if (StringUtils.hasText(riga.getClassificazione()) && !validateClassificazione(riga.getClassificazione())) {
            erroriImportazione.setError(ColonneDelibera.classificazione, TipErroriImportazione.Flusso.TipoFlusso.VALIDAZIONE, "Formato errato. Il formato corretto è: CAT1/CLASS1/SOTTOCLASS2#CAT2#CAT1/CLAS3.");
            riga.setErrori(erroriImportazione);
        } 
        if (StringUtils.hasText(riga.getDataInizio()) && !validateData(riga.getDataInizio())) {
            erroriImportazione.setError(ColonneDelibera.dataInizio, TipErroriImportazione.Flusso.TipoFlusso.VALIDAZIONE, "Formato errato. Il formato corretto è: dd/MM/yyyy.");
            riga.setErrori(erroriImportazione);
        }
        if (StringUtils.hasText(riga.getDataFine()) && !validateData(riga.getDataFine())) {
            erroriImportazione.setError(ColonneDelibera.dataFine, TipErroriImportazione.Flusso.TipoFlusso.VALIDAZIONE, "Formato errato. Il formato corretto è: dd/MM/yyyy.");
            riga.setErrori(erroriImportazione);
        }
        if (StringUtils.hasText(riga.getAnnoPubblicazione()) && !validateAnno(riga.getAnnoPubblicazione())) {
            erroriImportazione.setError(ColonneDelibera.annoPubblicazione, TipErroriImportazione.Flusso.TipoFlusso.VALIDAZIONE, "Formato errato, il formato corretto è: yyyy");
            riga.setErrori(erroriImportazione);
        }
        if (StringUtils.hasText(riga.getConservato()) && !validateBoolean(riga.getConservato())) {
            erroriImportazione.setError(ColonneDelibera.conservato, TipErroriImportazione.Flusso.TipoFlusso.VALIDAZIONE, "Formato errato, il formato corretto è: true/false");
            riga.setErrori(erroriImportazione);
        } else if (Boolean.parseBoolean(riga.getConservato()) && !StringUtils.hasText(riga.getDataInvioConservazione())) {
            erroriImportazione.setError(ColonneDelibera.conservato, TipErroriImportazione.Flusso.TipoFlusso.VALIDAZIONE, "Se si indica che il documento è conservato è necessario inserire la data di conservazione");
            riga.setErrori(erroriImportazione);
        }
        if (StringUtils.hasText(riga.getCollegamentoPrecedente()) && !validateNumeroDocumentoPrecedente(riga.getCollegamentoPrecedente())) {
            erroriImportazione.setError(ColonneDelibera.collegamentoPrecedente, TipErroriImportazione.Flusso.TipoFlusso.VALIDAZIONE, "Formato errato. Il formato corretto è: numero/yyyy");
            riga.setErrori(erroriImportazione);
        }
        if (StringUtils.hasText(riga.getAnnullato()) && !validateBoolean(riga.getAnnullato())) {
            erroriImportazione.setError(ColonneDelibera.annullato, TipErroriImportazione.Flusso.TipoFlusso.VALIDAZIONE, "Formato errato, il formato corretto è: true/false");
            riga.setErrori(erroriImportazione);
        }
        if (!StringUtils.hasText(riga.getAnnullato()) && StringUtils.hasText(riga.getDataAnnullamento())) {
            erroriImportazione.setWarning(ColonneDelibera.dataAnnullamento, TipErroriImportazione.Flusso.TipoFlusso.VALIDAZIONE, "Il dato sarà ignorato perché non è stato inserito true nella colonna annullato");
            riga.setErrori(erroriImportazione);
        } else if (StringUtils.hasText(riga.getDataAnnullamento()) && !validateData(riga.getDataAnnullamento())) {
            erroriImportazione.setError(ColonneDelibera.dataAnnullamento, TipErroriImportazione.Flusso.TipoFlusso.VALIDAZIONE, "Formato errato. Il formato corretto è: dd/MM/yyyy.");
            riga.setErrori(erroriImportazione);
        }
        if ((!StringUtils.hasText(riga.getAnnullato()) || !Boolean.parseBoolean(riga.getAnnullato())) && StringUtils.hasText(riga.getNoteAnnullamento())) {
            erroriImportazione.setWarning(ColonneDelibera.noteAnnullamento, TipErroriImportazione.Flusso.TipoFlusso.VALIDAZIONE, "Il dato sarà ignorato perché non è stato inserito true nella colonna annullato");
            riga.setErrori(erroriImportazione);
        }
        if (StringUtils.hasText(riga.getDataInvioConservazione()) && !validateData(riga.getDataInvioConservazione())) {
            erroriImportazione.setError(ColonneDelibera.dataInvioConservazione, TipErroriImportazione.Flusso.TipoFlusso.VALIDAZIONE, "Formato errato. Il formato corretto è: dd/MM/yyyy.");
            riga.setErrori(erroriImportazione);
        }
        if (StringUtils.hasText(riga.getAllegati()) && !validaAllegati(riga.getAllegati())) {
            erroriImportazione.setError(ColonneDelibera.allegati, TipErroriImportazione.Flusso.TipoFlusso.VALIDAZIONE, "Gli allegati contengono caratteri non validi. I catatteri non validi sono: *, ?, <, >, |, :, \" ");
            riga.setErrori(erroriImportazione);
        }
        if (StringUtils.hasText(riga.getRedattore()) && !validaAttori(riga.getRedattore())) {
            erroriImportazione.setError(ColonneDelibera.redattore, TipErroriImportazione.Flusso.TipoFlusso.VALIDAZIONE, "la stringa attori non è nel formato CodiceFiscale:Cognome:Nome#CodiceFiscale:Cognome:Nome#...");
        }
        if (StringUtils.hasText(riga.getRedattore()) && !validaAttori(riga.getPareri())) {
            erroriImportazione.setError(ColonneDelibera.pareri, TipErroriImportazione.Flusso.TipoFlusso.VALIDAZIONE, "la stringa attori non è nel formato CodiceFiscale:Cognome:Nome#CodiceFiscale:Cognome:Nome#...");
        }
        if (StringUtils.hasText(riga.getRedattore()) && !validaAttori(riga.getProponente())) {
            erroriImportazione.setError(ColonneDelibera.proponente, TipErroriImportazione.Flusso.TipoFlusso.VALIDAZIONE, "la stringa attori non è nel formato CodiceFiscale:Cognome:Nome#CodiceFiscale:Cognome:Nome#...");
        }
        if (StringUtils.hasText(riga.getRedattore()) && !validaAttori(riga.getVisto())) {
            erroriImportazione.setError(ColonneDelibera.visto, TipErroriImportazione.Flusso.TipoFlusso.VALIDAZIONE, "la stringa attori non è nel formato CodiceFiscale:Cognome:Nome#CodiceFiscale:Cognome:Nome#...");
        }
        if (StringUtils.hasText(riga.getRedattore()) && !validaAttori(riga.getDirettoreAmministrativo())) {
            erroriImportazione.setError(ColonneDelibera.direttoreAmministrativo, TipErroriImportazione.Flusso.TipoFlusso.VALIDAZIONE, "la stringa attori non è nel formato CodiceFiscale:Cognome:Nome#CodiceFiscale:Cognome:Nome#...");
        }
        if (StringUtils.hasText(riga.getRedattore()) && !validaAttori(riga.getDirettoreSanitario())) {
            erroriImportazione.setError(ColonneDelibera.direttoreSanitario, TipErroriImportazione.Flusso.TipoFlusso.VALIDAZIONE, "la stringa attori non è nel formato CodiceFiscale:Cognome:Nome#CodiceFiscale:Cognome:Nome#...");
        }
        if (StringUtils.hasText(riga.getRedattore()) && !validaAttori(riga.getDirettoreGenerale())) {
            erroriImportazione.setError(ColonneDelibera.direttoreGenerale, TipErroriImportazione.Flusso.TipoFlusso.VALIDAZIONE, "la stringa attori non è nel formato CodiceFiscale:Cognome:Nome#CodiceFiscale:Cognome:Nome#...");
        }
        if (StringUtils.hasText(riga.getRedattore()) && !validaAttori(riga.getVicarioDirettoreGenerale())) {
            erroriImportazione.setError(ColonneDelibera.vicarioDirettoreGenerale, TipErroriImportazione.Flusso.TipoFlusso.VALIDAZIONE, "la stringa attori non è nel formato CodiceFiscale:Cognome:Nome#CodiceFiscale:Cognome:Nome#...");
        }
        return erroriImportazione;
    }
    
}
