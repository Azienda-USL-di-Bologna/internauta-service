package it.bologna.ausl.internauta.service.interceptors.scrivania;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.Expressions;
import edu.emory.mathcs.backport.java.util.Arrays;
import it.bologna.ausl.blackbox.PermissionManager;
import it.bologna.ausl.blackbox.exceptions.BlackBoxPermissionException;
import it.bologna.ausl.internauta.service.authorization.AuthenticatedSessionData;
import it.bologna.ausl.internauta.service.authorization.UserInfoService;
import it.bologna.ausl.internauta.service.interceptors.InternautaBaseInterceptor;
import it.bologna.ausl.internauta.service.utils.InternautaConstants;
import it.bologna.ausl.internauta.service.utils.InternautaUtils;
import it.bologna.ausl.model.entities.baborg.Azienda;
import it.bologna.ausl.model.entities.baborg.AziendaParametriJson;
import it.bologna.ausl.model.entities.baborg.Ruolo;
import it.bologna.ausl.model.entities.baborg.Utente;
import it.bologna.ausl.model.entities.scrivania.Menu;
import it.bologna.ausl.model.entities.scrivania.QMenu;
import it.nextsw.common.annotations.NextSdrInterceptor;
import it.nextsw.common.interceptors.exceptions.AbortLoadInterceptorException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import javax.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

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
    
    @Autowired
    InternautaUtils internautaUtils;
    
    @Override
    public Class getTargetEntityClass() {
        return Menu.class;
    }
    
    /**
     * Le voci del menu verranno filtrare in base ai permessi dell'utente ed alle aziende a cui appartiene
     * @param initialPredicate
     * @param additionalData
     * @param request
     * @param mainEntity
     * @param projectionClass
     * @return
     * @throws AbortLoadInterceptorException 
     */
    
    
    @Override 
    public Predicate beforeSelectQueryInterceptor(Predicate initialPredicate, Map<String, String> additionalData, HttpServletRequest request, boolean mainEntity, Class projectionClass) 
            throws AbortLoadInterceptorException {
        
        AuthenticatedSessionData authenticatedSessionData = getAuthenticatedUserProperties();
        Utente user = authenticatedSessionData.getUser();
        Utente utenteReale = authenticatedSessionData.getRealUser();
        List<Utente> utentiPersona = userInfoService.getUtentiPersonaByUtente(user, utenteReale == null);              
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
        List<String> ruoliCACI = authenticatedSessionData.getUser().getMappaRuoli().get(Ruolo.ModuliRuolo.GENERALE.toString()).stream().map(ruolo -> ruolo.getNomeBreve().toString()).collect(Collectors.toList());
        LOGGER.info("ruoliCACI " + ruoliCACI);
        
        BooleanExpression booleanTemplate = Expressions.booleanTemplate("tools.array_overlap({0}, string_to_array({1}, ','))=true",
                QMenu.menu.ruoliSufficienti, String.join(",", ruoliCACI));
        
//        if (filterAziendaUtente == null)
//            filterAziendaUtente = getFilterAziendaIn(aziendePersona).and(booleanTemplate);
//        else
//            filterAziendaUtente = filterAziendaUtente.or(getFilterAziendaIn(aziendePersona).and(booleanTemplate));
        if (filterAziendaUtente == null)
            filterAziendaUtente = getFilterAziendaIn(aziendePersona).and(booleanTemplate);
        else
            filterAziendaUtente = filterAziendaUtente.or(getFilterAziendaIn(aziendePersona).and(booleanTemplate));
  
        if (filterAziendaUtente != null)
            LOGGER.info("PREDICATO MENU INTERCEPTOR BEFORE SELECT: " + filterAziendaUtente.and(initialPredicate).toString());
        else
            LOGGER.info("PREDICATO MENU INTERCEPTOR BEFORE SELECT: null");
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
        Menu menu = (Menu) entity;
        if (menu.getRuoliSufficienti() != null && menu.getRuoliSufficienti().length > 0) {
            String modulo = menu.getModulo();
            Ruolo.ModuliRuolo moduloRuolo = null;
            if (modulo == null) {
                moduloRuolo = Ruolo.ModuliRuolo.GENERALE;
            } else {
                moduloRuolo = Ruolo.ModuliRuolo.valueOf(modulo);
            }
            List<String> ruoliMenu = Arrays.asList(menu.getRuoliSufficienti());
            List<String> ruoliUtente = authenticatedSessionData.getUser().getMappaRuoli().get(moduloRuolo.toString()).stream().map(ruolo -> ruolo.getNomeBreve().toString()).collect(Collectors.toList());
            Set<String> overlap = ruoliMenu.stream().distinct().filter(ruoliUtente::contains).collect(Collectors.toSet());
            if (overlap == null || overlap.isEmpty()) {
                return null;
            }
        }
        
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
        
        try {
            Azienda aziendaLogin = authenticatedSessionData.getUser().getIdAzienda();
            Azienda aziendaTarget; 
            if (menu.getIdAzienda() != null) {
                aziendaTarget = menu.getIdAzienda();
            } else {
                aziendaTarget = aziendaLogin;
            }
            String url = "";
            if(menu.getOpenCommand() != null && !menu.getOpenCommand().equals("")){
                url = menu.getOpenCommand();
            }
            String assembledUrl = internautaUtils.getUrl(authenticatedSessionData, url, menu.getIdApplicazione().getId(), aziendaTarget);

    //        String targetLoginPath;
    //        String targetBasePath;
    //        String entityId = parametriAziendaOrigine.getEntityId();
    //        String crossLoginUrlTemplate = parametriAziendaOrigine.getCrossLoginUrlTemplate();
    //        String simpleCrossLoginUrlTemplate = parametriAziendaOrigine.getSimpleCrossLoginUrlTemplate();         

            /*
            String paramWithContextInformation = "";
            String paramWithoutContextInformation = "";
            if(menu.getOpenCommand() != null && !menu.getOpenCommand().equals("")){
                paramWithContextInformation = menu.getOpenCommand();
                paramWithoutContextInformation = menu.getOpenCommand();
            }
            if(authenticatedSessionData.getPerson().getCodiceFiscale() != null && authenticatedSessionData.getPerson().getCodiceFiscale().length() > 0){
                paramWithContextInformation += (paramWithContextInformation.length() > 0 && paramWithContextInformation.startsWith("?")) ? "&utente=" : "?utente=";
                paramWithContextInformation += authenticatedSessionData.getPerson().getCodiceFiscale(); // non so se serve alle applicazioni INDE o a internauta o a tutti e 2
            }

            if (authenticatedSessionData.getRealPerson() != null) {
                paramWithContextInformation += "&realUser=" + authenticatedSessionData.getRealPerson().getCodiceFiscale();
                paramWithContextInformation += "&impersonatedUser=" + authenticatedSessionData.getPerson().getCodiceFiscale();
                paramWithContextInformation += "&utenteLogin=" + authenticatedSessionData.getRealPerson().getCodiceFiscale(); // serve alle applicazioni INDE
            } else {
                paramWithContextInformation += "&user=" + authenticatedSessionData.getPerson().getCodiceFiscale();
                paramWithContextInformation += "&utenteLogin=" + authenticatedSessionData.getPerson().getCodiceFiscale(); // serve alle applicazioni INDE
            }

            paramWithContextInformation += "&utenteImpersonato=" + authenticatedSessionData.getPerson().getCodiceFiscale(); // serve alle applicazioni INDE
            paramWithContextInformation += "&idSessionLog=" + authenticatedSessionData.getIdSessionLog();
            paramWithContextInformation += FROM;
            paramWithContextInformation += "&modalitaAmministrativa=0"; // serve alle applicazioni INDE
            Azienda azienda = authenticatedSessionData.getUser().getIdAzienda();
            if (menu.getIdAzienda() != null) {
                azienda = menu.getIdAzienda();
            }

            paramWithContextInformation += "&idAzienda=" + azienda.getId();
            paramWithContextInformation += "&aziendaImpersonatedUser=" + azienda.getId();
            try {
                AziendaParametriJson parametriAziendaTarget = AziendaParametriJson.parse(this.objectMapper, azienda.getParametri());
                targetLoginPath = parametriAziendaTarget.getLoginPath();
                targetBasePath = parametriAziendaTarget.getBasePath();
            } catch (IOException ex) {
                throw new AbortLoadInterceptorException("errore nella lettura dei parametri dell'azienda target", ex);
            }
            String encodedParamsWithContextInformation = "";
            String encodedParamsWithoutContextInformation = "";
            try {
                encodedParamsWithContextInformation = URLEncoder.encode(paramWithContextInformation, "UTF-8");
                encodedParamsWithoutContextInformation = URLEncoder.encode(paramWithoutContextInformation, "UTF-8");
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

            String assembledURL = null;
            switch (menu.getIdApplicazione().getUrlGenerationStrategy()) {
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
                    replace("[params]", paramWithContextInformation);
                break;
                case RELATIVE_WITHOUT_CONTEXT_INFORMATION:
                    assembledURL = simpleCrossLoginUrlTemplate.
                    replace("[target-login-path]", targetBasePath).
                    replace("[app]", applicationURL).
                    replace("[params]", paramWithoutContextInformation);
                break;
                case ABSOLUTE_WITH_CONTEXT_INFORMATION:
                    assembledURL = applicationURL + paramWithContextInformation;
                break;
                case ABSOLUTE_WITHOUT_CONTEXT_INFORMATION:
                    assembledURL = paramWithoutContextInformation;
                break;
            }
            */
            menu.setCompiledUrl(assembledUrl);
        } catch (Exception ex) {
            throw new AbortLoadInterceptorException("errore in MenuInterceptor in afterSelectQueryInterceptor: ", ex);
        }
        
        return menu;
    }   
    
    @Override
    public Collection<Object> afterSelectQueryInterceptor(Collection<Object> entities, Map<String, String> additionalData, HttpServletRequest request, boolean mainEntity, Class projectionClass) throws AbortLoadInterceptorException {
//        getAuthenticatedUserProperties();
        List<Object> res = new ArrayList();
        for (Object entity : entities) {
//            LOGGER.info("ENTITY PRIMA " + entity.toString());
            entity = afterSelectQueryInterceptor(entity, additionalData, request, mainEntity, projectionClass);
            if (entity != null) {
                res.add(entity);
            }
        }
        return res;
    }
    
}
