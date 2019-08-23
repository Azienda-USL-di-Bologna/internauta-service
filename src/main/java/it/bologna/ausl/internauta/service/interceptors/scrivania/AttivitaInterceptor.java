package it.bologna.ausl.internauta.service.interceptors.scrivania;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.dsl.BooleanExpression;
import it.bologna.ausl.internauta.service.authorization.AuthenticatedSessionData;
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
import it.bologna.ausl.internauta.service.repositories.scrivania.AttivitaFatteRepository;
import it.bologna.ausl.internauta.service.utils.InternautaConstants;
import it.bologna.ausl.model.entities.baborg.AziendaParametriJson;
import it.bologna.ausl.model.entities.scrivania.AttivitaFatta;
import it.nextsw.common.interceptors.exceptions.AbortSaveInterceptorException;
import it.nextsw.common.interceptors.exceptions.SkipDeleteInterceptorException;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;


/**
 *
 * @author Sal
 * @Refactoring by gdm
 */
@Component
@NextSdrInterceptor(name = "attivita-interceptor")
public class AttivitaInterceptor extends InternautaBaseInterceptor {

    private static final Logger LOGGER = LoggerFactory.getLogger(AttivitaInterceptor.class);
    
    private static final String FROM = "&from=INTERNAUTA";

    @Autowired
    UserInfoService userInfoService;
        
    @Autowired
    ObjectMapper objectMapper;
    
    @Autowired
    AttivitaFatteRepository attivitaFatteRepository;
    
    @PersistenceContext
    EntityManager em;

    @Override
    public Class getTargetEntityClass() {
        return Attivita.class;
    }
    
    
    @Override
    public Predicate beforeSelectQueryInterceptor(Predicate initialPredicate, Map<String, String> additionalData, HttpServletRequest request, boolean mainEntity, Class projectionClass) throws AbortLoadInterceptorException {
        AuthenticatedSessionData authenticatedSessionData = super.getAuthenticatedUserProperties();
        BooleanExpression filterUtenteConnesso = QAttivita.attivita.idPersona.id.eq(authenticatedSessionData.getUser().getIdPersona().getId());
        List<Integer> collect = userInfoService.getUtentiPersonaByUtente(authenticatedSessionData.getUser()).stream().map(
                x -> 
                        x.getIdAzienda().getId()
        ).collect(Collectors.toList());
        BooleanExpression filterUtenteAttivo = QAttivita.attivita.idAzienda.id.in(collect);   
        
        return filterUtenteConnesso.and(filterUtenteAttivo).and(initialPredicate);
    }

