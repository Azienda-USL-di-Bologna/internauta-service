package it.bologna.ausl.model.entities.baborg.projections;

import it.bologna.ausl.model.entities.baborg.Struttura;
import it.bologna.ausl.model.entities.baborg.projections.generated.StrutturaWithPlainFields;
import it.bologna.ausl.model.entities.permessi.Entita;
import it.bologna.ausl.model.entities.permessi.projections.generated.EntitaWithPlainFields;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.rest.core.config.Projection;

/**
 *
 * @author Mr. Sal
 */
@Projection(name = "StrutturaPlainWithEntitaExpandedCustom", types = Struttura.class)
public interface StrutturaPlainWithEntitaExpandedCustom extends StrutturaWithPlainFields {

    @Value("#{@projectionBeans.getEntita(target)}")
    public Object getEntita();
}