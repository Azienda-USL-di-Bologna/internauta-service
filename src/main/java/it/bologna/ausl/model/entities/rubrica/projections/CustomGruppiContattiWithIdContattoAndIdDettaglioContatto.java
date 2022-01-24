package it.bologna.ausl.model.entities.rubrica.projections;

import it.bologna.ausl.model.entities.rubrica.GruppiContatti;
import it.bologna.ausl.model.entities.rubrica.projections.generated.GruppiContattiWithIdContattoAndIdDettaglioContatto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.rest.core.config.Projection;

/**
 *
 * @author gusgus
 */
@Projection(name = "CustomGruppiContattiWithIdContattoAndIdDettaglioContatto", types = GruppiContatti.class)
public interface CustomGruppiContattiWithIdContattoAndIdDettaglioContatto extends GruppiContattiWithIdContattoAndIdDettaglioContatto {
    
    //TODO devo prendere l'id persona e l'id struttura 
    @Value("#{@rubricaProjectionUtils.getContattoWithIdStrutturaAndIdPersonaByGruppoContatto(target)}")
    @Override
    public CustomContattoWithIdStrutturaAndIdPersona getIdContatto();
    
    @Value("#{@rubricaProjectionsUtilis.getDettaglioContattoWithUtenteStrutturaAndIdStutturaAndIdAziendaByGruppoContatto(target)}")
    @Override
    public CustomDettaglioContattoWithUtenteStrutturaAndIdStutturaAndIdAzienda getIdDettaglioContatto();

}
