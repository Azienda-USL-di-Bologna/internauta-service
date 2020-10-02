
package it.bologna.ausl.model.entities.baborg.projections;

import it.bologna.ausl.model.entities.baborg.UtenteStruttura;
import org.springframework.beans.factory.annotation.Value;

import org.springframework.data.rest.core.config.Projection;


@Projection(name = "UtenteStrutturaWithAfferenzaPrimaraCustom", types = UtenteStruttura.class)
public interface UtenteStrutturaWithAfferenzaPrimaraCustom extends UtenteStrutturaWithIdAfferenzaStrutturaCustom {
    
    // metto l'utente con la persona
    @Override
    @Value("#{@projectionsInterceptorLauncher.lanciaInterceptor(target, 'getIdUtente', 'UtenteWithIdPersona')}")
    public Object getIdUtente();
    
    // metto la struttura con l'azienda
    @Override
    @Value("#{@projectionsInterceptorLauncher.lanciaInterceptor(target, 'getIdStruttura', 'StrutturaWithAttributiStrutturaAndIdAzienda')}")
    public Object getIdStruttura();
    
        
    @Value("#{@userInfoService.getUtenteStrutturaAfferenzaPrincipaleAttiva(target)}")
    public Object getUtenteStrutturaAfferenzaPrincipaleAttiva();


}
