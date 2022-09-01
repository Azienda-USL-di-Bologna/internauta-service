package it.bologna.ausl.model.entities.baborg.projections.persona;

import it.bologna.ausl.model.entities.baborg.projections.utente.UtenteWithStruttureAndResponsabiliCustom;
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
    
    @Value("#{@personaProjectionUtils.getUtenteWithStruttureAndResponsabiliCustom(target)}")
    @Override
    public List<UtenteWithStruttureAndResponsabiliCustom> getUtenteList();
}
