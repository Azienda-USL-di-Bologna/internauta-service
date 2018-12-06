package it.bologna.ausl.internauta.service.interceptors.scrivania;

import it.bologna.ausl.internauta.service.interceptors.InternautaBaseInterceptor;
import it.bologna.ausl.internauta.service.utils.InternautaConstants;
import it.bologna.ausl.internauta.service.utils.InternautaUtils;
import it.bologna.ausl.internauta.service.utils.ParametriAziende;
import it.bologna.ausl.model.entities.configuration.ParametroAziende;
import it.bologna.ausl.model.entities.scrivania.AttivitaFatta;
import it.nextsw.common.annotations.NextSdrInterceptor;
import it.nextsw.common.interceptors.exceptions.AbortLoadInterceptorException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.servlet.http.HttpServletRequest;
import org.jose4j.json.internal.json_simple.JSONArray;
import org.jose4j.json.internal.json_simple.JSONObject;
import org.jose4j.json.internal.json_simple.parser.JSONParser;
import org.jose4j.json.internal.json_simple.parser.ParseException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 *
 * @author Sal
 */
@Component
@NextSdrInterceptor(name = "attivitafatta-interceptor")
public class AttivitaFattaInterceptor extends InternautaBaseInterceptor {

    private static final String LOGIN_SSO_URL = "/Shibboleth.sso/Login?entityID=";
    private static final String SSO_TARGET = "/idp/shibboleth&target=";
    private static final String FROM = "&from=INTERNAUTA";
    private static final String HTTPS = "https://";

    @Autowired
    ParametriAziende parametriAziende;
    
    @Override
    public Class getTargetEntityClass() {
        return AttivitaFatta.class;
    }

    @Override
    public Collection<Object> afterSelectQueryInterceptor(Collection<Object> entities, Map<String, String> additionalData, HttpServletRequest request) throws AbortLoadInterceptorException {

        String destinationURL;
        String fromURL;
        String applicationURL;
        String randomGuid = UUID.randomUUID().toString();
        JSONArray jsonArray;
        String crossUrlTemplate;
        
        try {
            List<ParametroAziende> parametriAzienda = parametriAziende.getParameters(InternautaConstants.Configurazione.ParametriAzienda.crossUrlTemplate.toString());
            ParametroAziende parametroAzienda = parametriAzienda.get(0);
            crossUrlTemplate = parametriAziende.getValue(parametroAzienda, String.class);
        }
        catch (IOException ex) {
            throw new AbortLoadInterceptorException("errore nella lettura del crossUrlTemplate", ex);
        }

        // si prende utente reale e utente impersonato dal token
        getAuthenticatedUserProperties();
        
        // composizione dell'indirizzo dell'azienda di provenienza
        fromURL = HTTPS + InternautaUtils.getURLByIdAzienda(user.getIdAzienda());

        for (Object entity : entities) {
            AttivitaFatta attivitaFatta = (AttivitaFatta) entity;

            // Se sono attività, o notifiche di applicazioni pico/dete/deli, allora...
            if (attivitaFatta.getTipo().equals(AttivitaFatta.TipoAttivitaFatta.ATTIVITA.toString()) 
                || (attivitaFatta.getTipo().equals(AttivitaFatta.TipoAttivitaFatta.NOTIFICA.toString())
                    && (attivitaFatta.getIdApplicazione().getId().equals(AttivitaFatta.IdApplicazione.PICO.toString()) 
                    || attivitaFatta.getIdApplicazione().getId().equals(AttivitaFatta.IdApplicazione.DELI.toString())
                    || attivitaFatta.getIdApplicazione().getId().equals(AttivitaFatta.IdApplicazione.DETE.toString())
                    ))) {
                // composizione dell'indirizzo dell'azienda di destinazione
                destinationURL = HTTPS + InternautaUtils.getURLByIdAzienda(attivitaFatta.getIdAzienda());

                // composizione dell'applicazione (es: /Procton/Procton.htm)
                applicationURL = attivitaFatta.getIdApplicazione().getBaseUrl() + "/" + attivitaFatta.getIdApplicazione().getIndexPage();

                JSONParser parser = new JSONParser();

                try {
                    if (attivitaFatta.getUrls() != null) {
                        jsonArray = (JSONArray) parser.parse(attivitaFatta.getUrls());

                        if (jsonArray != null) {
                            // per ogni url del campo urls di attivita, componi e fai encode dell'url calcolato
                            for (int i = 0; i < jsonArray.size(); i++) {

                                JSONObject json = (JSONObject) jsonArray.get(i);
                                if (json != null && !json.toString().equals("")) {
                                    String urlAttivitaFatta = (String) json.get("url");

                                    String stringToEncode = urlAttivitaFatta;

                                    stringToEncode += "&utente=" + person.getCodiceFiscale();

                                    // stringToEncode += "&richiesta=" + randomGuid;

                                    stringToEncode += "&utenteLogin=" + realPerson.getCodiceFiscale();

                                    stringToEncode += "&utenteImpersonato=" + person.getCodiceFiscale();

                                    stringToEncode += "&idSessionLog=" + idSessionLog;

                                    stringToEncode += FROM;

                                    stringToEncode += "&modalitaAmministrativa=0";

                                    String encodedParams = URLEncoder.encode(stringToEncode, "UTF-8");
//                                    String assembledURL = destinationURL + LOGIN_SSO_URL + fromURL + SSO_TARGET + applicationURL + encode;
                                    String assembledURL = crossUrlTemplate.
                                            replace("[target-path]", destinationURL).
                                            replace("[source-path]", fromURL).
                                            replace("[app]", applicationURL).
                                            replace("[encoded-params]", encodedParams);
                                    json.put("url", assembledURL);
                                }
                                jsonArray.set(i, json);
                            }
                            // risetta gli urls aggiornati
                            attivitaFatta.setUrls(jsonArray.toJSONString());
                        }
                    }
                } catch (ParseException | UnsupportedEncodingException ex) {
                    throw new AbortLoadInterceptorException("errore in AttivitaFattaInterceptor in afterSelectQueryInterceptor: ", ex);
                }
            }
        }
        return entities;
    }

}
