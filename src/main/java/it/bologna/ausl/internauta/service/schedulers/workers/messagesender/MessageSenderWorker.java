package it.bologna.ausl.internauta.service.schedulers.workers.messagesender;

import it.bologna.ausl.blackbox.exceptions.BlackBoxPermissionException;
import it.bologna.ausl.internauta.service.utils.InternautaConstants;
import it.bologna.ausl.internauta.service.utils.IntimusUtils;
import it.bologna.ausl.internauta.service.utils.ParametriAziende;
import it.bologna.ausl.model.entities.configuration.ParametroAziende;
import it.bologna.ausl.model.entities.messaggero.AmministrazioneMessaggio;
import java.util.List;
import java.util.logging.Level;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.stereotype.Component;

/**
 *
 * @author gdm
 */
@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class MessageSenderWorker implements Runnable {
    private static final Logger log = LoggerFactory.getLogger(MessageSenderWorker.class);

    /*
    {
	"dest": [{
                    "id_persona": row["id_persona"],
                    "id_aziende": [],
                    "apps": ["scrivania"]
		}
	],
	"command": {
            "params": {
                "id_attivita": row["id"],
                "operation": TD["event"]
            },
            "command": "RefreshAttivita"
	}
    }
    */
    
    @Autowired
    private IntimusUtils intimusUtils;


    private AmministrazioneMessaggio message;
    
    public void setMessaggio(AmministrazioneMessaggio message) {
        this.message = message;
    }
    
    @Override
    public void run() {
        log.info(" in run di " + getClass().getSimpleName() + "con message: " + message.toString());
        sendSendMessageIntimusCommand();
    }
    
    public void sendSendMessageIntimusCommand() {
        try {
            IntimusUtils.IntimusCommand command = intimusUtils.buildIntimusShowMessageCommand(message);
            log.info("invio comando intimus: " + command.buildIntimusStringCommand()+ "...");
            intimusUtils.sendCommand(command);
            log.info("comando inviato");
        } catch (Exception ex) {
            log.error("Errore nel lancio del messaggio: " + message.getId(), ex);
        }
    }
    
}
