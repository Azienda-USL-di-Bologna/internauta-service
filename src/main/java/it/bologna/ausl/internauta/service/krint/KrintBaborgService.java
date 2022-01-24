
package it.bologna.ausl.internauta.service.krint;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.bologna.ausl.model.entities.baborg.AttributiStruttura;
import it.bologna.ausl.model.entities.baborg.Persona;
import it.bologna.ausl.model.entities.baborg.Struttura;
import it.bologna.ausl.model.entities.baborg.TipologiaStruttura;
import it.bologna.ausl.model.entities.baborg.Utente;
import it.bologna.ausl.model.entities.baborg.UtenteStruttura;
import it.bologna.ausl.model.entities.logs.Krint;
import it.bologna.ausl.model.entities.logs.OperazioneKrint;
import it.bologna.ausl.model.entities.logs.projections.KrintBaborgPersona;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.projection.ProjectionFactory;
import org.springframework.stereotype.Service;
import it.bologna.ausl.model.entities.logs.projections.KrintBaborgStruttura;
import it.bologna.ausl.model.entities.logs.projections.KrintBaborgTipologiaStruttura;

/**
 *
 * @author gusgus
 */
@Service
public class KrintBaborgService {
    
    @Autowired
    ProjectionFactory factory;
   
    @Autowired
    ObjectMapper objectMapper;
    
    @Autowired
    KrintService krintService;
    
    /**
     */
    public void writeUfficioCreation(Struttura struttura, OperazioneKrint.CodiceOperazione codiceOperazione) {
        try {
            // Informazioni oggetto
            KrintBaborgStruttura krintBaborgStruttura = factory.createProjection(KrintBaborgStruttura.class, struttura);
            String jsonKrintUfficio = objectMapper.writeValueAsString(krintBaborgStruttura);
            
            krintService.writeKrintRow(
                struttura.getId().toString(),
                Krint.TipoOggettoKrint.BABORG_UFFICIO,
                struttura.getNome(),
                jsonKrintUfficio,
                null,
                null,
                null,
                null,
                codiceOperazione);
        } catch (Exception ex) {
            Integer idOggetto = null;
            try {
                ex.printStackTrace();
                idOggetto = struttura.getId();
            } catch (Exception exa) {}
            krintService.writeKrintError(idOggetto, "writeUfficioCreation", codiceOperazione);
        }
    }
    
    public void writeUfficioDelete(Struttura struttura, OperazioneKrint.CodiceOperazione codiceOperazione){
       try{
           KrintBaborgStruttura krintBaborgStruttura = factory.createProjection(KrintBaborgStruttura.class, struttura);
           String jsonKrintUfficio = objectMapper.writeValueAsString(krintBaborgStruttura);

           krintService.writeKrintRow(struttura.getId().toString(),
               Krint.TipoOggettoKrint.BABORG_UFFICIO,
               struttura.getNome(),
               jsonKrintUfficio,
               null,
               null,
               null,
               null,
               codiceOperazione);

       } catch (Exception ex){
           Integer idOggetto = null;
           try {
               ex.printStackTrace();
               idOggetto = struttura.getId();
           } catch (Exception exa) {}
           krintService.writeKrintError(idOggetto, "writeUfficioDelete", codiceOperazione);
       }
    }
    
