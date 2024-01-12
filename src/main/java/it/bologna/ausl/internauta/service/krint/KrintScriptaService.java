package it.bologna.ausl.internauta.service.krint;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.bologna.ausl.internauta.service.repositories.scripta.ArchivioRepository;
import it.bologna.ausl.internauta.service.utils.CachedEntities;
import it.bologna.ausl.internauta.model.bds.types.EntitaStoredProcedure;
import it.bologna.ausl.internauta.model.bds.types.PermessoStoredProcedure;
import it.bologna.ausl.internauta.utils.masterjobs.workers.jobs.gestionemassivaabilitazioniarchivi.InfoArchivio;
import it.bologna.ausl.internauta.utils.masterjobs.workers.jobs.gestionemassivaabilitazioniarchivi.InfoPersona;
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
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
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
            HashMap<String, Object> map = getArchivioPropertiesAsMap(archivio);
            
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
            HashMap<String, Object> map = getArchivioPropertiesAsMap(archivio);
            
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
            HashMap<String, Object> map = getArchivioPropertiesAsMap(archivioPadre);
            
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

    
    
    /**Questo serve per loggare quando un utente accetta la responsabilità di un archivio
     * @param attoreArchivio
     * @param responsabileOld
     * @param codiceOperazione
     */
    public void writeAttoreArchivioAccetataResp(AttoreArchivio attoreArchivio,Persona responsabileOld, OperazioneKrint.CodiceOperazione codiceOperazione) {
        try {
            // Informazioni oggetto
            KrintScriptaAttoreArchivio krintScriptaAttoreArchivio = factory.createProjection(KrintScriptaAttoreArchivio.class, attoreArchivio);
//            String jsonKrintAttore = objectMapper.writeValueAsString(krintScriptaAttoreArchivio);

            // Informazioni oggetto contenitore
            KrintScriptaArchivio krintScriptaArchivio = factory.createProjection(KrintScriptaArchivio.class, attoreArchivio.getIdArchivio());
//            String jsonKrintArchivio = objectMapper.writeValueAsString(krintScriptaArchivio);
            HashMap<String, Object> krintArchivio = objectMapper.convertValue(krintScriptaArchivio, new TypeReference<HashMap<String, Object>>(){});
            HashMap<String, Object> krintAttore = objectMapper.convertValue(krintScriptaAttoreArchivio, new TypeReference<HashMap<String, Object>>(){});
            String descrizionePersonaDestinazione =  "in sostituzione di " + responsabileOld.getDescrizione();
            HashMap<String, Object> infoOggetto = new HashMap();
                infoOggetto.put("krintAttore", krintAttore);
                infoOggetto.put("descrizionePersonaDestinazione", descrizionePersonaDestinazione);
            krintService.writeKrintRow(
                    attoreArchivio.getId().toString(), // idOggetto
                    Krint.TipoOggettoKrint.SCRIPTA_ATTORE_ARCHIVIO, // tipoOggetto
                    attoreArchivio.getIdPersona().getDescrizione(), // descrizioneOggetto
                    infoOggetto, // informazioniOggetto
                    attoreArchivio.getIdArchivio().getId().toString(), // Da qui si ripete ma per il conenitore
                    Krint.TipoOggettoKrint.SCRIPTA_ARCHIVIO,
                    attoreArchivio.getIdArchivio().getNumerazioneGerarchica(),
                    krintArchivio,
                    codiceOperazione);
        } catch (Exception ex) {
            log.error("Errore nella writeAttoreArchivioAccetataResp con archivio " + attoreArchivio.getId().toString(), ex);
            krintService.writeKrintError(attoreArchivio.getId(), "writeAttoreArchivioAccetataResp", codiceOperazione);
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
            HashMap<String, Object> mapKrintArchivio = getArchivioPropertiesAsMap(archivio);
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
    private HashMap<String, Object> getArchivioPropertiesAsMap(Archivio archivio) throws JsonProcessingException {
        KrintScriptaArchivio krintScriptaArchivio = factory.createProjection(KrintScriptaArchivio.class, archivio);            
        String jsonKrintArchivio = objectMapper.writeValueAsString(krintScriptaArchivio);
        @SuppressWarnings("unchecked")
        HashMap<String, Object> map = objectMapper.readValue(jsonKrintArchivio, HashMap.class);
        return map;
    }
    
    
    public void writeSostituzioneResponsabileDaAmministratoreGedi(
            HashMap<String, Object> sameInfo,
            Integer idMassiveActionLog,
            OperazioneKrint.CodiceOperazione operazione
    ) {
        try {
            // Informazioni oggetto contenitore
            HashMap<String, Object> krintArchivio = new HashMap();
            krintArchivio.put("id", sameInfo.get("idArchivio"));
            krintArchivio.put("numerazioneGerarchica", sameInfo.get("numerazioneGerarchica"));
            krintArchivio.put("idMassiveActionLog", idMassiveActionLog);
            
            krintService.writeKrintRow(
                    sameInfo.get("idAttoreArchivioNewResponsabile").toString(), // idOggetto
                    Krint.TipoOggettoKrint.SCRIPTA_ATTORE_ARCHIVIO, // tipoOggetto
                    sameInfo.get("descrizioneNewResponsabile").toString(), // descrizioneOggetto
                    sameInfo, // informazioniOggetto
                    sameInfo.get("idArchivio").toString(), // Da qui si ripete ma per il conenitore
                    Krint.TipoOggettoKrint.SCRIPTA_ARCHIVIO,
                    sameInfo.get("numerazioneGerarchica").toString(),
                    krintArchivio,
                    operazione
            );
        } catch (Exception ex) {
            log.error("Errore nella writeSostituzioneResponsabileDaAmministratoreGedi con archivio " + sameInfo.get("idAttoreArchivioNewResponsabile").toString(), ex);
            krintService.writeKrintError((Integer) sameInfo.get("idAttoreArchivioNewResponsabile"), "writeSostituzioneResponsabileDaAmministratoreGedi", operazione);
        }
    }
    
    /**
     * Esempio di frase del log: 
     * L'amministratore Bingo Bongo ha modificato le abilitazioni del fascicolo.Ha reso vicari gli utenti Cassandra Cassetti, Pel Dicarota.Ha rimosso i vicari Gino Formaggino.Ha dato i permessi VISUALIZZA a Tania Lania, Peppa Pig; 
 MODIFICA a Santi Numi; 
 ELIMINA a Trovato Nascosto.Ha tolto i permessi a Fernandello Mio.La frase del log verrà per lo più costruita a mano qui e non sarà quindi usato il metodo standard. 
     * Questo perché ci sono liste e voci da mostrare o meno in determinate condizioni.
 Il motore del krint oggi non supporterebbe tale costruzione della frase.
     * @param idArchivio
     * @param idMassiveActionLog
     * @param infoArchivio
     * @param mappaPersone
     * @param operazione
     */
    public void writeGestioneMassivaAbilitazioniArchiviDaAmministratoreGedi(
            Integer idArchivio,
            Integer idMassiveActionLog,
            InfoArchivio infoArchivio,
            Map<Integer, InfoPersona> mappaPersone,
            OperazioneKrint.CodiceOperazione operazione
    ) {
        try {
            // Informazioni oggetto contenitore
            HashMap<String, Object> krintArchivio = new HashMap();
            krintArchivio.put("id", idArchivio);
            krintArchivio.put("numerazioneGerarchica", infoArchivio.getNumerazioneGerarchica());
            
            // Costuisco la frase del log
            boolean almenoUnCambiamentoAvvenuto = false; // Diventa true se c'è stato almeno una vera modifica
            String descrizioneAzione = "";
            // Frase della rimozione vicari
            List<Integer> vicariEliminati = infoArchivio.getVicariEliminati();
            if (!vicariEliminati.isEmpty()) {
                almenoUnCambiamentoAvvenuto = true;
                descrizioneAzione = descrizioneAzione + "<br>Ha rimosso i vicari: ";
                for (Integer vicarioRimosso : vicariEliminati) {
                    InfoPersona vicario = mappaPersone.get(vicarioRimosso);
                    descrizioneAzione = descrizioneAzione + "<b>" + vicario.getDescrizione() + "</b>, ";
                }
                descrizioneAzione = descrizioneAzione.substring(0, descrizioneAzione.length() - 2) + ".";
            }
            // Frase dell'aggiunta vicari
            List<Integer> vicariAggiunti = infoArchivio.getVicariAggiunti();
            if (!vicariAggiunti.isEmpty()) {
                almenoUnCambiamentoAvvenuto = true;
                descrizioneAzione = descrizioneAzione + "<br>Ha reso vicari gli utenti: ";
                for (Integer vicarioAggiunto : vicariAggiunti) {
                    InfoPersona vicario = mappaPersone.get(vicarioAggiunto);
                    descrizioneAzione = descrizioneAzione + "<b>" + vicario.getDescrizione() + "</b>, ";
                }
                descrizioneAzione = descrizioneAzione.substring(0, descrizioneAzione.length() - 2) + ".";
            }
            // Frase della rimozione permessi
            List<Integer> permessiPersonaRimossi = infoArchivio.getPermessiPersonaRimossi();
            if (!permessiPersonaRimossi.isEmpty()) {
                almenoUnCambiamentoAvvenuto = true;
                descrizioneAzione = descrizioneAzione + "<br>Ha rimosso i permessi agli utenti: ";
                for (Integer idPersona : permessiPersonaRimossi) {
                    InfoPersona persona = mappaPersone.get(idPersona);
                    descrizioneAzione = descrizioneAzione + "<b>" + persona.getDescrizione() + "</b>, ";
                }
                descrizioneAzione = descrizioneAzione.substring(0, descrizioneAzione.length() - 2) + ".";
            }
            // Frase dell'aggiunta permessi
            Map<String, List<Integer>> permessiPersonaAggiunti = infoArchivio.getPermessiPersonaAggiunti();
            if (permessiPersonaAggiunti != null) {
                List<Integer> visualizza = permessiPersonaAggiunti.get("VISUALIZZA");
                List<Integer> modifica = permessiPersonaAggiunti.get("MODIFICA");
                List<Integer> elimina = permessiPersonaAggiunti.get("ELIMINA");
                if (!visualizza.isEmpty() || !modifica.isEmpty() || !elimina.isEmpty()) {
                    almenoUnCambiamentoAvvenuto = true;
                    descrizioneAzione = descrizioneAzione + "<br>Ha dato i seguenti permessi:";
                    if (!visualizza.isEmpty()) {
                        descrizioneAzione = descrizioneAzione + " <b>VISUALIZZA</b> a ";
                        for (Integer idPersona : visualizza) {
                            InfoPersona persona = mappaPersone.get(idPersona);
                            descrizioneAzione = descrizioneAzione + "<b>" + persona.getDescrizione() + "</b>, ";
                        }
                        descrizioneAzione = descrizioneAzione.substring(0, descrizioneAzione.length() - 2) + ";";
                    }
                    if (!modifica.isEmpty()) {
                        descrizioneAzione = descrizioneAzione + " <b>MODIFICA</b> a ";
                        for (Integer idPersona : modifica) {
                            InfoPersona persona = mappaPersone.get(idPersona);
                            descrizioneAzione = descrizioneAzione + "<b>" + persona.getDescrizione() + "</b>, ";
                        }
                        descrizioneAzione = descrizioneAzione.substring(0, descrizioneAzione.length() - 2) + ";";
                    }
                    if (!elimina.isEmpty()) {
                        descrizioneAzione = descrizioneAzione + " <b>ELIMINA</b> a ";
                        for (Integer idPersona : elimina) {
                            InfoPersona persona = mappaPersone.get(idPersona);
                            descrizioneAzione = descrizioneAzione + "<b>" + persona.getDescrizione() + "</b>, ";
                        }
                        descrizioneAzione = descrizioneAzione.substring(0, descrizioneAzione.length() - 2) + ";";
                    }
                    descrizioneAzione = descrizioneAzione.substring(0, descrizioneAzione.length() - 1) + ".";
                }
            }
            
            if (almenoUnCambiamentoAvvenuto) {
                HashMap<String, Object> infoOggetto = new HashMap();
                infoOggetto.put("descrizioneAzione", descrizioneAzione);
                infoOggetto.put("infoArchivio", infoArchivio);
                infoOggetto.put("idMassiveActionLog", idMassiveActionLog);

                krintService.writeKrintRow(
                        idArchivio.toString(), // idOggetto
                        Krint.TipoOggettoKrint.SCRIPTA_ARCHIVIO, // tipoOggetto
                        infoArchivio.getNumerazioneGerarchica(), // descrizioneOggetto
                        infoOggetto, // informazioniOggetto
                        idArchivio.toString(), // Da qui si ripete ma per il conenitore
                        Krint.TipoOggettoKrint.SCRIPTA_ARCHIVIO,
                        infoArchivio.getNumerazioneGerarchica(),
                        krintArchivio,
                        operazione
                );
            }
        } catch (Exception ex) {
            log.error("Errore nella writeGestioneMassivaAbilitazioniArchiviDaAmministratoreGedi con archivio " + idArchivio.toString(), ex);
            krintService.writeKrintError(idArchivio, "writeGestioneMassivaAbilitazioniArchiviDaAmministratoreGedi", operazione);
        }
    }
    
    
    /**
     * Andiamo a loggare l'evento massivo di copia o di trasferimento delle abiltiazioni
     * negli archivi. Le frasi tipiche saranno ad esempio:
     * - L'amministratore Bingo Bongo ha copiato le abilitazioni sul fascicolo di Trovato Nascosto dandole a Fernandello Mio. Le abilitazioni ottenute da Fernandello Mio sono: Vicario, Elimina.
     * - L'amministratore Bingo Bongo ha trasferito le abilitazioni sul fascicolo di Trovato Nascosto dandole a Fernandello Mio. Le abilitazioni ottenute da Fernandello Mio sono: Responsabile, Vicario, Elimina.
     * In caso di trasferimento dove il destinario aveva già le abilitazioni:
     * - L'amministratore Bingo Bongo ha trasferito le abilitazioni sul fascicolo di Trovato Nascosto dandole a Fernandello Mio. Fernandello Mio non ha ottenuto ulteriori abilitazioni oltre a quelle già possedute.
     * @param idArchivio
     * @param personaSorgente
     * @param personaDestinazione
     * @param abilitazioniAggiunte
     * @param numerazioneGerarchica
     * @param idMassiveActionLog
     * @param idArchivioRadice
     * @param operazione 
     */
    public void writeCopiaTrasferimentoAbilitazioniArchivi(
            Integer idArchivio,
            Persona personaSorgente,
            Persona personaDestinazione,
            List<String> abilitazioniAggiunte,
            String numerazioneGerarchica,
            Integer idArchivioRadice,
            Integer idMassiveActionLog,
            OperazioneKrint.CodiceOperazione operazione
    ) {
        try {
            String descrizioneAzione = null;
            if (operazione.equals(OperazioneKrint.CodiceOperazione.SCRIPTA_ARCHIVIO_TRASFERIMENTO_MASSIVO_ABILITAZIONI)) {
                if (abilitazioniAggiunte.isEmpty()) {
                    descrizioneAzione = String.format("<b>%1$s</b> non ha ottenuto ulteriori abilitazioni oltre a quelle già possedute.", personaDestinazione.getDescrizione());
                } else {
                    descrizioneAzione = String.format("L'abilitazione ottenuta da <b>%1$s</b> è: <b>" + String.join("</b>, <b>", abilitazioniAggiunte) + "</b>.", personaDestinazione.getDescrizione());
                }
            } else {
                descrizioneAzione = String.format("L'abilitazione ottenuta da <b>%1$s</b> è: <b>" + String.join("</b>, <b>", abilitazioniAggiunte) + "</b>.", personaDestinazione.getDescrizione());
            }
            
            // Informazioni oggetto contenitore
            HashMap<String, Object> krintArchivio = new HashMap();
            krintArchivio.put("idArchivio", idArchivio);
            krintArchivio.put("numerazioneGerarchica", numerazioneGerarchica);
            krintArchivio.put("idMassiveActionLog", idMassiveActionLog);
            krintArchivio.put("idPersonaSorgente", personaSorgente.getId());
            krintArchivio.put("descrizionePersonaSorgente", personaSorgente.getDescrizione());
            krintArchivio.put("idPersonaDestinazione", personaDestinazione.getId());
            krintArchivio.put("descrizionePersonaDestinazione", personaDestinazione.getDescrizione());
            krintArchivio.put("abilitazioniAggiunte", abilitazioniAggiunte);
            krintArchivio.put("descrizioneAzione", descrizioneAzione);
            
            Pattern pattern = Pattern.compile("(\\d+)-?(\\d+)?-?(\\d+)?(\\/)(\\d+)");
            Matcher matcher = pattern.matcher(numerazioneGerarchica);
            String numerazioneGerarchicaRadice = matcher.replaceAll("$1/$5");
            
            krintService.writeKrintRow(
                    idArchivio.toString(), // idOggetto
                    Krint.TipoOggettoKrint.SCRIPTA_ARCHIVIO, // tipoOggetto
                    numerazioneGerarchica, // descrizioneOggetto
                    krintArchivio, // informazioniOggetto
                    idArchivioRadice.toString(), // Da qui si ripete ma per il conenitore
                    Krint.TipoOggettoKrint.SCRIPTA_ARCHIVIO,
                    numerazioneGerarchicaRadice,
                    krintArchivio,
                    operazione
            );
        } catch (Exception ex) {
            log.error("Errore nella writeCopiaTrasferimentoAbilitazioniArchivi con archivio " + idArchivio.toString(), ex);
            krintService.writeKrintError(idArchivio, "writeCopiaTrasferimentoAbilitazioniArchivi", operazione);
        }
    }
}
