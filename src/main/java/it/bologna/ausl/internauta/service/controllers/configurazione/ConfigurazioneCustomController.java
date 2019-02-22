package it.bologna.ausl.internauta.service.controllers.configurazione;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.querydsl.core.types.dsl.BooleanExpression;
import it.bologna.ausl.internauta.service.authorization.AuthenticatedSessionData;
import it.bologna.ausl.internauta.service.authorization.AuthenticatedSessionDataBuilder;
import it.bologna.ausl.internauta.service.repositories.baborg.PersonaRepository;
import it.bologna.ausl.internauta.service.repositories.configurazione.ApplicazioneRepository;
import it.bologna.ausl.internauta.service.repositories.configurazione.ImpostazioniApplicazioniRepository;
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
    public void setImpostazioniApplicazioni(@RequestBody Map impostazioniVisualizzazione) throws JsonProcessingException {
        AuthenticatedSessionData authenticatedUserProperties = authenticatedSessionDataBuilder.getAuthenticatedUserProperties();
        LOGGER.info(String.format("realPerson: %s", objectMapper.writeValueAsString(authenticatedUserProperties.getRealPerson())));
        LOGGER.info(String.format("realUser: %s", objectMapper.writeValueAsString(authenticatedUserProperties.getRealUser())));
        LOGGER.info(String.format("applicazione realPerson: %s", authenticatedUserProperties.getRealPerson().getApplicazione()));
        BooleanExpression impostazioniFilter = QImpostazioniApplicazioni.impostazioniApplicazioni.idApplicazione.id.eq(authenticatedUserProperties.getRealPerson().getApplicazione())
                .and(QImpostazioniApplicazioni.impostazioniApplicazioni.idPersona.id.eq(authenticatedUserProperties.getRealPerson().getId()));
        Optional<ImpostazioniApplicazioni> impostazioniOp = this.impostazioniApplicazioniRepository.findOne(impostazioniFilter);
        ImpostazioniApplicazioni impostazioni;
        if (impostazioniOp.isPresent()) {
            impostazioni = impostazioniOp.get();
        }
        else {
            impostazioni = new ImpostazioniApplicazioni();
            impostazioni.setIdApplicazione(applicazioneRepository.getOne(authenticatedUserProperties.getRealPerson().getApplicazione()));
            impostazioni.setIdPersona(personaRepository.getOne(authenticatedUserProperties.getRealPerson().getId()));
        }
        impostazioni.setImpostazioniVisualizzazione(objectMapper.writeValueAsString(impostazioniVisualizzazione));
        this.impostazioniApplicazioniRepository.save(impostazioni);
    }
}
