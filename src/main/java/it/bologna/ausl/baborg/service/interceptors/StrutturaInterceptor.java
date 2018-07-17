package it.bologna.ausl.baborg.service.interceptors;

import it.bologna.ausl.baborg.model.entities.Struttura;
import it.bologna.ausl.baborg.model.entities.Utente;
import it.nextsw.common.annotations.Interceptor;
import it.nextsw.common.interceptors.EmptyInterceptor;
import it.nextsw.common.interceptors.exceptions.RollBackInterceptorException;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 *
 * @author gdm
 */
@Component
@Interceptor(target = Struttura.class, name = "struttura-interceptorTest")
public class StrutturaInterceptor extends EmptyInterceptor {

    @Override
    public Object afterSelectQueryInterceptor(Object entity, Map<String, String> additionalData, HttpServletRequest request) {
        System.out.println("in: afterSelectQueryInterceptor di " + entity.getClass().getSimpleName());
        
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Utente utente = (Utente) authentication.getPrincipal();
        System.out.println("utente Connesso: " + utente.getUsername());
        
        Struttura struttura = (Struttura) entity;
        return struttura;
    }

    @Override
    public Object beforeCreateInterceptor(Object entity, Map<String, String> additionalData, HttpServletRequest request) throws RollBackInterceptorException {
        System.out.println("in: beforeCreateInterceptor di Albo con " + entity.getClass().getSimpleName());
        Struttura struttura = (Struttura) entity;
        return struttura;
    }

    @Override
    public Object beforeUpdateInterceptor(Object entity, Map<String, String> additionalData, HttpServletRequest request) throws RollBackInterceptorException {
        System.out.println("in: beforeUpdateInterceptor di " + entity.getClass().getSimpleName());
        Struttura struttura = (Struttura) entity;
        return struttura;
    }
}
