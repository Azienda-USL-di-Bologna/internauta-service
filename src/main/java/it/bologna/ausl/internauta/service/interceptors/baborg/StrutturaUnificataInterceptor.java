package it.bologna.ausl.internauta.service.interceptors.baborg;

import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.Expressions;
import it.bologna.ausl.internauta.service.authorization.AuthenticatedSessionData;
import it.bologna.ausl.internauta.service.interceptors.InternautaBaseInterceptor;
import it.bologna.ausl.model.entities.baborg.QStrutturaUnificata;
import it.bologna.ausl.model.entities.baborg.StrutturaUnificata;
import it.nextsw.common.annotations.NextSdrInterceptor;
import it.nextsw.common.interceptors.exceptions.AbortSaveInterceptorException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 *
 * @author gus and zolo
 */
@Component
@NextSdrInterceptor(name = "struttura-unificata-interceptor")
public class StrutturaUnificataInterceptor extends InternautaBaseInterceptor {
    private static final Logger LOGGER = LoggerFactory.getLogger(StrutturaUnificataInterceptor.class);
    
    private static final String GET_DATA_BY_STATO = "getDataByStato";

    private static enum Stati {
        Bozza, Corrente, Storico
    };


    @Override
    public Class getTargetEntityClass() {
        return StrutturaUnificata.class;
    }

    @Override
    public Predicate beforeSelectQueryInterceptor(Predicate initialPredicate, Map<String, String> additionalData, HttpServletRequest request, boolean mainEntity, Class projectionClass) {
        LOGGER.info("in: beforeSelectQueryInterceptor di Struttura-Unificata");

        String getDataByStatoValue = additionalData.get(GET_DATA_BY_STATO);
        

        if (getDataByStatoValue != null) {
            LocalDateTime today = LocalDate.now().atTime(0, 0);
            QStrutturaUnificata strutturaUnificata = QStrutturaUnificata.strutturaUnificata;
//            AuthenticatedSessionData authenticatedSessionData = getAuthenticatedUserProperties();

            BooleanExpression customFilter;
            if (getDataByStatoValue.equals(Stati.Bozza.toString())) {
                /*  La bozza ha la data disattivazione a null.
                    La data attivazione può essere null.
                    Se non è null deve essere maggiore di oggi
                    oppure può essere minore/uguale ad oggi purché la data accensione ativazione sia null sia disattivo.  */
                customFilter = strutturaUnificata.dataDisattivazione.isNull()
                        .and(strutturaUnificata.dataAttivazione.isNull()
                                .or(strutturaUnificata.dataAttivazione.isNotNull().and(strutturaUnificata.dataAttivazione.gt(today))
                                        .or(strutturaUnificata.dataAttivazione.isNotNull().and(strutturaUnificata.dataAttivazione.loe(today)).and(strutturaUnificata.dataAccensioneAttivazione.isNull()))));

                initialPredicate = customFilter.and(initialPredicate);

            } else if (getDataByStatoValue.equals(Stati.Corrente.toString())) {
                customFilter = strutturaUnificata.dataDisattivazione.isNull()
                        .or(strutturaUnificata.dataDisattivazione.isNotNull().and(strutturaUnificata.dataDisattivazione.gt(today)))
                        .and(strutturaUnificata.dataAttivazione.isNotNull()
                                .and(strutturaUnificata.dataAttivazione.loe(today)
                                        .and(strutturaUnificata.dataAccensioneAttivazione.isNotNull())));

                initialPredicate = customFilter.and(initialPredicate);

            } else if (getDataByStatoValue.equals(Stati.Storico.toString())) {
                customFilter = strutturaUnificata.dataDisattivazione.isNotNull()
                        .and(strutturaUnificata.dataDisattivazione.loe(today));

                initialPredicate = customFilter.and(initialPredicate);

            } else {
                customFilter = Expressions.asBoolean(true).isFalse();
                initialPredicate = customFilter.and(initialPredicate);
            }
        }
        return initialPredicate;
    }

    @Override
    public Object beforeCreateEntityInterceptor(Object entity, Map<String, String> additionalData, HttpServletRequest request, boolean mainEntity, Class projectionClass) throws AbortSaveInterceptorException {
        LOGGER.info("in: beforeCreateEntityInterceptor di Struttura-Unificata");
        AuthenticatedSessionData authenticatedSessionData = getAuthenticatedUserProperties();
        if (!isCI(authenticatedSessionData.getUser())) {
            throw new AbortSaveInterceptorException();
        }

        return entity;
    }

    @Override
    public Object beforeUpdateEntityInterceptor(Object entity, Object beforeUpdateEntity, Map<String, String> additionalData, HttpServletRequest request, boolean mainEntity, Class projectionClass) throws AbortSaveInterceptorException {
        LOGGER.info("in: beforeUpdateEntityInterceptor di Struttura-Unificata");
        AuthenticatedSessionData authenticatedSessionData = getAuthenticatedUserProperties();
        if (!isCI(authenticatedSessionData.getUser())) {
            throw new AbortSaveInterceptorException();
        }

        return entity;
    }
   
}