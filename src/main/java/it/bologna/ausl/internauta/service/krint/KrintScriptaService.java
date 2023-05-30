package it.bologna.ausl.internauta.service.krint;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.bologna.ausl.internauta.service.repositories.scripta.ArchivioRepository;
import it.bologna.ausl.internauta.service.utils.CachedEntities;
import it.bologna.ausl.internauta.utils.bds.types.EntitaStoredProcedure;
import it.bologna.ausl.internauta.utils.bds.types.PermessoStoredProcedure;
import it.bologna.ausl.model.entities.baborg.Persona;
import it.bologna.ausl.model.entities.baborg.Struttura;
import it.bologna.ausl.model.entities.logs.Krint;
import it.bologna.ausl.model.entities.logs.OperazioneKrint;
import it.bologna.ausl.model.entities.logs.projections.KrintBaborgPersona;
import it.bologna.ausl.model.entities.logs.projections.KrintBaborgStruttura;
import it.bologna.ausl.model.entities.logs.projections.KrintScriptaArchivio;
import it.bologna.ausl.model.entities.logs.projections.KrintScriptaAttoreArchivio;
import it.bologna.ausl.model.entities.logs.projections.KrintScriptaDoc;
import it.bologna.ausl.model.entities.scripta.Archivio;
import it.bologna.ausl.model.entities.scripta.ArchivioDoc;
import it.bologna.ausl.model.entities.scripta.AttoreArchivio;
import it.bologna.ausl.model.entities.scripta.Doc;
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
public class KrintScriptaService {
    
    private static Logger log = LoggerFactory.getLogger(KrintScriptaService.class);

    @Autowired
    private ProjectionFactory factory;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private KrintService krintService;

    @Autowired
    private CachedEntities cachedEntities;

    @Autowired
    private ArchivioRepository archivioRepository;
    
    /**
     * Scrive il log di creazione di un archivio.
     * @param archivio L'archivio creato.
     * @param codiceOperazione Il codice dell'operazione
     */
    public void writeArchivioCreation(Archivio archivio, OperazioneKrint.CodiceOperazione codiceOperazione) {
        writeArchivioCreation(archivio, null, codiceOperazione);
    }

    /**
     * Scrive il log di creazione di un archivio.
     * @param archivio L'archivio creato.
     * @param archivioDiRiferimento Archivio di riferimento se è stato generato da una copia o un duplica.
     * @param codiceOperazione Il codice dell'operazione
     */
    public void writeArchivioCreation(Archivio archivio, Archivio archivioDiRiferimento, OperazioneKrint.CodiceOperazione codiceOperazione) {
        try {
            // Informazioni oggetto
            Map<String, Object> map = getArchivioPropertiesAsMap(archivio);
            
            if (archivioDiRiferimento != null) {
                KrintScriptaArchivio krintScriptaArchivioRif = factory.createProjection(KrintScriptaArchivio.class, archivioDiRiferimento);
                map.put("archivioDiRiferimento", krintScriptaArchivioRif);  
            }
//            String jsonKrintArchivio = objectMapper.writeValueAsString(map);   
             
            krintService.writeKrintRow(
                    archivio.getId().toString(), // idOggetto
                    Krint.TipoOggettoKrint.SCRIPTA_ARCHIVIO, // tipoOggetto
                    archivio.getNumerazioneGerarchica(), // descrizioneOggetto
                    map, // informazioniOggetto
                    null, // Da qui si ripete ma per il conenitore
                    null,
                    null,
                    null,
                    codiceOperazione);
            
            // Logghiamo la stessa operazione anche nel padre
            if (archivio.getIdArchivioPadre() != null) {
                krintService.writeKrintRow(
                    archivio.getIdArchivioPadre().getId().toString(), // idOggetto
                    Krint.TipoOggettoKrint.SCRIPTA_ARCHIVIO, // tipoOggetto
                    archivio.getNumerazioneGerarchica(), // descrizioneOggetto
                    map, // informazioniOggetto
                    null, // Da qui si ripete ma per il conenitore
                    null,
                    null,
                    null,
                    codiceOperazione);
            }
        } catch (Exception ex) {
            log.error("Errore nella writeArchivioCreation con archivio" + archivio.getId().toString(), ex);
            krintService.writeKrintError(archivio.getId(), "writeArchivioCreation", codiceOperazione);
        }
    }
    
