package it.bologna.ausl.model.entities.logs.projections;

import it.bologna.ausl.model.entities.shpeck.Folder;
import org.springframework.data.rest.core.config.Projection;

/**
 *
 * @author gusgus
 */
@Projection(name = "KrintShpeckFolder", types = Folder.class)
public interface KrintShpeckFolder{
    
    Integer getId();
    
    String getDescription();
        
}
