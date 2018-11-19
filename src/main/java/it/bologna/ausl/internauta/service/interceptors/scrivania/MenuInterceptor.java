package it.bologna.ausl.internauta.service.interceptors.scrivania;

import com.querydsl.core.types.Expression;
import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.BooleanTemplate;
import com.querydsl.core.types.dsl.Expressions;
import it.bologna.ausl.blackbox.PermissionManager;
import it.bologna.ausl.blackbox.exceptions.BlackBoxPermissionException;
import it.bologna.ausl.internauta.service.authorization.TokenBasedAuthentication;
import it.bologna.ausl.internauta.service.authorization.UserInfoService;
import it.bologna.ausl.internauta.service.authorization.jwt.LoginController;
import it.bologna.ausl.internauta.service.repositories.baborg.UtenteRepository;
import it.bologna.ausl.internauta.service.utils.CachedEntities;
import it.bologna.ausl.model.entities.baborg.Azienda;
import it.bologna.ausl.model.entities.baborg.Persona;
import it.bologna.ausl.model.entities.baborg.Utente;
import it.bologna.ausl.model.entities.scrivania.Menu;
import it.bologna.ausl.model.entities.scrivania.QMenu;
import it.nextsw.common.annotations.NextSdrInterceptor;
import it.nextsw.common.interceptors.NextSdrEmptyControllerInterceptor;
import it.nextsw.common.interceptors.exceptions.AbortLoadInterceptorException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 *
 * @author Sal
 */
@Component
@NextSdrInterceptor(name = "menu-interceptor")
public class MenuInterceptor extends NextSdrEmptyControllerInterceptor {

    private static final Logger logger = LoggerFactory.getLogger(MenuInterceptor.class);
    private static final String LOGIN_SSO_URL = "/Shibboleth.sso/Login?entityID=";
    private static final String SSO_TARGET = "/idp/shibboleth&target=";
    private static final String FROM = "&from=INTERNAUTA";
    private static final String HTTPS = "https://";
    private Utente user, realUser;
    private Persona person, realPerson;
    int idSessionLog;
    
    @Autowired
    CachedEntities cachedEntities;
    
    @Autowired
    UserInfoService userInfoService;

    @PersistenceContext
    EntityManager em;
    
    @Autowired
    UtenteRepository utenteRepository;
    
    @Autowired
    PermissionManager permissionManager;
    
    @Override
    public Class getTargetEntityClass() {
        return Menu.class;
    }

    @Override
    public Collection<Object> afterSelectQueryInterceptor(Collection<Object> entities, Map<String, String> additionalData, HttpServletRequest request) throws AbortLoadInterceptorException {
        initializeClassProperties();
        for (Object entity : entities) {
            entity = afterSelectQueryInterceptor(entity, additionalData, request);
        }
        return entities;
    }
    
    /**
     * Le voci del menu verranno filtrare in base ai permessi dell'utente connesso sulle aziende a cui appartiene
     * @param initialPredicate
     * @param additionalData
     * @param request
     * @return
     * @throws AbortLoadInterceptorException 
     */
    @Override
    public Predicate beforeSelectQueryInterceptor(Predicate initialPredicate, Map<String, String> additionalData, HttpServletRequest request) throws AbortLoadInterceptorException {
        Utente user = getUtente();
        List<Utente> utentiPersona = userInfoService.getUtentiPersona(user);              
        BooleanExpression filterAziendaUtente = null;
        
        List<String> ambiti = new ArrayList();
        ambiti.add("PICO");
        ambiti.add("DETE");
        ambiti.add("DELI");
        
        if (utentiPersona != null && !utentiPersona.isEmpty()) {
            for (Utente up : utentiPersona) {
                try {
                    List<String> predicatiAzienda = permissionManager.getPermission(up, ambiti, "FLUSSO");
                    BooleanTemplate booleanTemplate;
                    if (predicatiAzienda != null)
                        booleanTemplate = Expressions.booleanTemplate("tools.array_overlap({0}, string_to_array({1}, ','))=true", 
                            QMenu.menu.permessiNecessari, String.join(",", predicatiAzienda));
                    else
                        booleanTemplate = Expressions.booleanTemplate("false = true");
                    
                    if (filterAziendaUtente == null)
                        filterAziendaUtente = QMenu.menu.idAzienda.id.eq(up.getIdAzienda().getId()).and(QMenu.menu.permessiNecessari.isNull().or(booleanTemplate));
                    else
                        filterAziendaUtente = filterAziendaUtente.or(
                            QMenu.menu.idAzienda.id.eq(up.getIdAzienda().getId()).and(QMenu.menu.permessiNecessari.isNull().or(booleanTemplate)));
                } catch (BlackBoxPermissionException ex) {
                    logger.error("errore nel calcolo del predicato", ex);
                    throw new AbortLoadInterceptorException("errore nel calcolo del predicato", ex);
                }
            }
        }
        
        return filterAziendaUtente != null ? filterAziendaUtente.and(initialPredicate): Expressions.FALSE.eq(Boolean.TRUE);
    }
    
    
    private Utente getUtente(){
        TokenBasedAuthentication authentication = (TokenBasedAuthentication) SecurityContextHolder.getContext().getAuthentication();
        Utente user = (Utente) authentication.getPrincipal();
        return user;
    }
    
    private TokenBasedAuthentication getTokenBasedAuthentication() {
        return (TokenBasedAuthentication) SecurityContextHolder.getContext().getAuthentication();
    }
    
    private String getURLByIdAzienda(Azienda azienda) {
        String res = null;

        String[] paths = azienda.getPath();
        if (paths != null && paths.length > 0) {
            res = paths[0];
        }
        return res;
    }

    private void initializeClassProperties(){
        // TODO add url
        
        TokenBasedAuthentication authentication = getTokenBasedAuthentication();
        user = (Utente) authentication.getPrincipal();
        realUser = (Utente) authentication.getRealUser();
        idSessionLog = authentication.getIdSessionLog();
        person = cachedEntities.getPersona(user.getIdPersona().getId());
        realPerson = cachedEntities.getPersona(realUser.getIdPersona().getId());
    }
    
    @Override
    public Object afterSelectQueryInterceptor(Object entity, Map<String, String> additionalData, HttpServletRequest request) throws AbortLoadInterceptorException {
        initializeClassProperties();
        Menu menu = (Menu) entity;
        
        String stringToEncode = menu.getOpenCommand(); // url
        stringToEncode += "&utente=" + person.getCodiceFiscale();
        stringToEncode += "&utenteLogin=" + realPerson.getCodiceFiscale();
        stringToEncode += "&utenteImpersonato=" + person.getCodiceFiscale();
        stringToEncode += "&idSessionLog=" + idSessionLog;
        stringToEncode += FROM;
        stringToEncode += "&modalitaAmministrativa=0";
        String destinationURL = HTTPS + getURLByIdAzienda(menu.getIdAzienda());
        String encode = "";
        try {
            encode = URLEncoder.encode(stringToEncode, "UTF-8");
        } catch (UnsupportedEncodingException ex) {
            logger.error("errore nella creazione del link", ex);
        }
        String fromURL = HTTPS + getURLByIdAzienda(user.getIdAzienda());
        String applicationURL = menu.getIdApplicazione().getBaseUrl() + "/" + menu.getIdApplicazione().getIndexPage();
        String assembledURL = destinationURL + LOGIN_SSO_URL + fromURL + SSO_TARGET + applicationURL + encode;
        menu.setOpenCommand(assembledURL);
        return menu;
    }

}
