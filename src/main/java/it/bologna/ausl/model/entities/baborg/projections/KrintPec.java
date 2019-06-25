package it.bologna.ausl.model.entities.baborg.projections;

import it.bologna.ausl.model.entities.baborg.Persona;
import it.bologna.ausl.model.entities.shpeck.Message;
import org.springframework.data.rest.core.config.Projection;

/**
 *
 * @author Guido
 */
@Projection(name = "KrintPec", types = Message.class)
public interface KrintPec {
    
    Integer getId();
    
    String getIndirizzo();
    
 

}
