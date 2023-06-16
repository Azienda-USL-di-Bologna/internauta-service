package it.bologna.ausl.internauta.service.krint;

import com.fasterxml.jackson.core.type.TypeReference;
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
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.projection.ProjectionFactory;
import org.springframework.stereotype.Service;

/**
 *
 * @author gusgus
 */
@Service
public class KrintRubricaService {
    
    private static Logger log = LoggerFactory.getLogger(KrintRubricaService.class);

    @Autowired
    ProjectionFactory factory;

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    KrintService krintService;

    /**
     * @param contatto
     * @param codiceOperazione
     */
    public void writeContactCreation(Contatto contatto, OperazioneKrint.CodiceOperazione codiceOperazione) {
        try {
            // Informazioni oggetto
            KrintRubricaContatto krintRubricaContatto = factory.createProjection(KrintRubricaContatto.class, contatto);
            HashMap<String, Object> krintContatto = objectMapper.convertValue(krintRubricaContatto, new TypeReference<HashMap<String, Object>>() {
            });

            krintService.writeKrintRow(
                    contatto.getId().toString(),
                    Krint.TipoOggettoKrint.RUBRICA_CONTATTO,
                    contatto.getDescrizione(),
                    krintContatto,
                    null,
                    null,
                    null,
                    null,
                    codiceOperazione);
        } catch (Exception ex) {
            log.error("Errore nella writeContactCreation con contatto" + contatto.getId().toString(), ex);
            krintService.writeKrintError(contatto.getId(), "writeContactCreation", codiceOperazione);
        }
    }

    public void writeContactMerge(Contatto contatto, OperazioneKrint.CodiceOperazione codiceOperazione, String mergeStr) {
        try {
            // Informazioni oggetto
            KrintRubricaContatto krintRubricaContatto = factory.createProjection(KrintRubricaContatto.class, contatto);
//            String jsonKrintContatto = objectMapper.writeValueAsString(krintRubricaContatto);
            HashMap<String, Object> krintContatto = objectMapper.convertValue(krintRubricaContatto, new TypeReference<HashMap<String, Object>>() {
            });
            HashMap<String, Object> mergeContatto = objectMapper.convertValue(mergeStr, new TypeReference<HashMap<String, Object>>() {
            });
            krintService.writeKrintRow(
                    contatto.getId().toString(),
                    Krint.TipoOggettoKrint.RUBRICA_CONTATTO,
                    contatto.getDescrizione(),
                    krintContatto,
                    null,
                    null,
                    null,
                    mergeContatto,
                    codiceOperazione);
        } catch (Exception ex) {
            log.error("Errore nella writeContactMerge con contatto" + contatto.getId().toString(), ex);
            krintService.writeKrintError(contatto.getId(), "writeContactMerge", codiceOperazione);
        }
    }

    public void writeContactDetailCreation(DettaglioContatto dettaglioContatto, OperazioneKrint.CodiceOperazione codiceOperazione) {
        try {
            // Informazioni oggetto
            KrintRubricaDettaglioContatto krintRubricaDettaglioContatto = factory.createProjection(KrintRubricaDettaglioContatto.class, dettaglioContatto);
//            String jsonKrintDettaglioContatto = objectMapper.writeValueAsString(krintRubricaDettaglioContatto);
            HashMap<String, Object> krintDettaglioContatto = objectMapper.convertValue(krintRubricaDettaglioContatto, new TypeReference<HashMap<String, Object>>() {
            });
            krintService.writeKrintRow(
                    dettaglioContatto.getId().toString(),
                    Krint.TipoOggettoKrint.RUBRICA_DETTAGLIO_CONTATTO,
                    dettaglioContatto.getDescrizione(),
                    krintDettaglioContatto,
                    dettaglioContatto.getIdContatto().getId().toString(),
                    Krint.TipoOggettoKrint.RUBRICA_CONTATTO,
                    dettaglioContatto.getIdContatto().getDescrizione(),
                    null,
                    codiceOperazione);
        } catch (Exception ex) {
            log.error("Errore nella writeContactDetailCreation con contatto" + dettaglioContatto.getId().toString(), ex);
            krintService.writeKrintError(dettaglioContatto.getId(), "writeContactDetailCreation", codiceOperazione);
        }
    }

