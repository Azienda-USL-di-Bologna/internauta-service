package it.bologna.ausl.model.entities.baborg.projections;

import it.bologna.ausl.internauta.utils.bds.types.PermessoEntitaStoredProcedure;
import it.bologna.ausl.model.entities.baborg.Struttura;
import it.bologna.ausl.model.entities.baborg.projections.generated.StrutturaWithPlainFields;
import java.util.List;
import org.springframework.data.rest.core.config.Projection;

/**
 *
 * @author gusgus
 */
@Projection(name = "StrutturaPlainWithPermessiCustom", types = Struttura.class)
public interface StruttturaPlainWithPermessiCustom extends StrutturaWithPlainFields {
    public List<PermessoEntitaStoredProcedure> getPermessi();
}
