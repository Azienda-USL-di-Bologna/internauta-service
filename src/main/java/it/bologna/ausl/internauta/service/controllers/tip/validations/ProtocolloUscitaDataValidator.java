package it.bologna.ausl.internauta.service.controllers.tip.validations;

import it.bologna.ausl.model.entities.scripta.Registro;
import it.bologna.ausl.model.entities.tip.ImportazioneDocumento;
import it.bologna.ausl.model.entities.tip.ImportazioneOggetto;
import it.bologna.ausl.model.entities.tip.data.ColonneImportazioneOggettoEnums.ColonneProtocolloUscita;
import it.bologna.ausl.model.entities.tip.data.TipErroriImportazione;
import java.util.Arrays;
import org.apache.commons.lang3.EnumUtils;
import org.springframework.util.StringUtils;

/**
 *
 * @author gdm
 */
public class ProtocolloUscitaDataValidator extends TipDataValidator {

    @Override
    public TipErroriImportazione validate(ImportazioneOggetto rigaImportazione) {
        TipErroriImportazione erroriImportazione = new TipErroriImportazione();
        ImportazioneDocumento riga = (ImportazioneDocumento) rigaImportazione;
        if (StringUtils.hasText(riga.getRegistro()) && !EnumUtils.isValidEnumIgnoreCase(Registro.CodiceRegistro.class, riga.getRegistro())) {
            erroriImportazione.setError(
                    ColonneProtocolloUscita.registro, 
                    TipErroriImportazione.Flusso.TipoFlusso.VALIDAZIONE, 
                    String.format("valore non valido, i valori validi sono: %s", Arrays.asList(Registro.CodiceRegistro.values())));
            riga.setErrori(erroriImportazione);
        }
        if (!StringUtils.hasText(riga.getNumero())) {
            erroriImportazione.setError(ColonneProtocolloUscita.numero, TipErroriImportazione.Flusso.TipoFlusso.VALIDAZIONE, "Il campo è obbligatorio.");
            riga.setErrori(erroriImportazione);
        } else if (!TipDataValidator.validateNumber(riga.getNumero())) {
            erroriImportazione.setError(ColonneProtocolloUscita.numero, TipErroriImportazione.Flusso.TipoFlusso.VALIDAZIONE, "Il campo deve essere un numero intero potivo.");
            riga.setErrori(erroriImportazione);
        }
        if (!StringUtils.hasText(riga.getAnno())) {
            erroriImportazione.setError(ColonneProtocolloUscita.anno, TipErroriImportazione.Flusso.TipoFlusso.VALIDAZIONE, "Il campo è obbligatorio.");
            riga.setErrori(erroriImportazione);
        } else if (!validateAnno(riga.getAnno())) {
            erroriImportazione.setError(ColonneProtocolloUscita.anno, TipErroriImportazione.Flusso.TipoFlusso.VALIDAZIONE, "Formato errato, il formato corretto è: yyyy");
            riga.setErrori(erroriImportazione);
        }
        if (!StringUtils.hasText(riga.getOggetto())) {
            erroriImportazione.setError(ColonneProtocolloUscita.oggetto, TipErroriImportazione.Flusso.TipoFlusso.VALIDAZIONE, "Il campo è obbligatorio.");
            riga.setErrori(erroriImportazione);
        }
        if (!StringUtils.hasText(riga.getDataRegistrazione())) {
            erroriImportazione.setError(ColonneProtocolloUscita.dataRegistrazione, TipErroriImportazione.Flusso.TipoFlusso.VALIDAZIONE, "Il campo è obbligatorio.");
            riga.setErrori(erroriImportazione);
        } else if (!validateData(riga.getDataRegistrazione())) {
            erroriImportazione.setError(ColonneProtocolloUscita.dataRegistrazione, TipErroriImportazione.Flusso.TipoFlusso.VALIDAZIONE, "Formato errato. Il formato corretto è: dd/MM/yyyy.");
            riga.setErrori(erroriImportazione);
        }
        if (!StringUtils.hasText(riga.getDestinatariPrincipali())) {
            erroriImportazione.setError(ColonneProtocolloUscita.destinatariPrincipali, TipErroriImportazione.Flusso.TipoFlusso.VALIDAZIONE, "È obbligatorio almeno un destinatario principale.");
            riga.setErrori(erroriImportazione);
        } else {
            if (StringUtils.hasText(riga.getIndirizziDestinatariPrincipali()) && !validateNotazioniPosizionali(riga.getDestinatariPrincipali(), riga.getIndirizziDestinatariPrincipali(), ImportazioneDocumento.DEFAULT_STRING_SEPARATOR)) {
                erroriImportazione.setError(ColonneProtocolloUscita.destinatariPrincipali, TipErroriImportazione.Flusso.TipoFlusso.VALIDAZIONE, 
                        String.format ("Il campo deve avere lo stesso numero di elementi di %s", ColonneProtocolloUscita.indirizziDestinatariPrincipali.toString()));
                erroriImportazione.setError(ColonneProtocolloUscita.indirizziDestinatariPrincipali, TipErroriImportazione.Flusso.TipoFlusso.VALIDAZIONE, 
                            String.format ("Il campo deve avere lo stesso numero di elementi di %s", ColonneProtocolloUscita.destinatariPrincipali.toString()));
            }
            if (StringUtils.hasText(riga.getDescrizioneIndirizziPrincipali()) && !validateNotazioniPosizionali(riga.getDestinatariPrincipali(), riga.getDescrizioneIndirizziPrincipali(), ImportazioneDocumento.DEFAULT_STRING_SEPARATOR)) {
                erroriImportazione.setError(ColonneProtocolloUscita.destinatariPrincipali, TipErroriImportazione.Flusso.TipoFlusso.VALIDAZIONE, 
                        String.format ("Il campo deve avere lo stesso numero di elementi di %s", ColonneProtocolloUscita.descrizioneIndirizziPrincipali.toString()));
                erroriImportazione.setError(ColonneProtocolloUscita.descrizioneIndirizziPrincipali, TipErroriImportazione.Flusso.TipoFlusso.VALIDAZIONE, 
                            String.format ("Il campo deve avere lo stesso numero di elementi di %s", ColonneProtocolloUscita.destinatariPrincipali.toString()));
            }
        }
        
        if (StringUtils.hasText(riga.getAltriDestinatari()) && StringUtils.hasText(riga.getIndirizziAltriDestinatari()) && !validateNotazioniPosizionali(riga.getAltriDestinatari(), riga.getIndirizziAltriDestinatari(), ImportazioneDocumento.DEFAULT_STRING_SEPARATOR)) {
                    erroriImportazione.setError(ColonneProtocolloUscita.altriDestinatari, TipErroriImportazione.Flusso.TipoFlusso.VALIDAZIONE, 
                            String.format ("Il campo deve avere lo stesso numero di elementi di %s", ColonneProtocolloUscita.indirizziAltriDestinatari.toString()));
                    erroriImportazione.setError(ColonneProtocolloUscita.indirizziAltriDestinatari, TipErroriImportazione.Flusso.TipoFlusso.VALIDAZIONE, 
                            String.format ("Il campo deve avere lo stesso numero di elementi di %s", ColonneProtocolloUscita.altriDestinatari.toString()));
        } 
        if (StringUtils.hasText(riga.getAltriDestinatari()) && StringUtils.hasText(riga.getDescrizioneAltriIndirizzi()) && !validateNotazioniPosizionali(riga.getAltriDestinatari(), riga.getDescrizioneAltriIndirizzi(), ImportazioneDocumento.DEFAULT_STRING_SEPARATOR)) {
                    erroriImportazione.setError(ColonneProtocolloUscita.altriDestinatari, TipErroriImportazione.Flusso.TipoFlusso.VALIDAZIONE, 
                            String.format ("Il campo deve avere lo stesso numero di elementi di %s", ColonneProtocolloUscita.descrizioneAltriIndirizzi.toString()));
                    erroriImportazione.setError(ColonneProtocolloUscita.descrizioneAltriIndirizzi, TipErroriImportazione.Flusso.TipoFlusso.VALIDAZIONE, 
                            String.format ("Il campo deve avere lo stesso numero di elementi di %s", ColonneProtocolloUscita.altriDestinatari.toString()));
        }
        
        if (!StringUtils.hasText(riga.getProtocollatoDa())) {
            erroriImportazione.setError(ColonneProtocolloUscita.protocollatoDa, TipErroriImportazione.Flusso.TipoFlusso.VALIDAZIONE, "Il campo è obbligatorio.");
            riga.setErrori(erroriImportazione);
        }
        if ( (!StringUtils.hasText(riga.getIdFascicoloPregresso()) && !StringUtils.hasText(riga.getFascicolazione()))) {
            erroriImportazione.setWarning(ColonneProtocolloUscita.idFascicoloPregresso, TipErroriImportazione.Flusso.TipoFlusso.VALIDAZIONE, "Non è stata inserita la fascicolazione, sarà inserita quella di default.");
            erroriImportazione.setWarning(ColonneProtocolloUscita.fascicolazione, TipErroriImportazione.Flusso.TipoFlusso.VALIDAZIONE, "Non è stata inserita la fascicolazione, sarà inserita quella di default.");
            riga.setErrori(erroriImportazione);
        } else if ( (StringUtils.hasText(riga.getIdFascicoloPregresso()) && StringUtils.hasText(riga.getFascicolazione()))) {
            erroriImportazione.setError(ColonneProtocolloUscita.idFascicoloPregresso, TipErroriImportazione.Flusso.TipoFlusso.VALIDAZIONE, 
                    String.format("Un (e un solo) campo tra %s e %s è obbligatorio.", ColonneProtocolloUscita.idFascicoloPregresso, ColonneProtocolloUscita.fascicolazione));
            erroriImportazione.setError(ColonneProtocolloUscita.fascicolazione, TipErroriImportazione.Flusso.TipoFlusso.VALIDAZIONE, 
                    String.format("Un (e un solo) campo tra %s e %s è obbligatorio.", ColonneProtocolloUscita.fascicolazione, ColonneProtocolloUscita.idFascicoloPregresso));
            riga.setErrori(erroriImportazione);
        } else if (StringUtils.hasText(riga.getFascicolazione()) && !validateFascicolazioni(riga.getFascicolazione())) {
            erroriImportazione.setError(ColonneProtocolloUscita.fascicolazione, TipErroriImportazione.Flusso.TipoFlusso.VALIDAZIONE, "Formato errato. Il formato corretto è: num_fasc[-num_sotto_fascicolo](opzionale)[-num_insert](opzionale)/anno[#num_fasc[-num_sotto_fascicolo](opzionale)[-num_insert](opzionale)/anno](opzionale).");
            riga.setErrori(erroriImportazione);
        }
        
        if (StringUtils.hasText(riga.getClassificazione()) && !validateClassificazione(riga.getClassificazione())) {
            erroriImportazione.setError(ColonneProtocolloUscita.classificazione, TipErroriImportazione.Flusso.TipoFlusso.VALIDAZIONE, "Formato errato. Il formato corretto è: CAT1/CLASS1/SOTTOCLASS2#CAT2#CAT1/CLAS3.");
            riga.setErrori(erroriImportazione);
        }
        if (StringUtils.hasText(riga.getPecMittente()) && !validaIndirizzoEmail(riga.getPecMittente())) {
            erroriImportazione.setWarning(ColonneProtocolloUscita.pecMittente, TipErroriImportazione.Flusso.TipoFlusso.VALIDAZIONE, "Non è un indirizzo mail valido");
            riga.setErrori(erroriImportazione);
        }
        if (StringUtils.hasText(riga.getVisibilitaLimitata()) && !validateBoolean(riga.getVisibilitaLimitata())) {
            erroriImportazione.setError(ColonneProtocolloUscita.visibilitaLimitata, TipErroriImportazione.Flusso.TipoFlusso.VALIDAZIONE, "Formato errato, il formato corretto è: true/false");
            riga.setErrori(erroriImportazione);
        }
        if (StringUtils.hasText(riga.getRiservato()) && !validateBoolean(riga.getRiservato())) {
            erroriImportazione.setError(ColonneProtocolloUscita.riservato, TipErroriImportazione.Flusso.TipoFlusso.VALIDAZIONE, "Formato errato, il formato corretto è: true/false");
            riga.setErrori(erroriImportazione);
        }
        if (StringUtils.hasText(riga.getRiservato()) && parseBoolean(riga.getRiservato()) && StringUtils.hasText(riga.getVisibilitaLimitata())) {
            erroriImportazione.setWarning(
                ColonneProtocolloUscita.visibilitaLimitata, 
                TipErroriImportazione.Flusso.TipoFlusso.VALIDAZIONE, 
                String.format("Sarà considerato solo %s perché già include questo campo", ColonneProtocolloUscita.riservato));
            riga.setErrori(erroriImportazione);
        }
        if (StringUtils.hasText(riga.getAnnullato()) && !validateBoolean(riga.getAnnullato())) {
            erroriImportazione.setError(ColonneProtocolloUscita.annullato, TipErroriImportazione.Flusso.TipoFlusso.VALIDAZIONE, "Formato errato, il formato corretto è: true/false");
            riga.setErrori(erroriImportazione);
        }
        if ((!StringUtils.hasText(riga.getAnnullato()) || !Boolean.parseBoolean(riga.getAnnullato())) && StringUtils.hasText(riga.getDataAnnullamento())) {
            erroriImportazione.setWarning(ColonneProtocolloUscita.dataAnnullamento, TipErroriImportazione.Flusso.TipoFlusso.VALIDAZIONE, "Il dato sarà ignorato perché non è stato inserito true nella colonna annullato");
            riga.setErrori(erroriImportazione);
        } else if (StringUtils.hasText(riga.getDataAnnullamento()) && !validateData(riga.getDataAnnullamento())) {
            erroriImportazione.setError(ColonneProtocolloUscita.dataAnnullamento, TipErroriImportazione.Flusso.TipoFlusso.VALIDAZIONE, "Formato errato. Il formato corretto è: dd/MM/yyyy.");
            riga.setErrori(erroriImportazione);
        }
        if ((!StringUtils.hasText(riga.getAnnullato()) || !Boolean.parseBoolean(riga.getAnnullato())) && StringUtils.hasText(riga.getNoteAnnullamento())) {
            erroriImportazione.setWarning(ColonneProtocolloUscita.noteAnnullamento, TipErroriImportazione.Flusso.TipoFlusso.VALIDAZIONE, "Il dato sarà ignorato perché non è stato inserito true nella colonna annullato");
            riga.setErrori(erroriImportazione);
        }
        if (StringUtils.hasText(riga.getDataInvioConservazione()) && !validateData(riga.getDataInvioConservazione())) {
            erroriImportazione.setError(ColonneProtocolloUscita.dataInvioConservazione, TipErroriImportazione.Flusso.TipoFlusso.VALIDAZIONE, "Formato errato. Il formato corretto è: dd/MM/yyyy.");
            riga.setErrori(erroriImportazione);
        }
        if (StringUtils.hasText(riga.getCollegamentoPrecedente()) && !validateNumeroDocumentoPrecedente(riga.getCollegamentoPrecedente())) {
            erroriImportazione.setError(ColonneProtocolloUscita.collegamentoPrecedente, TipErroriImportazione.Flusso.TipoFlusso.VALIDAZIONE, "Formato errato. Il formato corretto è: numero/yyyy");
            riga.setErrori(erroriImportazione);
        }
        if (StringUtils.hasText(riga.getAllegati()) && !validaAllegati(riga.getAllegati())) {
            erroriImportazione.setError(ColonneProtocolloUscita.allegati, TipErroriImportazione.Flusso.TipoFlusso.VALIDAZIONE, "Gli allegati contengono caratteri non validi. I catatteri non validi sono: *, ?, <, >, |, :, \" ");
            riga.setErrori(erroriImportazione);
        }
        if (StringUtils.hasText(riga.getRedattore()) && !validaAttori(riga.getRedattore())) {
            erroriImportazione.setError(ColonneProtocolloUscita.redattore, TipErroriImportazione.Flusso.TipoFlusso.VALIDAZIONE, "la stringa attori non è nel formato CodiceFiscale:Cognome:Nome#CodiceFiscale:Cognome:Nome#...");
        }
        if (StringUtils.hasText(riga.getRedattore()) && !validaAttori(riga.getPareri())) {
            erroriImportazione.setError(ColonneProtocolloUscita.pareri, TipErroriImportazione.Flusso.TipoFlusso.VALIDAZIONE, "la stringa attori non è nel formato CodiceFiscale:Cognome:Nome#CodiceFiscale:Cognome:Nome#...");
        }
        if (StringUtils.hasText(riga.getRedattore()) && !validaAttori(riga.getFirmatari())) {
            erroriImportazione.setError(ColonneProtocolloUscita.firmatari, TipErroriImportazione.Flusso.TipoFlusso.VALIDAZIONE, "la stringa attori non è nel formato CodiceFiscale:Cognome:Nome#CodiceFiscale:Cognome:Nome#...");
        }
        return erroriImportazione;
    }
    
}
