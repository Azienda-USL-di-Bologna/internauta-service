package it.bologna.ausl.model.entities.rubrica.projections;

import it.bologna.ausl.model.entities.rubrica.Contatto;
import it.bologna.ausl.model.entities.rubrica.projections.generated.ContattoWithContattiDelGruppoListAndIdPersonaCreazioneAndIdUtenteCreazione;
import it.bologna.ausl.model.entities.rubrica.projections.generated.GruppiContattiWithIdContattoAndIdDettaglioContatto;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.rest.core.config.Projection;

/**
 *
 * @author gus
 */
@Projection(name = "CustomContattoGruppoDetail", types = Contatto.class)
public interface CustomContattoGruppoDetail extends ContattoWithContattiDelGruppoListAndIdPersonaCreazioneAndIdUtenteCreazione {

    @Value("#{@projectionBeans.getGruppiContattiWithIdContattoAndIdDettaglioContatto(target)}")
    @Override
    public List<GruppiContattiWithIdContattoAndIdDettaglioContatto> getContattiDelGruppoList();

}
// contatto -> gruppi_contatto(contatti del gruppo) -> dettaglictonatto & contatto