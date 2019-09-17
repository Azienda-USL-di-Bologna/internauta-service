
package it.bologna.ausl.model.entities.logs.projections;

import it.bologna.ausl.model.entities.baborg.Persona;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;

import org.springframework.data.rest.core.config.Projection;
import it.bologna.ausl.model.entities.logs.projections.KrintBaborgAzienda;


@Projection(name = "KrintBaborgPersona", types = Persona.class)
public interface KrintBaborgPersona{
      
    Integer getId();
    String getCodiceFiscale();
    String getDescrizione();
    
    // aziende
    @Value("#{@userInfoService.getAziendeKrint(target)}") 
    List<KrintBaborgAzienda> getAziende();    
        
    @Value("#{@userInfoService.getRuoli(target, false)}")
    Map<String,List<String>> getRuoli();
        
    // TODO da definire
    // permessi sulla persona o permessi divisi per azienda
    @Value("#{@userInfoService.getPermessiKrint(target)}")
    Map<String, Object> getPermessi();
    
    
    
//    // metto l'utente con la persona
//    @Value("#{@projectionBeans.getUtenteConPersona(target.getIdUtente())}")
//    public UtenteWithIdPersona getIdUtente();
//    
//    // metto la struttura con l'azienda
//    @Value("#{@projectionBeans.getStrutturaConAzienda(target.getIdStruttura())}")
//    public StrutturaWithIdAzienda getIdStruttura();
   
    
  
}
