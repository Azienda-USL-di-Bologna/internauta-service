package it.bologna.ausl.internauta.service.controllers.messaggero;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.querydsl.core.types.dsl.BooleanExpression;
import it.bologna.ausl.blackbox.exceptions.BlackBoxPermissionException;
import it.bologna.ausl.internauta.service.authorization.AuthenticatedSessionData;
import it.bologna.ausl.internauta.service.authorization.AuthenticatedSessionDataBuilder;
import it.bologna.ausl.internauta.service.repositories.baborg.PersonaRepository;
import it.bologna.ausl.internauta.service.repositories.messaggero.AmministrazioneMessaggioRepository;
import it.bologna.ausl.internauta.service.utils.IntimusUtils;
import it.bologna.ausl.model.entities.baborg.Persona;
import it.bologna.ausl.model.entities.messaggero.AmministrazioneMessaggio;
import it.bologna.ausl.model.entities.messaggero.QAmministrazioneMessaggio;
import it.nextsw.common.controller.exceptions.NotFoundResourceException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
    private AmministrazioneMessaggioRepository amministrazioneMessaggioRepository;

    @Autowired
    private IntimusUtils intimusUtils;
    
    @Autowired
    private AuthenticatedSessionDataBuilder authenticatedSessionDataBuilder;

    /**
     * Torna la persona reale connessa (cioè la persona connessa nel caso normale, la persona reale in caso di cambio utente)
     * @return la persona reale connessa
     * @throws NotFoundResourceException
     * @throws BlackBoxPermissionException 
     */
    private Persona getRealPerson() throws NotFoundResourceException, BlackBoxPermissionException {
        AuthenticatedSessionData authenticatedUserProperties = authenticatedSessionDataBuilder.getAuthenticatedUserProperties();
        Persona person;
        if (authenticatedUserProperties.getRealPerson() != null) {
            LOGGER.info("si real user");
            person = authenticatedUserProperties.getRealPerson();
        } else {
            LOGGER.info("no real user");
            person = authenticatedUserProperties.getPerson();
        }
        LOGGER.info(String.format("person: %s", person.getId()));
        
        Optional<Persona> personaOp = this.personaRepository.findById(person.getId());
        if (!personaOp.isPresent()) {
            throw new NotFoundResourceException("persona non trovata");
        }
        return personaOp.get();
    }
    
    @RequestMapping(value = "setMessaggiVisti", method = RequestMethod.POST)
    @Transactional(rollbackFor = {Throwable.class})
    public void setMessaggiVisti(@RequestBody List<Integer> messaggiVisti) throws JsonProcessingException, BlackBoxPermissionException, NotFoundResourceException {
        Set<Integer> newMessaggiVisti = new HashSet<>();
        Persona personaToUpdate = getRealPerson();
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
    
    @RequestMapping(value = "getMessaggiDaMostrare", method = RequestMethod.GET)
    @Transactional(rollbackFor = {Throwable.class})
    public ResponseEntity<List<IntimusUtils.ShowMessageParams>> getMessaggiDaMostrare() throws NotFoundResourceException, BlackBoxPermissionException {
        LocalDateTime now = LocalDateTime.now();
        Persona person = getRealPerson();
        
        BooleanExpression startedNotExpiredFilter = 
                (QAmministrazioneMessaggio.amministrazioneMessaggio.dataScadenza.goe(now).or(QAmministrazioneMessaggio.amministrazioneMessaggio.dataScadenza.isNull())).and
                (QAmministrazioneMessaggio.amministrazioneMessaggio.dataPubblicazione.loe(now));
        
        BooleanExpression notSeenFilter = QAmministrazioneMessaggio.amministrazioneMessaggio.id.notIn(person.getMessaggiVisti());
        Iterable<AmministrazioneMessaggio> activeMessages = amministrazioneMessaggioRepository.findAll(startedNotExpiredFilter.and(notSeenFilter));

        List<IntimusUtils.ShowMessageParams> res = new ArrayList();
        for (AmministrazioneMessaggio activeMessage : activeMessages) {
            res.add(intimusUtils.buildShowMessageParams(activeMessage));
        }
        return new ResponseEntity(res, HttpStatus.OK);
    }
            
}