    /**
     * Scrive il log di aggiornamento di un archivio.
     * @param archivio L'archivio aggiornato.
     * @param codiceOperazione Il codice dell'operazione.
     */
    public void writeArchivioUpdate(Archivio archivio, OperazioneKrint.CodiceOperazione codiceOperazione){
        writeArchivioUpdate(archivio, null, codiceOperazione);
    }
        
    /**
     * Scrive il log di aggiornamento di un archivio.
     * @param archivio L'archivio aggiornato.
     * @param archivioDiRiferimento L'archivio di riferimento utile al log specifico.
     * @param codiceOperazione Il codice dell'operazione.
     */
    public void writeArchivioUpdate(Archivio archivio, Archivio archivioDiRiferimento, OperazioneKrint.CodiceOperazione codiceOperazione) {
        writeArchivioUpdate(archivio, archivioDiRiferimento, codiceOperazione, false);
    }
        
    /**
     * Scrive il log di aggiornamento di un archivio.
     * @param archivio L'archivio aggiornato.
     * @param archivioDiRiferimento L'archivio di riferimento utile al log specifico.
     * @param codiceOperazione Il codice dell'operazione.
     * @param canLogOnPadre {@code true} per fare il log sull'archivio padre per archivio.
     */
    public void writeArchivioUpdate(Archivio archivio, Archivio archivioDiRiferimento, OperazioneKrint.CodiceOperazione codiceOperazione, boolean canLogOnPadre ) {
        
        try {
            // Informazioni oggetto
            Map<String, Object> map = getArchivioPropertiesAsMap(archivio);
            
            if (archivioDiRiferimento != null) {
                KrintScriptaArchivio krintScriptaArchivioRif = factory.createProjection(KrintScriptaArchivio.class, archivioDiRiferimento);
                map.put("archivioDiRiferimento", krintScriptaArchivioRif);  
            }
//            String jsonKrintArchivio = objectMapper.writeValueAsString(map);
            
            krintService.writeKrintRow(
                    archivio.getId().toString(), // idOggetto
                    Krint.TipoOggettoKrint.SCRIPTA_ARCHIVIO, // tipoOggetto
                    archivio.getNumerazioneGerarchica(), // descrizioneOggetto
                    map, // informazioniOggetto
                    null, // Da qui si ripete ma per il conenitore
                    null,
                    null,
                    null,
                    codiceOperazione); 
                        if (canLogOnPadre && archivio.getIdArchivioPadre() != null && 
                                OperazioneKrint.CodiceOperazione.SCRIPTA_ARCHIVIO_SPOSTA.equals(codiceOperazione)) 
                krintService.writeKrintRow(
                    archivio.getIdArchivioPadre().getId().toString(), // idOggetto
                    Krint.TipoOggettoKrint.SCRIPTA_ARCHIVIO, // tipoOggetto
                    archivio.getNumerazioneGerarchica(), // descrizioneOggetto
                    map, // informazioniOggetto
                    null, // Da qui si ripete ma per il conenitore
                    null,
                    null,
                    null,
                    codiceOperazione);                 
            // Se è uno SPOSTA FASCICOLO logghiamo sul vecchio padre lo stesso messaggio
            if (archivioDiRiferimento != null && archivioDiRiferimento.getIdArchivioPadre() != null && 
                    OperazioneKrint.CodiceOperazione.SCRIPTA_ARCHIVIO_SPOSTA.equals(codiceOperazione))
                krintService.writeKrintRow(
                    archivioDiRiferimento.getIdArchivioPadre().getId().toString(), // idOggetto
                    Krint.TipoOggettoKrint.SCRIPTA_ARCHIVIO, // tipoOggetto
                    archivio.getNumerazioneGerarchica(), // descrizioneOggetto
                    map, // informazioniOggetto
                    null, // Da qui si ripete ma per il conenitore
                    null,
                    null,
                    null,
                    codiceOperazione);
        
        } catch (Exception ex) {
            log.error("Errore nella writeContactCreation con archivio " + archivio.getId().toString(), ex);
            krintService.writeKrintError(archivio.getId(), "writeArchivioUpdate", codiceOperazione);
        }
    }
    
