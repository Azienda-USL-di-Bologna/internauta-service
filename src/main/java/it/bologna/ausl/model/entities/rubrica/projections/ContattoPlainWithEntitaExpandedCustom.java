package it.bologna.ausl.model.entities.rubrica.projections;

import it.bologna.ausl.model.entities.rubrica.Contatto;
import it.bologna.ausl.model.entities.rubrica.projections.generated.ContattoWithPlainFields;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.rest.core.config.Projection;

/**
 *
 * @author Mr. Sal
 */
@Projection(name = "ContattoPlainWithEntitaExpandedCustom", types = Contatto.class)
public interface ContattoPlainWithEntitaExpandedCustom extends ContattoWithPlainFields {

    @Value("#{@permessiProjectionsUtils.getEntita(target)}")
    public Object getEntita();
}
