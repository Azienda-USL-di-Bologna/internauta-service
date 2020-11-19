
package it.bologna.ausl.model.entities.logs.projections;

import it.bologna.ausl.model.entities.baborg.Utente;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;

import org.springframework.data.rest.core.config.Projection;


@Projection(name = "KrintInformazioniRealUser", types = Utente.class)
public interface KrintInformazioniRealUser{
      
    @Value("#{@userInfoService.getRuoliGenerali(target, true)}")
    Map<String,List<String>> getRuoli();
    
    @Value("#{@userInfoService.getPermessiAvatar(target)}")
    Map<String, Object> getPermessiAvatar();
}
