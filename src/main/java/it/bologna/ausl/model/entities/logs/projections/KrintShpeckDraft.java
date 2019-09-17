package it.bologna.ausl.model.entities.logs.projections;

import it.bologna.ausl.model.entities.shpeck.Draft;
import org.springframework.data.rest.core.config.Projection;

/**
 *
 * @author gusgus
 */
@Projection(name = "KrintShpeckDraft", types = Draft.class)
public interface KrintShpeckDraft{
    
    Integer getId();
    
}
