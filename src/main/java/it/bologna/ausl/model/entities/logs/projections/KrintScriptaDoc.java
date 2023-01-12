package it.bologna.ausl.model.entities.logs.projections;

import it.bologna.ausl.model.entities.scripta.Doc;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.rest.core.config.Projection;

/**
 *
 * @author conte
 */
@Projection(name = "KrintScriptaDoc", types = Doc.class)
public interface KrintScriptaDoc{
    
    @Value("#{target.getId()}") 
    Integer getId();
    
    @Value("#{target.getOggetto()}") 
    String getOggetto();
    
    @Value("#{target.getIdPersonaCreazione().getId()}") 
    Integer getIdPersonaCreazione();
    
    @Value("#{target.getTipologia()}")
    String getTipologia();

    @Value("#{target.getRuolo().name()}") 
    String visibilita();
}
