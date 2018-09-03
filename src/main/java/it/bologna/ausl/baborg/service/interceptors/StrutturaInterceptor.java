package it.bologna.ausl.baborg.service.interceptors;

import it.bologna.ausl.baborg.model.entities.Struttura;
import it.nextsw.common.annotations.NextSdrInterceptor;
import it.nextsw.common.interceptors.NextSdrEmptyControllerInterceptor;
import it.nextsw.common.interceptors.exceptions.RollBackInterceptorException;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;

/**
 *
 * @author gdm
 */
@Component
@NextSdrInterceptor(name = "struttura-interceptorTest")
public class StrutturaInterceptor extends NextSdrEmptyControllerInterceptor {

    @Override
    public Class getTargetEntityClass() {
        return Struttura.class;
    }
    
    @Override
    public Object afterSelectQueryInterceptor(Object entity, Map<String, String> additionalData, HttpServletRequest request) {
        System.out.println("in: afterSelectQueryInterceptor di " + entity.getClass().getSimpleName());
        
//        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
//        Utente utente = (Utente) authentication.getPrincipal();
//        System.out.println("utente Connesso: " + utente.getUsername());
        
        Struttura struttura = (Struttura) entity;
        return struttura;
    }

    @Override
    public Object beforeCreateEntityInterceptor(Object entity, Map<String, String> additionalData, HttpServletRequest request) throws RollBackInterceptorException {
        System.out.println("in: beforeCreateInterceptor di Albo con " + entity.getClass().getSimpleName());
        Struttura struttura = (Struttura) entity;
        return struttura;
    }

    @Override
    public Object beforeUpdateEntityInterceptor(Object entity, Map<String, String> additionalData, HttpServletRequest request) throws RollBackInterceptorException {
        System.out.println("in: beforeUpdateInterceptor di " + entity.getClass().getSimpleName());
        Struttura struttura = (Struttura) entity;
        return struttura;
    }
}
