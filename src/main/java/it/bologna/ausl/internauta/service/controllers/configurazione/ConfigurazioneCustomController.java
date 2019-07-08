package it.bologna.ausl.internauta.service.controllers.configurazione;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.querydsl.core.types.dsl.BooleanExpression;
import it.bologna.ausl.blackbox.exceptions.BlackBoxPermissionException;
import it.bologna.ausl.internauta.service.authorization.AuthenticatedSessionData;
import it.bologna.ausl.internauta.service.authorization.AuthenticatedSessionDataBuilder;
import it.bologna.ausl.internauta.service.repositories.baborg.PersonaRepository;
import it.bologna.ausl.internauta.service.repositories.configurazione.ApplicazioneRepository;
import it.bologna.ausl.internauta.service.repositories.configurazione.ImpostazioniApplicazioniRepository;
import it.bologna.ausl.model.entities.baborg.Persona;
import it.bologna.ausl.model.entities.baborg.Utente;
import it.bologna.ausl.model.entities.configuration.Applicazione.Applicazioni;
import it.bologna.ausl.model.entities.configuration.ImpostazioniApplicazioni;
import it.bologna.ausl.model.entities.configuration.QImpostazioniApplicazioni;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = "${configurazione.mapping.url.root}" + "/custom")
public class ConfigurazioneCustomController {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConfigurazioneCustomController.class);
    
    @Autowired
    private ImpostazioniApplicazioniRepository impostazioniApplicazioniRepository;
    
    @Autowired
    private ApplicazioneRepository applicazioneRepository;
    
    @Autowired
    private PersonaRepository personaRepository;

    @Autowired
    private AuthenticatedSessionDataBuilder authenticatedSessionDataBuilder;
    
    @Autowired
    ObjectMapper objectMapper;
    
    @RequestMapping(value = "setImpostazioniApplicazioni", method = RequestMethod.POST)
    @Transactional(rollbackFor = {Exception.class, Error.class})
    public void setImpostazioniApplicazioni(@RequestBody Map impostazioniVisualizzazione) throws JsonProcessingException, BlackBoxPermissionException {
        AuthenticatedSessionData authenticatedUserProperties = authenticatedSessionDataBuilder.getAuthenticatedUserProperties();
        Persona person;
        Utente user;
        if (authenticatedUserProperties.getRealPerson() != null) {
            LOGGER.info("si real user");
            person = authenticatedUserProperties.getRealPerson();
            user = authenticatedUserProperties.getRealUser();
        } else {
            LOGGER.info("no real user");
            person = authenticatedUserProperties.getPerson();
            user = authenticatedUserProperties.getUser();
        }
        Applicazioni applicazione = authenticatedUserProperties.getApplicazione();
        LOGGER.info(String.format("person: %s", objectMapper.writeValueAsString(person)));
        LOGGER.info(String.format("user: %s", objectMapper.writeValueAsString(user)));
        LOGGER.info(String.format("applicazione: %s", applicazione.toString()));
        
        BooleanExpression impostazioniFilter = QImpostazioniApplicazioni.impostazioniApplicazioni.idApplicazione.id
                .eq(applicazione.toString())
                .and(QImpostazioniApplicazioni.impostazioniApplicazioni.idPersona.id
                        .eq(person.getId()));

        Optional<ImpostazioniApplicazioni> impostazioniOp = this.impostazioniApplicazioniRepository.findOne(impostazioniFilter);
        ImpostazioniApplicazioni impostazioni;
        if (impostazioniOp.isPresent()) {
            impostazioni = impostazioniOp.get();
        }
        else {
            impostazioni = new ImpostazioniApplicazioni();
            impostazioni.setIdApplicazione(applicazioneRepository.getOne(applicazione.toString()));
            impostazioni.setIdPersona(personaRepository.getOne(person.getId()));
        }
        impostazioni.setImpostazioniVisualizzazione(objectMapper.writeValueAsString(impostazioniVisualizzazione));
        this.impostazioniApplicazioniRepository.save(impostazioni);
    }
}
