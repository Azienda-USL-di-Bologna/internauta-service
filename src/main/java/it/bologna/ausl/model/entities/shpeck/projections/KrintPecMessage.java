package it.bologna.ausl.model.entities.shpeck.projections;

import it.bologna.ausl.model.entities.baborg.Persona;
import it.bologna.ausl.model.entities.shpeck.Message;
import org.springframework.data.rest.core.config.Projection;

/**
 *
 * @author Guido
 */
@Projection(name = "KrintPecMessage", types = Message.class)
public interface KrintPecMessage {
    
    Integer getId();
    Integer getUuidMessge();
    
    Integer getSubject();
    
    // pec.. che metto nella pec?
    
}
