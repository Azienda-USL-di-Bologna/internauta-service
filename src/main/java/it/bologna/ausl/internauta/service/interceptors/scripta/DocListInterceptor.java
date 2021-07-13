package it.bologna.ausl.internauta.service.interceptors.scripta;

import com.querydsl.core.types.Predicate;
import it.bologna.ausl.internauta.service.authorization.AuthenticatedSessionData;
import it.bologna.ausl.internauta.service.interceptors.InternautaBaseInterceptor;
import it.bologna.ausl.model.entities.baborg.Persona;
import it.bologna.ausl.model.entities.baborg.Utente;
import it.bologna.ausl.model.entities.scripta.DocList;
import it.nextsw.common.annotations.NextSdrInterceptor;
import it.nextsw.common.interceptors.exceptions.AbortLoadInterceptorException;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;

/**
 *
 * @author gusgus
 */
@Component
@NextSdrInterceptor(name = "doclist-interceptor")
public class DocListInterceptor extends InternautaBaseInterceptor {

    @Override
    public Class getTargetEntityClass() {
        return DocList.class;
    }

    @Override
    public Predicate beforeSelectQueryInterceptor(Predicate initialPredicate, Map<String, String> additionalData, HttpServletRequest request, boolean mainEntity, Class projectionClass) throws AbortLoadInterceptorException {
        AuthenticatedSessionData authenticatedSessionData = getAuthenticatedUserProperties();
        Utente user = authenticatedSessionData.getUser();
        Persona persona = user.getIdPersona();
        
//        BooleanExpression filter = Expressions.booleanTemplate(
//            String.format("FUNCTION('jsonb_contains', {0}, '[{\"idPersona\": %d}]') = true", persona.getId()),
//            QDocList.docList.personeVedenti
//        );

        System.out.println("ecco lo id persona" + persona.getId());

//        initialPredicate = filter.and(initialPredicate);
        
        return super.beforeSelectQueryInterceptor(initialPredicate, additionalData, request, mainEntity, projectionClass);
    }
}
