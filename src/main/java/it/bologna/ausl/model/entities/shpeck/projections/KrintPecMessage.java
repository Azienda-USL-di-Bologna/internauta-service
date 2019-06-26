package it.bologna.ausl.model.entities.shpeck.projections;

import it.bologna.ausl.model.entities.baborg.Persona;
import it.bologna.ausl.model.entities.baborg.projections.KrintPec;
import it.bologna.ausl.model.entities.baborg.projections.KrintPersona;
import it.bologna.ausl.model.entities.shpeck.Message;
import it.bologna.ausl.model.entities.shpeck.projections.generated.MessageWithIdPec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.rest.core.config.Projection;

/**
 *
 * @author Guido
 */
@Projection(name = "KrintPecMessage", types = Message.class)
public interface KrintPecMessage{
    
    Integer getId();
    
    String getUuidMessage();
    
    String getSubject();
    
    
    @Value("#{@projectionBeans.getPecKrint(target)}")
    KrintPec getIdPec();
      
    
    
}
