package it.bologna.ausl.model.entities.scripta.projections;

import it.bologna.ausl.model.entities.scripta.Related;
import it.bologna.ausl.model.entities.scripta.projections.generated.RelatedWithSpedizioneList;
import it.bologna.ausl.model.entities.scripta.projections.generated.SpedizioneWithIdMezzo;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.rest.core.config.Projection;

/**
 *
 * @author gdm
 */
@Projection(name = "CustomRelatedWithSpedizioneList", types = Related.class)
public interface CustomRelatedWithSpedizioneList extends RelatedWithSpedizioneList {

    @Override
    @Value("#{@projectionBeans.getSpedizioneWithIdMezzo(target.getSpedizioneList())}")
    public List<SpedizioneWithIdMezzo> getSpedizioneList();
}
