package it.bologna.ausl.internauta.service.interceptors.scrivania;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.dsl.BooleanExpression;
import it.bologna.ausl.internauta.service.authorization.AuthenticatedSessionData;
import it.bologna.ausl.internauta.service.authorization.UserInfoService;
import it.bologna.ausl.internauta.service.interceptors.InternautaBaseInterceptor;
import it.bologna.ausl.model.entities.scrivania.AttivitaFatta;
import it.bologna.ausl.model.entities.scrivania.QAttivitaFatta;
import it.nextsw.common.annotations.NextSdrInterceptor;
import it.nextsw.common.interceptors.exceptions.AbortLoadInterceptorException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 *
 * @author Sal
 * @Refactoring by gdm
 */
@Component
@NextSdrInterceptor(name = "attivitafatta-interceptor")
public class AttivitaFattaInterceptor extends InternautaBaseInterceptor {

    private static final String FROM = "&from=INTERNAUTA";
    
    @Autowired
    UserInfoService userInfoService;
    
    @Autowired
    ObjectMapper objectMapper;
    
    @Override
    public Class getTargetEntityClass() {
        return AttivitaFatta.class;
    }

    @Override
    public Predicate beforeSelectQueryInterceptor(Predicate initialPredicate, Map<String, String> additionalData, HttpServletRequest request, boolean mainEntity, Class projectionClass) throws AbortLoadInterceptorException {
        AuthenticatedSessionData authenticatedSessionData = getAuthenticatedUserProperties();
        BooleanExpression filterUtenteConnesso = QAttivitaFatta.attivitaFatta.idPersona.id.eq(authenticatedSessionData.getUser().getIdPersona().getId());
        List<Integer> collect = userInfoService.getUtentiPersonaByUtente(authenticatedSessionData.getUser()).stream().map(x -> x.getIdAzienda().getId()).collect(Collectors.toList());
        BooleanExpression filterUtenteAttivo = QAttivitaFatta.attivitaFatta.idAzienda.id.in(collect); 
//        List<Integer> collect = userInfoService.getUtentiPersona(user).stream().map(x -> x.getIdAzienda().getId()).collect(Collectors.toList());
        
        return filterUtenteConnesso.and(filterUtenteAttivo).and(initialPredicate);
    }
    
    /*
     * commentato perché per ora non vogliamo mostrare i link nella attività nello storico, non cancellarlo perché si prevedono cambiamenti di idea
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
        AttivitaFatta attivitaFatta = (AttivitaFatta) entity;

        // Se sono attività, o notifiche di applicazioni pico/dete/deli, allora...
        if (attivitaFatta.getTipo().equals(AttivitaFatta.TipoAttivitaFatta.ATTIVITA.toString()) 
                || (attivitaFatta.getTipo().equals(AttivitaFatta.TipoAttivitaFatta.NOTIFICA.toString())
                    && (attivitaFatta.getIdApplicazione().getId().equals(AttivitaFatta.IdApplicazione.PICO.toString()) 
                    || attivitaFatta.getIdApplicazione().getId().equals(AttivitaFatta.IdApplicazione.DELI.toString())
                    || attivitaFatta.getIdApplicazione().getId().equals(AttivitaFatta.IdApplicazione.DETE.toString())
                    ))) {
            try {
                // composizione dell'indirizzo dell'azienda di destinazione

                AziendaParametriJson parametriAziendaTarget = AziendaParametriJson.parse(this.objectMapper, attivitaFatta.getIdAzienda().getParametri());
                targetLoginPath = parametriAziendaTarget.getLoginPath();
//                targetLoginPath = HTTPS + InternautaUtils.getURLByIdAzienda(attivita.getIdAzienda());
            } catch (IOException ex) {
                throw new AbortLoadInterceptorException("errore nella lettura dei parametri dell'azienda target", ex);
            }

            // composizione dell'applicazione (es: /Procton/Procton.htm)
            applicationURL = attivitaFatta.getIdApplicazione().getBaseUrl() + "/" + attivitaFatta.getIdApplicazione().getIndexPage();

            try {
                if (attivitaFatta.getUrls() != null) {
                    List urls = objectMapper.readValue(attivitaFatta.getUrls(), List.class);
//                        jsonArray = (JSONArray) parser.parse(attivita.getUrls());
                    List compiledUrls;
                    if (urls != null) {
                        if (StringUtils.hasText(attivitaFatta.getCompiledUrls())) {
                            compiledUrls = objectMapper.readValue(attivitaFatta.getCompiledUrls(), ArrayList.class);
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
                        attivitaFatta.setCompiledUrls(objectMapper.writeValueAsString(compiledUrls));
                    }
                }
            } catch (Exception ex) {
                throw new AbortLoadInterceptorException("errore in AttivitaInterceptor in afterSelectQueryInterceptor: ", ex);
            }
        }
        return attivitaFatta;
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
    */
}
