package it.bologna.ausl.internauta.service.schedulers.workers.messagesender;

import com.google.common.collect.Lists;
import com.querydsl.core.types.dsl.BooleanTemplate;
import com.querydsl.core.types.dsl.Expressions;
import it.bologna.ausl.internauta.service.repositories.baborg.PersonaRepository;
import it.bologna.ausl.internauta.service.repositories.messaggero.AmministrazioneMessaggioRepository;
import it.bologna.ausl.model.entities.baborg.Persona;
import it.bologna.ausl.model.entities.baborg.QPersona;
import it.bologna.ausl.model.entities.messaggero.AmministrazioneMessaggio;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.ScheduledFuture;
import java.util.logging.Level;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 *
 * @author gdm
 */
@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class MessageSeenCleanerWorker implements Runnable {
    private static final Logger log = LoggerFactory.getLogger(MessageSeenCleanerWorker.class);

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
    public void run() {
        log.info(" in run di " + getClass().getSimpleName() + "con message: " + message.toString());
        if (isActive()) {
            MessageSeenCleanerWorker.cleanSeenFromPersone(message.getId(), personaRepository);
        } else {
            scheduleObject.cancel(true);
//            MessageSenderWorker.purgeSeenFromPersone(message.getId(), personaRepository);
        }
    }

    private Boolean isActive() {
        Optional<AmministrazioneMessaggio> messageOp = amministrazioneMessaggioRepository.findById(message.getId());
        return messageOp.isPresent() && messageOp.get().getVersion().truncatedTo(ChronoUnit.MILLIS).equals(message.getVersion().truncatedTo(ChronoUnit.MILLIS)) && messageOp.get().getDataScadenza() != null && messageOp.get().getDataScadenza().isBefore(LocalDateTime.now());
    }

    public static void cleanSeenFromPersone(Integer messageId, PersonaRepository personaRepository) {
        BooleanTemplate whoAsSeenThisMessage = Expressions.booleanTemplate("arraycontains({0}, tools.string_to_integer_array({1}, ','))=true", QPersona.persona.messaggiVisti, String.valueOf(messageId));
        Iterable<Persona> persons = personaRepository.findAll(whoAsSeenThisMessage);
        for (Persona person : persons) {
            List<Integer> seenMessages = Lists.newArrayList(person.getMessaggiVisti());
            seenMessages.remove(messageId);
//             personaRepository.updateSeenMessage(messageId, org.apache.commons.lang3.StringUtils.join(seenMessages.toArray(new Integer[0]), ","), "gdmDario");
            person.setMessaggiVisti(seenMessages.toArray(new Integer[0]));
            boolean saved = false;
            while (!saved) {
                try {
                    personaRepository.saveAndFlush(person);
                    saved = true;
                } catch (Throwable t) {
                    try {
                        Thread.sleep(new Random(System.currentTimeMillis()).nextInt(1000) + 1);
                    } catch (InterruptedException ex) {
                        java.util.logging.Logger.getLogger(MessageSeenCleanerWorker.class.getName()).log(Level.SEVERE, null, ex);
                    }
                    log.error("aaaaaaaaaaaaaaaaaaaa", t);
                }
            }
        }
        //personaRepository.saveAll(persons);
    }
}
