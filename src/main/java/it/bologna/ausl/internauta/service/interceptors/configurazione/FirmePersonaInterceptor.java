package it.bologna.ausl.internauta.service.interceptors.configurazione;

import it.bologna.ausl.internauta.service.exceptions.FirmaModuleException;
import it.bologna.ausl.internauta.service.interceptors.InternautaBaseInterceptor;
import it.bologna.ausl.internauta.service.interceptors.baborg.AziendaInterceptor;
import it.bologna.ausl.internauta.service.utils.InternautaConstants;
import it.bologna.ausl.internauta.utils.firma.data.remota.UserInformation;
import it.bologna.ausl.internauta.utils.firma.data.remota.arubasignservice.ArubaUserInformation;
import it.bologna.ausl.internauta.utils.firma.data.remota.infocertsignservice.InfocertUserInformation;
import it.bologna.ausl.internauta.utils.firma.data.remota.namirialsignservice.NamirialUserInformation;
import it.bologna.ausl.internauta.utils.firma.remota.controllers.FirmaRemotaArubaController;
import it.bologna.ausl.internauta.utils.firma.remota.controllers.FirmaRemotaRestController;
import it.bologna.ausl.internauta.utils.firma.remota.exceptions.FirmaRemotaConfigurationException;
import it.bologna.ausl.internauta.utils.firma.remota.exceptions.http.FirmaRemotaHttpException;
import it.bologna.ausl.model.entities.configurazione.FirmePersona;
import it.bologna.ausl.model.entities.firma.DominioAruba;
import it.nextsw.common.data.annotations.NextSdrInterceptor;
import it.nextsw.common.controller.BeforeUpdateEntityApplier;
import it.nextsw.common.interceptors.exceptions.AbortLoadInterceptorException;
import it.nextsw.common.interceptors.exceptions.AbortSaveInterceptorException;
import it.nextsw.common.interceptors.exceptions.SkipDeleteInterceptorException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 *
 * @author utente
 */
@Component
@NextSdrInterceptor(name = "firme-persona-interceptor")
public class FirmePersonaInterceptor extends InternautaBaseInterceptor {

    private static final Logger LOGGER = LoggerFactory.getLogger(AziendaInterceptor.class);

    @Autowired
    private FirmaRemotaRestController firmaRemotaRestController;
    
    @Autowired
    private FirmaRemotaArubaController firmaRemotaArubaController;

    @Override
    public Class getTargetEntityClass() {
        return FirmePersona.class;
    }

    @Override
    public Object afterSelectQueryInterceptor(Object entity, Map<String, String> additionalData, HttpServletRequest request, boolean mainEntity, Class projectionClass) throws AbortLoadInterceptorException {
        FirmePersona firmaPersona = (FirmePersona)entity;
//        firmaPersona.set$additionalData(firmaPersona.getAdditionalData());
        return firmaPersona;
    }

