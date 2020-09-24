package it.bologna.ausl.model.entities.baborg.projections;

import it.bologna.ausl.internauta.utils.bds.types.PermessoEntitaStoredProcedure;
import it.bologna.ausl.model.entities.baborg.Struttura;
import it.bologna.ausl.model.entities.baborg.projections.generated.StrutturaWithPlainFields;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.rest.core.config.Projection;

/**
 *
 * @author spritz
 */
@Projection(name = "UfficioWithStruttureConnesseCustom", types = Struttura.class)
public interface UfficioWithStruttureConnesseCustom extends StrutturaWithPlainFields {

    @Value("#{@projectionBeans.getStruttureConnesseAUfficio(target)}")
    public List<PermessoEntitaStoredProcedure> getPermessiUfficio();

}
