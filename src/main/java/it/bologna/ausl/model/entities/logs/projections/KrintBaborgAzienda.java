
package it.bologna.ausl.model.entities.logs.projections;

import it.bologna.ausl.model.entities.baborg.Azienda;
import org.springframework.data.rest.core.config.Projection;

/**
 *
 * @author guido
 */
@Projection(name = "KrintBaborgAzienda", types = Azienda.class)
public interface KrintBaborgAzienda {
    
    Integer getId();
    
    String getCodice();    
        
}
