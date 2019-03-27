package it.bologna.ausl.internauta.service.interceptors;

import it.bologna.ausl.internauta.service.authorization.TokenBasedAuthentication;
import it.bologna.ausl.internauta.service.authorization.jwt.LoginController;
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
import org.springframework.security.core.context.SecurityContextHolder;

/**
 *
 * @author gdm
 */
public abstract class InternautaBaseInterceptor extends NextSdrEmptyControllerInterceptor {

    // ThreadLocal fa si che la variabile valga solo per il thread corrente (nella singola chiamata). 
    // Dato che il bean è unico per tutti i thread in questo modo ogni thread ha la sua copia della variabile
    protected final ThreadLocal<TokenBasedAuthentication> threadLocalAuthentication = new ThreadLocal();
    protected Utente user, realUser;
    protected Persona person, realPerson;
    protected int idSessionLog;

    @Autowired
    protected CachedEntities cachedEntities;
    
    @Autowired
    protected HttpSessionData httpSessionData;
    
    private static final Logger log = LoggerFactory.getLogger(InternautaBaseInterceptor.class);

    protected void getAuthenticatedUserProperties() {
        // TODO add url

        //if (threadLocalAuthentication.get() == null) {
            setAuthentication();
            user = (Utente) threadLocalAuthentication.get().getPrincipal();
            realUser = (Utente) threadLocalAuthentication.get().getRealUser();
            idSessionLog = threadLocalAuthentication.get().getIdSessionLog();
            person = cachedEntities.getPersona(user);
            realPerson = cachedEntities.getPersona(realUser);
        //}
    }

    private void setAuthentication() {
        threadLocalAuthentication.set((TokenBasedAuthentication) SecurityContextHolder.getContext().getAuthentication());
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
