package it.bologna.ausl.model.entities.rubrica.projections;

import it.bologna.ausl.internauta.model.bds.types.PermessoEntitaStoredProcedure;
import it.bologna.ausl.model.entities.rubrica.Contatto;
import it.bologna.ausl.model.entities.rubrica.projections.generated.ContattoWithContattiDelGruppoListAndIdPersonaCreazioneAndIdUtenteCreazione;
import it.bologna.ausl.model.entities.rubrica.projections.generated.DettaglioContattoWithPlainFields;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.rest.core.config.Projection;

/**
 *
 * @author gus
 */
@Projection(name = "CustomContattoGruppoDetail", types = Contatto.class)
public interface CustomContattoGruppoDetail extends ContattoWithContattiDelGruppoListAndIdPersonaCreazioneAndIdUtenteCreazione {

    @Value("#{@projectionsInterceptorLauncher.lanciaInterceptorCollection(target, 'getContattiDelGruppoList', 'CustomGruppiContattiWithIdContattoAndIdDettaglioContatto')}")
    @Override
    public List<CustomGruppiContattiWithIdContattoAndIdDettaglioContatto> getContattiDelGruppoList();
    
    @Value("#{@rubricaProjectionsUtils.getPermessiContatto(target)}")
    public List<PermessoEntitaStoredProcedure> getPermessiContatto();
    
}
// contatto -> gruppi_contatto(contatti del gruppo) -> dettaglictonatto & contatto