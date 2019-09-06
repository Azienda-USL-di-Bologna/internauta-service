package it.bologna.ausl.internauta.service.interceptors.messaggero;

import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.dsl.BooleanExpression;
import it.bologna.ausl.internauta.service.interceptors.InternautaBaseInterceptor;
import it.bologna.ausl.internauta.service.utils.InternautaConstants;
import it.bologna.ausl.model.entities.messaggero.AmministrazioneMessaggio;
import it.bologna.ausl.model.entities.messaggero.QAmministrazioneMessaggio;
import it.nextsw.common.annotations.NextSdrInterceptor;
import it.nextsw.common.interceptors.exceptions.AbortLoadInterceptorException;
import it.nextsw.common.interceptors.exceptions.AbortSaveInterceptorException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import it.bologna.ausl.internauta.service.schedulers.MessageSenderManager;
import it.nextsw.common.interceptors.exceptions.SkipDeleteInterceptorException;
import org.springframework.beans.factory.annotation.Autowired;

/**
 *
 * @author Giuseppe Russo <g.russo@nsi.it> with GDM and Gus collaboration
 */
@Component
@NextSdrInterceptor(name = "amministrazionemessaggi-interceptor")
public class AmministrazioneMessaggiInterceptor extends InternautaBaseInterceptor {
    private static final Logger LOGGER = LoggerFactory.getLogger(AmministrazioneMessaggiInterceptor.class);

    @Autowired
    private MessageSenderManager messageSenderManager;
    
    @Override
    public Class getTargetEntityClass() {
        return AmministrazioneMessaggio.class;
    }

    @Override
    public Predicate beforeSelectQueryInterceptor(Predicate initialPredicate, Map<String, String> additionalData, HttpServletRequest request, boolean mainEntity, Class projectionClass) throws AbortLoadInterceptorException {
        BooleanExpression filtroAmministrazioneMessaggi = null;

        List<InternautaConstants.AdditionalData.OperationsRequested> operationsRequested = InternautaConstants.AdditionalData.getOperationRequested(InternautaConstants.AdditionalData.Keys.OperationRequested, additionalData);
        if (operationsRequested != null && !operationsRequested.isEmpty()) {
            for (InternautaConstants.AdditionalData.OperationsRequested operationRequested : operationsRequested) {
                switch (operationRequested) {
                    case GetAmministrazioneMessaggiAttivi:
                        filtroAmministrazioneMessaggi = QAmministrazioneMessaggio.amministrazioneMessaggio.dataScadenza.isNull()
                                .or( QAmministrazioneMessaggio.amministrazioneMessaggio.dataScadenza.gt(LocalDateTime.now()));
                        break;
                    case GetAmministrazioneMessaggiStorico:
                        filtroAmministrazioneMessaggi = QAmministrazioneMessaggio.amministrazioneMessaggio.dataScadenza.loe(LocalDateTime.now());
                        break;
               }
           }
        }
        return filtroAmministrazioneMessaggi != null ? filtroAmministrazioneMessaggi.and(initialPredicate) : initialPredicate;
    }

    @Override
    public Object afterCreateEntityInterceptor(Object entity, Map<String, String> additionalData, HttpServletRequest request, boolean mainEntity, Class projectionClass) throws AbortSaveInterceptorException {
        AmministrazioneMessaggio amministrazioneMessaggio = (AmministrazioneMessaggio) entity;
        if (mainEntity && amministrazioneMessaggio.getInvasivita() == AmministrazioneMessaggio.InvasivitaEnum.POPUP) {
            messageSenderManager.scheduleMessage(amministrazioneMessaggio, LocalDateTime.now());
        }
        return super.afterCreateEntityInterceptor(entity, additionalData, request, mainEntity, projectionClass);
    }

    @Override
    public Object afterUpdateEntityInterceptor(Object entity, Object beforeUpdateEntity, Map<String, String> additionalData, HttpServletRequest request, boolean mainEntity, Class projectionClass) throws AbortSaveInterceptorException {
        AmministrazioneMessaggio amministrazioneMessaggio = (AmministrazioneMessaggio) entity;
        if (mainEntity && amministrazioneMessaggio.getInvasivita() == AmministrazioneMessaggio.InvasivitaEnum.POPUP) {
            messageSenderManager.scheduleMessage(amministrazioneMessaggio, LocalDateTime.now());
        }
        return super.afterUpdateEntityInterceptor(entity, beforeUpdateEntity, additionalData, request, mainEntity, projectionClass); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void afterDeleteEntityInterceptor(Object entity, Map<String, String> additionalData, HttpServletRequest request, boolean mainEntity, Class projectionClass) throws AbortSaveInterceptorException, SkipDeleteInterceptorException {
        AmministrazioneMessaggio amministrazioneMessaggio = (AmministrazioneMessaggio) entity;
        if (mainEntity && amministrazioneMessaggio.getInvasivita() == AmministrazioneMessaggio.InvasivitaEnum.POPUP) {
            messageSenderManager.stopSchedule(amministrazioneMessaggio);
        }
    }
    
    
}