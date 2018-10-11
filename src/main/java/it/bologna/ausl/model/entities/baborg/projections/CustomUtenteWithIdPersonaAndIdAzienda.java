/**
 * Auto-Generated using the Jenesis Syntax API
 */
package it.bologna.ausl.model.entities.baborg.projections;

import it.bologna.ausl.model.entities.baborg.Azienda;
import it.bologna.ausl.model.entities.baborg.Persona;
import it.bologna.ausl.model.entities.baborg.Ruolo;
import it.bologna.ausl.model.entities.baborg.Utente;
import it.bologna.ausl.model.entities.baborg.projections.generated.AziendaWithPlainFields;
import it.bologna.ausl.model.entities.baborg.projections.generated.UtenteWithIdAziendaAndIdPersona;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;

import org.springframework.data.rest.core.config.Projection;

@Projection(name = "CustomUtenteWithIdPersona", types = Utente.class)
public interface CustomUtenteWithIdPersonaAndIdAzienda extends UtenteWithIdAziendaAndIdPersona {

    @Override
    public Persona getIdPersona();

    @Override
    public Azienda getIdAzienda();

    @Value("#{@userInfoService.getRuoli(target)}")
    public List<Ruolo> getRuoli();

    @Value("#{@userInfoService.getAziendePersona(target)}")
    public List<AziendaWithPlainFields> getAziende();

}