    // TODO: Creare funzione writeGroupCreation
    // Creare projection del contattoGruppo che espande verso la gruppi contatti che espande verso id_contatto e verso id_dettaglio_contatto
    // serve crerare sul db il codice operazione etc
    public void writeGroupCreation(Contatto gruppo, OperazioneKrint.CodiceOperazione codiceOperazione) {
        try {
            KrintRubricaGruppo krintRubricaGruppo = factory.createProjection(KrintRubricaGruppo.class, gruppo);
//            String jsonKrintGruppo = objectMapper.writeValueAsString(krintRubricaGruppo);
            HashMap<String, Object> krintRubricaGruppoMap = objectMapper.convertValue(krintRubricaGruppo, new TypeReference<HashMap<String, Object>>() {
            });
            krintService.writeKrintRow(gruppo.getId().toString(),
                    Krint.TipoOggettoKrint.RUBRICA_GRUPPO,
                    gruppo.getDescrizione(),
                    krintRubricaGruppoMap,
                    null,
                    null,
                    null,
                    null,
                    codiceOperazione);

        } catch (Exception ex) {
            log.error("Errore nella writeContactDetailCreation con contatto" + gruppo.getId().toString(), ex);
            krintService.writeKrintError(gruppo.getId(), "writeGroupCreation", codiceOperazione);
        }
    }

    public void writeGroupContactCreation(GruppiContatti gruppoContatto, OperazioneKrint.CodiceOperazione codiceOperazione) {
        try {
            KrintRubricaGruppoContatto krintRubricaGruppoContatto = factory.createProjection(KrintRubricaGruppoContatto.class, gruppoContatto);
//           String jsonKrintGruppo = objectMapper.writeValueAsString(krintRubricaGruppoContatto);
            HashMap<String, Object> krintRubricaGruppoContattoMap = objectMapper.convertValue(krintRubricaGruppoContatto, new TypeReference<HashMap<String, Object>>() {
            });
            krintService.writeKrintRow(gruppoContatto.getId().toString(),
                    Krint.TipoOggettoKrint.RUBRICA_GRUPPO_CONTATTO,
                    gruppoContatto.getId().toString(),
                    krintRubricaGruppoContattoMap,
                    gruppoContatto.getIdGruppo().getId().toString(),
                    Krint.TipoOggettoKrint.RUBRICA_GRUPPO,
                    gruppoContatto.getIdGruppo().getDescrizione(),
                    null,
                    codiceOperazione);

        } catch (Exception ex) {
            log.error("Errore nella writeGroupContactCreation con contatto" + gruppoContatto.getId().toString(), ex);
            krintService.writeKrintError(gruppoContatto.getId(), "writeGroupContactCreation", codiceOperazione);
        }
    }

    public void writeGroupContactDelete(GruppiContatti gruppoContatto, OperazioneKrint.CodiceOperazione codiceOperazione) {
        try {
            KrintRubricaGruppoContatto krintRubricaGruppoContatto = factory.createProjection(KrintRubricaGruppoContatto.class, gruppoContatto);
//            String jsonKrintGruppo = objectMapper.writeValueAsString(krintRubricaGruppoContatto);
            HashMap<String, Object> krintRubricaGruppoContattoMap = objectMapper.convertValue(krintRubricaGruppoContatto, new TypeReference<HashMap<String, Object>>() {
            });
            krintService.writeKrintRow(gruppoContatto.getId().toString(),
                    Krint.TipoOggettoKrint.RUBRICA_GRUPPO_CONTATTO,
                    gruppoContatto.getId().toString(),
                    krintRubricaGruppoContattoMap,
                    gruppoContatto.getIdGruppo().getId().toString(),
                    Krint.TipoOggettoKrint.RUBRICA_GRUPPO,
                    gruppoContatto.getIdGruppo().getDescrizione(),
                    null,
                    codiceOperazione);

        } catch (Exception ex) {
            log.error("Errore nella writeGroupContactDelete con contatto" + gruppoContatto.getId().toString(), ex);
            krintService.writeKrintError(gruppoContatto.getId(), "writeGroupContactDelete", codiceOperazione);
        }
    }

    public void writeGroupContactUpdate(GruppiContatti gruppoContatto, OperazioneKrint.CodiceOperazione codiceOperazione) {
        try {
            KrintRubricaGruppoContatto krintRubricaGruppoContatto = factory.createProjection(KrintRubricaGruppoContatto.class, gruppoContatto);
//            String jsonKrintGruppo = objectMapper.writeValueAsString(krintRubricaGruppoContatto);
            HashMap<String, Object> krintRubricaGruppoContattoMap = objectMapper.convertValue(krintRubricaGruppoContatto, new TypeReference<HashMap<String, Object>>() {
            });
            krintService.writeKrintRow(
                    gruppoContatto.getId().toString(),
                    Krint.TipoOggettoKrint.RUBRICA_GRUPPO_CONTATTO,
                    gruppoContatto.getId().toString(),
                    krintRubricaGruppoContattoMap,
                    gruppoContatto.getIdGruppo().getId().toString(),
                    Krint.TipoOggettoKrint.RUBRICA_GRUPPO,
                    gruppoContatto.getIdGruppo().getDescrizione(),
                    null,
                    codiceOperazione);

        } catch (Exception ex) {
            log.error("Errore nella writeGroupContactUpdate con contatto" + gruppoContatto.getId().toString(), ex);
            krintService.writeKrintError(gruppoContatto.getId(), "writeGroupContactUpdate", codiceOperazione);
        }
    }

