package it.bologna.ausl.internauta.service.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.bologna.ausl.internauta.service.authorization.AuthenticatedSessionData;
import it.bologna.ausl.internauta.model.bds.types.EntitaStoredProcedure;
import it.bologna.ausl.model.entities.baborg.Azienda;
import it.bologna.ausl.model.entities.baborg.AziendaParametriJson;
import it.bologna.ausl.model.entities.baborg.Persona;
import it.bologna.ausl.model.entities.baborg.Ruolo;
import it.bologna.ausl.model.entities.configurazione.Applicazione;
import java.io.IOException;
import java.net.URLEncoder;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 *
 * @author gdm
 */
@Component
public class InternautaUtils {

    private static final Logger log = LoggerFactory.getLogger(InternautaUtils.class);

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    CachedEntities cachedEntities;

    /**
     * Ottiene URL dell'azienda passata come parametro. Questo perchè ci possono
     * essere più url che si riferiscono alla stessa azienda, ma per il nostro
     * scopo basta sapere il primo.
     *
     * @param azienda
     * @return il primo URL dell'azienda corrispondente
     */
    public String getURLByIdAzienda(Azienda azienda) {
        String res = null;

        String[] paths = azienda.getPath();
        if (paths != null && paths.length > 0) {
            res = paths[0];
        }
        return res;
    }

    public String getUrl(AuthenticatedSessionData authenticatedSessionData, String urlToChange, String idApplicazione, Azienda aziendaTarget) throws IOException {

        Integer idSessionLog = authenticatedSessionData.getIdSessionLog();
        Persona realPerson = null;
        if (authenticatedSessionData.getRealPerson() != null) {
            realPerson = authenticatedSessionData.getRealPerson();
        }
        Persona person = authenticatedSessionData.getPerson();
        Azienda aziendaLogin = authenticatedSessionData.getUser().getIdAzienda();

        String paramsWithoutContextInformation = urlToChange;
        String paramsWithContextInformation = buildContextInformations(urlToChange, realPerson, person, aziendaLogin, idSessionLog);

        AziendaParametriJson parametriAziendaLogin = aziendaLogin.getParametri();
        AziendaParametriJson parametriAziendaTarget = aziendaTarget.getParametri();
        String crossLoginUrlTemplate = parametriAziendaTarget.getCrossLoginUrlTemplate();
        String simpleCrossLoginUrlTemplate = parametriAziendaTarget.getSimpleCrossLoginUrlTemplate();
        String entityId = parametriAziendaLogin.getEntityId();

        String targetLoginPath = parametriAziendaTarget.getLoginPath();
        String targetBasePath = parametriAziendaTarget.getBasePath();

//        log.info("getUrl authenticatedSessionData.isFromInternet(): " + authenticatedSessionData.isFromInternet());
        if (authenticatedSessionData.isFromInternet()) {
            targetBasePath = parametriAziendaTarget.getInternetBasePath();
        }

        String encodedParamsWithContextInformation = URLEncoder.encode(paramsWithContextInformation, "UTF-8");
        String encodedParamsWithoutContextInformation = URLEncoder.encode(paramsWithoutContextInformation, "UTF-8");

        Applicazione app = cachedEntities.getApplicazione(idApplicazione);
        String applicationURL = app.getBaseUrl();
        if (applicationURL != null) {
            String indexPage = app.getIndexPage();
            if (indexPage != null && indexPage.length() > 0) {
                applicationURL += "/" + indexPage;
            }
        } else {
            applicationURL = "";
        }

        String assembledURL = null;
        switch (app.getUrlGenerationStrategy()) {
            case TRUSTED_URL_WITH_CONTEXT_INFORMATION:
                assembledURL = crossLoginUrlTemplate.
                        replace("[target-login-path]", targetLoginPath).
                        replace("[entity-id]", entityId).
                        replace("[app]", applicationURL).
                        replace("[encoded-params]", encodedParamsWithContextInformation);
                break;
            case TRUSTED_URL_WITHOUT_CONTEXT_INFORMATION:
                assembledURL = crossLoginUrlTemplate.
                        replace("[target-login-path]", targetLoginPath).
                        replace("[entity-id]", entityId).
                        replace("[app]", applicationURL).
                        replace("[encoded-params]", encodedParamsWithoutContextInformation);
                break;
            case RELATIVE_WITH_CONTEXT_INFORMATION:
                assembledURL = simpleCrossLoginUrlTemplate.
                        replace("[target-login-path]", targetBasePath).
                        replace("[app]", applicationURL).
                        replace("[params]", paramsWithContextInformation);
                break;
            case RELATIVE_WITHOUT_CONTEXT_INFORMATION:
                assembledURL = simpleCrossLoginUrlTemplate.
                        replace("[target-login-path]", targetBasePath).
                        replace("[app]", applicationURL).
                        replace("[params]", paramsWithoutContextInformation);
                break;
            case ABSOLUTE_WITH_CONTEXT_INFORMATION:
                assembledURL = applicationURL + paramsWithContextInformation;
                break;
            case ABSOLUTE_WITHOUT_CONTEXT_INFORMATION:
                assembledURL = paramsWithoutContextInformation;
                break;
        }
        return assembledURL;
    }

