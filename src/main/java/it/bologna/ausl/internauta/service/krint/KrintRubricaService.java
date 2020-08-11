
package it.bologna.ausl.internauta.service.krint;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.bologna.ausl.model.entities.logs.Krint;
import it.bologna.ausl.model.entities.logs.OperazioneKrint;
import it.bologna.ausl.model.entities.logs.projections.KrintRubricaContatto;
import it.bologna.ausl.model.entities.logs.projections.KrintRubricaDettaglioContatto;
import it.bologna.ausl.model.entities.logs.projections.KrintRubricaGruppo;
import it.bologna.ausl.model.entities.logs.projections.KrintRubricaGruppoContatto;
import it.bologna.ausl.model.entities.rubrica.Contatto;
import it.bologna.ausl.model.entities.rubrica.DettaglioContatto;
import it.bologna.ausl.model.entities.rubrica.GruppiContatti;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.projection.ProjectionFactory;
import org.springframework.stereotype.Service;

/**
 *
 * @author gusgus
 */
@Service
public class KrintRubricaService {
    
    @Autowired
    ProjectionFactory factory;
   
    @Autowired
    ObjectMapper objectMapper;
    
    @Autowired
    KrintService krintService;
    
    /**
     */
    public void writeContactCreation(Contatto contatto, OperazioneKrint.CodiceOperazione codiceOperazione) {
        try {
            // Informazioni oggetto
            KrintRubricaContatto krintRubricaContatto = factory.createProjection(KrintRubricaContatto.class, contatto);
            String jsonKrintContatto = objectMapper.writeValueAsString(krintRubricaContatto);
            
            krintService.writeKrintRow(
                contatto.getId().toString(),
                Krint.TipoOggettoKrint.RUBRICA_CONTATTO,
                contatto.getDescrizione(),
                jsonKrintContatto,
                null,
                null,
                null,
                null,
                codiceOperazione);
        } catch (Exception ex) {
            Integer idOggetto = null;
            try {
                ex.printStackTrace();
                idOggetto = contatto.getId();
            } catch (Exception exa) {}
            krintService.writeKrintError(idOggetto, "writeContactCreation", codiceOperazione);
        }
    }
    

    public void writeContactDetailCreation(DettaglioContatto dettaglioContatto, OperazioneKrint.CodiceOperazione codiceOperazione) {
        try {
            // Informazioni oggetto
            KrintRubricaDettaglioContatto krintRubricaDettaglioContatto = factory.createProjection(KrintRubricaDettaglioContatto.class, dettaglioContatto);
            String jsonKrintDettaglioContatto = objectMapper.writeValueAsString(krintRubricaDettaglioContatto);
            
            krintService.writeKrintRow(
                dettaglioContatto.getId().toString(),
                Krint.TipoOggettoKrint.RUBRICA_DETTAGLIO_CONTATTO,
                dettaglioContatto.getDescrizione(),
                jsonKrintDettaglioContatto,
                dettaglioContatto.getIdContatto().getId().toString(),
                Krint.TipoOggettoKrint.RUBRICA_CONTATTO,
                dettaglioContatto.getIdContatto().getDescrizione(),
                null,
                codiceOperazione);
        } catch (Exception ex) {
            Integer idOggetto = null;
            try {
                ex.printStackTrace();
                idOggetto = dettaglioContatto.getId();
            } catch (Exception exa) {}
            krintService.writeKrintError(idOggetto, "writeContactDetailCreation", codiceOperazione);
        }
    }
    
    // TODO: Creare funzione writeGroupCreation
    // Creare projection del contattoGruppo che espande verso la gruppi contatti che espande verso id_contatto e verso id_dettaglio_contatto
    // serve crerare sul db il codice operazione etc
    public void writeGroupCreation(Contatto gruppo, OperazioneKrint.CodiceOperazione codiceOperazione){
        try{
            KrintRubricaGruppo krintRubricaGruppo = factory.createProjection(KrintRubricaGruppo.class, gruppo);
            String jsonKrintGruppo = objectMapper.writeValueAsString(krintRubricaGruppo);
            
            krintService.writeKrintRow(gruppo.getId().toString(),
                Krint.TipoOggettoKrint.RUBRICA_GRUPPO,
                gruppo.getDescrizione(),
                jsonKrintGruppo,
                null,
                null,
                null,
                null,
                codiceOperazione);
            
        } catch (Exception ex){
            Integer idOggetto = null;
            try {
                ex.printStackTrace();
                idOggetto = gruppo.getId();
            } catch (Exception exa) {}
            krintService.writeKrintError(idOggetto, "writeGroupCreation", codiceOperazione);
        }
    }
    
     public void writeGroupContactCreation(GruppiContatti gruppoContatto, OperazioneKrint.CodiceOperazione codiceOperazione){
        try{
            KrintRubricaGruppoContatto krintRubricaGruppoContatto = factory.createProjection(KrintRubricaGruppoContatto.class, gruppoContatto);
            String jsonKrintGruppo = objectMapper.writeValueAsString(krintRubricaGruppoContatto);
            
            krintService.writeKrintRow(gruppoContatto.getId().toString(),
                Krint.TipoOggettoKrint.RUBRICA_GRUPPO_CONTATTO,
                gruppoContatto.getId().toString(),
                jsonKrintGruppo,
                gruppoContatto.getIdGruppo().getId().toString(),
                Krint.TipoOggettoKrint.RUBRICA_GRUPPO,
                gruppoContatto.getIdGruppo().getDescrizione(),
                null,
                codiceOperazione);
            
        } catch (Exception ex){
            Integer idOggetto = null;
            try {
                ex.printStackTrace();
                idOggetto = gruppoContatto.getId();
            } catch (Exception exa) {}
            krintService.writeKrintError(idOggetto, "writeGroupContactCreation", codiceOperazione);
        }
    }
}
