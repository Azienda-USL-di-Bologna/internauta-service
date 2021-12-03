package it.bologna.ausl.model.entities.baborg.projections;

import it.bologna.ausl.model.entities.baborg.Persona;
import it.bologna.ausl.model.entities.baborg.projections.generated.PersonaWithUtenteList;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.rest.core.config.Projection;

/**
 *
 * @author Mr. Sal
 */
@Projection(name = "PersonaPlainWithEntitaExpandedCustom", types = Persona.class)
public interface PersonaPlainWithEntitaExpandedCustom extends PersonaWithUtenteList {

    @Value("#{@projectionBeans.getEntita(target)}")
    public Object getEntita();

////    public Azienda getIdAziendaDefault();
//    public List<Utente> getUtenteList();
}
