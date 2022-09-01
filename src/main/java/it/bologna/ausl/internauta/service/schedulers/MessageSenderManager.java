package it.bologna.ausl.internauta.service.schedulers;

import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.JPAExpressions;
import it.bologna.ausl.internauta.service.repositories.baborg.PersonaRepository;
import it.bologna.ausl.internauta.service.repositories.messaggero.AmministrazioneMessaggioRepository;
import it.bologna.ausl.internauta.service.schedulers.workers.messagesender.MessageSeenCleanerWorker;
import it.bologna.ausl.internauta.service.schedulers.workers.messagesender.MessageSenderWorker;
import it.bologna.ausl.internauta.service.utils.RedisSharedDataManager;
import it.bologna.ausl.model.entities.baborg.QPersona;
import it.bologna.ausl.model.entities.messaggero.AmministrazioneMessaggio;
import it.bologna.ausl.model.entities.messaggero.QAmministrazioneMessaggio;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 *
 * @author gdm
 */
@Component
public class MessageSenderManager {
    private static final Logger log = LoggerFactory.getLogger(MessageSenderManager.class);
    
    @Autowired
    private ScheduledThreadPoolExecutor scheduledThreadPoolExecutor;
    
    @Autowired
    private BeanFactory beanFactory;
    
    @Autowired
    private RedisSharedDataManager redisSharedData;
    
    @Autowired
    private AmministrazioneMessaggioRepository amministrazioneMessaggioRepository;
    
    @Autowired
    PersonaRepository personaRepository;

    private final String MESSAGE_THREAD_MAP_NAME = "messageThreadsMap";
    
//    private final Map<Integer, ScheduledFuture> messageThreadsMap = new HashMap();
//
//    public Map<Integer, ScheduledFuture> getMessageThreadsMap() {
//        return messageThreadsMap;
//    }

    public void scheduleMessageSenderAtBoot(ZonedDateTime now) {
        
        BooleanExpression onlyPopup = QAmministrazioneMessaggio.amministrazioneMessaggio.invasivita.eq(AmministrazioneMessaggio.InvasivitaEnum.POPUP.toString());
        
        BooleanExpression notStartedFilter = 
                (QAmministrazioneMessaggio.amministrazioneMessaggio.dataScadenza.isNull().or(QAmministrazioneMessaggio.amministrazioneMessaggio.dataScadenza.after(now))).and
                (QAmministrazioneMessaggio.amministrazioneMessaggio.dataPubblicazione.after(now));
        
        BooleanExpression startedWithInterval = 
                (QAmministrazioneMessaggio.amministrazioneMessaggio.dataScadenza.after(now).or(QAmministrazioneMessaggio.amministrazioneMessaggio.dataScadenza.isNull())).and
                (QAmministrazioneMessaggio.amministrazioneMessaggio.dataPubblicazione.before(now)).and
                (QAmministrazioneMessaggio.amministrazioneMessaggio.tipologia.eq(AmministrazioneMessaggio.TipologiaEnum.RIPRESENTA_CON_INTERVALLO.toString()));
        Iterable<AmministrazioneMessaggio> activeMessages = amministrazioneMessaggioRepository.findAll(onlyPopup.and(notStartedFilter.or(startedWithInterval)));
        for (AmministrazioneMessaggio activeMessage: activeMessages) {
            this.scheduleMessageSender(activeMessage, now);
        }
    }
    
    public void scheduleSeenCleanerAtBoot(ZonedDateTime now) {
        
        BooleanExpression notCleand = JPAExpressions.selectFrom(QPersona.persona)
                .where(Expressions.booleanTemplate("arraycontains({0}, tools.string_to_integer_array(cast({1} as text), ','))=true", QPersona.persona.messaggiVisti, QAmministrazioneMessaggio.amministrazioneMessaggio.id))
                .exists();
        
        BooleanExpression withExpireDate = QAmministrazioneMessaggio.amministrazioneMessaggio.dataScadenza.isNotNull();
        
        BooleanExpression expiredNotCleaned = 
                QAmministrazioneMessaggio.amministrazioneMessaggio.dataScadenza.before(now).and(notCleand);
        BooleanExpression notExpired = 
                QAmministrazioneMessaggio.amministrazioneMessaggio.dataScadenza.after(now);
        
        BooleanExpression toCleanFilter = withExpireDate.and(expiredNotCleaned.or(notExpired));
        
        Iterable<AmministrazioneMessaggio> messages = amministrazioneMessaggioRepository.findAll(toCleanFilter);
        for (AmministrazioneMessaggio message: messages) {
            scheduleSeenCleaner(message, now);
        }
    }
    
//    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public ScheduledFuture<?> scheduleMessageSender(AmministrazioneMessaggio message, ZonedDateTime now) {
        if (message.getDataScadenza() == null || message.getDataScadenza().isAfter(now)) {
            MessageSenderWorker messageSenderWorker = beanFactory.getBean(MessageSenderWorker.class);
            messageSenderWorker.setMessaggio(message);
            ZonedDateTime dataPubblicazione = message.getDataPubblicazione();
            long initialDelayMillis = 1000;
            long perdiodMillis = 0;
            if (dataPubblicazione.isAfter(now)) {
                initialDelayMillis = ChronoUnit.MILLIS.between(now, dataPubblicazione);
            }
            if (message.getTipologia() == AmministrazioneMessaggio.TipologiaEnum.RIPRESENTA_CON_INTERVALLO) {
                perdiodMillis = message.getIntervallo() * 60 * 1000;
            }
            ScheduledFuture<?> schedule;
            log.info("schedulazione " + message.toString() + " initialMillis: " + initialDelayMillis + " perdiodMillis: " + perdiodMillis);
            if (perdiodMillis == 0) {
                schedule = scheduledThreadPoolExecutor.schedule(messageSenderWorker, initialDelayMillis, TimeUnit.MILLISECONDS);
            } else {
                schedule = scheduledThreadPoolExecutor.scheduleAtFixedRate(messageSenderWorker, initialDelayMillis, perdiodMillis, TimeUnit.MILLISECONDS);
            }
            messageSenderWorker.setScheduleObject(schedule);
            return schedule;
        } else {
            return null;
        }
    }
    
    public ScheduledFuture<?> scheduleSeenCleaner(AmministrazioneMessaggio message, ZonedDateTime now) {
        ScheduledFuture<?> schedule = null;
        if (message.getDataScadenza() != null) {
            MessageSeenCleanerWorker messageSeenCleanerWorker = beanFactory.getBean(MessageSeenCleanerWorker.class);
            messageSeenCleanerWorker.setMessaggio(message);
            long initialDelayMillis = ChronoUnit.MILLIS.between(now, message.getDataScadenza());
            schedule = scheduledThreadPoolExecutor.schedule(messageSeenCleanerWorker, initialDelayMillis, TimeUnit.MILLISECONDS);
            messageSeenCleanerWorker.setScheduleObject(schedule);
        }
        return schedule;
    }
    
//    public void stopSchedule(AmministrazioneMessaggio message) {
//        if (this.redisSharedData.get(MESSAGE_THREAD_MAP_NAME, message.getId()) != null) {
//            ScheduledFuture scheduledMessage = this.redisSharedData.get(MESSAGE_THREAD_MAP_NAME, message.getId());
//            scheduledMessage.cancel(false);
//            this.redisSharedData.remove(MESSAGE_THREAD_MAP_NAME, message.getId());
//            purgeSeenFromPersone(message);
//        }
//    }

}
