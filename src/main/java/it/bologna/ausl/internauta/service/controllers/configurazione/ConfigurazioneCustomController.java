package it.bologna.ausl.internauta.service.controllers.configurazione;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.Expressions;
import it.bologna.ausl.blackbox.exceptions.BlackBoxPermissionException;
import it.bologna.ausl.internauta.service.authorization.AuthenticatedSessionData;
import it.bologna.ausl.internauta.service.authorization.AuthenticatedSessionDataBuilder;
import it.bologna.ausl.internauta.service.repositories.baborg.PersonaRepository;
import it.bologna.ausl.internauta.service.repositories.configurazione.ApplicazioneRepository;
import it.bologna.ausl.internauta.service.repositories.configurazione.ImpostazioniApplicazioniRepository;
import it.bologna.ausl.internauta.service.utils.CacheableFunctions;
import it.bologna.ausl.internauta.utils.parameters.manager.repositories.ParametroAziendeRepository;
import it.bologna.ausl.model.entities.baborg.Persona;
import it.bologna.ausl.model.entities.baborg.Utente;
import it.bologna.ausl.model.entities.configurazione.Applicazione.Applicazioni;
import it.bologna.ausl.model.entities.configurazione.ImpostazioniApplicazioni;
import it.bologna.ausl.model.entities.configurazione.ParametroAziende;
import it.bologna.ausl.model.entities.configurazione.QImpostazioniApplicazioni;
import it.bologna.ausl.model.entities.configurazione.QParametroAziende;
import java.util.List;
import java.util.Map;
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
import org.springframework.web.bind.annotation.RequestParam;
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
    private ParametroAziendeRepository parametroAziendeRepository;
    
    @Autowired
    private PersonaRepository personaRepository;

    @Autowired
    private AuthenticatedSessionDataBuilder authenticatedSessionDataBuilder;
    
    @Autowired
    private CacheableFunctions cacheableFunctions;

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
//        LOGGER.info(String.format("person: %s", objectMapper.writeValueAsString(person)));
//        user.getIdPersona().getIdContatto();
//        LOGGER.info(String.format("user: %s", objectMapper.writeValueAsString(user)));
        LOGGER.info(String.format("applicazione: %s", applicazione.toString()));

        BooleanExpression impostazioniFilter = QImpostazioniApplicazioni.impostazioniApplicazioni.idApplicazione.id
                .eq(applicazione.toString())
                .and(QImpostazioniApplicazioni.impostazioniApplicazioni.idPersona.id
                        .eq(person.getId()));

        Optional<ImpostazioniApplicazioni> impostazioniOp = this.impostazioniApplicazioniRepository.findOne(impostazioniFilter);
        ImpostazioniApplicazioni impostazioni;
        if (impostazioniOp.isPresent()) {
            impostazioni = impostazioniOp.get();
        } else {
            impostazioni = new ImpostazioniApplicazioni();
            impostazioni.setIdApplicazione(applicazioneRepository.getOne(applicazione.toString()));
            impostazioni.setIdPersona(personaRepository.getOne(person.getId()));
        }
        impostazioni.setImpostazioniVisualizzazione(objectMapper.writeValueAsString(impostazioniVisualizzazione));
        this.impostazioniApplicazioniRepository.save(impostazioni);
    }
    
    @RequestMapping(value = "getParametriAziende", method = RequestMethod.GET)
    public ResponseEntity<Iterable<ParametroAziende>> getParametriAziende(
            @RequestParam(required = false) String nome,
            @RequestParam(required = false) List<String> idApplicazioni,
            @RequestParam(required = false) List<Integer> idAziende) {
                
        Predicate initialPredicate = Expressions.TRUE.eq(true);
        
        if (nome != null) {
            BooleanExpression filter = QParametroAziende.parametroAziende.nome.eq(nome);
            initialPredicate = filter.and(initialPredicate);
        }
        if (idApplicazioni != null) {
            BooleanExpression filter = 
                Expressions.booleanTemplate(
                    "tools.array_overlap({0}, string_to_array({1}, ','))=true",
                    QParametroAziende.parametroAziende.idApplicazioni, 
                    String.join(",", idApplicazioni)
                );
            initialPredicate = filter.and(initialPredicate);
        }
        if (idAziende != null) {
            BooleanExpression filter = 
                Expressions.booleanTemplate(
                    "tools.array_overlap({0}, tools.string_to_integer_array({1}, ','))=true",
                    QParametroAziende.parametroAziende.idAziende, 
                    org.apache.commons.lang3.StringUtils.join(
                            idAziende, ",")
                );
            initialPredicate = filter.and(initialPredicate);
        }
        
        Iterable<ParametroAziende> res = parametroAziendeRepository.findAll(initialPredicate);
        
//        List<ParametroAziende> res = new ArrayList();
//        for (ParametroAziende ParametroAziende : findAll) {
//            res.add(intimusUtils.buildShowMessageParams(activeMessage));
//        }
        return new ResponseEntity(res, HttpStatus.OK);
    }
    
    /**
     * 
     * @return
     * @throws BlackBoxPermissionException 
     */
    @RequestMapping(value = "firmaRemotaProviders", method = RequestMethod.GET)
    public ResponseEntity<Set<String>> getFirmaRemotaProviders() throws BlackBoxPermissionException {
        
        return new ResponseEntity(cacheableFunctions.getFirmaRemotaProvidersInfo(), HttpStatus.OK);
    }
}
