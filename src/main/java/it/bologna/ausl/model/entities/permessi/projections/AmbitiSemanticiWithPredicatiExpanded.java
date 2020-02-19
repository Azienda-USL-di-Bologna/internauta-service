package it.bologna.ausl.model.entities.permessi.projections;

import it.bologna.ausl.model.entities.permessi.AmbitoSemantico;
import it.bologna.ausl.model.entities.permessi.projections.generated.AmbitoSemanticoWithPlainFields;
import it.bologna.ausl.model.entities.permessi.projections.generated.PredicatoWithPlainFields;
import java.util.List;
import org.springframework.data.rest.core.config.Projection;
import org.springframework.beans.factory.annotation.Value;

/**
 *
 * @author Guido
 */
@Projection(name = "AmbitiSemanticiWithPredicatiExpanded", types = AmbitoSemantico.class)
public interface AmbitiSemanticiWithPredicatiExpanded extends AmbitoSemanticoWithPlainFields {

    @Value("#{@projectionBeans.expandPredicati(target.getIdPredicati())}")
    public List<PredicatoWithPlainFields> getPredicati();
}