    @Override
    public Collection<Object> afterSelectQueryInterceptor(Collection<Object> entities, Map<String, String> additionalData, HttpServletRequest request, boolean mainEntity, Class projectionClass) throws AbortLoadInterceptorException {
        for (Object entity : entities) {
            afterSelectQueryInterceptor(entity, additionalData, request, mainEntity, projectionClass);
        }
        return entities;
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
        List<InternautaConstants.AdditionalData.OperationsRequested> operationsRequested = InternautaConstants.AdditionalData.getOperationRequested(InternautaConstants.AdditionalData.Keys.OperationRequested, additionalData);
        if (operationsRequested != null && !operationsRequested.isEmpty()) {
            for (InternautaConstants.AdditionalData.OperationsRequested operationRequested : operationsRequested) {
                switch (operationRequested) {
                    case UpdateProfiloFirma: 
                        String password = firmePersona.getPassword();
                        List<FirmePersona> firmePersonaOldList = new ArrayList();
                        try {
                            beforeUpdateEntityApplier.beforeUpdateApply(oldEntity -> {
                                firmePersonaOldList.add((FirmePersona) oldEntity);
                            });
                        } catch (Exception ex) {
                            throw new AbortSaveInterceptorException("errore nel reperire la vecchia firma", ex);
                        }
                        if (firmePersonaOldList.get(0) != null && firmePersonaOldList.get(0) != firmePersona && firmePersona.getPredefinita().equals(firmePersonaOldList.get(0).getPredefinita())) {
                            if (firmePersona.getTipo() == FirmePersona.TipoFirma.REMOTA) {
                                // prendo l'azienda della persona e vedo nei parametri aziende se è attivo il credential proxy
//                                Azienda azienda = getAziendaFromUser();
                                FirmePersona.AdditionalDataFirma firmaPersonaAdditionalData = firmePersona.getAdditionalData();
                                try {
                                    if (!StringUtils.hasText(firmaPersonaAdditionalData.getHostId())) {
                                        String hostId = getHostId(firmePersona.getTramite(), firmaPersonaAdditionalData);
                                        firmaPersonaAdditionalData.setHostId(hostId);
                                    }
                                } catch (FirmaRemotaHttpException | FirmaRemotaConfigurationException ex) {
                                    throw new AbortSaveInterceptorException("errore nel reperire l'hostId associato al dominio", ex);
                                }
                                UserInformation userInfo = createUserInformation(firmePersona.getTramite(), firmaPersonaAdditionalData, password);
                                if (StringUtils.hasText(password)) {
                                    Boolean haveCredentialsBeenSet;
                                    try {
                                        haveCredentialsBeenSet = setCredential(firmePersona, userInfo);
                                        if (haveCredentialsBeenSet) {
                                            LOGGER.info("The user's credentials have been set correctly into Credential Proxy");
                                            firmaPersonaAdditionalData.setSavedCredential(true);
                                        }
                                    } catch (Exception ex) {
                                        String errorMessage = "Errore credenziali errate";
                                        LOGGER.error(errorMessage, ex);
                                        throw new AbortSaveInterceptorException(errorMessage, ex);
                                    }
                                }
                            }
                        }
                    break;
                    case RemovePassword:
                        if (firmePersona.getTipo() == FirmePersona.TipoFirma.REMOTA) {
                                // prendo l'azienda della persona e vedo nei parametri aziende se è attivo il credential proxy
//                                Azienda azienda = getAziendaFromUser();
                            FirmePersona.AdditionalDataFirma firmaPersonaAdditionalData = firmePersona.getAdditionalData();
                            try {
                                if (!StringUtils.hasText(firmaPersonaAdditionalData.getHostId())) {
                                    String hostId = getHostId(firmePersona.getTramite(), firmaPersonaAdditionalData);
                                    firmaPersonaAdditionalData.setHostId(hostId);
                                }
                            } catch (FirmaRemotaHttpException | FirmaRemotaConfigurationException ex) {
                                throw new AbortSaveInterceptorException("errore nel reperire l'hostId associato al dominio", ex);
                            }
                            UserInformation userInfo = createUserInformation(firmePersona.getTramite(), firmaPersonaAdditionalData, "");
                            Boolean haveCredentialBeenRemoved;
                            try {
                                haveCredentialBeenRemoved = removeCredential(firmePersona, userInfo);
                                if (haveCredentialBeenRemoved) {
                                    firmaPersonaAdditionalData.setSavedCredential(false);
                                    LOGGER.info("The user's credentials have been removed correctly from Credential Proxy");
                                } else {
                                    LOGGER.warn("The user's credentials haven't been removed correctly from Credential Proxy");
                                }
                            } catch (Exception ex) {
                                String errorMessage = "Errore eliminazione credenziali";
                                LOGGER.error(errorMessage, ex);
                                throw new AbortSaveInterceptorException(errorMessage, ex);
                            }
                        }
                    break;
                }
            }
            
        }
        
        return entity;
    }

    /**
     * Quando cancello l'entità, se la firma configurata è remota elimino le credenziali dal credential proxy (interno o esterno)
     * @param entity
     * @param additionalData
     * @param request
     * @param mainEntity
     * @param projectionClass
     * @throws AbortSaveInterceptorException
     * @throws SkipDeleteInterceptorException 
     */
    @Override
    public void beforeDeleteEntityInterceptor(Object entity, Map<String, String> additionalData, HttpServletRequest request, boolean mainEntity, Class projectionClass) throws AbortSaveInterceptorException, SkipDeleteInterceptorException {
        LOGGER.info("in: beforeUpdateEntityInterceptor di FirmePersona");
        FirmePersona firmePersona = (FirmePersona) entity;
        if (firmePersona.getAdditionalData() != null && firmePersona.getAdditionalData().getAutenticazione() != null && firmePersona.getTipo() == FirmePersona.TipoFirma.REMOTA) {
            // prendo l'azienda della persona e vedo nei parametri aziende se è attivo il credential proxy
//            Azienda azienda = getAziendaFromUser();
            try {
                FirmePersona.AdditionalDataFirma firmaPersonaAdditionalData = firmePersona.getAdditionalData();
                try {
                    if (!StringUtils.hasText(firmaPersonaAdditionalData.getHostId())) {
                        String hostId = getHostId(firmePersona.getTramite(), firmePersona.getAdditionalData());
                        firmaPersonaAdditionalData.setHostId(hostId);
                    }
                } catch (FirmaRemotaHttpException | FirmaRemotaConfigurationException ex) {
                    throw new AbortSaveInterceptorException("errore nel reperire l'hostId associato al dominio", ex);
                }
                UserInformation userInfo = createUserInformation(firmePersona.getTramite(), firmaPersonaAdditionalData, null);
                Boolean haveCredentialBeenRemoved;
                haveCredentialBeenRemoved = removeCredential(firmePersona, userInfo);
                if (haveCredentialBeenRemoved) {
                    LOGGER.info("The user's credentials have been removed correctly from Aruba Credential Proxy");
                } else {
                    LOGGER.warn("The user's credentials haven't been removed correctly from Aruba Credential Proxy");
                }
            } catch (FirmaModuleException ex) {
                  throw new AbortSaveInterceptorException("errore eliminazione credenziali dal credential proxy", ex);
            }
        }
    }
    
   
    /*
    * Creazione delle informazioni utente per il Provider Aruba
    * prende in input il json con gli additionaldata che vengono dall'oggetto firmapersona
     */
    public UserInformation createUserInformation(FirmePersona.TramiteFirma tramiteFirma, FirmePersona.AdditionalDataFirma additionalData, String password) {
        UserInformation userInfo = null;
        switch (tramiteFirma) {
            case ARUBA:
                ArubaUserInformation arubaUserInfo = new ArubaUserInformation();
                arubaUserInfo.setUsername(additionalData.getUsername());
                arubaUserInfo.setPassword(password);
                arubaUserInfo.setDominioFirma(additionalData.getDominio());
                String modalitaAutenticazione = additionalData.getAutenticazione();
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
                userInfo = arubaUserInfo;
                break;
            case INFOCERT:
                InfocertUserInformation infocertUserInfo = new InfocertUserInformation();
                infocertUserInfo.setUsername(additionalData.getUsername());
                String modalitaInfocert = additionalData.getAutenticazione();
                switch (modalitaInfocert) {
                    case "OTP":
                        infocertUserInfo.setModalitaFirma(InfocertUserInformation.ModalitaFirma.OTP);
                        infocertUserInfo.setPassword(password);
                        break;
                    case "AUTO":
                    case "AUTOMATICA":
                        infocertUserInfo.setModalitaFirma(InfocertUserInformation.ModalitaFirma.AUTOMATICA);
                        break;
                }
                userInfo = infocertUserInfo;
                break;
            case NAMIRIAL:
                NamirialUserInformation namirialUserInformation = new NamirialUserInformation();
                namirialUserInformation.setUsername(additionalData.getUsername());
                String modalitaNamirial = additionalData.getAutenticazione();
                switch (modalitaNamirial) {
                    case "OTP":
                        namirialUserInformation.setModalitaFirma(NamirialUserInformation.ModalitaFirma.OTP);
                        namirialUserInformation.setPassword(password);
                        break;
                    case "AUTO":
                    case "AUTOMATICA":
                        namirialUserInformation.setModalitaFirma(NamirialUserInformation.ModalitaFirma.AUTOMATICA);
                        break;
                }
                userInfo = namirialUserInformation;
                break;
        }
        return userInfo;
    }
    
