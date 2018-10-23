
package it.bologna.ausl.model.entities.baborg.projections;

import it.bologna.ausl.model.entities.baborg.UtenteStruttura;
import it.bologna.ausl.model.entities.baborg.projections.generated.StrutturaWithIdAzienda;
import it.bologna.ausl.model.entities.baborg.projections.generated.UtenteStrutturaWithIdAfferenzaStruttura;
import it.bologna.ausl.model.entities.baborg.projections.generated.UtenteWithIdPersona;
import org.springframework.beans.factory.annotation.Value;

import org.springframework.data.rest.core.config.Projection;


@Projection(name = "UtenteStrutturaWithIdAfferenzaStrutturaCustom", types = UtenteStruttura.class)
public interface UtenteStrutturaWithIdAfferenzaStrutturaCustom extends UtenteStrutturaWithIdAfferenzaStruttura{
    
    // metto l'utente con la persona
    @Value("#{@projectionBeans.getUtenteConPersona(target.getIdUtente())}")
    public UtenteWithIdPersona getIdUtente();
    
    // metto la struttura con l'azienda
    @Value("#{@projectionBeans.getStrutturaConAzienda(target.getIdStruttura())}")
    public StrutturaWithIdAzienda getIdStruttura();
   
  
}
