package it.bologna.ausl.model.entities.baborg.projections;

import it.bologna.ausl.model.entities.baborg.Persona;
import it.bologna.ausl.model.entities.baborg.projections.generated.PersonaWithUtenteList;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.rest.core.config.Projection;

/**
 *
 * @author gusgus
 */
@Projection(name = "PersonaWithUtentiAndStruttureAndAfferenzeCustom", types = Persona.class)
public interface PersonaWithUtentiAndStruttureAndAfferenzeCustom extends PersonaWithUtenteList {
    
    @Value("#{@projectionBeans.getUtenteWithStruttureAndResponsabiliCustom(target)}")
    @Override
    public List<UtenteWithStruttureAndResponsabiliCustom> getUtenteList();
}