    public void writeGroupDelete(Contatto contatto, OperazioneKrint.CodiceOperazione codiceOperazione) {
        try {
            KrintRubricaGruppo krintRubricaGruppo = factory.createProjection(KrintRubricaGruppo.class, contatto);
//            String jsonKrintGruppo = objectMapper.writeValueAsString(krintRubricaGruppo);
            HashMap<String, Object> krintContattoMap = objectMapper.convertValue(krintRubricaGruppo, new TypeReference<HashMap<String, Object>>() {
            });
            krintService.writeKrintRow(
                    contatto.getId().toString(),
                    Krint.TipoOggettoKrint.RUBRICA_GRUPPO,
                    contatto.getDescrizione(),
                    krintContattoMap,
                    null,
                    null,
                    null,
                    null,
                    codiceOperazione);

        } catch (Exception ex) {
            log.error("Errore nella writeGroupDelete con contatto" + contatto.getId().toString(), ex);
            krintService.writeKrintError(contatto.getId(), "writeGroupDelete", codiceOperazione);
        }
    }

    public void writeGroupUpdate(Contatto contatto, OperazioneKrint.CodiceOperazione codiceOperazione) {
        try {
            KrintRubricaGruppo krintRubricaGruppo = factory.createProjection(KrintRubricaGruppo.class, contatto);
//            String jsonKrintGruppo = objectMapper.writeValueAsString(krintRubricaGruppo);
            HashMap<String, Object> krintContattoMap = objectMapper.convertValue(krintRubricaGruppo, new TypeReference<HashMap<String, Object>>() {});
            krintService.writeKrintRow(
                    contatto.getId().toString(),
                    Krint.TipoOggettoKrint.RUBRICA_GRUPPO,
                    contatto.getDescrizione(),
                    krintContattoMap,
                    null,
                    null,
                    null,
                    null,
                    codiceOperazione);

        } catch (Exception ex) {
            log.error("Errore nella writeGroupUpdate con contatto" + contatto.getId().toString(), ex);
            krintService.writeKrintError(contatto.getId(), "writeGroupUpdate", codiceOperazione);
        }
    }

    public void writeGroupRiservato(Contatto contatto, OperazioneKrint.CodiceOperazione codiceOperazione) {
        try {
            KrintRubricaGruppo krintRubricaGruppo = factory.createProjection(KrintRubricaGruppo.class, contatto);
//            String jsonKrintGruppo = objectMapper.writeValueAsString(krintRubricaGruppo);
            String azione = contatto.getRiservato() == true ? "reso " : "reso non";
            HashMap<String, Object> krintContattoMap = objectMapper.convertValue(krintRubricaGruppo, new TypeReference<HashMap<String, Object>>() {});
            krintService.writeKrintRow(
                    contatto.getId().toString(),
                    Krint.TipoOggettoKrint.RUBRICA_GRUPPO,
                    azione,
                    krintContattoMap,
                    null,
                    null,
                    null,
                    null,
                    codiceOperazione);

        } catch (Exception ex) {
            log.error("Errore nella writeGroupRiservato con contatto" + contatto.getId().toString(), ex);
            krintService.writeKrintError(contatto.getId(), "writeGroupRiservato", codiceOperazione);
        }
    }

    public void writeContactDelete(Contatto contatto, OperazioneKrint.CodiceOperazione codiceOperazione) {
        try {
            KrintRubricaContatto krintRubricaContatto = factory.createProjection(KrintRubricaContatto.class, contatto);
//            String jsonKrintContact = objectMapper.writeValueAsString(krintRubricaContatto);
            HashMap<String, Object> krintContattoMap = objectMapper.convertValue(krintRubricaContatto, new TypeReference<HashMap<String, Object>>() {});
            krintService.writeKrintRow(contatto.getId().toString(),
                    Krint.TipoOggettoKrint.RUBRICA_CONTATTO,
                    contatto.getId().toString(),
                    krintContattoMap,
                    null,
                    null,
                    null,
                    null,
                    codiceOperazione);

        } catch (Exception ex) {
            log.error("Errore nella writeContactDelete con contatto" + contatto.getId().toString(), ex);
            krintService.writeKrintError(contatto.getId(), "writeContactDelete", codiceOperazione);
        }
    }

