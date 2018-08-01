package it.bologna.ausl.baborg.service.interceptors;

import com.querydsl.core.types.Predicate;
import it.bologna.ausl.baborg.model.entities.QUtenteStruttura;
import it.bologna.ausl.baborg.model.entities.UtenteStruttura;
import it.nextsw.common.annotations.Interceptor;
import it.nextsw.common.interceptors.EmptyInterceptor;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;

/**
 *
 * @author gdm
 */
@Component
@Interceptor(target = UtenteStruttura.class, name = "utentestruttura-interceptorTest")
public class UtenteStrutturaInterceptor extends EmptyInterceptor {

    @Override
    public Predicate beforeSelectQueryInterceptor(Predicate initialPredicate, Map<String, String> additionalData, HttpServletRequest request) {
        System.out.println("in: beforeSelectQueryInterceptor di UtenteStruttura");
//        return QUtenteStruttura.utenteStruttura.id.ne(3023029).and(initialPredicate);
    return initialPredicate;
    }

    @Override
    public Object afterSelectQueryInterceptor(Object entity, Map<String, String> additionalData, HttpServletRequest request) {
        System.out.println("in: afterSelectQueryInterceptor di " + entity.getClass().getSimpleName());
        return entity;
    }
}
