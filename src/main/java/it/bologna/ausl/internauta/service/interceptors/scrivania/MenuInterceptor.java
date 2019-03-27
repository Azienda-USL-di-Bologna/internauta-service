package it.bologna.ausl.internauta.service.interceptors.scrivania;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.Expressions;
import edu.emory.mathcs.backport.java.util.Arrays;
import it.bologna.ausl.blackbox.PermissionManager;
import it.bologna.ausl.blackbox.exceptions.BlackBoxPermissionException;
import it.bologna.ausl.internauta.service.authorization.UserInfoService;
import it.bologna.ausl.internauta.service.interceptors.InternautaBaseInterceptor;
import it.bologna.ausl.internauta.service.utils.InternautaConstants;
import it.bologna.ausl.model.entities.baborg.Azienda;
import it.bologna.ausl.model.entities.baborg.AziendaParametriJson;
import it.bologna.ausl.model.entities.baborg.Utente;
import it.bologna.ausl.model.entities.scrivania.Menu;
import it.bologna.ausl.model.entities.scrivania.QMenu;
import it.nextsw.common.annotations.NextSdrInterceptor;
import it.nextsw.common.interceptors.exceptions.AbortLoadInterceptorException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.stream.Collectors;
import javax.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 *
 * @author Sal
 */
//@DependsOn("permessoRepository")
@Component
@NextSdrInterceptor(name = "menu-interceptor")
public class MenuInterceptor extends InternautaBaseInterceptor {

    private static final Logger LOGGER = LoggerFactory.getLogger(MenuInterceptor.class);
    private static final String FROM = "&from=INTERNAUTA";
    
    @Autowired
    UserInfoService userInfoService;
    
    @Autowired
    ObjectMapper objectMapper;
    
    @Autowired
    PermissionManager permissionManager;
    
    
    @Override
    public Class getTargetEntityClass() {
        return Menu.class;
    }
    
