package it.bologna.ausl.model.entities.scripta.projections;

import it.bologna.ausl.model.entities.scripta.Doc;
import it.bologna.ausl.model.entities.scripta.Related;
import it.bologna.ausl.model.entities.scripta.projections.generated.DocWithAllegatiAndCoinvoltiAndCompetentiAndIdAziendaAndIdPersonaCreazioneAndMittentiAndRegistroDocListAndRelated;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.rest.core.config.Projection;

/**
 *
 * @author gdm
 */
@Projection(name = "DocWithAll", types = Doc.class)
public interface DocWithAll extends DocWithAllegatiAndCoinvoltiAndCompetentiAndIdAziendaAndIdPersonaCreazioneAndMittentiAndRegistroDocListAndRelated {

    @Override
    @Value("#{@scriptaProjectionUtils.filterRelatedWithSpedizioneList(target.getRelated(), 'MITTENTE')}")
    public List<CustomRelatedWithSpedizioneList> getMittenti();

    @Override
    @Value("#{@scriptaProjectionUtils.filterRelated(target.getRelated(), 'A')}")
    public List<Related> getCompetenti();

    @Override
    @Value("#{@scriptaProjectionUtils.filterRelated(target.getRelated(), 'CC')}")
    public List<Related> getCoinvolti();
    
    @Value("#{@projectionsInterceptorLauncher.lanciaInterceptorCollection(target, 'getAllegati', 'AllegatoWithIdAllegatoPadre', @projectionsInterceptorLauncher.buildSort('asc', 'ordinale'))}")
    @Override
    public Object getAllegati();

    @Override    
    @Value("#{@projectionsInterceptorLauncher.lanciaInterceptorCollection(target, 'getRegistroDocList', 'RegistroDocWithIdRegistro')}")
    public Object getRegistroDocList();
}