    /**
     * Scrive il log di eliminazione di un archivio nell'archivio padre.
     * @param archivio L'archivio eliminato.
     * @param codiceOperazione Il codice dell'operazione.
     */
    public void writeArchivioDelete(Archivio archivio, OperazioneKrint.CodiceOperazione codiceOperazione){
        try {
            // Informazioni oggetto
            Archivio archivioPadre = archivio.getIdArchivioPadre();
            Map<String, Object> map = getArchivioPropertiesAsMap(archivioPadre);
            
            KrintScriptaArchivio krintScriptaArchivioRif = factory.createProjection(KrintScriptaArchivio.class, archivio);
            map.put("archivioDiRiferimento", krintScriptaArchivioRif);  
   
//            String jsonKrintArchivio = objectMapper.writeValueAsString(map);
            
            krintService.writeKrintRow(
                    archivioPadre.getId().toString(), // idOggetto
                    Krint.TipoOggettoKrint.SCRIPTA_ARCHIVIO, // tipoOggetto
                    archivioPadre.getNumerazioneGerarchica(), // descrizioneOggetto
                    map, // informazioniOggetto
                    null, // Da qui si ripete ma per il conenitore
                    null,
                    null,
                    null,
                    codiceOperazione);           
        } catch (Exception ex) {
            log.error("Errore nella writeArchivioUpdate con archivio " + archivio.getId().toString(), ex);
            krintService.writeKrintError(archivio.getId(), "writeArchivioUpdate", codiceOperazione);
        }
    }

    /**
     * @param attoreArchivio
     * @param codiceOperazione
     */
    public void writeAttoreArchivioCreation(AttoreArchivio attoreArchivio, OperazioneKrint.CodiceOperazione codiceOperazione) {
        try {
            // Informazioni oggetto
            KrintScriptaAttoreArchivio krintScriptaAttoreArchivio = factory.createProjection(KrintScriptaAttoreArchivio.class, attoreArchivio);
//            String jsonKrintAttore = objectMapper.writeValueAsString(krintScriptaAttoreArchivio);

            // Informazioni oggetto contenitore
            KrintScriptaArchivio krintScriptaArchivio = factory.createProjection(KrintScriptaArchivio.class, attoreArchivio.getIdArchivio());
//            String jsonKrintArchivio = objectMapper.writeValueAsString(krintScriptaArchivio);
            
            HashMap<String, Object> krintArchivio = objectMapper.convertValue(krintScriptaArchivio, new TypeReference<HashMap<String, Object>>(){});
            HashMap<String, Object> krintAttore = objectMapper.convertValue(krintScriptaAttoreArchivio, new TypeReference<HashMap<String, Object>>(){});
            krintService.writeKrintRow(
                    attoreArchivio.getId().toString(), // idOggetto
                    Krint.TipoOggettoKrint.SCRIPTA_ATTORE_ARCHIVIO, // tipoOggetto
                    attoreArchivio.getIdPersona().getDescrizione(), // descrizioneOggetto
                    krintAttore, // informazioniOggetto
                    attoreArchivio.getIdArchivio().getId().toString(), // Da qui si ripete ma per il conenitore
                    Krint.TipoOggettoKrint.SCRIPTA_ARCHIVIO,
                    attoreArchivio.getIdArchivio().getNumerazioneGerarchica(),
                    krintArchivio,
                    codiceOperazione);
        } catch (Exception ex) {
            log.error("Errore nella writeAttoreArchivioCreation con archivio " + attoreArchivio.getId().toString(), ex);
            krintService.writeKrintError(attoreArchivio.getId(), "writeAttoreArchivioCreation", codiceOperazione);
        }
    }

