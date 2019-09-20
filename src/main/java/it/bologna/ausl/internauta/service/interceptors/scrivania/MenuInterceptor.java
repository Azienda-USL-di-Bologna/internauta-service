package it.bologna.ausl.internauta.service.interceptors.scrivania;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.Expressions;
import it.bologna.ausl.blackbox.PermissionManager;
import it.bologna.ausl.blackbox.exceptions.BlackBoxPermissionException;
import it.bologna.ausl.internauta.service.authorization.AuthenticatedSessionData;
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
    public Predicate beforeSelectQueryInterceptor(Predicate initialPredicate, Map<String, String> additionalData, HttpServletRequest request, boolean mainEntity, Class projectionClass) 
            throws AbortLoadInterceptorException {
        
        AuthenticatedSessionData authenticatedSessionData = getAuthenticatedUserProperties();
        List<Utente> utentiPersona = userInfoService.getUtentiPersonaByUtente(authenticatedSessionData.getUser());              
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
                    
                    // Creo un filtro che sarà true quando tra i permessi dell'utente ci sarà almeno una voce dei permessiSufficienti della voce di menù.
                    if (predicatiAzienda != null)
                        booleanTemplate = Expressions.booleanTemplate("tools.array_overlap({0}, string_to_array({1}, ','))=true",
                                QMenu.menu.permessiSufficienti, String.join(",", predicatiAzienda));
                    else
                        // Se l'utente non ha permessi il filtro sarà smepre false
                        booleanTemplate = Expressions.FALSE.eq(Boolean.TRUE);
                    
                    // La voce di menù, di tale azienda, sarà tenuta qualora permessiNecessari sarà null o booleanTemplate sarà True. 
                    if (filterAziendaUtente == null)
                        filterAziendaUtente = getFilterAziendaUp(up).and(QMenu.menu.permessiSufficienti.isNull().or(booleanTemplate));
                    else
                        filterAziendaUtente = filterAziendaUtente.or(
                               getFilterAziendaUp(up).and(QMenu.menu.permessiSufficienti.isNull().or(booleanTemplate)));
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
            List<String> predicatiPec = permissionManager.getPermission(authenticatedSessionData.getUser().getIdPersona(), ambitiPecG, InternautaConstants.Permessi.Tipi.PEC.toString());
            BooleanExpression booleanTemplate;
            if (predicatiPec != null) {
                booleanTemplate = Expressions.booleanTemplate("tools.array_overlap({0}, string_to_array({1}, ','))=true",
                        QMenu.menu.permessiSufficienti, String.join(",", predicatiPec));
            } else {
                // Se l'utente non ha permessi il filtro sarà smepre false
                booleanTemplate = Expressions.FALSE.eq(Boolean.TRUE);
            }
            if (filterAziendaUtente == null)
                filterAziendaUtente = getFilterAziendaIn(aziendePersona)
                        .and(QMenu.menu.permessiSufficienti.isNull().or(booleanTemplate));
            else
                filterAziendaUtente = filterAziendaUtente.or(getFilterAziendaIn(aziendePersona).and(QMenu.menu.permessiSufficienti.isNull().or(booleanTemplate)));
        } catch (BlackBoxPermissionException ex) {
            LOGGER.error("errore nel calcolo del predicato", ex);
            throw new AbortLoadInterceptorException("errore nel calcolo del predicato", ex);
        }
        
