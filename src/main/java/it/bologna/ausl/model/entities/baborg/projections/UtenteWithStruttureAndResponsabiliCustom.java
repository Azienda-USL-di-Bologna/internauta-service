package it.bologna.ausl.model.entities.baborg.projections;

import it.bologna.ausl.internauta.utils.bds.types.PermessoEntitaStoredProcedure;
import it.bologna.ausl.model.entities.baborg.Utente;
import it.bologna.ausl.model.entities.baborg.projections.generated.UtenteWithIdPersona;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;

import org.springframework.data.rest.core.config.Projection;

@Projection(name = "UtenteWithStruttureAndResponsabiliCustom", types = Utente.class)
public interface UtenteWithStruttureAndResponsabiliCustom extends UtenteWithIdPersona {

    @Value("#{@projectionBeans.getStruttureUtenteWithReponsabile(target)}")
    public List<StrutturaWithUtenteResponsabileCustom> getStruttureUtenteWithReponsabile();
    
}
