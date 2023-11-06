package it.bologna.ausl.model.entities.baborg.projections.azienda;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.bologna.ausl.blackbox.exceptions.BlackBoxPermissionException;
import it.bologna.ausl.internauta.service.authorization.AuthenticatedSessionData;
import it.bologna.ausl.internauta.service.authorization.AuthenticatedSessionDataBuilder;
import it.bologna.ausl.internauta.service.utils.HttpSessionData;
import it.bologna.ausl.internauta.service.utils.InternautaConstants;
import it.bologna.ausl.internauta.service.utils.InternautaUtils;
import it.bologna.ausl.internauta.utils.parameters.manager.ParametriAziendeReader;
import it.bologna.ausl.model.entities.baborg.Azienda;
import it.bologna.ausl.model.entities.baborg.AziendaParametriJson;
import it.bologna.ausl.model.entities.baborg.Utente;
import it.bologna.ausl.model.entities.configurazione.Applicazione;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

/**
 *
 * @author Top
 */
@Component
public class AziendaProjectionUtils {

    @Autowired
    private AuthenticatedSessionDataBuilder authenticatedSessionDataBuilder;

    @Autowired
    private ParametriAziendeReader parametriAziende;
    
    @Autowired
    private InternautaUtils internautaUtils;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @Autowired
    private HttpSessionData httpSessionData;
    
    private static final Logger LOGGER = LoggerFactory.getLogger(AziendaProjectionUtils.class);

    @Cacheable(value = "getParametriAzienda", key = "{#azienda.getId()}")
    public Map<String, Object> getParametriAzienda(Azienda azienda) throws BlackBoxPermissionException {
        AuthenticatedSessionData authenticatedSessionData = authenticatedSessionDataBuilder.getAuthenticatedUserProperties();
        Applicazione.Applicazioni applicazione = authenticatedSessionData.getApplicazione();

        Map<String, Object> parametri = parametriAziende.getAllAziendaApplicazioneParameters(applicazione.toString(), azienda.getId(), false);

        return parametri;
    }
    
    /**
     * Restituisce gli url da mettere nelle aziende dell'utente, per chiamare le
     * funzioni dell'onCommand sulle applicazioni Inde
     *
     * @param aziendaTarget
     * @return
     * @throws IOException
     * @throws it.bologna.ausl.blackbox.exceptions.BlackBoxPermissionException
     */
    public Map<String, String> getUrlCommands(Azienda aziendaTarget) throws IOException, BlackBoxPermissionException {
        AuthenticatedSessionData authenticatedSessionData = authenticatedSessionDataBuilder.getAuthenticatedUserProperties();

        Map<String, String> result = new HashMap<>();

//        Utente utente = (Utente) httpSessionData.getData(InternautaConstants.HttpSessionData.Keys.UtenteLogin);
//        Integer idSessionLog = (Integer) httpSessionData.getData(InternautaConstants.HttpSessionData.Keys.IdSessionLog);
////        crossLoginUrlTemplate = "http://localhost:8080/Procton/Procton.htm?CMD=[encoded-params]";  // TODO: REMOVE, ONLY FOR LOCAL TESTS
//        Persona realPerson = null;
//        if (utente.getUtenteReale() != null) {
//            realPerson = utente.getUtenteReale().getIdPersona();
//        }
//        Persona person = utente.getIdPersona();
//        Azienda aziendaLogin = utente.getIdAzienda();
        result.put(InternautaConstants.UrlCommand.Keys.PROTOCOLLA_PEC_NEW.toString(),
                internautaUtils.getUrl(authenticatedSessionData, "?CMD=ricevi_from_pec;[id_message]&id_sorgente=[id_sorgente]&pec_ricezione=[pec_ricezione]", "procton", aziendaTarget));
        result.put(InternautaConstants.UrlCommand.Keys.PROTOCOLLA_PEC_ADD.toString(),
                internautaUtils.getUrl(authenticatedSessionData, "?CMD=add_from_pec;[id_message]&id_sorgente=[id_sorgente]&pec_ricezione=[pec_ricezione]", "procton", aziendaTarget));
        result.put(InternautaConstants.UrlCommand.Keys.ARCHIVE_MESSAGE.toString(),
                internautaUtils.getUrl(authenticatedSessionData, "?CMD=fascicola_shpeck;[id_message]", "babel", aziendaTarget));
        return result;
    }
    
    /**
     * Data un azienda torna il baseUrl corretto a secondo se si è fatto il
     * login da internet oppure no
     *
     * @param azienda
     * @return
     * @throws IOException
     * @throws BlackBoxPermissionException
     */
    public String getBaseUrl(Azienda azienda) throws IOException, BlackBoxPermissionException {
        AuthenticatedSessionData authenticatedSessionData = authenticatedSessionDataBuilder.getAuthenticatedUserProperties();
        String baseUrl;
        AziendaParametriJson aziendaParams = azienda.getParametri();
        if (authenticatedSessionData.isFromInternet()) {
            baseUrl = aziendaParams.getInternetBasePath();
        } else {
            baseUrl = aziendaParams.getBasePath();
        }
        return baseUrl;
    }
    
    /**
     * restituisce i parametri dell'azienda che servono al front end e non
     * contengono informazioni sensibili
     *
     * @return
     * @throws java.io.IOException
     * @throws it.bologna.ausl.blackbox.exceptions.BlackBoxPermissionException
     */
    public Map<String, String> getParametriAziendaFrontEnd() throws IOException, BlackBoxPermissionException {
        AuthenticatedSessionData authenticatedSessionData = authenticatedSessionDataBuilder.getAuthenticatedUserProperties();
        LOGGER.info("getParametriAziendaFrontEnd authenticatedSessionData.isFromInternet(): " + authenticatedSessionData.isFromInternet());
        final String LOGOUT_URL_KEY = "logoutUrl";

        Map<String, String> result = new HashMap<>();

        Utente utente = (Utente) httpSessionData.getData(InternautaConstants.HttpSessionData.Keys.UtenteLogin);

        AziendaParametriJson parametri = utente.getIdAzienda().getParametri();
        if (authenticatedSessionData.isFromInternet()) {
            try {
                parametri.setBasePath(parametri.getInternetBasePath());
                parametri.setLogoutUrl(parametri.getInternetLogoutUrl());
            } catch (Exception ex) {
                LOGGER.error("errore nel reperimento di isFromInternet", ex);
            }
        }

        result.put(LOGOUT_URL_KEY, parametri.getLogoutUrl());

        return result;
    }
}
