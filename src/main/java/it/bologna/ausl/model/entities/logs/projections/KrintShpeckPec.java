package it.bologna.ausl.model.entities.logs.projections;

import it.bologna.ausl.model.entities.baborg.Pec;
import org.springframework.data.rest.core.config.Projection;

/**
 *
 * @author Guido
 */
@Projection(name = "KrintShpeckPec", types = Pec.class)
public interface KrintShpeckPec {
    
    Integer getId();
    
    String getIndirizzo();

}
