package it.bologna.ausl.internauta.service.schedulers.workers.messagesender;

import it.bologna.ausl.internauta.service.repositories.baborg.PersonaRepository;
import it.bologna.ausl.internauta.service.schedulers.MessageSenderManager;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 *
 * @author gdm
 */
@Component
public class MessageThreadEventListener  {
    private static final Logger LOGGER = LoggerFactory.getLogger(MessageThreadEventListener.class);
    
    @Autowired
    private MessageSenderManager messageSenderManager;
    
    @Autowired
    private PersonaRepository personaRepository;
    
//    @Async
    @EventListener(MessageThreadEvent.class)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void scheduleMessageThreadEvent(MessageThreadEvent event) {
        LOGGER.info("in scheduleMessageThreadEvent con event: " + event.toString());
        ZonedDateTime now = ZonedDateTime.now();
        switch (event.getInterceptorPhase()) {
            case AFTER_INSERT:
                messageSenderManager.scheduleMessageSender(event.getAmministrazioneMessaggio(), now);
            break;
            case AFTER_UPDATE:
                messageSenderManager.scheduleMessageSender(event.getAmministrazioneMessaggio(), now);
            break;
        }
    }
}
