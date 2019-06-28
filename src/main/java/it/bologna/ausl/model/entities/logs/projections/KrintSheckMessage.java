package it.bologna.ausl.model.entities.logs.projections;

import it.bologna.ausl.model.entities.baborg.Persona;
import it.bologna.ausl.model.entities.shpeck.Message;
import it.bologna.ausl.model.entities.shpeck.projections.generated.MessageWithIdPec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.rest.core.config.Projection;
import it.bologna.ausl.model.entities.logs.projections.KrintShpeckPec;
import it.bologna.ausl.model.entities.logs.projections.KrintBaborgPersona;

/**
 *
 * @author Guido
 */
@Projection(name = "KrintSheckMessage", types = Message.class)
public interface KrintSheckMessage{
    
    Integer getId();
    
    String getUuidMessage();
    
    String getSubject();
    
    
    @Value("#{@projectionBeans.getPecKrint(target)}")
    KrintShpeckPec getIdPec();
      
    
    
}
