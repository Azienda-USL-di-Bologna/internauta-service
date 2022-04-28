/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.bologna.ausl.internauta.service.interceptors.configurazione;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.bologna.ausl.internauta.service.authorization.AuthenticatedSessionData;
import it.bologna.ausl.internauta.service.exceptions.FirmaModuleException;
import it.bologna.ausl.internauta.service.interceptors.InternautaBaseInterceptor;
import it.bologna.ausl.internauta.service.interceptors.baborg.AziendaInterceptor;
import it.bologna.ausl.internauta.service.repositories.baborg.PersonaRepository;
import it.bologna.ausl.internauta.utils.firma.data.remota.FirmaRemotaInformation;
import it.bologna.ausl.internauta.utils.firma.data.remota.FirmaRemotaInformation.FirmaRemotaProviders;
import it.bologna.ausl.internauta.utils.firma.data.remota.UserInformation;
import it.bologna.ausl.internauta.utils.firma.data.remota.arubasignservice.ArubaUserInformation;
import it.bologna.ausl.internauta.utils.firma.remota.controllers.FirmaRemotaRestController;
import it.bologna.ausl.internauta.utils.firma.remota.exceptions.FirmaRemotaConfigurationException;
import it.bologna.ausl.internauta.utils.firma.remota.exceptions.http.FirmaRemotaHttpException;
import it.bologna.ausl.internauta.utils.firma.remota.exceptions.http.InvalidCredentialException;
import it.bologna.ausl.internauta.utils.parameters.manager.ParametriAziendeReader;
import it.bologna.ausl.model.entities.baborg.Azienda;
import it.bologna.ausl.model.entities.baborg.Utente;
import it.bologna.ausl.model.entities.baborg.UtenteStruttura;
import it.bologna.ausl.model.entities.configurazione.FirmePersona;
import it.bologna.ausl.model.entities.configurazione.ParametroAziende;
import it.nextsw.common.annotations.NextSdrInterceptor;
import it.nextsw.common.controller.BeforeUpdateEntityApplier;
import it.nextsw.common.interceptors.exceptions.AbortLoadInterceptorException;
import it.nextsw.common.interceptors.exceptions.AbortSaveInterceptorException;
import it.nextsw.common.interceptors.exceptions.SkipDeleteInterceptorException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Level;
import javax.servlet.http.HttpServletRequest;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 *
 * @author utente
 */
@Component
@NextSdrInterceptor(name = "firme-persona-interceptor")
public class FirmePersonaInterceptor extends InternautaBaseInterceptor {

    private static final Logger LOGGER = LoggerFactory.getLogger(AziendaInterceptor.class);

    @Autowired
    private ParametriAziendeReader parametriAziendeReader;

    @Autowired
    private PersonaRepository personaRepository;

    @Autowired
    private FirmaRemotaRestController firmaRemotaRestController;
    
    @Autowired
    private ObjectMapper objectMapper;

    @Override
    public Class getTargetEntityClass() {
        return FirmePersona.class;
    }

    /**
     *
     * Ogni volta che viene aggiunta una firma remota bisogna controllare: -
     * controllare se il reverse proxy è attivo su quell'azienda - se è attivo,
     * salvare lì le credenziali - se è disattivo, salvarle sul db Faccio sempre
     * l'update e mai la insert perchè quando le inserisco da internauta prima
     * creo la riga, poi la modifico.
     * @param entity
     * @param beforeUpdateEntityApplier
     * @param additionalData
     * @param request
     * @param projectionClass
     * @param mainEntity
     * @return 
     * @throws it.nextsw.common.interceptors.exceptions.AbortSaveInterceptorException
     */
    @Override
    public Object beforeUpdateEntityInterceptor(Object entity, BeforeUpdateEntityApplier beforeUpdateEntityApplier, Map<String, String> additionalData, HttpServletRequest request, boolean mainEntity, Class projectionClass) throws AbortSaveInterceptorException {
        LOGGER.info("in: beforeUpdateEntityInterceptor di FirmePersona");
        FirmePersona firmePersona = (FirmePersona) entity;
        List<FirmePersona> firmePersonaOldList = new ArrayList();
        try {
            beforeUpdateEntityApplier.beforeUpdateApply(oldEntity -> {
                firmePersonaOldList.add((FirmePersona) oldEntity);
            });
        } catch (Exception ex) {
            throw new AbortSaveInterceptorException("errore nel reperire la vecchia firma", ex);
        }
        if (firmePersonaOldList.get(0) != null && firmePersonaOldList.get(0) != firmePersona && Objects.equals(firmePersona.getPredefinita(), firmePersonaOldList.get(0).getPredefinita())) {
            if (firmePersona.getTipo() == FirmePersona.TipoFirma.REMOTA) {
                // prendo l'azienda della persona e vedo nei parametri aziende se è attivo il credential proxy
                Azienda azienda = getAziendaFromUser();
                // controllo se il provider è aruba e se lo è creo l'oggetto arubaUserInformation
                if (firmePersona.getTramite() == FirmePersona.TramiteFirma.ARUBA) {
                    Boolean isCredentialProxyActive = checkIfArubaCredentialProxyActive(azienda);
                    if (isCredentialProxyActive) {
                        JSONObject jsonAdditionalData = new JSONObject(firmePersona.getAdditionalData());
                        JSONObject jsonAdditionalDataOld = new JSONObject(firmePersonaOldList.get(0).getAdditionalData());
                        ArubaUserInformation arubaUserInfo = createArubaUserInformation(jsonAdditionalData);
                        ArubaUserInformation arubaUserInfoOld;
                        if (jsonAdditionalDataOld.has("dominio")) {
                            arubaUserInfoOld = createArubaUserInformation(jsonAdditionalDataOld);
                        } else {
                            arubaUserInfoOld = null;
                        }
                        Boolean haveCredentialsBeenSet;
                        try {
                            haveCredentialsBeenSet = createOrSetCredentialInCredentialProxy(firmePersona, azienda, arubaUserInfo, arubaUserInfoOld, FirmaRemotaInformation.FirmaRemotaProviders.ARUBA);
                            if (haveCredentialsBeenSet) {
                                LOGGER.info("The user's credentials have been set correctly into Aruba Credential Proxy");
                            }
                        } catch (Exception ex) {
                            LOGGER.error("Errore credenziali errate", ex);
                            throw new AbortSaveInterceptorException("errore credenziali non corrette", ex);
                        }
                    }
                }
            }
        }
        return entity;
    }

