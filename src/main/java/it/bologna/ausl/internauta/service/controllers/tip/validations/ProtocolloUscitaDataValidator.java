package it.bologna.ausl.internauta.service.controllers.tip.validations;

import it.bologna.ausl.model.entities.tip.ImportazioneDocumento;
import it.bologna.ausl.model.entities.tip.ImportazioneOggetto;
import it.bologna.ausl.model.entities.tip.data.ColonneImportazioneOggettoEnums.ColonneProtocolloUscita;
import it.bologna.ausl.model.entities.tip.data.TipErroriImportazione;
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
        if (!StringUtils.hasText(riga.getNumero())) {
            erroriImportazione.setError(ColonneProtocolloUscita.numero, TipErroriImportazione.Flusso.TipoFlusso.VALIDAZIONE, "Il campo è obbligatorio.");
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
            if (StringUtils.hasText(riga.getIndirizziDestinatariPrincipali()) && StringUtils.hasText(riga.getDescrizioneIndirizziPrincipali())) {
                erroriImportazione.setError(ColonneProtocolloUscita.indirizziDestinatariPrincipali, TipErroriImportazione.Flusso.TipoFlusso.VALIDAZIONE, 
                    String.format ("deve essere popoloato solo un campo tra %s e %s", ColonneProtocolloUscita.indirizziDestinatariPrincipali,ColonneProtocolloUscita.descrizioneIndirizziPrincipali.toString()));
                erroriImportazione.setError(ColonneProtocolloUscita.descrizioneIndirizziPrincipali, TipErroriImportazione.Flusso.TipoFlusso.VALIDAZIONE, 
                    String.format ("deve essere popoloato solo un campo tra %s e %s", ColonneProtocolloUscita.indirizziDestinatariPrincipali,ColonneProtocolloUscita.descrizioneIndirizziPrincipali.toString()));
            } else if (StringUtils.hasText(riga.getIndirizziDestinatariPrincipali()) && !validateNotazioniPosizionali(riga.getDestinatariPrincipali(), riga.getIndirizziDestinatariPrincipali(), TipDataValidator.DEFAULT_STRING_SEPARATOR)) {
                    erroriImportazione.setError(ColonneProtocolloUscita.destinatariPrincipali, TipErroriImportazione.Flusso.TipoFlusso.VALIDAZIONE, 
                            String.format ("Il campo deve avere lo stesso numero di elementi di %s", ColonneProtocolloUscita.indirizziDestinatariPrincipali.toString()));
                    erroriImportazione.setError(ColonneProtocolloUscita.indirizziDestinatariPrincipali, TipErroriImportazione.Flusso.TipoFlusso.VALIDAZIONE, 
                            String.format ("Il campo deve avere lo stesso numero di elementi di %s", ColonneProtocolloUscita.destinatariPrincipali.toString()));
            } else if (StringUtils.hasText(riga.getDescrizioneIndirizziPrincipali()) && !validateNotazioniPosizionali(riga.getDestinatariPrincipali(), riga.getDescrizioneIndirizziPrincipali(), TipDataValidator.DEFAULT_STRING_SEPARATOR)) {
                    erroriImportazione.setError(ColonneProtocolloUscita.destinatariPrincipali, TipErroriImportazione.Flusso.TipoFlusso.VALIDAZIONE, 
                            String.format ("Il campo deve avere lo stesso numero di elementi di %s", ColonneProtocolloUscita.descrizioneIndirizziPrincipali.toString()));
                    erroriImportazione.setError(ColonneProtocolloUscita.descrizioneIndirizziPrincipali, TipErroriImportazione.Flusso.TipoFlusso.VALIDAZIONE, 
                            String.format ("Il campo deve avere lo stesso numero di elementi di %s", ColonneProtocolloUscita.destinatariPrincipali.toString()));
            }
        }
        
        if (StringUtils.hasText(riga.getAltriDestinatari()) && StringUtils.hasText(riga.getIndirizziAltriDestinatari()) && StringUtils.hasText(riga.getDescrizioneAltriIndirizzi())) {
                erroriImportazione.setError(ColonneProtocolloUscita.indirizziAltriDestinatari, TipErroriImportazione.Flusso.TipoFlusso.VALIDAZIONE, 
                    String.format ("deve essere popoloato solo un campo tra %s e %s", ColonneProtocolloUscita.indirizziAltriDestinatari,ColonneProtocolloUscita.descrizioneAltriIndirizzi.toString()));
                erroriImportazione.setError(ColonneProtocolloUscita.descrizioneAltriIndirizzi, TipErroriImportazione.Flusso.TipoFlusso.VALIDAZIONE, 
                    String.format ("deve essere popoloato solo un campo tra %s e %s", ColonneProtocolloUscita.descrizioneAltriIndirizzi,ColonneProtocolloUscita.indirizziAltriDestinatari.toString()));
        } else if (StringUtils.hasText(riga.getAltriDestinatari()) && StringUtils.hasText(riga.getIndirizziAltriDestinatari()) && !validateNotazioniPosizionali(riga.getAltriDestinatari(), riga.getIndirizziAltriDestinatari(), TipDataValidator.DEFAULT_STRING_SEPARATOR)) {
                    erroriImportazione.setError(ColonneProtocolloUscita.altriDestinatari, TipErroriImportazione.Flusso.TipoFlusso.VALIDAZIONE, 
                            String.format ("Il campo deve avere lo stesso numero di elementi di %s", ColonneProtocolloUscita.indirizziAltriDestinatari.toString()));
                    erroriImportazione.setError(ColonneProtocolloUscita.indirizziAltriDestinatari, TipErroriImportazione.Flusso.TipoFlusso.VALIDAZIONE, 
                            String.format ("Il campo deve avere lo stesso numero di elementi di %s", ColonneProtocolloUscita.altriDestinatari.toString()));
        } else if (StringUtils.hasText(riga.getAltriDestinatari()) && StringUtils.hasText(riga.getDescrizioneAltriIndirizzi()) && !validateNotazioniPosizionali(riga.getAltriDestinatari(), riga.getDescrizioneAltriIndirizzi(), TipDataValidator.DEFAULT_STRING_SEPARATOR)) {
                    erroriImportazione.setError(ColonneProtocolloUscita.altriDestinatari, TipErroriImportazione.Flusso.TipoFlusso.VALIDAZIONE, 
                            String.format ("Il campo deve avere lo stesso numero di elementi di %s", ColonneProtocolloUscita.descrizioneAltriIndirizzi.toString()));
                    erroriImportazione.setError(ColonneProtocolloUscita.descrizioneAltriIndirizzi, TipErroriImportazione.Flusso.TipoFlusso.VALIDAZIONE, 
                            String.format ("Il campo deve avere lo stesso numero di elementi di %s", ColonneProtocolloUscita.altriDestinatari.toString()));
        }
        
        if (!StringUtils.hasText(riga.getProtocollatoDa())) {
            erroriImportazione.setError(ColonneProtocolloUscita.protocollatoDa, TipErroriImportazione.Flusso.TipoFlusso.VALIDAZIONE, "Il campo è obbligatorio..");
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
        if (StringUtils.hasText(riga.getAnnullato()) && !validateBoolean(riga.getAnnullato())) {
            erroriImportazione.setError(ColonneProtocolloUscita.annullato, TipErroriImportazione.Flusso.TipoFlusso.VALIDAZIONE, "Formato errato, il formato corretto è: true/false");
            riga.setErrori(erroriImportazione);
        }
        if (StringUtils.hasText(riga.getDataAnnullamento()) && !validateData(riga.getDataAnnullamento())) {
            erroriImportazione.setError(ColonneProtocolloUscita.dataAnnullamento, TipErroriImportazione.Flusso.TipoFlusso.VALIDAZIONE, "Formato errato. Il formato corretto è: dd/MM/yyyy.");
            riga.setErrori(erroriImportazione);
        }
        if (StringUtils.hasText(riga.getDataInvioConservazione()) && !validateData(riga.getDataInvioConservazione())) {
            erroriImportazione.setError(ColonneProtocolloUscita.dataInvioConservazione, TipErroriImportazione.Flusso.TipoFlusso.VALIDAZIONE, "Formato errato. Il formato corretto è: dd/MM/yyyy.");
            riga.setErrori(erroriImportazione);
        }
        if (StringUtils.hasText(riga.getCollegamentoPrecedente()) && !validateNumeroDocumento(riga.getCollegamentoPrecedente())) {
            erroriImportazione.setError(ColonneProtocolloUscita.collegamentoPrecedente, TipErroriImportazione.Flusso.TipoFlusso.VALIDAZIONE, "Formato errato. Il formato corretto è: numero/yyyy");
            riga.setErrori(erroriImportazione);
        }
        return erroriImportazione;
    }
    
}
