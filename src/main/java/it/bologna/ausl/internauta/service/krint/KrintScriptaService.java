package it.bologna.ausl.internauta.service.krint;

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
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.projection.ProjectionFactory;
import org.springframework.stereotype.Service;

/**
 *
 * @author gusgus
 */
@Service
public class KrintScriptaService {

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

    public void writeArchivioCreation(Archivio archivio, OperazioneKrint.CodiceOperazione codiceOperazione) {
        try {
            // Informazioni oggetto
            KrintScriptaArchivio krintScriptaArchivio = factory.createProjection(KrintScriptaArchivio.class, archivio);
            String jsonKrintArchivio = objectMapper.writeValueAsString(krintScriptaArchivio);
             
            krintService.writeKrintRow(
                    archivio.getId().toString(), // idOggetto
                    Krint.TipoOggettoKrint.SCRIPTA_ARCHIVIO, // tipoOggetto
                    archivio.getNumerazioneGerarchica(), // descrizioneOggetto
                    jsonKrintArchivio, // informazioniOggetto
                    null, // Da qui si ripete ma per il conenitore
                    null,
                    null,
                    null,
                    codiceOperazione);
            
            if (archivio.getIdArchivioPadre() != null) {
                krintService.writeKrintRow(
                    archivio.getIdArchivioPadre().getId().toString(), // idOggetto
                    Krint.TipoOggettoKrint.SCRIPTA_ARCHIVIO, // tipoOggetto
                    archivio.getNumerazioneGerarchica(), // descrizioneOggetto
                    jsonKrintArchivio, // informazioniOggetto
                    null, // Da qui si ripete ma per il conenitore
                    null,
                    null,
                    null,
                    codiceOperazione);
            }
        } catch (Exception ex) {
            Integer idOggetto = null;
            try {
                ex.printStackTrace();
                idOggetto = archivio.getId();
            } catch (Exception exa) {
            }
            krintService.writeKrintError(idOggetto, "writeArchivioCreation", codiceOperazione);
        }
    }
    
