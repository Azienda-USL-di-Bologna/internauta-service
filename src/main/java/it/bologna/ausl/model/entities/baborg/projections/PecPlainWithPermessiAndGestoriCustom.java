package it.bologna.ausl.model.entities.baborg.projections;

import it.bologna.ausl.blackbox.types.PermessoEntitaStoredProcedure;
import it.bologna.ausl.model.entities.baborg.Pec;
import it.bologna.ausl.model.entities.baborg.Persona;
import it.bologna.ausl.model.entities.baborg.projections.generated.PecWithIdAzienda;
import it.bologna.ausl.model.entities.baborg.projections.generated.PecWithPecAziendaList;
import java.util.List;
import org.springframework.data.rest.core.config.Projection;

/**
 *
 * @author gusgus
 */
@Projection(name = "PecPlainWithPermessiAndGestoriCustom", types = Pec.class)
public interface PecPlainWithPermessiAndGestoriCustom extends PecWithPecProviderAndAziendaCustom {
    public List<PermessoEntitaStoredProcedure> getPermessi();
    
    @Override
    public List<Persona> getGestori();
}
