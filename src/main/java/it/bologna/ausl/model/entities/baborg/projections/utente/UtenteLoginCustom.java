package it.bologna.ausl.model.entities.baborg.projections.utente;

import it.bologna.ausl.internauta.utils.bds.types.PermessoEntitaStoredProcedure;
import it.bologna.ausl.model.entities.baborg.Ruolo;
import it.bologna.ausl.model.entities.baborg.Utente;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;

import org.springframework.data.rest.core.config.Projection;
import it.bologna.ausl.model.entities.baborg.projections.persona.CustomPersonaLogin;
import it.bologna.ausl.model.entities.baborg.projections.azienda.CustomAziendaLogin;

@Projection(name = "CustomUtenteWithIdPersona", types = Utente.class)
public interface UtenteLoginCustom extends UtenteWithIdPersonaAndPermessiCustom {

    @Override
    @Value("#{@projectionsInterceptorLauncher.lanciaInterceptor(target, 'getIdPersona',CustomPersonaLogin)}")
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
    @Value("#{@utenteProjectionUtils.getUtenteRealeWithIdPersonaImpostazioniApplicazioniList(target)}")
    @Override
    public UtenteLoginCustom getUtenteReale();

//    @Value("#{@userInfoService.getRuoliUtentiPersona(target, true)}")
//    @Override
//    public Map<String, List<Ruolo>> getRuoliUtentiPersona();
    @Value("#{@userInfoService.getRuoliUtentiPersona(target, true)}")
    @Override
    public Map<String, Map<String, List<String>>> getRuoliUtentiPersona();

    @Value("#{@userInfoService.getPermessiDiFlussoByCodiceAzienda(target)}")
    @Override
    public Map<String, List<PermessoEntitaStoredProcedure>> getPermessiDiFlussoByCodiceAzienda();
    
    @Value("#{@userInfoService.getStruttureDelSegretario(target.getIdPersona())}")
    @Override
    public Object getStruttureDelSegretario();

    @Value("#{@userInfoService.getPermessiAvatar(target).size() > 0}")
    public Boolean getHasPermessoAvatar();

}
