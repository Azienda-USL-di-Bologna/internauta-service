package it.bologna.ausl.model.entities.permessi.projections;

import it.bologna.ausl.model.entities.permessi.AmbitoSemantico;
import it.bologna.ausl.model.entities.permessi.projections.generated.AmbitoSemanticoWithPlainFields;
import java.util.List;
import org.springframework.data.rest.core.config.Projection;
import org.springframework.beans.factory.annotation.Value;

/**
 *
 * @author Guido
 */
@Projection(name = "AmbitiSemanticiWithPredicatiAmbitiExpanded", types = AmbitoSemantico.class)
public interface AmbitiSemanticiWithPredicatiAmbitiExpanded extends AmbitoSemanticoWithPlainFields {

    @Value("#{@projectionBeans.expandPredicatiAmbiti(target.getIdPredicatiAmbiti())}")
    public List<PredicatiAmbitiWithPredicatoAndPredicatiAmbitiImplicitiExpanded> getPredicatiAmbiti();
}
