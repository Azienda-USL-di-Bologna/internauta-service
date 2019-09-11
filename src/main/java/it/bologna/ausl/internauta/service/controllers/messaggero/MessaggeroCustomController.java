package it.bologna.ausl.internauta.service.controllers.messaggero;

import com.fasterxml.jackson.core.JsonProcessingException;
import it.bologna.ausl.blackbox.exceptions.BlackBoxPermissionException;
import it.bologna.ausl.internauta.service.authorization.AuthenticatedSessionData;
import it.bologna.ausl.internauta.service.authorization.AuthenticatedSessionDataBuilder;
import it.bologna.ausl.internauta.service.repositories.baborg.PersonaRepository;
import it.bologna.ausl.model.entities.baborg.Persona;
import it.nextsw.common.controller.exceptions.NotFoundResourceException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = "${messaggero.mapping.url.root}" + "/custom")
public class MessaggeroCustomController {
    private static final Logger LOGGER = LoggerFactory.getLogger(MessaggeroCustomController.class);

    @Autowired
    private PersonaRepository personaRepository;

    @Autowired
    private AuthenticatedSessionDataBuilder authenticatedSessionDataBuilder;

    @RequestMapping(value = "setMessaggiVisti", method = RequestMethod.POST)
    @Transactional(rollbackFor = {Throwable.class})
    public void setMessaggiVisti(@RequestBody List<Integer> messaggiVisti) throws JsonProcessingException, BlackBoxPermissionException, NotFoundResourceException {
        AuthenticatedSessionData authenticatedUserProperties = authenticatedSessionDataBuilder.getAuthenticatedUserProperties();
        Persona person;
//        Utente user;
        if (authenticatedUserProperties.getRealPerson() != null) {
            LOGGER.info("si real user");
            person = authenticatedUserProperties.getRealPerson();
//            user = authenticatedUserProperties.getRealUser();
        } else {
            LOGGER.info("no real user");
            person = authenticatedUserProperties.getPerson();
//            user = authenticatedUserProperties.getUser();
        }
//        Applicazione.Applicazioni applicazione = authenticatedUserProperties.getApplicazione();
        LOGGER.info(String.format("person: %s", person.getId()));
//        LOGGER.info(String.format("user: %s", objectMapper.writeValueAsString(user)));
//        LOGGER.info(String.format("applicazione: %s", applicazione.toString()));
        
        Optional<Persona> personaOp = this.personaRepository.findById(person.getId());
        Set<Integer> newMessaggiVisti = new HashSet<>();
        if (personaOp.isPresent()) {
            Persona personaToUpdate = personaOp.get();
            if (personaToUpdate.getMessaggiVisti() != null && personaToUpdate.getMessaggiVisti().length > 0) {
                List<Integer> previousMeggassiVisti = Arrays.asList(personaToUpdate.getMessaggiVisti());
                newMessaggiVisti.addAll(previousMeggassiVisti);
                newMessaggiVisti.addAll(messaggiVisti);
            } else {
                newMessaggiVisti.addAll(messaggiVisti);
            }
            personaToUpdate.setMessaggiVisti(newMessaggiVisti.toArray(new Integer[0]));
            personaRepository.save(personaToUpdate);
        }
        else {
            throw new NotFoundResourceException("persona non trovata");
        }
    }
}