    /*
    * Quando cancello le credenziali su babel, se è attivo il credential proxy,
    * le cancello anche lì.
     */
    @Override
    public void beforeDeleteEntityInterceptor(Object entity, Map<String, String> additionalData, HttpServletRequest request, boolean mainEntity, Class projectionClass) throws AbortSaveInterceptorException, SkipDeleteInterceptorException {
        LOGGER.info("in: beforeUpdateEntityInterceptor di FirmePersona");
        FirmePersona firmePersona = (FirmePersona) entity;
        if (firmePersona.getAdditionalData().getAutenticazione() != null && firmePersona.getTipo() == FirmePersona.TipoFirma.REMOTA) {
            // prendo l'azienda della persona e vedo nei parametri aziende se è attivo il credential proxy
            Azienda azienda = getAziendaFromUser();
            if (firmePersona.getTramite() == FirmePersona.TramiteFirma.ARUBA) {
                Boolean isCredentialProxyActive = checkIfArubaCredentialProxyActive(azienda);
                if (isCredentialProxyActive) {
                    JSONObject jsonAdditionalData = new JSONObject(firmePersona.getAdditionalData());
                    ArubaUserInformation arubaUserInfo = createArubaUserInformation(jsonAdditionalData);
                    Boolean haveCredentialBeenRemoved;
                    haveCredentialBeenRemoved = removeCredentialFromCredentialProxy(firmePersona, azienda, arubaUserInfo, FirmaRemotaInformation.FirmaRemotaProviders.ARUBA);
                    if (haveCredentialBeenRemoved) {
                        LOGGER.info("The user's credentials have been removed correctly from Aruba Credential Proxy");
                    }
                }
            }
        }
    }

    /*
    * Creazione delle informazioni utente per il Provider Aruba
    * prende in input il json con gli additionaldata che vengono dall'oggetto firmapersona
     */
    private ArubaUserInformation createArubaUserInformation(JSONObject jsonAdditionalData) {
        ArubaUserInformation arubaUserInfo = new ArubaUserInformation();
        arubaUserInfo.setUsername(jsonAdditionalData.get("username").toString());
        arubaUserInfo.setPassword(jsonAdditionalData.get("password").toString());
        arubaUserInfo.setDominioFirma(jsonAdditionalData.get("dominio").toString());
        String modalitaAutenticazione = jsonAdditionalData.get("autenticazione").toString();
        switch (modalitaAutenticazione) {
            case "Token":
                arubaUserInfo.setModalitaFirma(ArubaUserInformation.ModalitaFirma.OTP);
                break;
            case "Telefonata":
                arubaUserInfo.setModalitaFirma(ArubaUserInformation.ModalitaFirma.ARUBACALL);
                break;
            case "App":
                arubaUserInfo.setModalitaFirma(ArubaUserInformation.ModalitaFirma.APP);
                break;
        }
        arubaUserInfo.setCertId("1");
        return arubaUserInfo;
    }

    /*
    * Funzione che recupera l'azienda dall'utente loggato
     */
    private Azienda getAziendaFromUser() {
        AuthenticatedSessionData authenticatedSessionData = getAuthenticatedUserProperties();
        Utente utente = authenticatedSessionData.getUser();
        Azienda aziendaUser = utente.getIdAzienda();
        return aziendaUser;
    }