    public void writeContactUpdate(Contatto contatto, OperazioneKrint.CodiceOperazione codiceOperazione) {
        try {
            KrintRubricaContatto krintRubricaContatto = factory.createProjection(KrintRubricaContatto.class, contatto);
//            String jsonKrintContact = objectMapper.writeValueAsString(krintRubricaContatto);
            HashMap<String, Object> krintContattoMap = objectMapper.convertValue(krintRubricaContatto, new TypeReference<HashMap<String, Object>>() {});
            krintService.writeKrintRow(
                    contatto.getId().toString(),
                    Krint.TipoOggettoKrint.RUBRICA_CONTATTO,
                    contatto.getDescrizione(),
                    krintContattoMap,
                    null,
                    null,
                    null,
                    null,
                    codiceOperazione);

        } catch (Exception ex) {
            log.error("Errore nella writeContactUpdate con contatto" + contatto.getId().toString(), ex);
            krintService.writeKrintError(contatto.getId(), "writeContactUpdate", codiceOperazione);
        }
    }

    public void writeContactRiservato(Contatto contatto, OperazioneKrint.CodiceOperazione codiceOperazione) {
        try {
            KrintRubricaContatto krintRubricaContatto = factory.createProjection(KrintRubricaContatto.class, contatto);
//            String jsonKrintContact = objectMapper.writeValueAsString(krintRubricaContatto);
            String azione = contatto.getRiservato() == true ? "reso " : "reso non";
            HashMap<String, Object> krintContattoMap = objectMapper.convertValue(krintRubricaContatto, new TypeReference<HashMap<String, Object>>() {});
            krintService.writeKrintRow(
                    contatto.getId().toString(),
                    Krint.TipoOggettoKrint.RUBRICA_CONTATTO,
                    azione,
                    krintContattoMap,
                    null,
                    null,
                    null,
                    null,
                    codiceOperazione);

        } catch (Exception ex) {
            log.error("Errore nella writeContactRiservato con contatto" + contatto.getId().toString(), ex);
            krintService.writeKrintError(contatto.getId(), "writeContactRiservato", codiceOperazione);
        }
    }

    public void writeContactDetailUpdate(DettaglioContatto dettaglioContatto, DettaglioContatto dettaglioContattoOld, OperazioneKrint.CodiceOperazione codiceOperazione) {
        try {
            KrintRubricaDettaglioContatto krintRubricaDettaglioContatto = factory.createProjection(KrintRubricaDettaglioContatto.class, dettaglioContatto);
            KrintRubricaDettaglioContatto krintRubricaDettaglioContattoOld = factory.createProjection(KrintRubricaDettaglioContatto.class, dettaglioContattoOld);
            
            HashMap<String, Object> map = new HashMap();
            map.put("idDettaglioContatto", krintRubricaDettaglioContatto);
            map.put("idDettaglioContattoCorrelated", krintRubricaDettaglioContattoOld);
//            String jsonKrintDettaglioContatto = objectMapper.writeValueAsString(map);
            
            krintService.writeKrintRow(
                    dettaglioContatto.getId().toString(),
                    Krint.TipoOggettoKrint.RUBRICA_DETTAGLIO_CONTATTO,
                    dettaglioContatto.getDescrizione(),
                    map,
                    dettaglioContatto.getIdContatto().getId().toString(),
                    Krint.TipoOggettoKrint.RUBRICA_CONTATTO,
                    dettaglioContatto.getIdContatto().getDescrizione(),
                    null,
                    codiceOperazione);

        } catch (Exception ex) {
            log.error("Errore nella writeContactDetailUpdate con contatto" + dettaglioContatto.getId().toString(), ex);
            krintService.writeKrintError(dettaglioContatto.getId(), "writeContactDetailUpdate", codiceOperazione);
        }
    }

    public void writeContactDetailDelete(DettaglioContatto dettaglioContatto, OperazioneKrint.CodiceOperazione codiceOperazione) {
        try {
            KrintRubricaDettaglioContatto krintRubricaDettaglioContatto = factory.createProjection(KrintRubricaDettaglioContatto.class, dettaglioContatto);
//            String jsonKrintDettaglioContatto = objectMapper.writeValueAsString(krintRubricaDettaglioContatto);
            HashMap<String, Object> krintDettaglioContattoMap = objectMapper.convertValue(krintRubricaDettaglioContatto, new TypeReference<HashMap<String, Object>>() {});
            krintService.writeKrintRow(
                    dettaglioContatto.getId().toString(),
                    Krint.TipoOggettoKrint.RUBRICA_DETTAGLIO_CONTATTO,
                    dettaglioContatto.getDescrizione(),
                    krintDettaglioContattoMap,
                    dettaglioContatto.getIdContatto().getId().toString(),
                    Krint.TipoOggettoKrint.RUBRICA_CONTATTO,
                    dettaglioContatto.getIdContatto().getDescrizione(),
                    null,
                    codiceOperazione);

        } catch (Exception ex) {
            log.error("Errore nella writeContactDetailDelete con contatto" + dettaglioContatto.getId().toString(), ex);
            krintService.writeKrintError(dettaglioContatto.getId(), "writeContactDetailDelete", codiceOperazione);
        }
    }
}
