
package it.bologna.ausl.model.entities.logs.projections;

import it.bologna.ausl.model.entities.baborg.Utente;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;

import org.springframework.data.rest.core.config.Projection;


@Projection(name = "KrintInformazioniUtente", types = Utente.class)
public interface KrintInformazioniUtente {
      
    // azienda
    @Value("#{@userInfoService.getAziendaKrint(target)}") 
    KrintBaborgAzienda getAzienda();
            
    // strutture
    @Value("#{@userInfoService.getStruttureKrint(target)}")
    List<KrintBaborgUtenteStruttura> getStrutture();
    
    // persona
    @Value("#{@userInfoService.getPersonaKrint(target)}")
    KrintBaborgPersona getIdPersona();                 
    
  
}