    /**
     * 
     * @param struttura l'ufficio soggetto
     * @param codiceOperazione il tipo di operazione 
     * @param oggetto l'oggetto dell'operazione
     */
    public void writeUfficioUpdate(Struttura struttura, OperazioneKrint.CodiceOperazione codiceOperazione, Object oggetto){
        try{
            KrintBaborgStruttura krintBaborgStruttura = factory.createProjection(KrintBaborgStruttura.class, struttura);
            String jsonKrintUfficio = objectMapper.writeValueAsString(krintBaborgStruttura);
            
            String idOggetto = "";
            String descrizioneOggetto = "";
            Krint.TipoOggettoKrint tipoOggetto = null;
            String jsonKrintOggetto = "";

            switch(codiceOperazione){
                case BABORG_UFFICIO_ATTRIBUTI_STRUTTURA_UPDATE:
                    AttributiStruttura attributiStruttura = (AttributiStruttura) oggetto;
                    TipologiaStruttura tipologiaStruttura = attributiStruttura.getIdTipologiaStruttura();
                    idOggetto = tipologiaStruttura.getId().toString();
                    descrizioneOggetto = tipologiaStruttura.getTipologia();
                    tipoOggetto = Krint.TipoOggettoKrint.BABORG_TIPOLOGIA_STRUTTURA;
                    KrintBaborgTipologiaStruttura krintBaborgTipologiaStruttura = factory.createProjection(KrintBaborgTipologiaStruttura.class, tipologiaStruttura);
                    jsonKrintOggetto = objectMapper.writeValueAsString(krintBaborgTipologiaStruttura);
                    break;
                case BABORG_UFFICIO_NOME_UPDATE:
                    Struttura strutturaOggetto = (Struttura) oggetto;
                    idOggetto = strutturaOggetto.getId().toString();
                    descrizioneOggetto = strutturaOggetto.getNome();
                    tipoOggetto = Krint.TipoOggettoKrint.BABORG_UFFICIO;
                    KrintBaborgStruttura krintBaborgStrutturaOggetto = factory.createProjection(KrintBaborgStruttura.class, strutturaOggetto);
                    jsonKrintOggetto = objectMapper.writeValueAsString(krintBaborgStrutturaOggetto);
                    break;
                case BABORG_UFFICIO_STRUTTURA_PADRE_UPDATE:
                    Struttura strutturaPadreOggetto = (Struttura) oggetto;
                    idOggetto = strutturaPadreOggetto.getId().toString();
                    descrizioneOggetto = strutturaPadreOggetto.getNome();
                    tipoOggetto = Krint.TipoOggettoKrint.BABORG_STRUTTURA;
                    KrintBaborgStruttura krintBaborgStrutturaPadre = factory.createProjection(KrintBaborgStruttura.class, strutturaPadreOggetto);
                    jsonKrintOggetto = objectMapper.writeValueAsString(krintBaborgStrutturaPadre);
                    break;
                case BABORG_UFFICIO_UTENTE_STRUTTURA_LIST_ADD:
                case BABORG_UFFICIO_UTENTE_STRUTTURA_LIST_REMOVE:
                    UtenteStruttura utenteStruttura = (UtenteStruttura) oggetto;
                    Persona persona = utenteStruttura.getIdUtente().getIdPersona();
                    idOggetto = persona.getId().toString();
                    descrizioneOggetto = persona.getDescrizione();
                    tipoOggetto = Krint.TipoOggettoKrint.BABORG_PERSONA;
                    KrintBaborgPersona krintBaborgPersona = factory.createProjection(KrintBaborgPersona.class, persona);
                    jsonKrintOggetto = objectMapper.writeValueAsString(krintBaborgPersona);
                    break;
                case BABORG_UFFICIO_STRUTTURE_CONNESSE_LIST_ADD:
                case BABORG_UFFICIO_STRUTTURE_CONNESSE_LIST_REMOVE:
                    Struttura strutturaConnessaOggetto = (Struttura) oggetto;
                    idOggetto = strutturaConnessaOggetto.getId().toString();
                    descrizioneOggetto = strutturaConnessaOggetto.getNome();
                    tipoOggetto = Krint.TipoOggettoKrint.BABORG_STRUTTURA;
                    KrintBaborgStruttura krintBaborgStrutturaConnessaOggetto = factory.createProjection(KrintBaborgStruttura.class, strutturaConnessaOggetto);
                    jsonKrintOggetto = objectMapper.writeValueAsString(krintBaborgStrutturaConnessaOggetto);
                    break;
                case BABORG_UFFICIO_STRUTTURE_CONNESSE_LIST_PROPAGA_ADD:
                case BABORG_UFFICIO_STRUTTURE_CONNESSE_LIST_PROPAGA_REMOVE:
                    Struttura strutturaConnessaConPropagaOggetto = (Struttura) oggetto;
                    idOggetto = strutturaConnessaConPropagaOggetto.getId().toString();
                    descrizioneOggetto = strutturaConnessaConPropagaOggetto.getNome();
                    tipoOggetto = Krint.TipoOggettoKrint.BABORG_STRUTTURA;
                    KrintBaborgStruttura krintBaborgStrutturaConnessaConPropagaOggetto = factory.createProjection(KrintBaborgStruttura.class, strutturaConnessaConPropagaOggetto);
                    jsonKrintOggetto = objectMapper.writeValueAsString(krintBaborgStrutturaConnessaConPropagaOggetto);
                    break;
            }

            krintService.writeKrintRow(
                idOggetto,
                tipoOggetto,
                descrizioneOggetto,
                jsonKrintOggetto,
                struttura.getId().toString(),
                Krint.TipoOggettoKrint.BABORG_UFFICIO,
                struttura.getNome(),
                jsonKrintUfficio,
                codiceOperazione);

        } catch (Exception ex){
            Integer idOggetto = null;
            try {
                ex.printStackTrace();
                idOggetto = struttura.getId();
            } catch (Exception exa) {}
            krintService.writeKrintError(idOggetto, "writeUfficioUpdate", codiceOperazione);
        }
    }
    /*
    
    public void writeStrutturaMerge(Contatto contatto, OperazioneKrint.CodiceOperazione codiceOperazione, String mergeStr) {
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
                mergeStr,
                codiceOperazione);
        } catch (Exception ex) {
            Integer idOggetto = null;
            try {
                ex.printStackTrace();
                idOggetto = contatto.getId();
            } catch (Exception exa) {}
            krintService.writeKrintError(idOggetto, "writeContactMerge", codiceOperazione);
        }
    }
    

    public void writeStrutturaDetailCreation(DettaglioContatto dettaglioContatto, OperazioneKrint.CodiceOperazione codiceOperazione) {
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
    
    public void writeGroupStrutturaCreation(GruppiContatti gruppoContatto, OperazioneKrint.CodiceOperazione codiceOperazione){
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
    
    public void writeGroupStrutturaDelete(GruppiContatti gruppoContatto, OperazioneKrint.CodiceOperazione codiceOperazione){
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
           krintService.writeKrintError(idOggetto, "writeGroupContactDelete", codiceOperazione);
       }
    }
    
    public void writeGroupStrutturaUpdate(GruppiContatti gruppoContatto, OperazioneKrint.CodiceOperazione codiceOperazione){
       try{
           KrintRubricaGruppoContatto krintRubricaGruppoContatto = factory.createProjection(KrintRubricaGruppoContatto.class, gruppoContatto);
           String jsonKrintGruppo = objectMapper.writeValueAsString(krintRubricaGruppoContatto);

           krintService.writeKrintRow(
               gruppoContatto.getId().toString(),
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
           krintService.writeKrintError(idOggetto, "writeGroupContactUpdate", codiceOperazione);
       }
    }
    
    public void writeGroupDelete(Contatto contatto, OperazioneKrint.CodiceOperazione codiceOperazione){
       try{
           KrintRubricaGruppo krintRubricaGruppo = factory.createProjection(KrintRubricaGruppo.class, contatto);
           String jsonKrintGruppo = objectMapper.writeValueAsString(krintRubricaGruppo);

           krintService.writeKrintRow(
               contatto.getId().toString(),
               Krint.TipoOggettoKrint.RUBRICA_GRUPPO,
               contatto.getDescrizione(),
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
               idOggetto = contatto.getId();
           } catch (Exception exa) {}
           krintService.writeKrintError(idOggetto, "writeGroupDelete", codiceOperazione);
       }
    }
    
    public void writeGroupUpdate(Contatto contatto, OperazioneKrint.CodiceOperazione codiceOperazione){
       try{
           KrintRubricaGruppo krintRubricaGruppo = factory.createProjection(KrintRubricaGruppo.class, contatto);
           String jsonKrintGruppo = objectMapper.writeValueAsString(krintRubricaGruppo);

           krintService.writeKrintRow(
               contatto.getId().toString(),
               Krint.TipoOggettoKrint.RUBRICA_GRUPPO,
               contatto.getDescrizione(),
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
               idOggetto = contatto.getId();
           } catch (Exception exa) {}
           krintService.writeKrintError(idOggetto, "writeGroupUpdate", codiceOperazione);
       }
    }
    
    
    
    
    
    public void writeStrutturaDetailUpdate(DettaglioContatto dettaglioContatto, DettaglioContatto dettaglioContattoOld, OperazioneKrint.CodiceOperazione codiceOperazione){
        try{
            KrintRubricaDettaglioContatto krintRubricaDettaglioContatto = factory.createProjection(KrintRubricaDettaglioContatto.class, dettaglioContatto);
            KrintRubricaDettaglioContatto krintRubricaDettaglioContattoOld = factory.createProjection(KrintRubricaDettaglioContatto.class, dettaglioContattoOld);
            Map<String, KrintRubricaDettaglioContatto> map = new HashMap();
            map.put("idDettaglioContatto", krintRubricaDettaglioContatto);
            map.put("idDettaglioContattoCorrelated", krintRubricaDettaglioContattoOld);
            String jsonKrintDettaglioContatto = objectMapper.writeValueAsString(map);

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

        } catch (Exception ex){
            Integer idOggetto = null;
            try {
                ex.printStackTrace();
                idOggetto = dettaglioContatto.getId();
            } catch (Exception exa) {}
            krintService.writeKrintError(idOggetto, "writeContactDetailUpdate", codiceOperazione);
        }
    }
    
    public void writeStrutturaDetailDelete(DettaglioContatto dettaglioContatto, OperazioneKrint.CodiceOperazione codiceOperazione){
       try{
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

       } catch (Exception ex){
           Integer idOggetto = null;
           try {
               ex.printStackTrace();
               idOggetto = dettaglioContatto.getId();
           } catch (Exception exa) {}
           krintService.writeKrintError(idOggetto, "writeContactDetailDelete", codiceOperazione);
       }
    }*/
}
