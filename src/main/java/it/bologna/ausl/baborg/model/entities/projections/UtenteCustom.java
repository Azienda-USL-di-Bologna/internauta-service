package it.bologna.ausl.baborg.model.entities.projections;

import it.bologna.ausl.model.entities.baborg.projections.generated.UtenteWithIdPersona;
import org.springframework.data.rest.core.config.Projection;

/**
 *
 * @author gdm
 */
@Projection(name = "UtenteCustom", types = it.bologna.ausl.model.entities.baborg.Utente.class)
public interface UtenteCustom extends UtenteWithIdPersona{

    @Override
    public Object getIdPersona();
    
}
