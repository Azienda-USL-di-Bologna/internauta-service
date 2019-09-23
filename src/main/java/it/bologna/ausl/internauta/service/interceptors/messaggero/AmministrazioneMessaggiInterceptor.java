package it.bologna.ausl.internauta.service.interceptors.messaggero;

import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.dsl.BooleanExpression;
import it.bologna.ausl.internauta.service.interceptors.InternautaBaseInterceptor;
import it.bologna.ausl.internauta.service.repositories.baborg.PersonaRepository;
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
import it.bologna.ausl.internauta.service.schedulers.workers.messagesender.MessageSeenCleanerWorker;
import it.bologna.ausl.internauta.service.schedulers.workers.messagesender.MessageThreadEvent;
import it.bologna.ausl.internauta.service.schedulers.workers.messagesender.MessageThreadEventListener;
import it.nextsw.common.interceptors.exceptions.SkipDeleteInterceptorException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;

/**
 *
 * @author Giuseppe Russo <g.russo@nsi.it> with GDM and Gus collaboration
 */
@Component
@NextSdrInterceptor(name = "amministrazionemessaggi-interceptor")
public class AmministrazioneMessaggiInterceptor extends InternautaBaseInterceptor {
    private static final Logger LOGGER = LoggerFactory.getLogger(AmministrazioneMessaggiInterceptor.class);

//    @Autowired
//    private MessageSenderManager messageSenderManager;
    
    @Autowired
    private ApplicationEventPublisher applicationEventPublisher;
    
//    @Autowired
//    private PersonaRepository personaRepository;
    
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
//            messageSenderManager.scheduleMessageSender(amministrazioneMessaggio, LocalDateTime.now());
            MessageThreadEvent messageThreadEvent = new MessageThreadEvent(amministrazioneMessaggio, MessageThreadEvent.InterceptorPhase.AFTER_INSERT);
//            messageThreadEventListener.scheduleMessageThreadEvent(messageThreadEvent);
            applicationEventPublisher.publishEvent(messageThreadEvent);
        }
        return super.afterCreateEntityInterceptor(entity, additionalData, request, mainEntity, projectionClass);
    }

    @Override
    public Object afterUpdateEntityInterceptor(Object entity, Object beforeUpdateEntity, Map<String, String> additionalData, HttpServletRequest request, boolean mainEntity, Class projectionClass) throws AbortSaveInterceptorException {
        AmministrazioneMessaggio amministrazioneMessaggio = (AmministrazioneMessaggio) entity;
        if (mainEntity && amministrazioneMessaggio.getInvasivita() == AmministrazioneMessaggio.InvasivitaEnum.POPUP) {
//            LocalDateTime now = LocalDateTime.now();
//            MessageSeenCleanerWorker.cleanSeenFromPersone(amministrazioneMessaggio.getId(), personaRepository);
//            if (amministrazioneMessaggio.getInvasivita() == AmministrazioneMessaggio.InvasivitaEnum.POPUP) {
//                messageSenderManager.scheduleMessageSender(amministrazioneMessaggio, now);
//            }
            MessageThreadEvent messageThreadEvent = new MessageThreadEvent(amministrazioneMessaggio, MessageThreadEvent.InterceptorPhase.AFTER_UPDATE);
//            messageThreadEventListener.scheduleMessageThreadEvent(messageThreadEvent);
            applicationEventPublisher.publishEvent(messageThreadEvent);
        }
        return super.afterUpdateEntityInterceptor(entity, beforeUpdateEntity, additionalData, request, mainEntity, projectionClass);
    }

//    @Override
//    public void afterDeleteEntityInterceptor(Object entity, Map<String, String> additionalData, HttpServletRequest request, boolean mainEntity, Class projectionClass) throws AbortSaveInterceptorException, SkipDeleteInterceptorException {
//        AmministrazioneMessaggio amministrazioneMessaggio = (AmministrazioneMessaggio) entity;
//        if (mainEntity) {
//            MessageSeenCleanerWorker.cleanSeenFromPersone(amministrazioneMessaggio.getId(), personaRepository);
//        }
//    }
    
}