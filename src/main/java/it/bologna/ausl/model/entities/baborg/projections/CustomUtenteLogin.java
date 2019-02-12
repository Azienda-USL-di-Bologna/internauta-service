/**
 * Auto-Generated using the Jenesis Syntax API
 */
package it.bologna.ausl.model.entities.baborg.projections;

import it.bologna.ausl.blackbox.types.PermessoEntitaStoredProcedure;
import it.bologna.ausl.model.entities.baborg.Azienda;
import it.bologna.ausl.model.entities.baborg.Persona;
import it.bologna.ausl.model.entities.baborg.Ruolo;
import it.bologna.ausl.model.entities.baborg.Utente;
import it.bologna.ausl.model.entities.baborg.projections.generated.AziendaWithPlainFields;
import it.bologna.ausl.model.entities.baborg.projections.generated.UtenteWithIdAziendaAndIdPersona;
import it.bologna.ausl.model.entities.configuration.projections.generated.ImpostazioniApplicazioniWithPlainFields;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;

import org.springframework.data.rest.core.config.Projection;

@Projection(name = "CustomUtenteWithIdPersona", types = Utente.class)
public interface CustomUtenteLogin extends UtenteWithIdAziendaAndIdPersona {

    @Override
    @Value("#{@projectionBeans.getIdPersonaWithImpostazioniApplicazioniList(target)}")
    public CustomPersonaWithImpostazioniApplicazioniList getIdPersona();

    @Override
    public Azienda getIdAzienda();

    @Value("#{@userInfoService.getRuoli(target, null)}")
    @Override
    public List<Ruolo> getRuoli();
    
    @Value("#{@userInfoService.getPermessiDiFlusso(target)}")
    @Override
    public List<PermessoEntitaStoredProcedure> getPermessiDiFlusso();

    @Value("#{@userInfoService.getAziendePersonaWithPlainField(target)}")
    public List<AziendaWithPlainFields> getAziende();

    @Override
    @Value("#{null}")
    public String getPasswordHash();
    
    @Value("#{@projectionBeans.getUtenteRealeWithIdPersonaImpostazioniApplicazioniList(target)}")
    @Override
    public CustomUtenteLogin getUtenteReale();

    @Value("#{@userInfoService.getRuoliUtentiPersona(target)}")
    public Map<String, List<Ruolo>> getRuoliUtentiPersona();
}
