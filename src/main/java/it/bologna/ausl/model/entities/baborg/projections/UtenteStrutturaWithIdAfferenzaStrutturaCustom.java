
package it.bologna.ausl.model.entities.baborg.projections;

import it.bologna.ausl.model.entities.baborg.UtenteStruttura;
import it.bologna.ausl.model.entities.baborg.projections.generated.UtenteStrutturaWithIdAfferenzaStruttura;
import org.springframework.beans.factory.annotation.Value;

import org.springframework.data.rest.core.config.Projection;


@Projection(name = "UtenteStrutturaWithIdAfferenzaStrutturaCustom", types = UtenteStruttura.class)
public interface UtenteStrutturaWithIdAfferenzaStrutturaCustom extends UtenteStrutturaWithIdAfferenzaStruttura{
    
    // metto l'utente con la persona
    @Value("#{@projectionsInterceptorLauncher.lanciaInterceptor(target, 'getIdUtente', 'UtenteWithIdPersona')}")
    public Object getIdUtente();
    
    // metto la struttura con l'azienda
    @Value("#{@projectionsInterceptorLauncher.lanciaInterceptor(target, 'getIdStruttura', 'StrutturaWithAttributiStrutturaAndIdAzienda')}")
    public Object getIdStruttura();

}
