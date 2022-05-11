package it.bologna.ausl.internauta.service.controllers.configurazione;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.Expressions;
import it.bologna.ausl.blackbox.exceptions.BlackBoxPermissionException;
import it.bologna.ausl.internauta.service.authorization.AuthenticatedSessionData;
import it.bologna.ausl.internauta.service.authorization.AuthenticatedSessionDataBuilder;
import it.bologna.ausl.internauta.service.authorization.UserInfoService;
import it.bologna.ausl.internauta.service.interceptors.configurazione.FirmePersonaInterceptor;
import it.bologna.ausl.internauta.service.repositories.baborg.PersonaRepository;
import it.bologna.ausl.internauta.service.repositories.configurazione.ApplicazioneRepository;
import it.bologna.ausl.internauta.service.repositories.configurazione.FirmePersonaRepository;
import it.bologna.ausl.internauta.service.repositories.configurazione.ImpostazioniApplicazioniRepository;
import it.bologna.ausl.internauta.utils.firma.data.remota.UserInformation;
import it.bologna.ausl.internauta.utils.firma.remota.controllers.FirmaRemotaArubaController;
import it.bologna.ausl.internauta.utils.firma.remota.controllers.FirmaRemotaRestController;
import it.bologna.ausl.internauta.utils.firma.remota.exceptions.FirmaRemotaConfigurationException;
import it.bologna.ausl.internauta.utils.parameters.manager.repositories.ParametroAziendeRepository;
import it.bologna.ausl.model.entities.baborg.Persona;
import it.bologna.ausl.model.entities.baborg.Utente;
import it.bologna.ausl.model.entities.configurazione.Applicazione.Applicazioni;
import it.bologna.ausl.model.entities.configurazione.FirmePersona;
import it.bologna.ausl.model.entities.configurazione.ImpostazioniApplicazioni;
import it.bologna.ausl.model.entities.configurazione.ParametroAziende;
import it.bologna.ausl.model.entities.configurazione.QFirmePersona;
import it.bologna.ausl.model.entities.configurazione.QImpostazioniApplicazioni;
import it.bologna.ausl.model.entities.configurazione.QParametroAziende;
import it.bologna.ausl.model.entities.firma.DominioAruba;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = "${configurazione.mapping.url.root}" + "/debug")
public class ConfigurazioneDebugController {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConfigurazioneDebugController.class);

    @Autowired
    private FirmePersonaRepository firmePersonaRepository;

    @Autowired
    private UserInfoService userInfoService;
    
    @Autowired
    private FirmePersonaInterceptor firmePersonaInterceptor;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @Autowired
    private FirmaRemotaArubaController firmaRemotaArubaController;
    
    @Autowired
    private FirmaRemotaRestController firmaRemotaRestController;

    @Autowired
    private AuthenticatedSessionDataBuilder authenticatedSessionDataBuilder;


    @RequestMapping(value = "fixFirmePersona", method = RequestMethod.GET)
    @Transactional(rollbackFor = {Exception.class, Error.class})
    public void fixFirmePersona(@RequestBody Map impostazioniVisualizzazione) throws JsonProcessingException, BlackBoxPermissionException {
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
        LOGGER.info(String.format("applicazione: %s", applicazione.toString()));
        
        if (userInfoService.isSD(user)) {
            LOGGER.info(String.format("ciao %s, sei SD quindi procedo...", person.getDescrizione()));
            BooleanExpression filter = 
                    QFirmePersona.firmePersona.tipo.eq(FirmePersona.TipoFirma.REMOTA.toString())
                            .and(
                    QFirmePersona.firmePersona.tramite.eq(FirmePersona.TramiteFirma.ARUBA.toString()));
            Iterable<FirmePersona> firePersonaDaFixare = firmePersonaRepository.findAll(filter);
            for (FirmePersona firmePersona : firePersonaDaFixare) {
                FirmePersona.AdditionalData additionalData = firmePersona.getAdditionalData();
                if (additionalData != null) {
                    LOGGER.info(objectMapper.writeValueAsString(additionalData));
                    if (StringUtils.hasText(additionalData.getDominio())) {
                        DominioAruba.DominiAruba dominio;
                        try {
                            dominio = DominioAruba.DominiAruba.valueOf(additionalData.getDominio());
                            try {
                                String hostIdFromDominio = firmaRemotaArubaController.getHostIdFromDominio(dominio);
                                if (hostIdFromDominio != null) {
                                    additionalData.setHostId(hostIdFromDominio);
                                    String username = additionalData.getUsername();
                                    String password = additionalData.getPassword();
                                    if (StringUtils.hasText(username) && StringUtils.hasText(password)) {
                                        try {
                                            UserInformation userInformation = firmePersonaInterceptor.createUserInformation(FirmePersona.TramiteFirma.ARUBA, additionalData, additionalData.getPassword());
                                            Boolean settedCredential = firmaRemotaRestController.setCredential(userInformation, hostIdFromDominio);
                                            if (settedCredential) {
                                                additionalData.setSavedCredential(true);
                                                additionalData.setPassword(null);
                                            } else {
                                                LOGGER.warn(String.format("il setCredential delle credenziali sul credential proxy per la FirmePersona %s ha tornato false", firmePersona.getId()));
                                            }
                                        } catch (Exception ex) {
                                            LOGGER.warn(String.format("errore nel setting delle credenziali sul credential proxy per la FirmePersona %s", firmePersona.getId()), ex);
    //                                        if (additionalData.getSavedCredential() == null) {
                                            additionalData.setSavedCredential(false);
                                            additionalData.setPassword(null);
    //                                        }
                                        }

                                    } else {
                                        additionalData.setSavedCredential(false);
                                        additionalData.setPassword(null);
                                        LOGGER.warn(String.format("usarname o password vuoti per la FirmePersona %s, salto la memorizzazione sul credential proxy...", firmePersona.getId()));
                                    } 
                                } else {
                                    LOGGER.warn(String.format("hostId non trovato per la FirmePersona %s, salto la memorizzazione sul credential proxy...", firmePersona.getId()));
                                    additionalData.setSavedCredential(null);
                                    additionalData.setHostId(null);
                                }
                            } catch (Exception ex) {
                                LOGGER.warn(String.format("errore nel reperimento dell'hostId dal dominio %s per la FirmePersona %s non è valido, setto solo savedCredential a false", additionalData.getDominio(), firmePersona.getId()), ex);
                                additionalData.setSavedCredential(false);
//                                additionalData.setPassword(null);
                            }
                        } catch (Exception ex) {
                            LOGGER.warn(String.format("dominio %s per la FirmePersona %s non è valido, setto solo savedCredential a null", additionalData.getDominio(), firmePersona.getId()), ex);
                            additionalData.setSavedCredential(null);
                            //additionalData.setPassword(null);
                        }
                    } else {
                        LOGGER.warn(String.format("dominio per la FirmePersona %s è null o vuoto, la salto...", firmePersona.getId()));
                    }
                    LOGGER.info(String.format("salvo la firmePersona con id %s", firmePersona.getId()));
                    firmePersona.setAdditionalData(additionalData);
                    firmePersonaRepository.save(firmePersona);
                } else {
                    LOGGER.warn(String.format("additional data per la FirmePersona %s è null, la salto...", firmePersona.getId()));
                }
                
            }
        } else {
            LOGGER.error("non sei SD e quindi non faccio nulla!");
        }
    }
    
}
