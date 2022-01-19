package it.bologna.ausl.model.entities.permessi.projections;

import it.bologna.ausl.model.entities.permessi.PredicatoAmbito;
import it.bologna.ausl.model.entities.permessi.projections.generated.PredicatoAmbitoWithIdPredicato;
import java.util.List;
import org.springframework.data.rest.core.config.Projection;
import org.springframework.beans.factory.annotation.Value;

/**
 *
 * @author Guido
 */
@Projection(name = "PredicatiAmbitiWithPredicatoAndPredicatiAmbitiImplicitiExpanded", types = PredicatoAmbito.class)
public interface PredicatiAmbitiWithPredicatoAndPredicatiAmbitiImplicitiExpanded extends PredicatoAmbitoWithIdPredicato {

    @Value("#{@projectionBeans.expandPredicatiAmbiti(target.getIdPredicatiAmbitiImpliciti())}")
    public List<PredicatiAmbitiWithPredicatoAndPredicatiAmbitiImplicitiExpanded> getPredicatiAmbitiImpliciti();
}
