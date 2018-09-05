package it.bologna.ausl.baborg.model.entities.projections;

import it.bologna.ausl.model.entities.baborg.projections.generated.UtenteStrutturaWithIdAfferenzaStruttura;
import it.bologna.ausl.model.entities.baborg.projections.generated.UtenteWithIdPersona;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.rest.core.config.Projection;


@Projection(name = "UtenteStrutturaCustom", types = it.bologna.ausl.model.entities.baborg.UtenteStruttura.class)
public interface UtenteStrutturaCustom extends UtenteStrutturaWithIdAfferenzaStruttura{
    
    @Value("#{@projectionBeans.getUtenteConPersona(target.getIdUtente())}")
    public UtenteWithIdPersona getIdUtente();
    
}
