package it.bologna.ausl.model.entities.rubrica.projections;

import it.bologna.ausl.model.entities.rubrica.Contatto;
import it.bologna.ausl.model.entities.rubrica.projections.generated.ContattoWithContattiDelGruppoListAndEmailListAndIdPersonaCreazioneAndIndirizziListAndTelefonoList;
//import it.bologna.ausl.model.entities.rubrica.projections.generated.GruppiContattiWithIdContattoAndIdDettaglioContatto;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.rest.core.config.Projection;

/**
 *
 * @author gus
 */
@Projection(name = "CustomContattoAllWithGruppoExpanded", types = Contatto.class)
public interface CustomContattoAllWithGruppoExpanded extends ContattoWithContattiDelGruppoListAndEmailListAndIdPersonaCreazioneAndIndirizziListAndTelefonoList {

//    @Value("#{@projectionBeans.getGruppiContattiWithIdContattoAndIdDettaglioContatto(target)}")
    @Value("#{@projectionsInterceptorLauncher.lanciaInterceptorCollection(target, 'getContattiDelGruppoList', 'CustomGruppiContattiWithIdContattoAndIdDettaglioContatto')}")
    @Override
    public List<CustomGruppiContattiWithIdContattoAndIdDettaglioContatto> getContattiDelGruppoList();
}
// contatto -> gruppi_contatto(contatti del gruppo) -> dettaglictonatto & contatto