    /**
     * @param attoreArchivio
     * @param codiceOperazione
     */
    public void writeAttoreArchivioUpdate(AttoreArchivio attoreArchivio, OperazioneKrint.CodiceOperazione codiceOperazione) {
        try {
            // Informazioni oggetto
            KrintScriptaAttoreArchivio krintScriptaAttoreArchivio = factory.createProjection(KrintScriptaAttoreArchivio.class, attoreArchivio);
//            String jsonKrintAttore = objectMapper.writeValueAsString(krintScriptaAttoreArchivio);
            
            HashMap<String, Object> krintAttore = objectMapper.convertValue(krintScriptaAttoreArchivio, new TypeReference<HashMap<String, Object>>(){});
            // Informazioni oggetto contenitore
            KrintScriptaArchivio krintScriptaArchivio = factory.createProjection(KrintScriptaArchivio.class, attoreArchivio.getIdArchivio());
//            String jsonKrintArchivio = objectMapper.writeValueAsString(krintScriptaArchivio);
            HashMap<String, Object> krintArchivio = objectMapper.convertValue(krintScriptaArchivio, new TypeReference<HashMap<String, Object>>(){});
            krintService.writeKrintRow(
                    attoreArchivio.getId().toString(), // idOggetto
                    Krint.TipoOggettoKrint.SCRIPTA_ATTORE_ARCHIVIO, // tipoOggetto
                    attoreArchivio.getIdPersona().getDescrizione(), // descrizioneOggetto
                    krintAttore, // informazioniOggetto
                    attoreArchivio.getIdArchivio().getId().toString(), // Da qui si ripete ma per il conenitore
                    Krint.TipoOggettoKrint.SCRIPTA_ARCHIVIO,
                    attoreArchivio.getIdArchivio().getNumerazioneGerarchica(),
                    krintArchivio,
                    codiceOperazione);
        } catch (Exception ex) {
            log.error("Errore nella writeAttoreArchivioUpdate con archivio" + attoreArchivio.getId().toString(), ex);
            krintService.writeKrintError(attoreArchivio.getId(), "writeAttoreArchivioUpdate", codiceOperazione);
        }
    }

    /**
     * @param attoreArchivio
     * @param codiceOperazione
     */
    public void writeAttoreArchivioDelete(AttoreArchivio attoreArchivio, OperazioneKrint.CodiceOperazione codiceOperazione) {
        try {
            // Informazioni oggetto
            KrintScriptaAttoreArchivio krintScriptaAttoreArchivio = factory.createProjection(KrintScriptaAttoreArchivio.class, attoreArchivio);
//            String jsonKrintAttore = objectMapper.writeValueAsString(krintScriptaAttoreArchivio);

            // Informazioni oggetto contenitore
            KrintScriptaArchivio krintScriptaArchivio = factory.createProjection(KrintScriptaArchivio.class, attoreArchivio.getIdArchivio());
//            String jsonKrintArchivio = objectMapper.writeValueAsString(krintScriptaArchivio);
            HashMap<String, Object> krintArchivio = objectMapper.convertValue(krintScriptaArchivio, new TypeReference<HashMap<String, Object>>(){});
            HashMap<String, Object> krintAttore = objectMapper.convertValue(krintScriptaAttoreArchivio, new TypeReference<HashMap<String, Object>>(){});

            krintService.writeKrintRow(
                    attoreArchivio.getId().toString(), // idOggetto
                    Krint.TipoOggettoKrint.SCRIPTA_ATTORE_ARCHIVIO, // tipoOggetto
                    attoreArchivio.getIdPersona().getDescrizione(), // descrizioneOggetto
                    krintAttore, // informazioniOggetto
                    attoreArchivio.getIdArchivio().getId().toString(), // Da qui si ripete ma per il conenitore
                    Krint.TipoOggettoKrint.SCRIPTA_ARCHIVIO,
                    attoreArchivio.getIdArchivio().getNumerazioneGerarchica(),
                    krintArchivio,
                    codiceOperazione);
        } catch (Exception ex) {
            log.error("Errore nella writeAttoreArchivioDelete con archivio " + attoreArchivio.getId().toString(), ex);
            krintService.writeKrintError(attoreArchivio.getId(), "writeAttoreArchivioDelete", codiceOperazione);
        }
    }

