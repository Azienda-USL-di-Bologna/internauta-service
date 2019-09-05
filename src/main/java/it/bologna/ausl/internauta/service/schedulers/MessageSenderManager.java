package it.bologna.ausl.internauta.service.schedulers;

import com.querydsl.core.types.dsl.BooleanExpression;
import it.bologna.ausl.internauta.service.repositories.messaggero.AmministrazioneMessaggioRepository;
import it.bologna.ausl.internauta.service.schedulers.workers.messagesender.MessageSenderWorker;
import it.bologna.ausl.model.entities.messaggero.AmministrazioneMessaggio;
import it.bologna.ausl.model.entities.messaggero.QAmministrazioneMessaggio;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;
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
    private AmministrazioneMessaggioRepository amministrazioneMessaggioRepository;
    
    @Autowired
    ScheduledThreadPoolExecutor scheduledThreadPoolExecutor;
    
    @Autowired
    BeanFactory beanFactory;
    
    private final Map<Integer, ScheduledFuture> messageThreadsMap = new HashMap();

    public Map<Integer, ScheduledFuture> getMessageThreadsMap() {
        return messageThreadsMap;
    }

    public void scheduleNotExpired() {
        LocalDateTime now = LocalDateTime.now();
        
        BooleanExpression onlyPopup = QAmministrazioneMessaggio.amministrazioneMessaggio.invasivita.eq(AmministrazioneMessaggio.InvasivitaEnum.POPUP.toString());
        
        BooleanExpression notStartedFilter = 
                (QAmministrazioneMessaggio.amministrazioneMessaggio.dataScadenza.after(now).or(QAmministrazioneMessaggio.amministrazioneMessaggio.dataScadenza.isNull())).and
                (QAmministrazioneMessaggio.amministrazioneMessaggio.dataPubblicazione.after(now));
        
        BooleanExpression startedWithInterval = 
                (QAmministrazioneMessaggio.amministrazioneMessaggio.dataScadenza.after(now).or(QAmministrazioneMessaggio.amministrazioneMessaggio.dataScadenza.isNull())).and
                (QAmministrazioneMessaggio.amministrazioneMessaggio.dataPubblicazione.before(now)).and
                (QAmministrazioneMessaggio.amministrazioneMessaggio.tipologia.eq(AmministrazioneMessaggio.TipologiaEnum.RIPRESENTA_CON_INTERVALLO.toString()));
        Iterable<AmministrazioneMessaggio> activeMessages = amministrazioneMessaggioRepository.findAll(onlyPopup.and(notStartedFilter.or(startedWithInterval)));
        for (AmministrazioneMessaggio activeMessage: activeMessages) {
            scheduleMessage(activeMessage, now);
        }
    }
    
    public ScheduledFuture<?> scheduleMessage(AmministrazioneMessaggio message, LocalDateTime now) {
        if (message.getDataScadenza() == null || message.getDataScadenza().isAfter(now)) {
            if (this.messageThreadsMap.get(message.getId()) != null) {
                ScheduledFuture scheduledMessage = this.messageThreadsMap.get(message.getId());
                scheduledMessage.cancel(false);
                this.messageThreadsMap.remove(message.getId());
            }
            MessageSenderWorker messageSenderWorker = beanFactory.getBean(MessageSenderWorker.class);
            messageSenderWorker.setMessaggio(message);
            LocalDateTime dataPubblicazione = message.getDataPubblicazione();
            long initialDelayMillis = 0;
            long perdiodMillis = 0;
            if (dataPubblicazione.isAfter(now)) {
                initialDelayMillis = ChronoUnit.MILLIS.between(now, dataPubblicazione);
            }
            if (message.getTipologia() == AmministrazioneMessaggio.TipologiaEnum.RIPRESENTA_CON_INTERVALLO) {
                perdiodMillis = message.getIntervallo() * 60 * 1000;
            }
            ScheduledFuture<?> schedule;
            log.info("schedulazione " + message.toString() + " initialMillis: " + initialDelayMillis + "perdiodMillis: " + perdiodMillis);
            if (perdiodMillis == 0) {
                schedule = scheduledThreadPoolExecutor.schedule(messageSenderWorker, initialDelayMillis, TimeUnit.MILLISECONDS);
            } else {
                schedule = scheduledThreadPoolExecutor.scheduleAtFixedRate(messageSenderWorker, initialDelayMillis, perdiodMillis, TimeUnit.MILLISECONDS);
            }
            this.messageThreadsMap.put(message.getId(), schedule);
            return schedule;
        } else {
            return null;
        }
    }
    
    public void stopSchedule(AmministrazioneMessaggio message) {
        if (this.messageThreadsMap.get(message.getId()) != null) {
            ScheduledFuture scheduledMessage = this.messageThreadsMap.get(message.getId());
            scheduledMessage.cancel(false);
            this.messageThreadsMap.remove(message.getId());
        }
    }
}
