package it.bologna.ausl.internauta.service.interceptors.scrivania;

import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.dsl.BooleanExpression;
import it.bologna.ausl.internauta.service.authorization.TokenBasedAuthentication;
import it.bologna.ausl.internauta.service.authorization.UserInfoService;
import it.bologna.ausl.internauta.service.repositories.baborg.UtenteRepository;
import it.bologna.ausl.internauta.service.utils.CachedEntities;
import it.bologna.ausl.model.entities.baborg.Azienda;
import it.bologna.ausl.model.entities.baborg.Persona;
import it.bologna.ausl.model.entities.baborg.Utente;
import it.bologna.ausl.model.entities.baborg.projections.generated.AziendaWithPlainFields;
import it.bologna.ausl.model.entities.scrivania.Attivita;
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
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.servlet.http.HttpServletRequest;
import org.jose4j.json.internal.json_simple.JSONArray;
import org.jose4j.json.internal.json_simple.JSONObject;
import org.jose4j.json.internal.json_simple.parser.JSONParser;
import org.jose4j.json.internal.json_simple.parser.ParseException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 *
 * @author Sal
 */
@Component
@NextSdrInterceptor(name = "menu-interceptor")
public class MenuInterceptor extends NextSdrEmptyControllerInterceptor {

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

    @Override
    public Predicate beforeSelectQueryInterceptor(Predicate initialPredicate, Map<String, String> additionalData, HttpServletRequest request) throws AbortLoadInterceptorException {
        Utente user = getUtente();
        List<Azienda> aziendePersona = userInfoService.getAziendePersona(user);
        List<Integer> idAziende = new ArrayList();
        if (aziendePersona != null && !aziendePersona.isEmpty()) {
            aziendePersona.stream().forEach(ap -> {idAziende.add(ap.getId());});
        }
        BooleanExpression filterAziendaUtente = QMenu.menu.idAzienda.id.in(idAziende);
        return filterAziendaUtente.and(initialPredicate);
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
            Logger.getLogger(MenuInterceptor.class.getName()).log(Level.SEVERE, null, ex);
        }
        String fromURL = HTTPS + getURLByIdAzienda(user.getIdAzienda());
        String applicationURL = menu.getIdApplicazione().getBaseUrl() + "/" + menu.getIdApplicazione().getIndexPage();
        String assembledURL = destinationURL + LOGIN_SSO_URL + fromURL + SSO_TARGET + applicationURL + encode;
        menu.setOpenCommand(assembledURL);
        return menu;
    }

}
