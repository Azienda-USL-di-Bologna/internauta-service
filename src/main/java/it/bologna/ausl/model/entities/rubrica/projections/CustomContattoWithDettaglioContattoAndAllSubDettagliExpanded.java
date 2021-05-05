package it.bologna.ausl.model.entities.rubrica.projections;

import it.bologna.ausl.model.entities.rubrica.Contatto;
import it.bologna.ausl.model.entities.rubrica.projections.generated.ContattoWithContattiDelGruppoListAndDettaglioContattoListAndIdPersonaCreazione;
import it.bologna.ausl.model.entities.rubrica.projections.generated.DettaglioContattoWithEmailAndIndirizzoAndTelefono;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.rest.core.config.Projection;

/**
 *
 * @author gus
 */
@Projection(name = "CustomContattoWithDettaglioContattoAndAllSubDettagliExpanded", types = Contatto.class)
public interface CustomContattoWithDettaglioContattoAndAllSubDettagliExpanded extends ContattoWithContattiDelGruppoListAndDettaglioContattoListAndIdPersonaCreazione {

    @Override
    @Value("#{@projectionsInterceptorLauncher.lanciaInterceptorCollection(target, 'getDettaglioContattoList', 'DettaglioContattoWithEmailAndIndirizzoAndTelefono')}")
    public List<DettaglioContattoWithEmailAndIndirizzoAndTelefono> getDettaglioContattoList();
    
    @Value("#{@projectionsInterceptorLauncher.lanciaInterceptorCollection(target, 'getContattiDelGruppoList', 'CustomGruppiContattiWithDettaglioContattoExpanded')}")
    @Override
    public List<CustomGruppiContattiWithDettaglioContattoExpanded> getContattiDelGruppoList();
}
// contatto -> gruppi_contatto(contatti del gruppo) -> dettaglictonatto & contatto
