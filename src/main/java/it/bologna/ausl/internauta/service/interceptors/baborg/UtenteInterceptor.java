package it.bologna.ausl.internauta.service.interceptors.baborg;

import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.Expressions;
import it.bologna.ausl.blackbox.PermissionManager;
import it.bologna.ausl.blackbox.exceptions.BlackBoxPermissionException;
import it.bologna.ausl.internauta.service.authorization.AuthenticatedSessionData;
import it.bologna.ausl.internauta.service.authorization.UserInfoService;
import it.bologna.ausl.internauta.service.interceptors.InternautaBaseInterceptor;
import it.bologna.ausl.internauta.service.repositories.baborg.PersonaRepository;
import it.bologna.ausl.internauta.service.utils.InternautaConstants.AdditionalData;
import it.bologna.ausl.model.entities.baborg.QUtente;
import it.bologna.ausl.model.entities.baborg.Utente;
import it.nextsw.common.annotations.NextSdrInterceptor;
import it.nextsw.common.interceptors.exceptions.AbortLoadInterceptorException;
import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 *
 * @author salo
 */
@Component
@NextSdrInterceptor(name = "utente-interceptor")
@Order(1)
public class UtenteInterceptor extends InternautaBaseInterceptor {

    private static final Logger LOGGER = LoggerFactory.getLogger(UtenteInterceptor.class);

    @Autowired
    PermissionManager permissionManager;

    @Autowired
    PersonaRepository personaRepository;

    @Autowired
    UserInfoService userInfoService;

    @Override
    public Class getTargetEntityClass() {
        return Utente.class;
    }

    @Override
    public Predicate beforeSelectQueryInterceptor(Predicate initialPredicate, Map<String, String> additionalData, HttpServletRequest request, boolean mainEntity, Class projectionClass) throws AbortLoadInterceptorException {
        AuthenticatedSessionData authenticatedSessionData = getAuthenticatedUserProperties();
        List<AdditionalData.OperationsRequested> operationsRequested = AdditionalData.getOperationRequested(AdditionalData.Keys.OperationRequested, additionalData);
        if (operationsRequested != null && !operationsRequested.isEmpty()) {
            for (AdditionalData.OperationsRequested operationRequested : operationsRequested) {
                switch (operationRequested) {
                    case CambioUtente:
                        Utente utente;
                        if (authenticatedSessionData.getRealUser() != null) {
                            utente = authenticatedSessionData.getRealUser();
                        } else {
                            utente = authenticatedSessionData.getUser();
                        }

                        if (!isSD(utente)) {
                            try {
                                // Devo controlalre se sono DELEGATO. Chiedo alla black-box
                                List<Integer> idUtentiDelega = userInfoService.getPermessiDelega(utente);
                                if (idUtentiDelega != null && idUtentiDelega.size() > 0) {
                                    BooleanExpression filterUtentiDelega = QUtente.utente.id.in(idUtentiDelega);
                                    initialPredicate = filterUtentiDelega.and(initialPredicate);
                                } else {
                                    initialPredicate = Expressions.FALSE.eq(true);
                                }
                            } catch (BlackBoxPermissionException ex) {
                                throw new AbortLoadInterceptorException(ex);
                            }
                            break;
                        }
                }
            }
        }
        return initialPredicate;
    }
}