    /**
     * Le voci del menu verranno filtrare in base ai permessi dell'utente ed alle aziende a cui appartiene
     * @param initialPredicate
     * @param additionalData
     * @param request
     * @return
     * @throws AbortLoadInterceptorException 
     */
    @Override
    public Predicate beforeSelectQueryInterceptor(Predicate initialPredicate, Map<String, String> additionalData, HttpServletRequest request, boolean mainEntity) 
            throws AbortLoadInterceptorException {
        getAuthenticatedUserProperties();
        List<Utente> utentiPersona = userInfoService.getUtentiPersonaByUtente(super.user);              
        BooleanExpression filterAziendaUtente = null;
        
        List<String> ambitiFlusso = new ArrayList();
        ambitiFlusso.add(InternautaConstants.Permessi.Ambiti.PICO.toString());
        ambitiFlusso.add(InternautaConstants.Permessi.Ambiti.DETE.toString());
        ambitiFlusso.add(InternautaConstants.Permessi.Ambiti.DELI.toString());
        
        List<String> tipi = new ArrayList();
        List<Integer> aziendePersona = new ArrayList();
        tipi.add(InternautaConstants.Permessi.Tipi.FLUSSO.toString());
        
        if (utentiPersona != null && !utentiPersona.isEmpty()) {
            for (Utente up : utentiPersona) {
                try {
                    aziendePersona.add(up.getIdAzienda().getId());
                    // I permessi di interesse sono quelli di tipo FLUSSO e con ambito PICO-DETE-DELI.
                    List<String> predicatiAzienda = permissionManager.getPermission(up, ambitiFlusso, tipi);
                    BooleanExpression booleanTemplate;
                    
                    // Creo un filtro che sarà true quando tra i permessi dell'utente ci sarà almeno una voce dei permessiNecessari della voce di menù.
                    if (predicatiAzienda != null)
                        booleanTemplate = Expressions.booleanTemplate("tools.array_overlap({0}, string_to_array({1}, ','))=true", 
                            QMenu.menu.permessiSufficienti, String.join(",", predicatiAzienda));
                    else
                        // Se l'utente non ha permessi il filtro sarà smepre false
                        booleanTemplate = Expressions.FALSE.eq(Boolean.TRUE);
                    
                    // La voce di menù, di tale azienda, sarà tenuta qualora permessiNecessari sarà null o booleanTemplate sarà True. 
                    if (filterAziendaUtente == null)
                        filterAziendaUtente = QMenu.menu.idAzienda.id.eq(up.getIdAzienda().getId()).and(QMenu.menu.permessiSufficienti.isNull().or(booleanTemplate));
                    else
                        filterAziendaUtente = filterAziendaUtente.or(
                            QMenu.menu.idAzienda.id.eq(up.getIdAzienda().getId()).and(QMenu.menu.permessiSufficienti.isNull().or(booleanTemplate)));
                } catch (BlackBoxPermissionException ex) {
                    LOGGER.error("errore nel calcolo del predicato", ex);
                    throw new AbortLoadInterceptorException("errore nel calcolo del predicato", ex);
                }
            }
        }
        
        // estraggo anche i permessi delle PEC per gestire la visibilità della voce di menù relativa a PECG
        List<String> ambitiPecG = new ArrayList();
        ambitiPecG.add(InternautaConstants.Permessi.Ambiti.PECG.toString());
        try {
            List<String> predicatiPec = permissionManager.getPermission(super.user.getIdPersona(), ambitiPecG, InternautaConstants.Permessi.Tipi.PEC.toString());
            BooleanExpression booleanTemplate;
            if (predicatiPec != null) {
                booleanTemplate = Expressions.booleanTemplate("tools.array_overlap({0}, string_to_array({1}, ','))=true", 
                    QMenu.menu.permessiSufficienti, String.join(",", predicatiPec));
            } else {
                // Se l'utente non ha permessi il filtro sarà smepre false
                booleanTemplate = Expressions.FALSE.eq(Boolean.TRUE);
            }
            if (filterAziendaUtente == null)
                filterAziendaUtente = QMenu.menu.idAzienda.id.in(aziendePersona).and(QMenu.menu.permessiSufficienti.isNull().or(booleanTemplate));
            else
                filterAziendaUtente = filterAziendaUtente.or(QMenu.menu.idAzienda.id.in(aziendePersona).and(QMenu.menu.permessiSufficienti.isNull().or(booleanTemplate)));
        } catch (BlackBoxPermissionException ex) {
            LOGGER.error("errore nel calcolo del predicato", ex);
            throw new AbortLoadInterceptorException("errore nel calcolo del predicato", ex);
        }
        
        ambitiPecG.add(InternautaConstants.Permessi.Ambiti.PECG.toString());

            LOGGER.info("USER " + super.user.getId());
            List<String> ruoliCACI = super.user.getRuoli().stream().map(ruolo -> ruolo.getNomeBreve().toString()).collect(Collectors.toList());          
            LOGGER.info("ruoliCACI " + ruoliCACI);
            
            BooleanExpression booleanTemplate = Expressions.booleanTemplate("tools.array_overlap({0}, string_to_array({1}, ','))=true", 
                    QMenu.menu.ruoliSufficienti, String.join(",", ruoliCACI));
            
        if (filterAziendaUtente == null)
            filterAziendaUtente = QMenu.menu.idAzienda.id.in(aziendePersona).and(booleanTemplate);
        else
            filterAziendaUtente = filterAziendaUtente.or(QMenu.menu.idAzienda.id.in(aziendePersona).and(booleanTemplate));
  
        
        // Aggiungo il filtro al predicato. Se il filtro è vuoto allora nulla dev'essere visibile all'utente quindi il predicato di ritorno è una espressione False.
        return filterAziendaUtente != null ? filterAziendaUtente.and(initialPredicate): Expressions.FALSE.eq(Boolean.TRUE);
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
        String entityId = parametriAziendaOrigine.getEntityId();
        String crossLoginUrlTemplate = parametriAziendaOrigine.getCrossLoginUrlTemplate();
        Menu menu = (Menu) entity;
         
        
        String stringToEncode = "";
        if(menu.getOpenCommand() != null && !menu.getOpenCommand().equals("")){
            stringToEncode = menu.getOpenCommand();
        }
        if(person.getCodiceFiscale() != null && person.getCodiceFiscale().length() > 0){
            stringToEncode += (stringToEncode.length() > 0 && stringToEncode.startsWith("?")) ? "&utente=" : "?utente=";
            stringToEncode += person.getCodiceFiscale();
        }
        stringToEncode += "&utenteLogin=" + realPerson.getCodiceFiscale();
        stringToEncode += "&utenteImpersonato=" + person.getCodiceFiscale();
        stringToEncode += "&idSessionLog=" + idSessionLog;
        stringToEncode += FROM;
        stringToEncode += "&modalitaAmministrativa=0";
        stringToEncode += "&idAzienda="+menu.getIdAzienda().getId();
        
        try {
            AziendaParametriJson parametriAziendaTarget = AziendaParametriJson.parse(this.objectMapper, menu.getIdAzienda().getParametri());
            targetLoginPath = parametriAziendaTarget.getLoginPath();
        } catch (IOException ex) {
            throw new AbortLoadInterceptorException("errore nella lettura dei parametri dell'azienda target", ex);
        }
        String encodedParams = "";
        try {
            encodedParams = URLEncoder.encode(stringToEncode, "UTF-8");
        } catch (UnsupportedEncodingException ex) {
            LOGGER.error("errore nella creazione del link", ex);
            throw new AbortLoadInterceptorException("errore nella creazione del link", ex);
        }
        
        String applicationURL = menu.getIdApplicazione().getBaseUrl();
        
        String indexPage = menu.getIdApplicazione().getIndexPage();
        if(indexPage != null && indexPage.length() > 0){
            applicationURL += "/" + indexPage;
        }
//        String assembledURL = destinationURL + LOGIN_SSO_URL + fromURL + SSO_TARGET + applicationURL + encode;
        String assembledURL = crossLoginUrlTemplate.
            replace("[target-login-path]", targetLoginPath).
            replace("[entity-id]", entityId).
            replace("[app]", applicationURL).
            replace("[encoded-params]", encodedParams);
        menu.setCompiledUrl(assembledURL);
        return menu;
    }

    @Override
    public Collection<Object> afterSelectQueryInterceptor(Collection<Object> entities, Map<String, String> additionalData, HttpServletRequest request) throws AbortLoadInterceptorException {
        getAuthenticatedUserProperties();
        for (Object entity : entities) {
            entity = afterSelectQueryInterceptor(entity, additionalData, request);
        }
        return entities;
    }
    
}
