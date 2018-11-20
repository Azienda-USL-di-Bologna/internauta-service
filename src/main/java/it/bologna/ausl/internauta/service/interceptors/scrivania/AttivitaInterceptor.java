package it.bologna.ausl.internauta.service.interceptors.scrivania;

import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.dsl.BooleanExpression;
import it.bologna.ausl.internauta.service.interceptors.InternautaBaseInterceptor;
import it.bologna.ausl.internauta.service.utils.InternautaUtils;
import it.bologna.ausl.model.entities.scrivania.Attivita;
import it.bologna.ausl.model.entities.scrivania.QAttivita;
import it.nextsw.common.annotations.NextSdrInterceptor;
import it.nextsw.common.interceptors.exceptions.AbortLoadInterceptorException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Collection;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import org.jose4j.json.internal.json_simple.JSONArray;
import org.jose4j.json.internal.json_simple.JSONObject;
import org.jose4j.json.internal.json_simple.parser.JSONParser;
import org.jose4j.json.internal.json_simple.parser.ParseException;
import org.springframework.stereotype.Component;

/**
 *
 * @author Sal
 */
@Component
@NextSdrInterceptor(name = "attivita-interceptor")
public class AttivitaInterceptor extends InternautaBaseInterceptor {

    private static final String LOGIN_SSO_URL = "/Shibboleth.sso/Login?entityID=";
    private static final String SSO_TARGET = "/idp/shibboleth&target=";
    private static final String FROM = "&from=INTERNAUTA";
    private static final String HTTPS = "https://";

    @Override
    public Class getTargetEntityClass() {
        return Attivita.class;
    }
    
    @Override
    public Predicate beforeSelectQueryInterceptor(Predicate initialPredicate, Map<String, String> additionalData, HttpServletRequest request) throws AbortLoadInterceptorException {
        getAuthenticatedUserProperties();
        BooleanExpression filterUtenteConnesso = QAttivita.attivita.idPersona.id.eq(user.getIdPersona().getId());
        return filterUtenteConnesso.and(initialPredicate);
    }

    @Override
    public Collection<Object> afterSelectQueryInterceptor(Collection<Object> entities, Map<String, String> additionalData, HttpServletRequest request) throws AbortLoadInterceptorException {

        String destinationURL;
        String fromURL;
        String applicationURL;
        JSONArray jsonArray;

        // si prende utente reale e utente impersonato dal token
        getAuthenticatedUserProperties();

        // composizione dell'indirizzo dell'azienda di provenienza
        fromURL = HTTPS + InternautaUtils.getURLByIdAzienda(user.getIdAzienda());

        for (Object entity : entities) {
            Attivita attivita = (Attivita) entity;

            // Se sono attività, o notifiche di applicazioni pico/dete/deli, allora...
            if (attivita.getTipo().equals(Attivita.TipoAttivita.ATTIVITA.toString())
                    || (attivita.getTipo().equals(Attivita.TipoAttivita.NOTIFICA.toString())
                    && attivita.getIdApplicazione().getId().equals(Attivita.IdApplicazione.PICO.toString())
                    || attivita.getIdApplicazione().getId().equals(Attivita.IdApplicazione.DETE.toString())
                    || attivita.getIdApplicazione().getId().equals(Attivita.IdApplicazione.DELI.toString()))) {
                // composizione dell'indirizzo dell'azienda di destinazione
                destinationURL = HTTPS + InternautaUtils.getURLByIdAzienda(attivita.getIdAzienda());

                // composizione dell'applicazione (es: /Procton/Procton.htm)
                applicationURL = attivita.getIdApplicazione().getBaseUrl() + "/" + attivita.getIdApplicazione().getIndexPage();

                JSONParser parser = new JSONParser();

                try {
                    if (attivita.getUrls() != null) {
                        jsonArray = (JSONArray) parser.parse(attivita.getUrls());

                        if (jsonArray != null) {
                            // per ogni url del campo urls di attivita, componi e fai encode dell'url calcolato
                            for (int i = 0; i < jsonArray.size(); i++) {

                                JSONObject json = (JSONObject) jsonArray.get(i);
                                if (json != null && !json.toString().equals("")) {
                                    String urlAttivita = (String) json.get("url");

                                    String stringToEncode = urlAttivita;

                                    stringToEncode += "&utente=" + person.getCodiceFiscale();

                                    // stringToEncode += "&richiesta=" + UUID.randomUUID().toString();
                                    stringToEncode += "&utenteLogin=" + realPerson.getCodiceFiscale();

                                    stringToEncode += "&utenteImpersonato=" + person.getCodiceFiscale();

                                    stringToEncode += "&idSessionLog=" + idSessionLog;

                                    stringToEncode += this.FROM;

                                    stringToEncode += "&modalitaAmministrativa=0";

                                    String encode = URLEncoder.encode(stringToEncode, "UTF-8");
                                    String assembledURL = destinationURL + LOGIN_SSO_URL + fromURL + SSO_TARGET + applicationURL + encode;

                                    json.put("url", assembledURL);
                                }
                                jsonArray.set(i, json);
                            }
                            // risetta gli urls aggiornati
                            attivita.setUrls(jsonArray.toJSONString());
                        }
                    }
                } catch (ParseException | UnsupportedEncodingException ex) {
                    throw new AbortLoadInterceptorException("errore in AttivitaInterceptor in afterSelectQueryInterceptor: ", ex);
                }
            }
        }
        return entities;
    }
}
