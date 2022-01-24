
package it.bologna.ausl.model.entities.logs.projections;

import it.bologna.ausl.model.entities.baborg.Struttura;
import it.bologna.ausl.model.entities.baborg.TipologiaStruttura;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.rest.core.config.Projection;

/**
 *
 * @author guido
 */
@Projection(name = "KrintBaborgTipologiaStruttura", types = TipologiaStruttura.class)
public interface KrintBaborgTipologiaStruttura {
    
    @Value("#{target.getId()}") 
    String getId();
    
    @Value("#{target.getTipologia()}")
    String getTipologia();
    
}
