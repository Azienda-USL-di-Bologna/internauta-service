package it.bologna.ausl.model.entities.logs.projections;

import it.bologna.ausl.model.entities.shpeck.Message;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.rest.core.config.Projection;

/**
 *
 * @author Guido
 */
@Projection(name = "KrintShpeckMessage", types = Message.class)
public interface KrintShpeckMessage{
    
    Integer getId();
    
    String getUuidMessage();
    
    String getSubject();
    
//    // idPec lo metto come oggettoContenitore, quindi non lo metto qui
    @Value("#{@projectionBeans.getPecKrint(target)}")
    KrintShpeckPec getIdPec();
      
    
    
}
