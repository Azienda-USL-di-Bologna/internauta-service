
package it.bologna.ausl.model.entities.logs.projections;

import it.bologna.ausl.model.entities.baborg.Struttura;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.rest.core.config.Projection;

/**
 *
 * @author guido
 */
@Projection(name = "KrintBaborgStruttura", types = Struttura.class)
public interface KrintBaborgStruttura {
    
    @Value("#{target.getId()}") 
    String getId();
    
    @Value("#{target.getNome()}")
    String getNome();
    
    @Value("#{target.getIdCasella()}")
    String getIdCasella();
    
}
