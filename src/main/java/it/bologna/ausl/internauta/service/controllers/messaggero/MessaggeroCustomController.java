package it.bologna.ausl.internauta.service.controllers.messaggero;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.Expressions;
import it.bologna.ausl.blackbox.exceptions.BlackBoxPermissionException;
import it.bologna.ausl.internauta.service.authorization.AuthenticatedSessionData;
import it.bologna.ausl.internauta.service.authorization.AuthenticatedSessionDataBuilder;
import it.bologna.ausl.internauta.service.authorization.UserInfoService;
import it.bologna.ausl.internauta.service.repositories.baborg.PersonaRepository;
import it.bologna.ausl.internauta.service.repositories.baborg.UtenteRepository;
import it.bologna.ausl.internauta.service.repositories.messaggero.AmministrazioneMessaggioRepository;
import it.bologna.ausl.internauta.service.utils.IntimusUtils;
import it.bologna.ausl.model.entities.baborg.Persona;
import it.bologna.ausl.model.entities.baborg.Utente;
import it.bologna.ausl.model.entities.configurazione.Applicazione;
import it.bologna.ausl.model.entities.messaggero.AmministrazioneMessaggio;
import it.bologna.ausl.model.entities.messaggero.QAmministrazioneMessaggio;
import it.nextsw.common.controller.exceptions.NotFoundResourceException;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
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
    private UtenteRepository utenteRepository;

    @Autowired
    private AmministrazioneMessaggioRepository amministrazioneMessaggioRepository;

    @Autowired
    private UserInfoService userInfoService;

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
    
     private Utente getRealUser() throws NotFoundResourceException, BlackBoxPermissionException {
        AuthenticatedSessionData authenticatedUserProperties = authenticatedSessionDataBuilder.getAuthenticatedUserProperties();
        Utente user;
        if (authenticatedUserProperties.getRealUser()!= null) {
            LOGGER.info("si real user");
            user = authenticatedUserProperties.getRealUser();
        } else {
            LOGGER.info("no real user");
            user = authenticatedUserProperties.getUser();
        }
        LOGGER.info(String.format("person: %s", user.getId()));
        
        Optional<Utente> utenteOp = this.utenteRepository.findById(user.getId());
        if (!utenteOp.isPresent()) {
            throw new NotFoundResourceException("utente non trovato");
        }
        return utenteOp.get();
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
        ZonedDateTime now = ZonedDateTime.now();
        Persona person = getRealPerson();
        Utente user = getRealUser();
        AuthenticatedSessionData authenticatedUserProperties = authenticatedSessionDataBuilder.getAuthenticatedUserProperties();
        Applicazione.Applicazioni applicazione = authenticatedUserProperties.getApplicazione();
        
        BooleanExpression startedNotExpiredFilter = 
                (QAmministrazioneMessaggio.amministrazioneMessaggio.dataScadenza.goe(now).or(QAmministrazioneMessaggio.amministrazioneMessaggio.dataScadenza.isNull())).and
                (QAmministrazioneMessaggio.amministrazioneMessaggio.dataPubblicazione.loe(now));
        
        BooleanExpression perTuttiFilter = QAmministrazioneMessaggio.amministrazioneMessaggio.perTutti.eq(true);
        BooleanExpression aziendeFilter = QAmministrazioneMessaggio.amministrazioneMessaggio.idAziende.isNotNull().and(
                Expressions.booleanTemplate("tools.array_overlap({0}, tools.string_to_integer_array({1}, ','))=true", 
                        QAmministrazioneMessaggio.amministrazioneMessaggio.idAziende, org.apache.commons.lang3.StringUtils.join(userInfoService.getAziendePersona(person).stream().map(a -> a.getId()).collect(Collectors.toList()), ",")));
        BooleanExpression struttureFilter = QAmministrazioneMessaggio.amministrazioneMessaggio.idStrutture.isNotNull().and(
                Expressions.booleanTemplate("tools.array_overlap({0}, tools.string_to_integer_array({1}, ','))=true", 
                        QAmministrazioneMessaggio.amministrazioneMessaggio.idStrutture, org.apache.commons.lang3.StringUtils.join(userInfoService.getUtenteStrutturaList(user, true).stream().map(us -> us.getIdStruttura().getId()).collect(Collectors.toList()), ",")));
        BooleanExpression personeFilter = QAmministrazioneMessaggio.amministrazioneMessaggio.idPersone.isNotNull().and(
                Expressions.booleanTemplate("arraycontains({0}, tools.string_to_integer_array({1}, ','))=true", QAmministrazioneMessaggio.amministrazioneMessaggio.idPersone, String.valueOf(person.getId())));
        
        BooleanExpression myMessageFilter = perTuttiFilter.or(aziendeFilter).or(struttureFilter).or(personeFilter);
        
        Integer[] messaggiVisti = person.getMessaggiVisti();
        if (messaggiVisti == null) {
            messaggiVisti = new Integer[0];
        }
        BooleanExpression notSeenFilter = QAmministrazioneMessaggio.amministrazioneMessaggio.id.notIn(messaggiVisti);
        
        BooleanExpression applicazioniFilter = Expressions.booleanTemplate("tools.array_overlap({0}, string_to_array({1}, ','))=true", QAmministrazioneMessaggio.amministrazioneMessaggio.idApplicazioni, applicazione.toString());
        
        Iterable<AmministrazioneMessaggio> activeMessages = amministrazioneMessaggioRepository.findAll(startedNotExpiredFilter.and(notSeenFilter).and(myMessageFilter).and(applicazioniFilter));

        
        List<IntimusUtils.ShowMessageParams> res = new ArrayList();
        for (AmministrazioneMessaggio activeMessage : activeMessages) {
            res.add(intimusUtils.buildShowMessageParams(activeMessage));
        }
        return new ResponseEntity(res, HttpStatus.OK);
    }
            
}
