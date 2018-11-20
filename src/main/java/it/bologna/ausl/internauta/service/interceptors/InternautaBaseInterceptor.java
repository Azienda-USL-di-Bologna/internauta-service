package it.bologna.ausl.internauta.service.interceptors;

import it.bologna.ausl.internauta.service.authorization.TokenBasedAuthentication;
import it.bologna.ausl.internauta.service.utils.CachedEntities;
import it.bologna.ausl.model.entities.baborg.Persona;
import it.bologna.ausl.model.entities.baborg.Utente;
import it.nextsw.common.interceptors.NextSdrEmptyControllerInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 *
 * @author gdm
 */
public abstract class InternautaBaseInterceptor extends NextSdrEmptyControllerInterceptor {

    // ThreadLocal fa si che la variabile rimanga settata solamente nel thread corrente (nella singola chiamata), e venga poi svuotata al termine
    // se non facessi così rimarebbe memorizzato sempre lo stesso utente.
    protected final ThreadLocal<TokenBasedAuthentication> threadLocalAuthentication = new ThreadLocal();
    protected Utente user, realUser;
    protected Persona person, realPerson;
    protected int idSessionLog;

    @Autowired
    CachedEntities cachedEntities;

    protected void getAuthenticatedUserProperties() {
        // TODO add url

        if (threadLocalAuthentication.get() == null) {
            setAuthentication();
            user = (Utente) threadLocalAuthentication.get().getPrincipal();
            realUser = (Utente) threadLocalAuthentication.get().getRealUser();
            idSessionLog = threadLocalAuthentication.get().getIdSessionLog();
            person = cachedEntities.getPersona(user.getIdPersona().getId());
            realPerson = cachedEntities.getPersona(realUser.getIdPersona().getId());
        }
    }

    private void setAuthentication() {
        this.threadLocalAuthentication.set((TokenBasedAuthentication) SecurityContextHolder.getContext().getAuthentication());
    }
}
