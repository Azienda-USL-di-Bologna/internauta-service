package it.bologna.ausl.model.entities.baborg.projections;

import it.bologna.ausl.model.entities.baborg.Utente;
import it.bologna.ausl.model.entities.baborg.projections.generated.UtenteWithIdPersonaAndUtenteStrutturaList;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;

import org.springframework.data.rest.core.config.Projection;

@Projection(name = "UtenteWithStruttureAndResponsabiliCustom", types = Utente.class)
public interface UtenteWithStruttureAndResponsabiliCustom extends UtenteWithIdPersonaAndUtenteStrutturaList {
    
    @Override
    @Value("#{@projectionBeans.getStruttureUtenteWithAfferenzaAndReponsabili(target)}")
    public List<UtenteStrutturaWithIdAfferenzaStrutturaAndIdStrutturaAndUtenteResponsabiliCustom> getUtenteStrutturaList();
    
}
