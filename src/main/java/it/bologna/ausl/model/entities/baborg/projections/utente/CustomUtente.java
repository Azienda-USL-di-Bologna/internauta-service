package it.bologna.ausl.model.entities.baborg.projections.utente;

import it.bologna.ausl.model.entities.baborg.Utente;
import it.bologna.ausl.model.entities.baborg.projections.generated.UtenteWithIdPersona;
import org.springframework.data.rest.core.config.Projection;

/**
 *
 * @author gdm
 */
@Projection(name = "UtenteCustom", types = Utente.class)
public interface CustomUtente extends UtenteWithIdPersona{

    @Override
    public Object getIdPersona();
    
}
