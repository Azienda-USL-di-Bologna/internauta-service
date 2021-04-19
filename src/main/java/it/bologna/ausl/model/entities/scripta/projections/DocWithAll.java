package it.bologna.ausl.model.entities.scripta.projections;

import it.bologna.ausl.model.entities.scripta.Doc;
import it.bologna.ausl.model.entities.scripta.Related;
import it.bologna.ausl.model.entities.scripta.projections.generated.DocWithAllegatiAndCoinvoltiAndCompetentiAndIdAziendaAndIdPersonaCreazioneAndMittentiAndRelated;
import it.bologna.ausl.model.entities.scripta.projections.generated.RelatedWithSpedizioneList;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.rest.core.config.Projection;

/**
 *
 * @author gdm
 */
@Projection(name = "DocWithAll", types = Doc.class)
public interface DocWithAll extends DocWithAllegatiAndCoinvoltiAndCompetentiAndIdAziendaAndIdPersonaCreazioneAndMittentiAndRelated {

    @Override
//    @Value("#{target.getRelated().stream().filter(r -> r.getTipo().toString() == Related.TipoRelated.MITTENTE)}")
    @Value("#{@projectionBeans.filterRelatedWithSpedizioneList(target.getRelated(), 'MITTENTE')}")
    public List<CustomRelatedWithSpedizioneList> getMittenti();

    @Override
    @Value("#{@projectionBeans.filterRelated(target.getRelated(), 'A')}")
    public List<Related> getCompetenti();

    @Override
    @Value("#{@projectionBeans.filterRelated(target.getRelated(), 'CC')}")
    public List<Related> getCoinvolti();
    
    @Value("#{@projectionsInterceptorLauncher.lanciaInterceptorCollection(target, 'getAllegati', @projectionsInterceptorLauncher.buildSort('asc', 'numeroAllegato'))}")
    @Override
    public Object getAllegati();

}
