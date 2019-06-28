
package it.bologna.ausl.model.entities.logs.projections;

import it.bologna.ausl.model.entities.baborg.Utente;
import it.bologna.ausl.model.entities.baborg.UtenteStruttura;
import it.bologna.ausl.model.entities.baborg.projections.generated.StrutturaWithIdAzienda;
import it.bologna.ausl.model.entities.baborg.projections.generated.UtenteStrutturaWithIdAfferenzaStruttura;
import it.bologna.ausl.model.entities.baborg.projections.generated.UtenteWithIdPersona;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;

import org.springframework.data.rest.core.config.Projection;
import it.bologna.ausl.model.entities.logs.projections.KrintBaborgAzienda;


@Projection(name = "KrintInformazioniUtente", types = Utente.class)
public interface KrintInformazioniUtente {
      
    // azienda
    @Value("#{@userInfoService.getAziendaKrint(target)}") 
    KrintBaborgAzienda getAzienda();
            
    // strutture
    @Value("#{@userInfoService.getStruttureKrint(target)}")
    List<KrintBaborgStruttura> getStrutture();
    
    // persona
    @Value("#{@userInfoService.getPersonaKrint(target)}")
    KrintBaborgPersona getIdPersona();                 
    
  
}
