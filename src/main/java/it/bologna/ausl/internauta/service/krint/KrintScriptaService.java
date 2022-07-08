
package it.bologna.ausl.internauta.service.krint;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.bologna.ausl.model.entities.logs.Krint;
import it.bologna.ausl.model.entities.logs.OperazioneKrint;
import it.bologna.ausl.model.entities.logs.projections.KrintScriptaArchivio;
import it.bologna.ausl.model.entities.logs.projections.KrintScriptaAttoreArchivio;
import it.bologna.ausl.model.entities.scripta.AttoreArchivio;
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
    ProjectionFactory factory;
   
    @Autowired
    ObjectMapper objectMapper;
    
    @Autowired
    KrintService krintService;
    
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
            } catch (Exception exa) {}
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
            } catch (Exception exa) {}
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
            } catch (Exception exa) {}
            krintService.writeKrintError(idOggetto, "writeAttoreArchivioDelete", codiceOperazione);
        }
    }
}
