package it.bologna.ausl.model.entities.baborg.projections;

import it.bologna.ausl.model.entities.baborg.Pec;
import org.springframework.data.rest.core.config.Projection;

/**
 *
 * @author Guido
 */
@Projection(name = "KrintPec", types = Pec.class)
public interface KrintPec {
    
    Integer getId();
    
    String getIndirizzo();
    
 

}
