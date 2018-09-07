package it.bologna.ausl.baborg.service.interceptors;

import com.querydsl.core.types.Predicate;
import it.bologna.ausl.model.entities.baborg.UtenteStruttura;
import it.nextsw.common.annotations.NextSdrInterceptor;
import it.nextsw.common.interceptors.NextSdrEmptyControllerInterceptor;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;

/**
 *
 * @author gdm
 */
@Component
@NextSdrInterceptor(name = "utentestruttura-interceptorTest")
public class UtenteStrutturaInterceptor extends NextSdrEmptyControllerInterceptor {

    @Override
    public Class getTargetEntityClass() {
        return UtenteStruttura.class;
    }
    
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
