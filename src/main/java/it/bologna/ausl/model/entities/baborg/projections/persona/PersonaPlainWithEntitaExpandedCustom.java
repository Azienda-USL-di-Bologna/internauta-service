package it.bologna.ausl.model.entities.baborg.projections.persona;

import it.bologna.ausl.model.entities.baborg.Persona;
import it.bologna.ausl.model.entities.baborg.Utente;
import it.bologna.ausl.model.entities.baborg.projections.generated.PersonaWithUtenteList;
import it.bologna.ausl.model.entities.baborg.projections.generated.UtenteWithIdAzienda;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.rest.core.config.Projection;

/**
 *
 * @author Mr. Sal
 */
@Projection(name = "PersonaPlainWithEntitaExpandedCustom", types = Persona.class)
public interface PersonaPlainWithEntitaExpandedCustom extends PersonaWithUtenteList {

    @Value("#{@permessiProjectionsUtils.getEntita(target)}")
    public Object getEntita();

////    public Azienda getIdAziendaDefault();
    @Override
    @Value("#{@projectionsInterceptorLauncher.lanciaInterceptorCollection(target, 'getUtenteList', 'UtenteWithIdAzienda')}")
    public List<UtenteWithIdAzienda> getUtenteList();
}
