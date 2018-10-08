package it.bologna.ausl.internauta.service.interceptors.scrivania;

import it.bologna.ausl.internauta.service.authorization.TokenBasedAuthentication;
import it.bologna.ausl.model.entities.baborg.Azienda;
import it.bologna.ausl.model.entities.baborg.Utente;
import it.bologna.ausl.model.entities.scrivania.Attivita;
import it.nextsw.common.annotations.NextSdrInterceptor;
import it.nextsw.common.interceptors.NextSdrEmptyControllerInterceptor;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.http.HttpServletRequest;
import org.jose4j.json.internal.json_simple.JSONArray;
import org.jose4j.json.internal.json_simple.JSONObject;
import org.jose4j.json.internal.json_simple.parser.JSONParser;
import org.jose4j.json.internal.json_simple.parser.ParseException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 *
 * @author Sal
 */
@Component
@NextSdrInterceptor(name = "attivita-interceptor")
public class AttivitaInterceptor extends NextSdrEmptyControllerInterceptor {

    private static final String LOGIN_SSO_URL = "/Shibboleth.sso/Login?entityID=";
    private static final String SSO_TARGET = "/idp/shibboleth&target=";
    private static final String FROM = "&from=INTERNAUTA";
    private static final String HTTPS = "https://";

    @Override
    public Class getTargetEntityClass() {
        return Attivita.class;
    }

    @Override
    public Collection<Object> afterSelectQueryInterceptor(Collection<Object> entities, Map<String, String> additionalData, HttpServletRequest request) {

        String destinationURL;
        String fromURL;
        String applicationURL;
        String randomGuid = UUID.randomUUID().toString();
        JSONArray jsonArray;

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        TokenBasedAuthentication authentication2 = (TokenBasedAuthentication) SecurityContextHolder.getContext().getAuthentication();
        Utente utente = (Utente) authentication.getPrincipal();
        Utente realUtente = utente;//(Utente) authentication2.getRealUser();
        int idSessionLog = 367660;//authentication2.getIdSessionLog();

        System.out.println("utente Connesso: " + utente.getUsername());
        System.out.println("real User: " + realUtente.getUsername());

        fromURL = HTTPS + getURLByIdAzienda(utente.getIdAzienda());

        for (Object entity : entities) {
            Attivita attivita = (Attivita) entity;

            if (attivita.getTipo().equals(Attivita.TipoAttivita.ATTIVITA.toString())) {

                destinationURL = HTTPS + getURLByIdAzienda(attivita.getIdAzienda());

                applicationURL = attivita.getIdApplicazione().getBaseUrl() + "/" + attivita.getIdApplicazione().getIndexPage();

                JSONParser parser = new JSONParser();

                try {

                    if (attivita.getUrls() != null) {
                        jsonArray = (JSONArray) parser.parse(attivita.getUrls());

                        if (jsonArray != null) {

                            ArrayList<String> res = new ArrayList<>();

                            for (int i = 0; i < jsonArray.size(); i++) {

                                JSONObject json = (JSONObject) jsonArray.get(i);
                                if (json != null && !json.toString().equals("")) {
                                    String urlAttivita = (String) json.get("url");

                                    String stringToEncode = applicationURL + urlAttivita;

                                    stringToEncode += "&utente=" + utente.getCodiceFiscale();

                                    stringToEncode += "&richiesta=" + randomGuid;

                                    stringToEncode += "&utenteLogin=" + realUtente.getCodiceFiscale();

                                    stringToEncode += "&utenteImpersonato=" + utente.getCodiceFiscale();

                                    stringToEncode += "&idSessionLog=" + idSessionLog;

                                    stringToEncode += this.FROM;

                                    stringToEncode += "&modalitaAmministrativa=0";

                                    String encode = URLEncoder.encode(stringToEncode, "UTF-8");
                                    String assembledURL = destinationURL + LOGIN_SSO_URL + fromURL + SSO_TARGET + encode;

                                    System.out.println("assembledURL -> " + assembledURL);

                                    json.put("url", assembledURL);
                                }
                                jsonArray.set(i, json);

                            }
                            // risetto gli url aggiornati
                            attivita.setUrls(jsonArray.toJSONString());
                        }
                    }
                } catch (ParseException ex) {
                    Logger.getLogger(AttivitaInterceptor.class.getName()).log(Level.SEVERE, null, ex);
                } catch (UnsupportedEncodingException ex) {
                    Logger.getLogger(AttivitaInterceptor.class.getName()).log(Level.SEVERE, null, ex);
                }
            }

        }

        return entities;
    }

    private String getURLByIdAzienda(Azienda azienda) {
        String res = null;

        String[] paths = azienda.getPath();
        if (paths != null && paths.length > 0) {
            res = paths[0];
        }
        return res;
    }

}
