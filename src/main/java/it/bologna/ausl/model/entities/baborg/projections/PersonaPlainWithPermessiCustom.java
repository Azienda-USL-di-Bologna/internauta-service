package it.bologna.ausl.model.entities.baborg.projections;

import it.bologna.ausl.internauta.utils.bds.types.PermessoEntitaStoredProcedure;
import it.bologna.ausl.model.entities.baborg.Persona;
import it.bologna.ausl.model.entities.baborg.projections.generated.PersonaWithPlainFields;
import java.util.List;
import org.springframework.data.rest.core.config.Projection;

/**
 *
 * @author Giuseppe Russo <g.russo@nsi.it>
 */
@Projection(name = "PersonaPlainWithPermessiCustom", types = Persona.class)
public interface PersonaPlainWithPermessiCustom extends PersonaWithPlainFields {
    public List<PermessoEntitaStoredProcedure> getPermessi();
}
