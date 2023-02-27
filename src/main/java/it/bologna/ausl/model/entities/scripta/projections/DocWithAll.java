package it.bologna.ausl.model.entities.scripta.projections;

import it.bologna.ausl.model.entities.scripta.Doc;
import it.bologna.ausl.model.entities.scripta.projections.generated.AttoreDocWithIdPersona;
import it.bologna.ausl.model.entities.scripta.projections.generated.DocWithAllegatiAndArchiviDocListAndAttoriListAndCoinvoltiAndCompetentiAndIdAziendaAndIdPersonaCreazioneAndMittentiAndRegistroDocListAndRelated;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.rest.core.config.Projection;

/**
 *
 * @author gdm
 */
@Projection(name = "DocWithAll", types = Doc.class)
public interface DocWithAll extends DocWithAllegatiAndArchiviDocListAndAttoriListAndCoinvoltiAndCompetentiAndIdAziendaAndIdPersonaCreazioneAndMittentiAndRegistroDocListAndRelated {

    @Override
    @Value("#{@scriptaProjectionUtils.filterRelatedWithUltimaSpedizione(target.getRelated(), 'MITTENTE')}")
    public List<CustomRelatedWithUltimaSpedizione> getMittenti();
    
    @Override
    @Value("#{@scriptaProjectionUtils.filterRelatedWithUltimaSpedizione(target.getRelated(), 'A')}")
    public List<CustomRelatedWithUltimaSpedizione> getCompetenti();
    
    @Override
    @Value("#{@scriptaProjectionUtils.filterRelatedWithUltimaSpedizione(target.getRelated(), 'CC')}")
    public List<CustomRelatedWithUltimaSpedizione> getCoinvolti();
    
//    @Override
//    @Value("#{@scriptaProjectionUtils.filterRelated(target.getRelated(), 'A')}")
//    public List<Related> getCompetenti();
//
//    @Override
//    @Value("#{@scriptaProjectionUtils.filterRelated(target.getRelated(), 'CC')}")
//    public List<Related> getCoinvolti();
    
    @Value("#{@projectionsInterceptorLauncher.lanciaInterceptorCollection(target, 'getAllegati', 'AllegatoWithIdAllegatoPadre', @projectionsInterceptorLauncher.buildSort('asc', 'ordinale'))}")
    @Override
    public Object getAllegati();

    @Override
    @Value("#{@projectionsInterceptorLauncher.lanciaInterceptorCollection(target, 'getAttoriList', 'AttoreDocWithIdPersona', @projectionsInterceptorLauncher.buildSort('asc', 'ordinale'))}")
    public List<AttoreDocWithIdPersona> getAttoriList();
    
    @Value("#{@scriptaProjectionUtils.filterAttoreDocList(target.getAttoriList(), 'FIRMA')}")
    public List<AttoreDocWithIdPersona> getFirmatari();
    
    @Value("#{@scriptaProjectionUtils.filterAttoreDocList(target.getAttoriList(), 'PARERI')}")
    public List<AttoreDocWithIdPersona> getPareratori();
    
    @Value("#{@scriptaProjectionUtils.filterAttoreDocList(target.getAttoriList(), 'VISTI')}")
    public List<AttoreDocWithIdPersona> getVistatori();

    @Override    
    @Value("#{@projectionsInterceptorLauncher.lanciaInterceptorCollection(target, 'getRegistroDocList', 'RegistroDocWithIdRegistro')}")
    public Object getRegistroDocList();
    
    @Override    
    @Value("#{@projectionsInterceptorLauncher.lanciaInterceptorCollection(target, 'getArchiviDocList', 'ArchivioDocWithIdArchivioAndIdPersonaArchiviazione')}")
    public Object getArchiviDocList();
    
    
}