    private String getHostId(FirmePersona.TramiteFirma tramiteFirma, FirmePersona.AdditionalDataFirma additionalData) throws FirmaRemotaHttpException, FirmaRemotaConfigurationException {
        String hostId = null;
        switch (tramiteFirma) {
            case ARUBA:
                hostId = firmaRemotaArubaController.getHostIdFromDominio(DominioAruba.DominiAruba.valueOf(additionalData.getDominio()));
                break;
            case INFOCERT:
                hostId = additionalData.getHostId();
                break;
            case NAMIRIAL:
                hostId = additionalData.getHostId();
                break;
        }
        return hostId;
    }

    /**
     * Funzione che richiama il controller di internautautils.firma per settare le credenziali sul credential proxy (interno o esterno)
     * 
     * @param firmePersona
     * @param userInformation
     * @return true se le credenziali vengono settate correttamente, false altrimenti
     * @throws FirmaRemotaHttpException
     * @throws FirmaRemotaConfigurationException
     * @throws FirmaModuleException 
     */
    private Boolean setCredential(FirmePersona firmePersona, UserInformation userInformation) throws FirmaModuleException {
        Boolean areCredentialSet = false;
        FirmePersona.AdditionalDataFirma additionalData = firmePersona.getAdditionalData();
        try {
            areCredentialSet = firmaRemotaRestController.setCredential(userInformation, additionalData.getHostId());
        } catch (Exception ex) {
            String errorMessage = String.format("eccezione nel settaggio delle credenziali per l'hostId %s e username %s", 
                    additionalData.getHostId(), userInformation.getUsername());
            LOGGER.error(errorMessage, ex);
            throw new FirmaModuleException(errorMessage, ex);
        }
        return areCredentialSet;
    }

    /**
     * Funzione che richiama il controller di internautautils.firma per rimuovere le credenziali dal credential proxy
     * in input richiede l'azienda, le credenziali e il provider
     * @param firmePersona
     * @param userInformation
     * @return 
     */
    private Boolean removeCredential(FirmePersona firmePersona, UserInformation userInformation) throws FirmaModuleException {
        Boolean haveCredentialBeenRemoved = false;
        FirmePersona.AdditionalDataFirma additionalData = firmePersona.getAdditionalData();
        try {
            haveCredentialBeenRemoved = firmaRemotaRestController.removeCredential(userInformation, additionalData.getHostId());    
        } catch (FirmaRemotaHttpException | FirmaRemotaConfigurationException ex) {
            String errorMessage = String.format("eccezione nella rimozione delle credenziali per l'hostId %s e username %s", additionalData.getHostId(), userInformation.getUsername());
            LOGGER.error(errorMessage, ex);
            throw new FirmaModuleException(errorMessage, ex);
        }

        return haveCredentialBeenRemoved;
    }

}
