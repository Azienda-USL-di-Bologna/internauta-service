package it.bologna.ausl.model.entities.logs.projections;

import it.bologna.ausl.model.entities.scripta.Archivio;
import org.springframework.data.rest.core.config.Projection;

/**
 *
 * @author gusgus
 */
@Projection(name = "KrintScriptaArchivio", types = Archivio.class)
public interface KrintScriptaArchivio{
    
    Integer getId();
    
    String getOggetto();
    
    String getNumerazioneGerarchica();

}