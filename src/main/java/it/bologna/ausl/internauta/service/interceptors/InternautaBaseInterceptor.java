package it.bologna.ausl.internauta.service.interceptors;

import it.bologna.ausl.blackbox.exceptions.BlackBoxPermissionException;
import it.bologna.ausl.internauta.service.authorization.AuthenticatedSessionData;
import it.bologna.ausl.internauta.service.authorization.AuthenticatedSessionDataBuilder;
import it.bologna.ausl.internauta.service.authorization.TokenBasedAuthentication;
import it.bologna.ausl.internauta.service.utils.HttpSessionData;
import it.bologna.ausl.internauta.service.utils.CachedEntities;
import it.bologna.ausl.model.entities.baborg.Persona;
import it.bologna.ausl.model.entities.baborg.Ruolo;
import it.bologna.ausl.model.entities.baborg.Utente;
import it.nextsw.common.interceptors.NextSdrEmptyControllerInterceptor;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

/**
 *
 * @author gdm
 */
public abstract class InternautaBaseInterceptor extends NextSdrEmptyControllerInterceptor {

//    protected Utente user, realUser;
//    protected Persona person, realPerson;
//    protected int idSessionLog;

    @Autowired
    protected CachedEntities cachedEntities;
    
    @Autowired
    protected HttpSessionData httpSessionData;
    
    @Autowired
    private AuthenticatedSessionDataBuilder authenticatedSessionDataBuilder;
    
    private static final Logger LOGGER = LoggerFactory.getLogger(InternautaBaseInterceptor.class);

    protected AuthenticatedSessionData getAuthenticatedUserProperties()  {
        try {
            AuthenticatedSessionData authenticatedUserProperties = authenticatedSessionDataBuilder.getAuthenticatedUserProperties();
//            user = authenticatedUserProperties.getUser();
//            realUser = authenticatedUserProperties.getRealUser();
//            idSessionLog = authenticatedUserProperties.getIdSessionLog();
//            person = authenticatedUserProperties.getPerson();
//            realPerson = authenticatedUserProperties.getRealPerson();
            return authenticatedUserProperties;
        } catch (BlackBoxPermissionException ex) {
            LOGGER.error("errore nel reperimento delle AuthenticatedUserProperties", ex);
            return null;
        }
    }

    // Forse sarebbe più bello fare un'unica funzione hasRole(utente, ruolo)
    protected boolean isCA(Utente user) {
        List<Ruolo> ruoli = user.getRuoli();
        Boolean isCA = ruoli.stream().anyMatch(p -> p.getNomeBreve() == Ruolo.CodiciRuolo.CA);
        return isCA;
    }
    protected boolean isCI(Utente user) {
        List<Ruolo> ruoli = user.getRuoli();
        Boolean isCI = ruoli.stream().anyMatch(p -> p.getNomeBreve() == Ruolo.CodiciRuolo.CI);
        return isCI;
    }  
    protected boolean isAS(Utente user) {
        List<Ruolo> ruoli = user.getRuoli();
        Boolean isAS = ruoli.stream().anyMatch(p -> p.getNomeBreve() == Ruolo.CodiciRuolo.AS);
        return isAS;
    }  
    protected boolean isSD(Utente user) {
        List<Ruolo> ruoli = user.getRuoli();
        Boolean isSD = ruoli.stream().anyMatch(p -> p.getNomeBreve() == Ruolo.CodiciRuolo.SD);
        return isSD;
    }   

    
    
    
//    protected List<Azienda> getAziendeWherePersonaIsCa() {
//        Persona persona = personaRepository.getOne(person.getId());
//        List<Integer> aziende = persona.getUtenteList().stream().map(utente -> utente.getIdAzienda().getId()).collect(Collectors.toList());
//    }
}
