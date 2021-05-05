package it.bologna.ausl.model.entities.baborg.projections;

import it.bologna.ausl.internauta.utils.bds.types.PermessoEntitaStoredProcedure;
import it.bologna.ausl.model.entities.baborg.Azienda;
import it.bologna.ausl.model.entities.baborg.Ruolo;
import it.bologna.ausl.model.entities.baborg.Utente;
import it.bologna.ausl.model.entities.baborg.projections.generated.AziendaWithPlainFields;
import it.bologna.ausl.model.entities.baborg.projections.generated.UtenteWithIdAziendaAndIdPersona;
import it.bologna.ausl.model.entities.baborg.projections.generated.UtenteWithIdPersona;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;

import org.springframework.data.rest.core.config.Projection;

@Projection(name = "CustomUtenteWithIdPersona", types = Utente.class)
public interface CustomUtenteLogin extends UtenteWithIdPersonaAndPermessiCustom {

    @Override
    @Value("#{@projectionBeans.getIdPersonaWithImpostazioniApplicazioniList(target)}")
    public CustomPersonaLogin getIdPersona();

    @Value("#{@userInfoService.getAziendaCustomLogin(target)}")
    public CustomAziendaLogin getAziendaLogin();

    @Value("#{@userInfoService.getAllAziendeCustomLogin(target, false)}")
    public List<CustomAziendaLogin> getAziende();
    
    @Value("#{@userInfoService.getAllAziendeCustomLogin(target, true)}")
    public List<CustomAziendaLogin> getAziendeAttive();

//    @Value("#{@userInfoService.getRuoli(target, null)}")
//    @Override
//    public List<Ruolo> getRuoli();
    
    @Value("#{@userInfoService.getRuoliPerModuli(target, null)}")
    @Override
    public Map<String, List<Ruolo>> getMappaRuoli();

//    @Value("#{@userInfoService.getAziendePersonaWithPlainField(target)}")
//    public List<AziendaWithPlainFields> getAziende();
    @Value("#{@projectionBeans.getUtenteRealeWithIdPersonaImpostazioniApplicazioniList(target)}")
    @Override
    public CustomUtenteLogin getUtenteReale();

//    @Value("#{@userInfoService.getRuoliUtentiPersona(target, true)}")
//    @Override
//    public Map<String, List<Ruolo>> getRuoliUtentiPersona();
    @Value("#{@userInfoService.getRuoliUtentiPersona(target, true)}")
    @Override
    public Map<String, Map<String, List<String>>> getRuoliUtentiPersona();

    @Value("#{@userInfoService.getPermessiDiFlussoByCodiceAzienda(target)}")
    @Override
    public Map<String, List<PermessoEntitaStoredProcedure>> getPermessiDiFlussoByCodiceAzienda();

    @Value("#{@userInfoService.getPermessiAvatar(target).size() > 0}")
    public Boolean getHasPermessoAvatar();

}
