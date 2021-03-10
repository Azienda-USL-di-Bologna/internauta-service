package it.bologna.ausl.model.entities.scripta.projections;

import it.bologna.ausl.model.entities.scripta.Doc;
import it.bologna.ausl.model.entities.scripta.Related;
import it.bologna.ausl.model.entities.scripta.projections.generated.DocWithCoinvoltiAndCompetentiAndIdAziendaAndIdPersonaCreazioneAndMittentiAndRelated;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.rest.core.config.Projection;

/**
 *
 * @author gdm
 */
@Projection(name = "DocWithDestinatariAndIdAziendaAndIdPersonaCreazioneAndMittentiCustom", types = Doc.class)
public interface DocWithDestinatariAndIdAziendaAndIdPersonaCreazioneAndMittentiCustom extends DocWithCoinvoltiAndCompetentiAndIdAziendaAndIdPersonaCreazioneAndMittentiAndRelated {

    @Override
//    @Value("#{target.getRelated().stream().filter(r -> r.getTipo().toString() == Related.TipoRelated.MITTENTE)}")
    @Value("#{@projectionBeans.filterRelated(target.getRelated(), 'MITTENTE')}")
    public List<Related> getMittenti();

    @Override
    @Value("#{@projectionBeans.filterRelated(target.getRelated(), 'A')}")
    public List<Related> getCompetenti();
    
    @Override
    @Value("#{@projectionBeans.filterRelated(target.getRelated(), 'CC')}")
    public List<Related> getCoinvolti();
    
}
