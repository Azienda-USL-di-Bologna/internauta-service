package it.bologna.ausl.model.entities.scripta.projections;

import it.bologna.ausl.model.entities.scripta.Related;
import it.bologna.ausl.model.entities.scripta.projections.generated.RelatedWithPlainFields;
import it.bologna.ausl.model.entities.scripta.projections.generated.RelatedWithSpedizioneList;
import it.bologna.ausl.model.entities.scripta.projections.generated.SpedizioneWithIdMezzo;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.rest.core.config.Projection;

/**
 *
 * @author gdm
 */
@Projection(name = "CustomRelatedWithUltimaSpedizione", types = Related.class)
public interface CustomRelatedWithUltimaSpedizione extends RelatedWithPlainFields {

    @Value("#{@scriptaProjectionUtils.getUltimaSpedizione(target)}")
    public SpedizioneWithIdMezzo getUltimaSpedizione();
}