    private String buildContextInformations(String url, Persona realPerson, Persona person, Azienda aziendaUser, Integer idSessionLog) {

        if (person.getCodiceFiscale() != null && person.getCodiceFiscale().length() > 0) {
            url += (url.length() > 0 && url.startsWith("?")) ? "&utente=" : "?utente=";
            url += person.getCodiceFiscale(); // non so se serve alle applicazioni INDE o a internauta o a tutti e 2
        }

        if (realPerson != null) {
            url += "&realUser=" + realPerson.getCodiceFiscale();
            url += "&impersonatedUser=" + person.getCodiceFiscale();
            url += "&utenteLogin=" + realPerson.getCodiceFiscale(); // serve alle applicazioni INDE
        } else {
            url += "&user=" + person.getCodiceFiscale();
            url += "&utenteLogin=" + person.getCodiceFiscale(); // serve alle applicazioni INDE
        }

        url += "&utenteImpersonato=" + person.getCodiceFiscale(); // serve alle applicazioni INDE
        url += "&idSessionLog=" + Integer.toString(idSessionLog);
        url += "&from=INTERNAUTA";
        url += "&modalitaAmministrativa=0"; // serve alle applicazioni INDE

        url += "&idAzienda=" + aziendaUser.getId();
        url += "&aziendaImpersonatedUser=" + aziendaUser.getId();

//        String url = "";
//        url += "&richiesta=[richiesta]";        
//        url += "&utenteImpersonato=" + user.getIdPersona().getCodiceFiscale();
//        if(user.getUtenteReale() != null ){
//            url += "&utenteLogin=" + user.getUtenteReale().getIdPersona().getCodiceFiscale();
//        } else {
//            url += "&utenteLogin=" + user.getIdPersona().getCodiceFiscale();
//        }
//        url += "&idSessionLog=" + idSessionLog;
//        url += from;
//        url += "&modalitaAmministrativa=0";
        return url;
    }

    public Object getEntityFromEntitaStoredProcedure(EntitaStoredProcedure entitaStoredProcedure) {

        return null;
    }

    public Integer getSommaMascheraBit(String ruoliNomeBreveString) {
        Integer res = 0;
        String[] ruoliSplitted = ruoliNomeBreveString.split(";");
        for (String ruoloNomeBreve : ruoliSplitted) {
            Ruolo ruolo = cachedEntities.getRuoloByNomeBreve(Ruolo.CodiciRuolo.valueOf(ruoloNomeBreve.toUpperCase()));
            Integer mascheraBit = ruolo.getMascheraBit();
            res += mascheraBit;
        }
        return res;
    }

    /**
     * E' la stessa regex che abbiamo su ProctonUtils
     *
     * @param emailAddress è la l'indirizzo mail secco da verificare
     * @return true se la regex valida l'indirizzo passato come parametro, false
     * altrimenti
     */
    public boolean isValidEmailAddress(String emailAddress) {
        Pattern p = Pattern.compile("^[a-zA-Z0-9_+\'&.+-]+@[a-zA-Z0-9-]+\\.[a-zA-Z0-9-.]+$");

        // Match the given string with the pattern
        Matcher m = p.matcher(emailAddress);

        // check whether match is found
        boolean matchFound = m.matches();

        String[] emailSplitted = emailAddress.split("\\.");
        String lastToken = emailSplitted[emailSplitted.length - 1];

        // validate the country code
        if (matchFound && lastToken.length() >= 2
                && emailAddress.length() - 1 != lastToken.length()) {
            return true;
        } else {
            return false;
        }
    }
}