    private String translateLeveltoArchiveType(Integer livello) {
        String archivioType = "";
        switch (livello) {
            case 1:
                archivioType = "fascicolo";
                break;
            case 2:
            case 3:
                archivioType = "subfascicolo";
                break;
        }
        return archivioType;
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
    public void writeArchivioUpdate(Archivio archivio, Archivio archivioDiRiferimento, OperazioneKrint.CodiceOperazione codiceOperazione ) {
        
        try {
            // Informazioni oggetto
            KrintScriptaArchivio krintScriptaArchivio = factory.createProjection(KrintScriptaArchivio.class, archivio);            
            String jsonKrintArchivio = objectMapper.writeValueAsString(krintScriptaArchivio);
            @SuppressWarnings("unchecked")
            Map<String, Object> map = objectMapper.readValue(jsonKrintArchivio, Map.class);
            
            if (archivioDiRiferimento != null) {
                KrintScriptaArchivio krintScriptaArchivioRif = factory.createProjection(KrintScriptaArchivio.class, archivioDiRiferimento);
                map.put("archivioDiRiferimento", krintScriptaArchivioRif);  
            }
            jsonKrintArchivio = objectMapper.writeValueAsString(map);
            
            krintService.writeKrintRow(
                    archivio.getId().toString(), // idOggetto
                    Krint.TipoOggettoKrint.SCRIPTA_ARCHIVIO, // tipoOggetto
                    archivio.getNumerazioneGerarchica(), // descrizioneOggetto
                    jsonKrintArchivio, // informazioniOggetto
                    null, // Da qui si ripete ma per il conenitore
                    null,
                    null,
                    null,
                    codiceOperazione);
        } catch (Exception ex) {
            Integer idOggetto = null;
            try {
                ex.printStackTrace();
                idOggetto = archivio.getId();
            } catch (Exception exa) {
            }
            krintService.writeKrintError(idOggetto, "writeArchivioUpdate", codiceOperazione);
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
            String jsonKrintAttore = objectMapper.writeValueAsString(krintScriptaAttoreArchivio);

            // Informazioni oggetto contenitore
            KrintScriptaArchivio krintScriptaArchivio = factory.createProjection(KrintScriptaArchivio.class, attoreArchivio.getIdArchivio());
            String jsonKrintArchivio = objectMapper.writeValueAsString(krintScriptaArchivio);

            krintService.writeKrintRow(
                    attoreArchivio.getId().toString(), // idOggetto
                    Krint.TipoOggettoKrint.SCRIPTA_ATTORE_ARCHIVIO, // tipoOggetto
                    attoreArchivio.getIdPersona().getDescrizione(), // descrizioneOggetto
                    jsonKrintAttore, // informazioniOggetto
                    attoreArchivio.getIdArchivio().getId().toString(), // Da qui si ripete ma per il conenitore
                    Krint.TipoOggettoKrint.SCRIPTA_ARCHIVIO,
                    attoreArchivio.getIdArchivio().getNumerazioneGerarchica(),
                    jsonKrintArchivio,
                    codiceOperazione);
        } catch (Exception ex) {
            Integer idOggetto = null;
            try {
                ex.printStackTrace();
                idOggetto = attoreArchivio.getId();
            } catch (Exception exa) {
            }
            krintService.writeKrintError(idOggetto, "writeAttoreArchivioCreation", codiceOperazione);
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
            String jsonKrintAttore = objectMapper.writeValueAsString(krintScriptaAttoreArchivio);

            // Informazioni oggetto contenitore
            KrintScriptaArchivio krintScriptaArchivio = factory.createProjection(KrintScriptaArchivio.class, attoreArchivio.getIdArchivio());
            String jsonKrintArchivio = objectMapper.writeValueAsString(krintScriptaArchivio);

            krintService.writeKrintRow(
                    attoreArchivio.getId().toString(), // idOggetto
                    Krint.TipoOggettoKrint.SCRIPTA_ATTORE_ARCHIVIO, // tipoOggetto
                    attoreArchivio.getIdPersona().getDescrizione(), // descrizioneOggetto
                    jsonKrintAttore, // informazioniOggetto
                    attoreArchivio.getIdArchivio().getId().toString(), // Da qui si ripete ma per il conenitore
                    Krint.TipoOggettoKrint.SCRIPTA_ARCHIVIO,
                    attoreArchivio.getIdArchivio().getNumerazioneGerarchica(),
                    jsonKrintArchivio,
                    codiceOperazione);
        } catch (Exception ex) {
            Integer idOggetto = null;
            try {
                ex.printStackTrace();
                idOggetto = attoreArchivio.getId();
            } catch (Exception exa) {
            }
            krintService.writeKrintError(idOggetto, "writeAttoreArchivioUpdate", codiceOperazione);
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
            String jsonKrintAttore = objectMapper.writeValueAsString(krintScriptaAttoreArchivio);

            // Informazioni oggetto contenitore
            KrintScriptaArchivio krintScriptaArchivio = factory.createProjection(KrintScriptaArchivio.class, attoreArchivio.getIdArchivio());
            String jsonKrintArchivio = objectMapper.writeValueAsString(krintScriptaArchivio);

            krintService.writeKrintRow(
                    attoreArchivio.getId().toString(), // idOggetto
                    Krint.TipoOggettoKrint.SCRIPTA_ATTORE_ARCHIVIO, // tipoOggetto
                    attoreArchivio.getIdPersona().getDescrizione(), // descrizioneOggetto
                    jsonKrintAttore, // informazioniOggetto
                    attoreArchivio.getIdArchivio().getId().toString(), // Da qui si ripete ma per il conenitore
                    Krint.TipoOggettoKrint.SCRIPTA_ARCHIVIO,
                    attoreArchivio.getIdArchivio().getNumerazioneGerarchica(),
                    jsonKrintArchivio,
                    codiceOperazione);
        } catch (Exception ex) {
            Integer idOggetto = null;
            try {
                ex.printStackTrace();
                idOggetto = attoreArchivio.getId();
            } catch (Exception exa) {
            }
            krintService.writeKrintError(idOggetto, "writeAttoreArchivioDelete", codiceOperazione);
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
            KrintScriptaArchivio krintScriptaArchivio = factory.createProjection(KrintScriptaArchivio.class, archivio);
            String jsonKrintArchivio = objectMapper.writeValueAsString(krintScriptaArchivio);
            
            @SuppressWarnings("unchecked")
            Map<String, Object> mapKrintArchivio = objectMapper.readValue(jsonKrintArchivio, Map.class);
            mapKrintArchivio.put("predicato", permessoStoredProcedure.getPredicato());
            mapKrintArchivio.put("propagaOggetto", permessoStoredProcedure.getPropagaOggetto() ? "con" : "senza");
            
            String idOggetto = null;
            String descrizioneOggetto = null;
            Krint.TipoOggettoKrint tipoOggetto = null;
            String jsonKrintEntitaPermessoArchivio = null;
            switch (entita.getTable()) {
                case "persone":
                    Persona persona = cachedEntities.getPersona(entita.getIdProvenienza());
                    KrintBaborgPersona krintBaborgPersona = factory.createProjection(KrintBaborgPersona.class, persona);
                    jsonKrintEntitaPermessoArchivio = objectMapper.writeValueAsString(krintBaborgPersona);
                    idOggetto = krintBaborgPersona.getId().toString();
                    descrizioneOggetto = krintBaborgPersona.getDescrizione();
                    tipoOggetto = Krint.TipoOggettoKrint.BABORG_PERSONA;
                    break;
                case "strutture":
                    Struttura struttura = cachedEntities.getStruttura(entita.getIdProvenienza());
                    KrintBaborgStruttura krintBaborgStruttura = factory.createProjection(KrintBaborgStruttura.class, struttura);
                    jsonKrintEntitaPermessoArchivio = objectMapper.writeValueAsString(krintBaborgStruttura);
                    idOggetto = krintBaborgStruttura.getId().toString();
                    descrizioneOggetto = krintBaborgStruttura.getNome();
                    tipoOggetto = Krint.TipoOggettoKrint.BABORG_STRUTTURA;
                    mapKrintArchivio.put("propagaSoggetto", permessoStoredProcedure.getPropagaSoggetto() ? "con" : "senza");
                    break;
            }
            
            jsonKrintArchivio = objectMapper.writeValueAsString(mapKrintArchivio);
            
            krintService.writeKrintRow(
                    idOggetto,
                    tipoOggetto,
                    descrizioneOggetto,
                    jsonKrintEntitaPermessoArchivio,
                    archivio.getId().toString(), // idOggetto
                    Krint.TipoOggettoKrint.SCRIPTA_ARCHIVIO, // tipoOggetto
                    archivio.getNumerazioneGerarchica(), // descrizioneOggetto
                    jsonKrintArchivio, // informazioniOggetto
                    codiceOperazione);

        } catch (Exception ex) {
            Integer idOggetto = null;
            try {
                ex.printStackTrace();
                idOggetto = archivio.getId();
            } catch (Exception exa) {
            }
            krintService.writeKrintError(idOggetto, "writePermessiArchivio", codiceOperazione);
        }
    }

    public void writeArchivioDoc(ArchivioDoc archivioDoc, OperazioneKrint.CodiceOperazione codiceOperazione) {
        try {
            // Informazioni oggetto
            KrintScriptaDoc krintScriptaDoc = factory.createProjection(KrintScriptaDoc.class, archivioDoc.getIdDoc());
            String jsonKrintDoc = objectMapper.writeValueAsString(krintScriptaDoc);

            // Informazioni oggetto contenitore
            KrintScriptaArchivio krintScriptaArchivio = factory.createProjection(KrintScriptaArchivio.class, archivioDoc.getIdArchivio());
            String jsonKrintArchivio = objectMapper.writeValueAsString(krintScriptaArchivio);

            krintService.writeKrintRow(
                    archivioDoc.getIdDoc().getId().toString(), // idOggetto
                    Krint.TipoOggettoKrint.SCRIPTA_ARCHIVIO_DOC, // tipoOggetto
                    archivioDoc.getIdDoc().getOggetto(), // descrizioneOggetto
                    jsonKrintDoc, // informazioniOggetto
                    archivioDoc.getIdArchivio().getId().toString(), // Da qui si ripete ma per il conenitore
                    Krint.TipoOggettoKrint.SCRIPTA_ARCHIVIO,
                    archivioDoc.getIdArchivio().getNumerazioneGerarchica(),
                    jsonKrintArchivio,
                    codiceOperazione);
        } catch (Exception ex) {
            Integer idOggetto = null;
            try {
                ex.printStackTrace();
                idOggetto = archivioDoc.getIdDoc().getId();
            } catch (Exception exa) {
            }
            krintService.writeKrintError(idOggetto, "writeArchivioDoc", codiceOperazione);
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
            Integer idOggetto = null;
            try {
                ex.printStackTrace();
                idOggetto = doc.getId();
            } catch (Exception exa) {
            }
            krintService.writeKrintError(idOggetto, "writeDoc", codiceOperazione);
        }
    }
}
