package it.bologna.ausl.internauta.service.controllers.tip.validations;

import it.bologna.ausl.model.entities.tip.ImportazioneDocumento;
import it.bologna.ausl.model.entities.tip.ImportazioneOggetto;
import it.bologna.ausl.model.entities.tip.data.ColonneImportazioneOggettoEnums.ColonneProtocolloEntrata;
import it.bologna.ausl.model.entities.tip.data.TipErroriImportazione;
import org.springframework.util.StringUtils;

/**
 *
 * @author gdm
 */
public class ProtocolloEntrataDataValidation extends TipDataValidator {

    @Override
    public TipErroriImportazione validate(ImportazioneOggetto rigaImportazione) {
        TipErroriImportazione erroriImportazione = new TipErroriImportazione();
        ImportazioneDocumento riga = (ImportazioneDocumento) rigaImportazione;
        if (!StringUtils.hasText(riga.getNumero())) {
            erroriImportazione.setError(ColonneProtocolloEntrata.numero, TipErroriImportazione.Flusso.TipoFlusso.VALIDAZIONE, "Il campo è obbligatorio.");
            riga.setErrori(erroriImportazione);
        }
        if (!StringUtils.hasText(riga.getAnno())) {
            erroriImportazione.setError(ColonneProtocolloEntrata.anno, TipErroriImportazione.Flusso.TipoFlusso.VALIDAZIONE, "Il campo è obbligatorio.");
            riga.setErrori(erroriImportazione);
        } else if (!validateAnno(riga.getAnno())) {
            erroriImportazione.setError(ColonneProtocolloEntrata.anno, TipErroriImportazione.Flusso.TipoFlusso.VALIDAZIONE, "Formato errato, il formato corretto è: yyyy");
            riga.setErrori(erroriImportazione);
        }
        if (!StringUtils.hasText(riga.getOggetto())) {
            erroriImportazione.setError(ColonneProtocolloEntrata.oggetto, TipErroriImportazione.Flusso.TipoFlusso.VALIDAZIONE, "Il campo è obbligatorio.");
            riga.setErrori(erroriImportazione);
        }
        if (!StringUtils.hasText(riga.getDataRegistrazione())) {
            erroriImportazione.setError(ColonneProtocolloEntrata.dataRegistrazione, TipErroriImportazione.Flusso.TipoFlusso.VALIDAZIONE, "Il campo è obbligatorio.");
            riga.setErrori(erroriImportazione);
        } else if (!validateData(riga.getDataRegistrazione())) {
            erroriImportazione.setError(ColonneProtocolloEntrata.dataRegistrazione, TipErroriImportazione.Flusso.TipoFlusso.VALIDAZIONE, "Formato errato. Il formato corretto è: dd/MM/yyyy.");
            riga.setErrori(erroriImportazione);
        }
        if (StringUtils.hasText(riga.getDataArrivo()) && !validateData(riga.getDataArrivo())) {
            erroriImportazione.setError(ColonneProtocolloEntrata.dataArrivo, TipErroriImportazione.Flusso.TipoFlusso.VALIDAZIONE, "Formato errato. Il formato corretto è: dd/MM/yyyy.");
            riga.setErrori(erroriImportazione);
        }
        if (!StringUtils.hasText(riga.getProtocollatoDa())) {
            erroriImportazione.setError(ColonneProtocolloEntrata.protocollatoDa, TipErroriImportazione.Flusso.TipoFlusso.VALIDAZIONE, "il campo è obbligatorio.");
            riga.setErrori(erroriImportazione);
        }
        if (!StringUtils.hasText(riga.getMittente()) && !StringUtils.hasText(riga.getIndirizzoMittente())) {
            erroriImportazione.setError(ColonneProtocolloEntrata.mittente, 
                    TipErroriImportazione.Flusso.TipoFlusso.VALIDAZIONE, String.format("E' obbligatorio almeno uno tra %s e %s", ColonneProtocolloEntrata.mittente, ColonneProtocolloEntrata.indirizzoMittente));
            erroriImportazione.setError(ColonneProtocolloEntrata.indirizzoMittente, 
                    TipErroriImportazione.Flusso.TipoFlusso.VALIDAZIONE, String.format("E' obbligatorio almeno uno tra %s e %s", ColonneProtocolloEntrata.indirizzoMittente, ColonneProtocolloEntrata.mittente));
            riga.setErrori(erroriImportazione);
        }
        if (!StringUtils.hasText(riga.getMezzo())) {
            erroriImportazione.setError(ColonneProtocolloEntrata.mezzo, TipErroriImportazione.Flusso.TipoFlusso.VALIDAZIONE, "il campo è obbligatorio.");
            riga.setErrori(erroriImportazione);
        }
        if (StringUtils.hasText(riga.getDataProtocolloEsterno()) && !validateData(riga.getDataProtocolloEsterno())) {
            erroriImportazione.setError(ColonneProtocolloEntrata.dataProtocolloEsterno, TipErroriImportazione.Flusso.TipoFlusso.VALIDAZIONE, "Formato errato. Il formato corretto è: dd/MM/yyyy.");
            riga.setErrori(erroriImportazione);
        }
        if (!StringUtils.hasText(riga.getDestinatariInterniA())) {
            erroriImportazione.setError(ColonneProtocolloEntrata.destinatariInterniA, TipErroriImportazione.Flusso.TipoFlusso.VALIDAZIONE, "il campo è obbligatorio.");
            riga.setErrori(erroriImportazione);
        }
        
        if ( (!StringUtils.hasText(riga.getIdFascicoloPregresso()) && !StringUtils.hasText(riga.getFascicolazione()))) {
            erroriImportazione.setWarning(ColonneProtocolloEntrata.idFascicoloPregresso, TipErroriImportazione.Flusso.TipoFlusso.VALIDAZIONE, "Non è stata inserita la fascicolazione, sarà inserita quella di default.");
            erroriImportazione.setWarning(ColonneProtocolloEntrata.fascicolazione, TipErroriImportazione.Flusso.TipoFlusso.VALIDAZIONE, "Non è stata inserita la fascicolazione, sarà inserita quella di default.");
            riga.setErrori(erroriImportazione);
        } else if ( (StringUtils.hasText(riga.getIdFascicoloPregresso()) && StringUtils.hasText(riga.getFascicolazione()))) {
            erroriImportazione.setError(ColonneProtocolloEntrata.idFascicoloPregresso, TipErroriImportazione.Flusso.TipoFlusso.VALIDAZIONE, 
                    String.format("Un (e un solo) campo tra %s e %s è obbligatorio.", ColonneProtocolloEntrata.idFascicoloPregresso, ColonneProtocolloEntrata.fascicolazione));
            erroriImportazione.setError(ColonneProtocolloEntrata.fascicolazione, TipErroriImportazione.Flusso.TipoFlusso.VALIDAZIONE, 
                    String.format("Un (e un solo) campo tra %s e %s è obbligatorio.", ColonneProtocolloEntrata.fascicolazione, ColonneProtocolloEntrata.idFascicoloPregresso));
            riga.setErrori(erroriImportazione);
        } else if (StringUtils.hasText(riga.getFascicolazione()) && !validateFascicolazioni(riga.getFascicolazione())) {
            erroriImportazione.setError(ColonneProtocolloEntrata.fascicolazione, TipErroriImportazione.Flusso.TipoFlusso.VALIDAZIONE, "Formato errato. Il formato corretto è: num_fasc[-num_sotto_fascicolo](opzionale)[-num_insert](opzionale)/anno[#num_fasc[-num_sotto_fascicolo](opzionale)[-num_insert](opzionale)/anno](opzionale).");
            riga.setErrori(erroriImportazione);
        }
        
        if (StringUtils.hasText(riga.getVisibilitaLimitata()) && !validateBoolean(riga.getVisibilitaLimitata())) {
            erroriImportazione.setError(ColonneProtocolloEntrata.visibilitaLimitata, TipErroriImportazione.Flusso.TipoFlusso.VALIDAZIONE, "Formato errato, il formato corretto è: true/false");
            riga.setErrori(erroriImportazione);
        }
        if (StringUtils.hasText(riga.getRiservato()) && !validateBoolean(riga.getRiservato())) {
            erroriImportazione.setError(ColonneProtocolloEntrata.riservato, TipErroriImportazione.Flusso.TipoFlusso.VALIDAZIONE, "Formato errato, il formato corretto è: true/false");
            riga.setErrori(erroriImportazione);
        }
        if (StringUtils.hasText(riga.getAnnullato()) && !validateBoolean(riga.getAnnullato())) {
            erroriImportazione.setError(ColonneProtocolloEntrata.annullato, TipErroriImportazione.Flusso.TipoFlusso.VALIDAZIONE, "Formato errato, il formato corretto è: true/false");
            riga.setErrori(erroriImportazione);
        }
        if (StringUtils.hasText(riga.getDataAnnullamento()) && !validateData(riga.getDataAnnullamento())) {
            erroriImportazione.setError(ColonneProtocolloEntrata.dataAnnullamento, TipErroriImportazione.Flusso.TipoFlusso.VALIDAZIONE, "Formato errato. Il formato corretto è: dd/MM/yyyy.");
            riga.setErrori(erroriImportazione);
        }
        if (StringUtils.hasText(riga.getDataInvioConservazione()) && !validateData(riga.getDataInvioConservazione())) {
            erroriImportazione.setError(ColonneProtocolloEntrata.dataInvioConservazione, TipErroriImportazione.Flusso.TipoFlusso.VALIDAZIONE, "Formato errato. Il formato corretto è: dd/MM/yyyy.");
            riga.setErrori(erroriImportazione);
        }
        if (StringUtils.hasText(riga.getCollegamentoPrecedente()) && !validateNumeroDocumento(riga.getCollegamentoPrecedente())) {
            erroriImportazione.setError(ColonneProtocolloEntrata.collegamentoPrecedente, TipErroriImportazione.Flusso.TipoFlusso.VALIDAZIONE, "Formato errato. Il formato corretto è: numero/yyyy");
            riga.setErrori(erroriImportazione);
        }
        return erroriImportazione;
    }
    
}