    @Override
    public Object afterSelectQueryInterceptor(Object entity, Map<String, String> additionalData, HttpServletRequest request, boolean mainEntity, Class projectionClass) throws AbortLoadInterceptorException {
        AuthenticatedSessionData authenticatedSessionData = super.getAuthenticatedUserProperties();
        LOGGER.info(String.format("pre afterSelectQueryInterceptor on Attivita user: %d person: %s", authenticatedSessionData.getUser().getId(), authenticatedSessionData.getPerson().getCodiceFiscale()) );
        getAuthenticatedUserProperties();
        LOGGER.info(String.format("after afterSelectQueryInterceptor on Attivita user: %d person: %s", authenticatedSessionData.getUser().getId(), authenticatedSessionData.getPerson().getCodiceFiscale()) );
        AziendaParametriJson parametriAziendaOrigine = (AziendaParametriJson) this.httpSessionData.getData(InternautaConstants.HttpSessionData.Keys.ParametriAzienda);
        if (parametriAziendaOrigine == null) {
            try {
                parametriAziendaOrigine = AziendaParametriJson.parse(this.objectMapper, authenticatedSessionData.getUser().getIdAzienda().getParametri());
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

                                stringToEncode += "&utente=" + authenticatedSessionData.getPerson().getCodiceFiscale(); // non so se serve alle applicazioni INDE o a internauta o a tutti e 2

                                // stringToEncode += "&richiesta=" + UUID.randomUUID().toString();
                                if (authenticatedSessionData.getRealPerson() != null) {
                                    stringToEncode += "&realUser=" + authenticatedSessionData.getRealPerson().getCodiceFiscale();
                                    stringToEncode += "&impersonatedUser=" + authenticatedSessionData.getPerson().getCodiceFiscale();
                                    stringToEncode += "&utenteLogin=" + authenticatedSessionData.getRealPerson().getCodiceFiscale(); // serve alle applicazioni INDE
                                } else {
                                    stringToEncode += "&user=" + authenticatedSessionData.getPerson().getCodiceFiscale();
                                    stringToEncode += "&utenteLogin=" + authenticatedSessionData.getPerson().getCodiceFiscale(); // serve alle applicazioni INDE
                                }

                                stringToEncode += "&utenteImpersonato=" + authenticatedSessionData.getPerson().getCodiceFiscale(); // serve alle applicazioni INDE

                                stringToEncode += "&idSessionLog=" + authenticatedSessionData.getIdSessionLog();

                                stringToEncode += FROM;

                                stringToEncode += "&modalitaAmministrativa=0"; // serve alle applicazioni INDE

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
    public Collection<Object> afterSelectQueryInterceptor(Collection<Object> entities, Map<String, String> additionalData, HttpServletRequest request, boolean mainEntity, Class projectionClass) throws AbortLoadInterceptorException {
        // si prende utente reale e utente impersonato dal token
        getAuthenticatedUserProperties();
        
        for (Object entity : entities) {
            this.afterSelectQueryInterceptor(entity, additionalData, request, mainEntity, projectionClass);
        }
        return entities;
    }

    @Override
    public void beforeDeleteEntityInterceptor(Object entity, Map<String, String> additionalData, HttpServletRequest request, boolean mainEntity, Class projectionClass) throws AbortSaveInterceptorException, SkipDeleteInterceptorException {
        AuthenticatedSessionData authenticatedSessionData = getAuthenticatedUserProperties();
        
        Attivita attivita = (Attivita) entity;
        if(!attivita.getTipo().equals("notifica")){
            throw new AbortSaveInterceptorException("La riga che si sta tentando di eliminare non è una notifica");
        }
        
        if(!authenticatedSessionData.getPerson().getId().equals(attivita.getIdPersona().getId())) {
            throw new AbortSaveInterceptorException("non hai il permesso di eliminare la notifica");
        }
        
//        AttivitaFatta attivitaFatta = new AttivitaFatta();
//        attivitaFatta.setDatiAggiuntivi(attivita.getDatiAggiuntivi());
//        attivitaFatta.setDescrizione(attivita.getDescrizione());
//        attivitaFatta.setIdApplicazione(attivita.getIdApplicazione());
//        attivitaFatta.setIdAzienda(attivita.getIdAzienda());
//        attivitaFatta.setIdPersona(attivita.getIdPersona());
//        attivitaFatta.setNote(attivita.getNote());
//        attivitaFatta.setOggetto(attivita.getOggetto());
//        attivitaFatta.setOggettoEsterno(attivita.getOggettoEsterno());
//        attivitaFatta.setOggettoEsternoSecondario(attivita.getTipoOggettoEsternoSecondario());
//        attivitaFatta.setPriorita(attivita.getPriorita());
//        attivitaFatta.setProvenienza(attivita.getProvenienza());
//        attivitaFatta.setTags(attivita.getTags());
//        attivitaFatta.setTipo(attivita.getTipo());
//        attivitaFatta.setTipoOggettoEsterno(attivita.getTipoOggettoEsterno());
//        attivitaFatta.setTipoOggettoEsternoSecondario(attivita.getTipoOggettoEsternoSecondario());
//        attivitaFatta.setUrls(attivita.getUrls());
//        attivitaFatta.setAllegati(attivita.getAllegati());
//        attivitaFatta.setClasse(attivita.getClasse());
//        attivitaFatta.setData(attivita.getData());
//        attivitaFatta.setDataScadenza(attivita.getDataScadenza());
//        attivitaFatta.setDataUltimaModifica(attivita.getDataUltimaModifica());
//        attivitaFatta.setDataInserimentoRiga(LocalDateTime.now());
//        
//        attivitaFatteRepository.save(attivitaFatta);
    }
    
}
