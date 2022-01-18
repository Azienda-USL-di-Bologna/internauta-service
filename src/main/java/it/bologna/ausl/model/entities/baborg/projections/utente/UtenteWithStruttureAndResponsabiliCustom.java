package it.bologna.ausl.model.entities.baborg.projections.utente;

import it.bologna.ausl.model.entities.baborg.Utente;
import it.bologna.ausl.model.entities.baborg.projections.utentestruttura.UtenteStrutturaWithIdAfferenzaStrutturaAndIdStrutturaAndUtenteResponsabiliCustom;
import it.bologna.ausl.model.entities.baborg.projections.generated.UtenteWithIdAziendaAndIdPersonaAndUtenteStrutturaList;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;

import org.springframework.data.rest.core.config.Projection;

@Projection(name = "UtenteWithStruttureAndResponsabiliCustom", types = Utente.class)
public interface UtenteWithStruttureAndResponsabiliCustom extends UtenteWithIdAziendaAndIdPersonaAndUtenteStrutturaList {

    @Override
    @Value("#{@projectionsInterceptorLauncher.lanciaInterceptorCollection(target, 'getUtenteStrutturaList', 'UtenteStrutturaWithIdAfferenzaStrutturaAndIdStrutturaAndUtenteResponsabiliCustom')}")
    public List<UtenteStrutturaWithIdAfferenzaStrutturaAndIdStrutturaAndUtenteResponsabiliCustom> getUtenteStrutturaList();

}
