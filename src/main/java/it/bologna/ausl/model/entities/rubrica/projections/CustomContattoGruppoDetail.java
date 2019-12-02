package it.bologna.ausl.model.entities.rubrica.projections;

import it.bologna.ausl.model.entities.rubrica.Contatto;
import it.bologna.ausl.model.entities.rubrica.GruppiContatti;
import it.bologna.ausl.model.entities.rubrica.projections.generated.ContattoWithContattiDelGruppoListAndIdPersonaCreazioneAndIdUtenteCreazione;
import it.bologna.ausl.model.entities.rubrica.projections.generated.GruppiContattiWithIdContatto;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.rest.core.config.Projection;

/**
 *
 * @author gdm
 */

@Projection(name = "CustomContattoGruppoDetail", types = Contatto.class)
public interface CustomContattoGruppoDetail extends ContattoWithContattiDelGruppoListAndIdPersonaCreazioneAndIdUtenteCreazione {
        
    @Value("#{@projectionBeans.getContattiDelGruppoWithIdContatto(target)}")
    @Override
    public List<GruppiContattiWithIdContatto> getContattiDelGruppoList();
}
