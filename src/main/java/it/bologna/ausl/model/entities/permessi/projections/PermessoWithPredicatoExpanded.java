package it.bologna.ausl.model.entities.permessi.projections;

import it.bologna.ausl.internauta.service.permessi.Permesso;
import it.bologna.ausl.model.entities.permessi.Predicato;
import it.bologna.ausl.model.entities.permessi.projections.generated.PermessoWithPlainFields;
import org.springframework.data.rest.core.config.Projection;
import org.springframework.beans.factory.annotation.Value;

/**
 *
 * @author Guido
 */
@Projection(name = "PermessoWithPredicatoExpanded", types = Permesso.class)
public interface PermessoWithPredicatoExpanded extends PermessoWithPlainFields {

    @Value("#{@projectionBeans.getPredicato(target)}")
    public Predicato getPredicato();
}
