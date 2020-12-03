package it.bologna.ausl.model.entities.rubrica.projections;

import it.bologna.ausl.internauta.utils.bds.types.PermessoEntitaStoredProcedure;
import it.bologna.ausl.model.entities.rubrica.GruppiContatti;
import it.bologna.ausl.model.entities.rubrica.projections.generated.ContattoWithContattiDelGruppoListAndIdPersonaCreazioneAndIdUtenteCreazione;
import it.bologna.ausl.model.entities.rubrica.projections.generated.DettaglioContattoWithEmailAndIndirizzoAndTelefono;
import it.bologna.ausl.model.entities.rubrica.projections.generated.GruppiContattiWithIdDettaglioContattoAndIdGruppo;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.rest.core.config.Projection;

/**
 *
 * @author gus
 */
@Projection(name = "CustomGruppiContattiWithDettaglioContattoExpanded", types = GruppiContatti.class)
public interface CustomGruppiContattiWithDettaglioContattoExpanded extends GruppiContattiWithIdDettaglioContattoAndIdGruppo {

    @Override
    @Value("#{@projectionsInterceptorLauncher.lanciaInterceptor(target, 'getIdDettaglioContatto', 'DettaglioContattoWithEmailAndIndirizzoAndTelefono')}")
    public DettaglioContattoWithEmailAndIndirizzoAndTelefono getIdDettaglioContatto();

    
}