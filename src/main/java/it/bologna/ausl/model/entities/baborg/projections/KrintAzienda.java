
package it.bologna.ausl.model.entities.baborg.projections;

import it.bologna.ausl.model.entities.baborg.Azienda;
import org.springframework.data.rest.core.config.Projection;

/**
 *
 * @author guido
 */
@Projection(name = "KrintAzienda", types = Azienda.class)
public interface KrintAzienda {
    
    Integer getId();
    
    String getCodice();    
        
}