    /*
    * Chiedo attraverso parametersmanager se in parametri_aziende 
    * nell'azienda dell'utente è attivo il credentialproxy
     */
    private Boolean checkIfArubaCredentialProxyActive(Azienda azienda) {
        ParametroAziende parametroAziendale = parametriAziendeReader.getParameters(ParametriAziendeReader.ParametriAzienda.firmaRemota.toString(), new Integer[]{azienda.getId()}).get(0);
        JSONObject jsonObject = new JSONObject(parametroAziendale.getValore());
        JSONObject jsonAruba = (JSONObject) jsonObject.get("ArubaSignService");
        JSONObject jsonCredential = (JSONObject) jsonAruba.get("ArubaCredentialProxyAdminInfo");
        Boolean isCredentialProxyActive = Boolean.valueOf(jsonCredential.get("active").toString());
        return isCredentialProxyActive;
    }

    /*
    * Funzione che richiama il controller di internautautils.firma per settare le credenziali sul credential proxy
    * in input richiede l'azienda, le credenziali, il provider e le vecchie credenziali nel caso in cui quelle 
    * nuove non siano valide
     */
    private Boolean createOrSetCredentialInCredentialProxy(FirmePersona firmePersona, Azienda azienda, UserInformation userInformation, UserInformation userInformationOld, FirmaRemotaInformation.FirmaRemotaProviders provider) throws FirmaRemotaHttpException, FirmaRemotaConfigurationException, FirmaModuleException {
        Boolean areCredentialSet = false;
        Boolean areOldCredentialSet = false;
        FirmePersona.AdditionalData additionalData = firmePersona.getAdditionalData();
//     Chiedo se già esistono queste credenziali
        Boolean existingCredentials = firmaRemotaRestController.existingCredential(userInformation, additionalData.getHostId());
        if (existingCredentials == false) {
            LOGGER.info("No existing credentials found, try to set credentials");
            try {
                LOGGER.info(objectMapper.writeValueAsString(userInformation));
            } catch (JsonProcessingException ex) {
                java.util.logging.Logger.getLogger(FirmePersonaInterceptor.class.getName()).log(Level.SEVERE, null, ex);
            }
            areCredentialSet = firmaRemotaRestController.setCredential(userInformation, additionalData.getHostId());
            if (areCredentialSet) {
//                                TO DO: voglio fare in modo che non salvi qui la password
//                                firmePersona.setAdditionalData();
            } else {
                throw new FirmaModuleException("Credenziali non settate");
            }
        } else {
            LOGGER.info("Existing credentials found, try to remove credentials");
            
            LOGGER.info("Existing credentials removed, try to set new credentials");
            areCredentialSet = firmaRemotaRestController.setCredential(userInformation, additionalData.getHostId());
            if (areCredentialSet) {
//                                TO DO: voglio fare in modo che non salvi qui la password. Per ora le lascio
//                                firmePersona.setAdditionalData();
                LOGGER.info("New credentials have been set on arubacredentialproxy correctly");
            } else {
            // Se per qualunque motivo non sono riusciuto a settarle, risetto quelle vecchie
                LOGGER.info("New credentials have not been set on arubacredentialproxy correctly ");
                throw new FirmaModuleException("non sono valide nessuna delle credenziali");
            }
            
        }

        return areCredentialSet;
    }

    /*
    * Funzione che richiama il controller di internautautils.firma per rimuovere le credenziali dal credential proxy
    * in input richiede l'azienda, le credenziali e il provider
     */
    private Boolean removeCredentialFromCredentialProxy(FirmePersona firmePersona, Azienda azienda, UserInformation userInformation, FirmaRemotaInformation.FirmaRemotaProviders provider) {
        Boolean haveCredentialBeenRemoved = false;
        FirmePersona.AdditionalData additionalData = firmePersona.getAdditionalData();
        try {
//  Chiedo se già esistono queste credenziali
            Boolean existingCredentials = firmaRemotaRestController.existingCredential(userInformation, additionalData.getHostId());
            if (existingCredentials) {
                try {
                    haveCredentialBeenRemoved = firmaRemotaRestController.removeCredential(userInformation, additionalData.getHostId());
                } catch (FirmaRemotaHttpException ex) {
                    LOGGER.info("Errore nella rimozione delle credenziali");
                    throw new FirmaRemotaHttpException("Errore nella rimozione delle credenziali");
                }
            }
        } catch (FirmaRemotaHttpException | FirmaRemotaConfigurationException ex) {
            java.util.logging.Logger.getLogger(FirmePersonaInterceptor.class.getName()).log(Level.SEVERE, null, ex);
        }

        return haveCredentialBeenRemoved;
    }

}
