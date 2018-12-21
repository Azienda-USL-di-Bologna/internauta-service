package it.bologna.ausl.internauta.service.interceptors.scrivania;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.dsl.BooleanExpression;
import it.bologna.ausl.internauta.service.interceptors.InternautaBaseInterceptor;
import it.bologna.ausl.model.entities.scrivania.Attivita;
import it.bologna.ausl.model.entities.scrivania.QAttivita;
import it.nextsw.common.annotations.NextSdrInterceptor;
import it.nextsw.common.interceptors.exceptions.AbortLoadInterceptorException;
import java.net.URLEncoder;
import java.util.Collection;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Autowired;
import it.bologna.ausl.internauta.service.authorization.UserInfoService;
import it.bologna.ausl.internauta.service.utils.InternautaConstants;
import it.bologna.ausl.model.entities.baborg.AziendaParametriJson;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.util.StringUtils;


/**
 *
 * @author Sal
 * @Refactoring by gdm
 */
@Component
@NextSdrInterceptor(name = "attivita-interceptor")
public class AttivitaInterceptor extends InternautaBaseInterceptor {

    private static final String FROM = "&from=INTERNAUTA";

    @Autowired
    UserInfoService userInfoService;
        
    @Autowired
    ObjectMapper objectMapper;

    @Override
    public Class getTargetEntityClass() {
        return Attivita.class;
    }
    
    
    @Override
    public Predicate beforeSelectQueryInterceptor(Predicate initialPredicate, Map<String, String> additionalData, HttpServletRequest request) throws AbortLoadInterceptorException {
        getAuthenticatedUserProperties();
        BooleanExpression filterUtenteConnesso = QAttivita.attivita.idPersona.id.eq(user.getIdPersona().getId());
        List<Integer> collect = userInfoService.getUtentiPersona(user).stream().map(x -> x.getIdAzienda().getId()).collect(Collectors.toList());
        BooleanExpression filterUtenteAttivo = QAttivita.attivita.idAzienda.id.in(collect);   
        
        return filterUtenteConnesso.and(filterUtenteAttivo).and(initialPredicate);
    }

    @Override
    public Object afterSelectQueryInterceptor(Object entity, Map<String, String> additionalData, HttpServletRequest request) throws AbortLoadInterceptorException {
        getAuthenticatedUserProperties();
        AziendaParametriJson parametriAziendaOrigine = (AziendaParametriJson) this.httpSessionData.getData(InternautaConstants.HttpSessionData.Keys.ParametriAzienda);
        if (parametriAziendaOrigine == null) {
            try {
                parametriAziendaOrigine = AziendaParametriJson.parse(this.objectMapper, user.getIdAzienda().getParametri());
                this.httpSessionData.putData(InternautaConstants.HttpSessionData.Keys.ParametriAzienda, parametriAziendaOrigine);
            }
            catch (IOException ex) {
                throw new AbortLoadInterceptorException("errore nella lettura dei parametri dell'azienda origine", ex);
            }
        }
        String targetLoginPath;
        String applicationURL;
        String entityId = parametriAziendaOrigine.getEntityId();
        String crossLoginUrlTemplate = parametriAziendaOrigine.getCrossLoginUrlTemplate();
        Attivita attivita = (Attivita) entity;

        // Se sono attività, o notifiche di applicazioni pico/dete/deli, allora...
        if (attivita.getTipo().equals(Attivita.TipoAttivita.ATTIVITA.toString())
                || (attivita.getTipo().equals(Attivita.TipoAttivita.NOTIFICA.toString())
                && attivita.getIdApplicazione().getId().equals(Attivita.IdApplicazione.PICO.toString())
                || attivita.getIdApplicazione().getId().equals(Attivita.IdApplicazione.DETE.toString())
                || attivita.getIdApplicazione().getId().equals(Attivita.IdApplicazione.DELI.toString()))) {
            try {
                // composizione dell'indirizzo dell'azienda di destinazione

                AziendaParametriJson parametriAziendaTarget = AziendaParametriJson.parse(this.objectMapper, attivita.getIdAzienda().getParametri());
                targetLoginPath = parametriAziendaTarget.getLoginPath();
//                targetLoginPath = HTTPS + InternautaUtils.getURLByIdAzienda(attivita.getIdAzienda());
            } catch (IOException ex) {
                throw new AbortLoadInterceptorException("errore nella lettura dei parametri dell'azienda target", ex);
            }

            // composizione dell'applicazione (es: /Procton/Procton.htm)
            applicationURL = attivita.getIdApplicazione().getBaseUrl() + "/" + attivita.getIdApplicazione().getIndexPage();

            try {
                if (attivita.getUrls() != null) {
                    List urls = objectMapper.readValue(attivita.getUrls(), List.class);
//                        jsonArray = (JSONArray) parser.parse(attivita.getUrls());
                    List compiledUrls;
                    if (urls != null) {
                        if (StringUtils.hasText(attivita.getCompiledUrls())) {
                            compiledUrls = objectMapper.readValue(attivita.getCompiledUrls(), ArrayList.class);
                        }
                        else {
                            compiledUrls = new ArrayList();
                        }
                        // per ogni url del campo urls di attivita, componi e fai encode dell'url calcolato
                        for (Object url: urls) {
                            Map compiledUrlMap = new HashMap();
                            Map urlMap = (Map) url;
                            if (!urlMap.isEmpty()) {
                                compiledUrlMap.putAll(urlMap);
                                String urlAttivita = (String) urlMap.get("url");

                                String stringToEncode = urlAttivita;

                                stringToEncode += "&utente=" + person.getCodiceFiscale();

                                // stringToEncode += "&richiesta=" + UUID.randomUUID().toString();
                                stringToEncode += "&utenteLogin=" + realPerson.getCodiceFiscale();

                                stringToEncode += "&utenteImpersonato=" + person.getCodiceFiscale();

                                stringToEncode += "&idSessionLog=" + idSessionLog;

                                stringToEncode += FROM;

                                stringToEncode += "&modalitaAmministrativa=0";

                                String encodedParams = URLEncoder.encode(stringToEncode, "UTF-8");
//                                    String assembledURL = destinationURL + LOGIN_SSO_URL + fromURL + SSO_TARGET + applicationURL + encode;
                                String assembledURL = crossLoginUrlTemplate.
                                        replace("[target-login-path]", targetLoginPath).
                                        replace("[entity-id]", entityId).
                                        replace("[app]", applicationURL).
                                        replace("[encoded-params]", encodedParams);
                                compiledUrlMap.put("url", assembledURL);
                                compiledUrls.add(compiledUrlMap);
                            }
                        }
                        // risetta gli urls aggiornati
                        attivita.setCompiledUrls(objectMapper.writeValueAsString(compiledUrls));
                    }
                }
            } catch (Exception ex) {
                throw new AbortLoadInterceptorException("errore in AttivitaInterceptor in afterSelectQueryInterceptor: ", ex);
            }
        }
        return attivita;
    }

    
    
    @Override
    public Collection<Object> afterSelectQueryInterceptor(Collection<Object> entities, Map<String, String> additionalData, HttpServletRequest request) throws AbortLoadInterceptorException {
        // si prende utente reale e utente impersonato dal token
        getAuthenticatedUserProperties();
        
        for (Object entity : entities) {
            this.afterSelectQueryInterceptor(entity, additionalData, request);
        }
        return entities;
    }
}
