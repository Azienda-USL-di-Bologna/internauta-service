
package it.bologna.ausl.model.entities.baborg.projections.utentestruttura;

import it.bologna.ausl.model.entities.baborg.UtenteStruttura;
import it.bologna.ausl.model.entities.baborg.projections.generated.UtenteStrutturaWithIdAfferenzaStrutturaAndIdStrutturaAndIdStrutturaVeicolanteAndIdUtente;
import org.springframework.beans.factory.annotation.Value;

import org.springframework.data.rest.core.config.Projection;


@Projection(name = "UtenteStrutturaExpandedCustom", types = UtenteStruttura.class)
public interface UtenteStrutturaExpandedCustom extends UtenteStrutturaWithIdAfferenzaStrutturaAndIdStrutturaAndIdStrutturaVeicolanteAndIdUtente {
    
    // metto l'utente con la persona
    @Override
    @Value("#{@projectionsInterceptorLauncher.lanciaInterceptor(target, 'getIdUtente', 'UtenteWithIdPersona')}")
    public Object getIdUtente();
    
    // metto la struttura con l'azienda
    @Override
    @Value("#{@projectionsInterceptorLauncher.lanciaInterceptor(target, 'getIdStruttura', 'StrutturaWithAttributiStrutturaAndIdAzienda')}")
    public Object getIdStruttura();
    }
