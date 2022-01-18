package it.bologna.ausl.model.entities.baborg.projections.pec;

import it.bologna.ausl.internauta.utils.bds.types.PermessoEntitaStoredProcedure;
import it.bologna.ausl.model.entities.baborg.Pec;
import it.bologna.ausl.model.entities.baborg.projections.generated.PersonaWithPlainFields;
import java.util.List;
import org.springframework.data.rest.core.config.Projection;

/**
 *
 * @author gusgus
 */
@Projection(name = "PecPlainWithPermessiAndGestoriCustom", types = Pec.class)
public interface PecPlainWithPermessiAndGestoriCustom extends PecWithPecProviderAndAziendaCustom {

    @Override
    public List<PermessoEntitaStoredProcedure> getPermessi();

    public List<PersonaWithPlainFields> getGestori();
}
