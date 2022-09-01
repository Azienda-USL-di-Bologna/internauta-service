package it.bologna.ausl.model.entities.logs.projections;

import org.springframework.data.rest.core.config.Projection;
import it.bologna.ausl.model.entities.shpeck.Tag;

/**
 *
 * @author Guido
 */
@Projection(name = "KrintShpeckTag", types = Tag.class)
public interface KrintShpeckTag{
    
    Integer getId();
    
    String getDescription();
    
}
