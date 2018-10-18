package it.bologna.ausl.internauta.service.interceptors.baborg;

import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.dsl.BooleanExpression;
import it.bologna.ausl.model.entities.baborg.QUtenteStruttura;
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

    private static final String FILTER_COMBO = "filterCombo";
    
    @Override
    public Class getTargetEntityClass() {
        return UtenteStruttura.class;
    }
    
    @Override
    public Predicate beforeSelectQueryInterceptor(Predicate initialPredicate, Map<String, String> additionalData, HttpServletRequest request) {
        System.out.println("in: beforeSelectQueryInterceptor di UtenteStruttura");
        
        String filterComboValue = additionalData.get(FILTER_COMBO);
        
        if (filterComboValue != null) {
                    BooleanExpression customFilter = QUtenteStruttura.utenteStruttura.idUtente.idPersona.cognome
                        .concat(" ")
                        .concat(QUtenteStruttura.utenteStruttura.idUtente.idPersona.nome)
                        .containsIgnoreCase(filterComboValue);
            initialPredicate = customFilter.and(initialPredicate);
        }
        
        
    return initialPredicate;
    }

    @Override
    public Object afterSelectQueryInterceptor(Object entity, Map<String, String> additionalData, HttpServletRequest request) {
        System.out.println("in: afterSelectQueryInterceptor di " + entity.getClass().getSimpleName());
        return entity;
    }
}