    /**
     * Scrive il krint (log) della modifica dei permessi.
     * @param idArchivio L'id dell'archivio a cui si riferisce il log.
     * @param entita L'entity dell'archivio.
     * @param permessoStoredProcedure L'effettivo permesso dell'operazione.
     * @param codiceOperazione Può essere INSERT, UPDATE o DELETE.
     */
    public void writePermessiArchivio(Integer idArchivio, EntitaStoredProcedure entita, PermessoStoredProcedure permessoStoredProcedure, OperazioneKrint.CodiceOperazione codiceOperazione) {
        Archivio archivio = archivioRepository.getById(idArchivio);
        try {
            Map<String, Object> mapKrintArchivio = getArchivioPropertiesAsMap(archivio);
            mapKrintArchivio.put("predicato", permessoStoredProcedure.getPredicato());
            mapKrintArchivio.put("propagaOggetto", permessoStoredProcedure.getPropagaOggetto() ? "con" : "senza");
            
            String idOggetto = null;
            String descrizioneOggetto = null;
            Krint.TipoOggettoKrint tipoOggetto = null;
//            String jsonKrintEntitaPermessoArchivio = null;
            HashMap<String, Object> krintEntitaPermessoArchivio = null;
            
            switch (entita.getTable()) {
                case "persone":
                    Persona persona = cachedEntities.getPersona(entita.getIdProvenienza());
                    KrintBaborgPersona krintBaborgPersona = factory.createProjection(KrintBaborgPersona.class, persona);
//                    jsonKrintEntitaPermessoArchivio = objectMapper.writeValueAsString(krintBaborgPersona);
                    krintEntitaPermessoArchivio = objectMapper.convertValue(krintBaborgPersona, new TypeReference<HashMap<String, Object>>(){});
                    idOggetto = krintBaborgPersona.getId().toString();
                    descrizioneOggetto = krintBaborgPersona.getDescrizione();
                    tipoOggetto = Krint.TipoOggettoKrint.BABORG_PERSONA;
                    break;
                case "strutture":
                    Struttura struttura = cachedEntities.getStruttura(entita.getIdProvenienza());
                    KrintBaborgStruttura krintBaborgStruttura = factory.createProjection(KrintBaborgStruttura.class, struttura);
//                    jsonKrintEntitaPermessoArchivio = objectMapper.writeValueAsString(krintBaborgStruttura);
                    krintEntitaPermessoArchivio = objectMapper.convertValue(krintBaborgStruttura, new TypeReference<HashMap<String, Object>>(){});
                    idOggetto = krintBaborgStruttura.getId().toString();
                    descrizioneOggetto = krintBaborgStruttura.getNome();
                    tipoOggetto = Krint.TipoOggettoKrint.BABORG_STRUTTURA;
                    mapKrintArchivio.put("propagaSoggetto", permessoStoredProcedure.getPropagaSoggetto() ? "con" : "senza");
                    break;
            }
            
//            String jsonKrintArchivio = objectMapper.writeValueAsString(mapKrintArchivio);
            
            krintService.writeKrintRow(
                    idOggetto,
                    tipoOggetto,
                    descrizioneOggetto,
                    krintEntitaPermessoArchivio,
                    archivio.getId().toString(), // idOggetto
                    Krint.TipoOggettoKrint.SCRIPTA_ARCHIVIO, // tipoOggetto
                    archivio.getNumerazioneGerarchica(), // descrizioneOggetto
                    mapKrintArchivio, // informazioniOggetto
                    codiceOperazione);

        } catch (Exception ex) {
            log.error("Errore nella writePermessiArchivio con archivio" + archivio.getId().toString(), ex);
            krintService.writeKrintError(archivio.getId(), "writePermessiArchivio", codiceOperazione);
        }
    }

