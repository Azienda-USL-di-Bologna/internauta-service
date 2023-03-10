package it.bologna.ausl.model.entities.logs.projections;

import it.bologna.ausl.model.entities.scripta.Archivio;
import it.bologna.ausl.model.entities.scripta.Massimario;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.rest.core.config.Projection;

/**
 *
 * @author gusgus
 */
@Projection(name = "KrintScriptaArchivio", types = Archivio.class)
public interface KrintScriptaArchivio {

    @Value("#{target.getId()}")
    Integer getId();

    @Value("#{target.getOggetto()}")
    String getOggetto();

    @Value("#{target.getNumerazioneGerarchica()}")
    String getNumerazioneGerarchica();

    @Value("#{target.getIdTitolo()}")
    KrintScriptaTitolo getIdTitolo();
    
    @Value("#{target.getIdMassimario()}")
    Massimario getCategoriaDocumentale();
    @Value("#{target.getAnniTenuta()}")
    Integer getConservazione();

    @Value("#{target.getTipo().toString()}")
    String getTipo();
    
    @Value("#{target.getRiservato() == true ? \"riservato\" : \"non riservato\"}")
    String getRiservato();

    @Value("#{target.getNote()}")
    String getNote();
}
