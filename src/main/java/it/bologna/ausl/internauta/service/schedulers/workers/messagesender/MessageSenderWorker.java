package it.bologna.ausl.internauta.service.schedulers.workers.messagesender;

import com.google.common.collect.Lists;
import com.querydsl.core.types.dsl.BooleanTemplate;
import com.querydsl.core.types.dsl.Expressions;
import it.bologna.ausl.internauta.service.repositories.baborg.PersonaRepository;
import it.bologna.ausl.internauta.service.repositories.messaggero.AmministrazioneMessaggioRepository;
import it.bologna.ausl.internauta.service.utils.IntimusUtils;
import it.bologna.ausl.model.entities.baborg.Persona;
import it.bologna.ausl.model.entities.baborg.QPersona;
import it.bologna.ausl.model.entities.messaggero.AmministrazioneMessaggio;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ScheduledFuture;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

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

    @Autowired
    private AmministrazioneMessaggioRepository amministrazioneMessaggioRepository;

    @Autowired
    PersonaRepository personaRepository;

    private AmministrazioneMessaggio message;

    private ScheduledFuture<?> scheduleObject;

    public void setMessaggio(AmministrazioneMessaggio message) {
        this.message = message;
    }

    public void setScheduleObject(ScheduledFuture<?> scheduleObject) {
        this.scheduleObject = scheduleObject;
    }

    @Override
    @Transactional
    public void run() {
        log.info(" in run di " + getClass().getSimpleName() + "con message: " + message.toString());
        LocalDateTime now = LocalDateTime.now();
        if (isActive(now)) {
            sendSendMessageIntimusCommand();
        } else {
            scheduleObject.cancel(true);
//            MessageSenderWorker.purgeSeenFromPersone(message.getId(), personaRepository);
        }
    }

    private Boolean isActive(LocalDateTime now) {
        Optional<AmministrazioneMessaggio> messageOp = amministrazioneMessaggioRepository.findById(message.getId());
        return messageOp.isPresent() && messageOp.get().getVersion().truncatedTo(ChronoUnit.MILLIS).equals(message.getVersion().truncatedTo(ChronoUnit.MILLIS)) && 
                (messageOp.get().getDataScadenza() == null || messageOp.get().getDataScadenza().isAfter(now));
    }

    public static void purgeSeenFromPersone(Integer messageId, PersonaRepository personaRepository) {
        BooleanTemplate whoAsSeenThisMessage = Expressions.booleanTemplate("arraycontains({0}, tools.string_to_integer_array({1}, ','))=true", QPersona.persona.messaggiVisti, String.valueOf(messageId));
        Iterable<Persona> persons = personaRepository.findAll(whoAsSeenThisMessage);
        for (Persona person : persons) {
            List<Integer> seenMessages = Lists.newArrayList(person.getMessaggiVisti());
            seenMessages.remove(messageId);
            person.setMessaggiVisti(seenMessages.toArray(new Integer[0]));
        }
        personaRepository.saveAll(persons);
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
