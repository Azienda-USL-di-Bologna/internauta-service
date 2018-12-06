package it.bologna.ausl.internauta.service.interceptors.baborg;

import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.Expressions;
import it.bologna.ausl.internauta.service.interceptors.InternautaBaseInterceptor;
import it.bologna.ausl.model.entities.baborg.Pec;
import it.bologna.ausl.model.entities.baborg.Persona;
import it.bologna.ausl.model.entities.baborg.QStrutturaUnificata;
import it.bologna.ausl.model.entities.baborg.Ruolo;
import it.bologna.ausl.model.entities.baborg.StrutturaUnificata;
import it.nextsw.common.annotations.NextSdrInterceptor;
import it.nextsw.common.interceptors.exceptions.AbortSaveInterceptorException;
import it.nextsw.common.interceptors.exceptions.SkipDeleteInterceptorException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 *
 * @author g.russo@nsi.it with collaboration of GDM and Gus
 */
@Component
@NextSdrInterceptor(name = "pec-interceptor")
public class PecInterceptor extends InternautaBaseInterceptor {
    private static final Logger LOGGER = LoggerFactory.getLogger(PecInterceptor.class);

    @Override
    public Class getTargetEntityClass() {
        return Pec.class;
    }

    @Override
    public Object beforeUpdateEntityInterceptor(Object entity, Object beforeUpdateEntity, Map<String, String> additionalData, HttpServletRequest request) throws AbortSaveInterceptorException {
        return super.beforeUpdateEntityInterceptor(entity, beforeUpdateEntity, additionalData, request); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void beforeDeleteEntityInterceptor(Object entity, Map<String, String> additionalData, HttpServletRequest request) throws AbortSaveInterceptorException, SkipDeleteInterceptorException {
        super.beforeDeleteEntityInterceptor(entity, additionalData, request); //To change body of generated methods, choose Tools | Templates.
    }
    
}
