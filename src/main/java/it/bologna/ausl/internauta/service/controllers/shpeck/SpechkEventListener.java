package it.bologna.ausl.internauta.service.controllers.shpeck;

import it.bologna.ausl.blackbox.exceptions.BlackBoxPermissionException;
import it.bologna.ausl.internauta.service.authorization.AuthenticatedSessionData;
import it.bologna.ausl.internauta.service.authorization.AuthenticatedSessionDataBuilder;
import it.bologna.ausl.internauta.service.repositories.shpeck.MessageTagRepository;
import it.bologna.ausl.model.entities.baborg.Utente;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 *
 * @author gdm 
 */
@Component
public class SpechkEventListener {
    private static final Logger LOGGER = LoggerFactory.getLogger(SpechkEventListener.class);
    @Value("${intimus.redis.host}")
    private String intimusRedisHost;
    
    @Value("${intimus.redis.port}")
    private Integer intimusRedisPort;
    
    @Value("${intimus.redis.db}")
    private Integer intimusRedisDb;
    
    @Autowired
    private MessageTagRepository messageTagRepository;

    @Autowired
    AuthenticatedSessionDataBuilder authenticatedSessionDataBuilder;
    
    @EventListener(ShpeckEvent.class)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void scheduleMessageThreadEvent(ShpeckEvent event) throws BlackBoxPermissionException {
        LOGGER.info("in scheduleMessageThreadEvent con event: " + event.toString());
        AuthenticatedSessionData authenticatedUserProperties = authenticatedSessionDataBuilder.getAuthenticatedUserProperties();
        Utente user = authenticatedUserProperties.getUser();
        switch (event.getOperation()) {
            case SEND_CUSTOM_DELETE_INTIMUS_COMMAND:
                Map<String, Integer> data = (Map<String, Integer>) event.getData();
                messageTagRepository.sendCustomDeleteIntimusCommand(intimusRedisHost, intimusRedisPort, intimusRedisDb, user.getId(), "MESSAGE_TAG", data.get("idTag"), data.get("idMessage"));
            break;
        }
    }
}
