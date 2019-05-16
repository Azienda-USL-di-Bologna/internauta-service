
package it.bologna.ausl.model.entities.ribaltoneutils.projections;

import it.bologna.ausl.model.entities.baborg.projections.generated.UtenteWithIdPersona;
import it.bologna.ausl.model.entities.ribaltoneutils.RibaltoneDaLanciare;
import it.bologna.ausl.model.entities.ribaltoneutils.projections.generated.RibaltoneDaLanciareWithIdAziendaAndIdUtente;
import org.springframework.beans.factory.annotation.Value;

import org.springframework.data.rest.core.config.Projection;


@Projection(name = "RibaltoneDaLanciareWithIdAziendaAndIdUtenteCustom", types = RibaltoneDaLanciare.class)
public interface RibaltoneDaLanciareWithIdAziendaAndIdUtenteCustom extends RibaltoneDaLanciareWithIdAziendaAndIdUtente {
    
    @Value("#{@projectionBeans.getUtenteConPersona(target.getIdUtente())}")
    public UtenteWithIdPersona getIdUtente();
    
}