//        ambitiPecG.add(InternautaConstants.Permessi.Ambiti.PECG.toString());
        
        LOGGER.info("USER " + authenticatedSessionData.getUser().getId());
        List<String> ruoliCACI = authenticatedSessionData.getUser().getRuoli().stream().map(ruolo -> ruolo.getNomeBreve().toString()).collect(Collectors.toList());
        LOGGER.info("ruoliCACI " + ruoliCACI);
        
        BooleanExpression booleanTemplate = Expressions.booleanTemplate("tools.array_overlap({0}, string_to_array({1}, ','))=true",
                QMenu.menu.ruoliSufficienti, String.join(",", ruoliCACI));
        
        if (filterAziendaUtente == null)
            filterAziendaUtente = getFilterAziendaIn(aziendePersona).and(booleanTemplate);
        else
            filterAziendaUtente = filterAziendaUtente.or(getFilterAziendaIn(aziendePersona).and(booleanTemplate));
  
        LOGGER.info("PREDICATO MENU INTERCEPTOR BEFORE SELECT" + filterAziendaUtente.and(initialPredicate).toString());
        // Aggiungo il filtro al predicato. Se il filtro è vuoto allora nulla dev'essere visibile all'utente quindi il predicato di ritorno è una espressione False.
        return filterAziendaUtente != null ? filterAziendaUtente.and(initialPredicate): Expressions.FALSE.eq(Boolean.TRUE);
    }

    private BooleanExpression getFilterAziendaUp(Utente up) {
        BooleanExpression aziendaUtenteinVisibileAziende = Expressions.booleanTemplate("tools.array_overlap({0}, string_to_array({1}, ','))=true",
                QMenu.menu.visibileAziende, String.join(",", up.getIdAzienda().getId().toString()));
        
        return (
            (QMenu.menu.visibileAziende.isNull().and(QMenu.menu.idAzienda.id.isNull().or(QMenu.menu.idAzienda.id.eq(up.getIdAzienda().getId())))).or
            (QMenu.menu.visibileAziende.isNotNull().and(aziendaUtenteinVisibileAziende).and(QMenu.menu.idAzienda.id.isNull().or(QMenu.menu.idAzienda.id.eq(up.getIdAzienda().getId()))))
        );
//        return (QMenu.menu.idAzienda.id.isNull().or(QMenu.menu.idAzienda.id.eq(up.getIdAzienda().getId())));
    }
    
    private BooleanExpression getFilterAziendaIn(List<Integer> aziendePersona) {
        BooleanExpression aziendePersonaInVisibileAziende = Expressions.booleanTemplate("tools.array_overlap({0}, string_to_array({1}, ','))=true",
                QMenu.menu.visibileAziende, String.join(",", aziendePersona.stream().map(id -> id.toString()).collect(Collectors.toList())));
        return (
            (QMenu.menu.visibileAziende.isNull().and(QMenu.menu.idAzienda.id.isNull().or(QMenu.menu.idAzienda.id.in(aziendePersona)))).or
            (QMenu.menu.visibileAziende.isNotNull().and(aziendePersonaInVisibileAziende).and(QMenu.menu.idAzienda.id.isNull().or(QMenu.menu.idAzienda.id.in(aziendePersona))))
        );
    }
    
    @Override
    public Object afterSelectQueryInterceptor(Object entity, Map<String, String> additionalData, HttpServletRequest request, boolean mainEntity, Class projectionClass) throws AbortLoadInterceptorException {
        AuthenticatedSessionData authenticatedSessionData = getAuthenticatedUserProperties();

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
        String entityId = parametriAziendaOrigine.getEntityId();
        String crossLoginUrlTemplate = parametriAziendaOrigine.getCrossLoginUrlTemplate();
        Menu menu = (Menu) entity;
         
        
        
        String stringToEncode = "";
        if(menu.getOpenCommand() != null && !menu.getOpenCommand().equals("")){
            stringToEncode = menu.getOpenCommand();
        }
        if(authenticatedSessionData.getPerson().getCodiceFiscale() != null && authenticatedSessionData.getPerson().getCodiceFiscale().length() > 0){
            stringToEncode += (stringToEncode.length() > 0 && stringToEncode.startsWith("?")) ? "&utente=" : "?utente=";
            stringToEncode += authenticatedSessionData.getPerson().getCodiceFiscale(); // non so se serve alle applicazioni INDE o a internauta o a tutti e 2
        }
        
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
        Azienda azienda = authenticatedSessionData.getUser().getIdAzienda();
        if (menu.getIdAzienda() != null) {
            azienda = menu.getIdAzienda();
        }

        stringToEncode += "&idAzienda=" + azienda.getId();
        stringToEncode += "&aziendaImpersonatedUser=" + azienda.getId();
        try {
            AziendaParametriJson parametriAziendaTarget = AziendaParametriJson.parse(this.objectMapper, azienda.getParametri());
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
        
        if (applicationURL != null) {
            String indexPage = menu.getIdApplicazione().getIndexPage();
            if(indexPage != null && indexPage.length() > 0){
                applicationURL += "/" + indexPage;
            }
        } else {
            applicationURL = "";
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
    public Collection<Object> afterSelectQueryInterceptor(Collection<Object> entities, Map<String, String> additionalData, HttpServletRequest request, boolean mainEntity, Class projectionClass) throws AbortLoadInterceptorException {
//        getAuthenticatedUserProperties();
        for (Object entity : entities) {
            LOGGER.info("ENTITY PRIMA " + entity.toString());
            entity = afterSelectQueryInterceptor(entity, additionalData, request, mainEntity, projectionClass);
            LOGGER.info("ENTITY DOPO " + entity.toString());
        }
        return entities;
    }
    
}
