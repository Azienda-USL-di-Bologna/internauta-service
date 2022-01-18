package it.bologna.ausl.model.entities.logs.projections;

import it.bologna.ausl.model.entities.baborg.Persona;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.rest.core.config.Projection;


@Projection(name = "KrintBaborgPersona", types = Persona.class)
public interface KrintBaborgPersona{
      
    Integer getId();
    String getCodiceFiscale();
    String getDescrizione();
    
    // aziende
    @Value("#{@userInfoService.getAziendeKrint(target)}") 
    List<KrintBaborgAzienda> getAziende();
        
    // TODO da definire
    // permessi sulla persona o permessi divisi per azienda
    @Value("#{@userInfoService.getPermessiKrint(target)}")
    Map<String, Object> getPermessi();   
  
}
