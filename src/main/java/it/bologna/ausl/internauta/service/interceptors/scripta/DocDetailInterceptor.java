package it.bologna.ausl.internauta.service.interceptors.scripta;

import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.dsl.Expressions;
import it.bologna.ausl.internauta.service.interceptors.InternautaBaseInterceptor;
import it.bologna.ausl.model.entities.scripta.DocDetail;
import it.nextsw.common.annotations.NextSdrInterceptor;
import it.nextsw.common.interceptors.exceptions.AbortLoadInterceptorException;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 *
 * @author gusgus
 */
@Component
@NextSdrInterceptor(name = "docdetail-interceptor")
public class DocDetailInterceptor extends InternautaBaseInterceptor {

    private static final Logger LOGGER = LoggerFactory.getLogger(DocDetailInterceptor.class);

    @Override
    public Class getTargetEntityClass() {
        return DocDetail.class;
    }

    @Override
    public Predicate beforeSelectQueryInterceptor(Predicate initialPredicate, Map<String, String> additionalData, HttpServletRequest request, boolean mainEntity, Class projectionClass) throws AbortLoadInterceptorException {
        // Impedisco l'utilizzo di questa entità per le select. L'entità da usare è DocDetailView
        initialPredicate = Expressions.TRUE.eq(false).and(initialPredicate);
        return super.beforeSelectQueryInterceptor(initialPredicate, additionalData, request, mainEntity, projectionClass); //To change body of generated methods, choose Tools | Templates.
    }
}