    public void writeArchivioDoc(ArchivioDoc archivioDoc, OperazioneKrint.CodiceOperazione codiceOperazione) {
        try {
            // Informazioni oggetto
            KrintScriptaDoc krintScriptaDoc = factory.createProjection(KrintScriptaDoc.class, archivioDoc.getIdDoc());
//            String jsonKrintDoc = objectMapper.writeValueAsString(krintScriptaDoc);
            HashMap<String, Object> krintDoc = objectMapper.convertValue(krintScriptaDoc, new TypeReference<HashMap<String, Object>>(){});
            // Informazioni oggetto contenitore
            KrintScriptaArchivio krintScriptaArchivio = factory.createProjection(KrintScriptaArchivio.class, archivioDoc.getIdArchivio());
//            String jsonKrintArchivio = objectMapper.writeValueAsString(krintScriptaArchivio);
            HashMap<String, Object> krintArchivio = objectMapper.convertValue(krintScriptaArchivio, new TypeReference<HashMap<String, Object>>(){});
            krintService.writeKrintRow(
                    archivioDoc.getIdDoc().getId().toString(), // idOggetto
                    Krint.TipoOggettoKrint.SCRIPTA_ARCHIVIO_DOC, // tipoOggetto
                    archivioDoc.getIdDoc().getOggetto(), // descrizioneOggetto
                    krintDoc, // informazioniOggetto
                    archivioDoc.getIdArchivio().getId().toString(), // Da qui si ripete ma per il conenitore
                    Krint.TipoOggettoKrint.SCRIPTA_ARCHIVIO,
                    archivioDoc.getIdArchivio().getNumerazioneGerarchica(),
                    krintArchivio,
                    codiceOperazione);
        } catch (Exception ex) {
            log.error("Errore nella writeArchivioDoc con archivioDoc" + archivioDoc.getId().toString(), ex);
            krintService.writeKrintError(archivioDoc.getId(), "writeArchivioDoc", codiceOperazione);
        }

    }

    public void writeDoc(Doc doc, OperazioneKrint.CodiceOperazione codiceOperazione) {
        try {
            // Informazioni oggetto

            krintService.writeKrintRow(
                    doc.getId().toString(),
                    Krint.TipoOggettoKrint.SCRIPTA_DOC,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    codiceOperazione);
        } catch (Exception ex) {
            log.error("Errore nella writeDoc con doc" + doc.getId().toString(), ex);
            krintService.writeKrintError(doc.getId(), "writeDoc", codiceOperazione);
        }
    }
    
    public void writeActionDoc(Doc doc, Archivio arch, OperazioneKrint.CodiceOperazione codiceOperazione) {
        try {
            // Informazioni oggetto

            krintService.writeKrintRow(
                    doc.getId().toString(),
                    Krint.TipoOggettoKrint.SCRIPTA_DOC,
                    doc.getOggetto(),
                    null,
                    arch.getId().toString(),
                    Krint.TipoOggettoKrint.SCRIPTA_ARCHIVIO,
                    arch.getOggetto(),
                    null,
                    codiceOperazione);
        } catch (Exception ex) {
            Integer idOggetto = null;
            try {
                ex.printStackTrace();
                idOggetto = doc.getId();
            } catch (Exception exa) {
            }
            krintService.writeKrintError(idOggetto, "writeActionDoc", codiceOperazione);
        }
    }
    
    /**
     * Crea una mappa serializzando la classe Archivio.
     * @param archivio Da serializzare.
     * @return La mappa con le property del fascicolo.
     * @throws JsonProcessingException Errori nel parsing.
     */
    private Map<String, Object> getArchivioPropertiesAsMap(Archivio archivio) throws JsonProcessingException {
        KrintScriptaArchivio krintScriptaArchivio = factory.createProjection(KrintScriptaArchivio.class, archivio);            
        String jsonKrintArchivio = objectMapper.writeValueAsString(krintScriptaArchivio);
        @SuppressWarnings("unchecked")
        Map<String, Object> map = objectMapper.readValue(jsonKrintArchivio, Map.class);
        return map;
    }
}
