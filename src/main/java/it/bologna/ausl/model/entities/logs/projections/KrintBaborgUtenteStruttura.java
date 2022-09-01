
package it.bologna.ausl.model.entities.logs.projections;

import it.bologna.ausl.model.entities.baborg.UtenteStruttura;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.rest.core.config.Projection;

/**
 *
 * @author guido
 */
@Projection(name = "KrintBaborgUtenteStruttura", types = UtenteStruttura.class)
public interface KrintBaborgUtenteStruttura {
    
//    @Value("#{@userInfoService.getpermessiKrint(target)}")
    @Value("#{target.getIdStruttura().getId()}") 
//    @Value("#{@userInfoService.getIdStrutturaFromUtenteStruttura(target).getId()}") 
    String getId();
    
    @Value("#{target.getIdStruttura().getCodice()}") 
//    @Value("#{@userInfoService.getIdStrutturaFromUtenteStruttura(target).getCodice()}") 
    String getCodice();  
    
    @Value("#{target.getIdStruttura().getNome()}") 
//    @Value("#{@userInfoService.getIdStrutturaFromUtenteStruttura(target).getNome()}") 
    String getNome();
//    
    @Value("#{target.getIdAfferenzaStruttura().getCodice()}")
    String getAfferenza();
    
//    @Value("#{@userInfoService.getRuoliUtenteStruttura(target)}")
//    List<String> getRuoliUtenteStruttura();
    // TODO: non so se mettere solo l'id o ancqhe qui usare questa stessa projection
    // getIdStrutturaPadre();
          
    // getIdStrutturaSegretaria();
    
    // getIdStrutturaReplicata();    
    
}
