package it.bologna.ausl.internauta.service.controllers.configurazione;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.querydsl.core.types.dsl.BooleanExpression;
import it.bologna.ausl.internauta.service.authorization.TokenBasedAuthentication;
import it.bologna.ausl.internauta.service.repositories.baborg.PersonaRepository;
import it.bologna.ausl.internauta.service.repositories.configurazione.ApplicazioneRepository;
import it.bologna.ausl.internauta.service.repositories.configurazione.ImpostazioniApplicazioniRepository;
import it.bologna.ausl.internauta.service.utils.CachedEntities;
import it.bologna.ausl.model.entities.baborg.Persona;
import it.bologna.ausl.model.entities.baborg.Utente;
import it.bologna.ausl.model.entities.configuration.Applicazione;
import it.bologna.ausl.model.entities.configuration.ImpostazioniApplicazioni;
import it.bologna.ausl.model.entities.configuration.QImpostazioniApplicazioni;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
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
    private CachedEntities cachedEntities;
    
    @Autowired
    ObjectMapper objectMapper;
    
    private Utente user, realUser;
    private Persona person, realPerson;
    private int idSessionLog;

    private void setAuthenticatedUserProperties() {
        TokenBasedAuthentication authentication = (TokenBasedAuthentication) SecurityContextHolder.getContext().getAuthentication();
        user = (Utente) authentication.getPrincipal();
        realUser = (Utente) authentication.getRealUser();
        idSessionLog = authentication.getIdSessionLog();
        person = cachedEntities.getPersona(user);
        realPerson = cachedEntities.getPersona(realUser);
    }
    
    @RequestMapping(value = "setImpostazioniApplicazioni", method = RequestMethod.POST)
    @Transactional(rollbackFor = {Exception.class, Error.class})
    public void setImpostazioniApplicazioni(@RequestBody Map impostazioniVisualizzazione) throws JsonProcessingException {
        this.setAuthenticatedUserProperties();
        BooleanExpression impostazioniFilter = QImpostazioniApplicazioni.impostazioniApplicazioni.idApplicazione.id.eq(this.realPerson.getApplicazione())
                .and(QImpostazioniApplicazioni.impostazioniApplicazioni.idPersona.id.eq(this.realPerson.getId()));
        Optional<ImpostazioniApplicazioni> impostazioniOp = this.impostazioniApplicazioniRepository.findOne(impostazioniFilter);
        ImpostazioniApplicazioni impostazioni;
        if (impostazioniOp.isPresent()) {
            impostazioni = impostazioniOp.get();
        }
        else {
            impostazioni = new ImpostazioniApplicazioni();
            impostazioni.setIdApplicazione(applicazioneRepository.getOne(this.realPerson.getApplicazione()));
            impostazioni.setIdPersona(personaRepository.getOne(this.realPerson.getId()));
        }
        impostazioni.setImpostazioniVisualizzazione(objectMapper.writeValueAsString(impostazioniVisualizzazione));
        this.impostazioniApplicazioniRepository.save(impostazioni);
    }
}
