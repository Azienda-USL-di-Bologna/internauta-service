package it.bologna.ausl.internauta.service.interceptors.configurazione;

import com.querydsl.core.types.Predicate;
import it.bologna.ausl.internauta.service.interceptors.InternautaBaseInterceptor;
import it.bologna.ausl.internauta.service.interceptors.baborg.AziendaInterceptor;
import it.bologna.ausl.model.entities.configurazione.ParametroAziende;
import it.bologna.ausl.model.entities.configurazione.QParametroAziende;
import it.nextsw.common.data.annotations.NextSdrInterceptor;
import it.nextsw.common.interceptors.exceptions.AbortLoadInterceptorException;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 *
 * @author gdm
 */
@Component
@NextSdrInterceptor(name = "parametro-aziende-interceptor")
public class ParametroAziendeInterceptor  extends InternautaBaseInterceptor {

    private static final Logger LOGGER = LoggerFactory.getLogger(ParametroAziendeInterceptor.class);

    @Override
    public Class getTargetEntityClass() {
        return ParametroAziende.class;
    }

    @Override
    public Predicate beforeSelectQueryInterceptor(Predicate initialPredicate, Map<String, String> additionalData, HttpServletRequest request, boolean mainEntity, Class projectionClass) throws AbortLoadInterceptorException {
        return QParametroAziende.parametroAziende.hideFromApi.eq(false).and(initialPredicate);
    }
}
