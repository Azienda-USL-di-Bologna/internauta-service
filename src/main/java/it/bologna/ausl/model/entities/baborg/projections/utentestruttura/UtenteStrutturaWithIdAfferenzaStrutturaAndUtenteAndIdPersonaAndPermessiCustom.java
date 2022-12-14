package it.bologna.ausl.model.entities.baborg.projections.utentestruttura;

import it.bologna.ausl.model.entities.baborg.projections.utente.UtenteWithIdPersonaAndPermessiCustom;
import it.bologna.ausl.model.entities.baborg.UtenteStruttura;
import it.bologna.ausl.model.entities.baborg.projections.generated.UtenteStrutturaWithIdAfferenzaStrutturaAndIdUtente;
import org.springframework.beans.factory.annotation.Value;

import org.springframework.data.rest.core.config.Projection;

@Projection(name = "UtenteStrutturaWithIdAfferenzaStrutturaAndUtenteAndIdPersonaAndPermessiCustom", types = UtenteStruttura.class)
public interface UtenteStrutturaWithIdAfferenzaStrutturaAndUtenteAndIdPersonaAndPermessiCustom extends UtenteStrutturaWithIdAfferenzaStrutturaAndIdUtente {
    
    @Value("#{@projectionsInterceptorLauncher.lanciaInterceptor(target, 'getIdUtente', 'UtenteWithIdPersonaAndPermessiCustom')}")
    @Override
    public UtenteWithIdPersonaAndPermessiCustom getIdUtente();
}
