package it.bologna.ausl.internauta.service.authorization;

import it.bologna.ausl.blackbox.exceptions.BlackBoxPermissionException;
import it.bologna.ausl.internauta.service.utils.CachedEntities;
import it.bologna.ausl.model.entities.baborg.Persona;
import it.bologna.ausl.model.entities.baborg.Utente;
import it.bologna.ausl.model.entities.configuration.Applicazione;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 *
 * @author gdm
 */
@Component
public class AuthenticatedSessionDataBuilder {

    // ThreadLocal fa si che la variabile valga solo per il thread corrente (nella singola chiamata). 
    // Dato che il bean è unico per tutti i thread in questo modo ogni thread ha la sua copia della variabile
    protected final ThreadLocal<TokenBasedAuthentication> threadLocalAuthentication = new ThreadLocal();

    @Autowired
    private CachedEntities cachedEntities;
    
    public AuthenticatedSessionData getAuthenticatedUserProperties() throws BlackBoxPermissionException {
        setAuthentication();
        Utente user = (Utente) threadLocalAuthentication.get().getPrincipal();
        Utente realUser = (Utente) threadLocalAuthentication.get().getRealUser();
        int idSessionLog = threadLocalAuthentication.get().getIdSessionLog();
        Persona person = cachedEntities.getPersonaFromUtente(user);
        Persona realPerson = null;
        if (realUser != null) {
            realPerson = cachedEntities.getPersonaFromUtente(realUser);
        }
        Applicazione.Applicazioni applicazione =  threadLocalAuthentication.get().getApplicazione();

        return new AuthenticatedSessionData(user, realUser, person, realPerson, idSessionLog, applicazione);
    }

    private void setAuthentication() {
        threadLocalAuthentication.set((TokenBasedAuthentication) SecurityContextHolder.getContext().getAuthentication());
    }
    
}
