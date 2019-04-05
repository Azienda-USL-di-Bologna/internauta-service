package it.bologna.ausl.model.entities.baborg.projections;

import it.bologna.ausl.internauta.utils.bds.types.PermessoEntitaStoredProcedure;
import it.bologna.ausl.model.entities.baborg.Pec;
import it.bologna.ausl.model.entities.baborg.projections.generated.PecWithPlainFields;
import java.util.List;
import org.springframework.data.rest.core.config.Projection;

/**
 *
 * @author gusgus
 */
@Projection(name = "PecPlainWithPermessiCustom", types = Pec.class)
public interface PecPlainWithPermessiCustom extends PecWithPlainFields {
    public List<PermessoEntitaStoredProcedure> getPermessi();
}
