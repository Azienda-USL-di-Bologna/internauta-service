package it.bologna.ausl.baborg.service.interceptors;

import com.querydsl.core.types.Predicate;
import it.bologna.ausl.baborg.model.entities.Azienda;
import it.bologna.ausl.baborg.model.entities.QAzienda;
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
@Interceptor(target = Azienda.class, name = "azienda-interceptorTest")
public class AziendaInterceptor extends EmptyInterceptor {

    @Override
    public Predicate beforeSelectQueryInterceptor(Predicate initialPredicate, Map<String, String> additionalData, HttpServletRequest request) {
        return QAzienda.azienda.id.eq(2).and(initialPredicate);
    }
    
}
