package it.bologna.ausl.model.entities.scripta.projections;

import it.bologna.ausl.model.entities.scripta.Doc;
import it.bologna.ausl.model.entities.scripta.NotaDoc;
import it.bologna.ausl.model.entities.scripta.projections.generated.AttoreDocWithIdPersona;
import it.bologna.ausl.model.entities.scripta.projections.generated.DocWithAllegatiAndArchiviDocListAndAttoriListAndCoinvoltiAndCompetentiAndIdAziendaAndIdPersonaCreazioneAndMittentiAndNotaDocListAndRegistroDocListAndRelated;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.rest.core.config.Projection;

/**
 *
 * @author gdm
 */
@Projection(name = "DocWithAll", types = Doc.class)
public interface DocWithAll extends DocWithAllegatiAndArchiviDocListAndAttoriListAndCoinvoltiAndCompetentiAndIdAziendaAndIdPersonaCreazioneAndMittentiAndNotaDocListAndRegistroDocListAndRelated {

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
    
    @Value("#{@scriptaProjectionUtils.filterNotaDocList(target.getNotaDocList(), 'ANNULLAMENTO')}")
    public List<NotaDoc> getNotaAnnullamento();
    
    @Value("#{@scriptaProjectionUtils.filterNotaDocList(target.getNotaDocList(), 'VERSAMENTO')}")
    public List<NotaDoc> getNotaVersamento();
    
    @Value("#{@scriptaProjectionUtils.filterNotaDocList(target.getNotaDocList(), 'DOCUMENTO')}")
    public List<NotaDoc> getNotaDocumento();
    
    @Value("#{@scriptaProjectionUtils.filterNotaDocList(target.getNotaDocList(), 'FLUSSO')}")
    public List<NotaDoc> getNotaFlusso();
    
    @Override    
    @Value("#{@projectionsInterceptorLauncher.lanciaInterceptorCollection(target, 'getRegistroDocList', 'RegistroDocWithIdRegistro')}")
    public Object getRegistroDocList();
        
    @Override    
    @Value("#{@projectionsInterceptorLauncher.lanciaInterceptorCollection(target, 'getArchiviDocList', 'CustomArchivioDocWithIdTitolo')}")
    public Object getArchiviDocList();
    
    
}
