package it.bologna.ausl.internauta.service.interceptors.scrivania;

import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.Expressions;
import it.bologna.ausl.blackbox.PermissionManager;
import it.bologna.ausl.blackbox.exceptions.BlackBoxPermissionException;
import it.bologna.ausl.internauta.service.authorization.UserInfoService;
import it.bologna.ausl.internauta.service.interceptors.InternautaBaseInterceptor;
import it.bologna.ausl.internauta.service.utils.InternautaConstants;
import it.bologna.ausl.model.entities.baborg.Azienda;
import it.bologna.ausl.model.entities.baborg.Utente;
import it.bologna.ausl.model.entities.scrivania.Menu;
import it.bologna.ausl.model.entities.scrivania.QMenu;
import it.nextsw.common.annotations.NextSdrInterceptor;
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
    private static final String LOGIN_SSO_URL = "/Shibboleth.sso/Login?entityID=";
    private static final String SSO_TARGET = "/idp/shibboleth&target=";
    private static final String FROM = "&from=INTERNAUTA";
    private static final String HTTPS = "https://";
    
    @Autowired
    UserInfoService userInfoService;
    
    @Autowired
    PermissionManager permissionManager;
    
    @Override
    public Class getTargetEntityClass() {
        return Menu.class;
    }

    @Override
    public Collection<Object> afterSelectQueryInterceptor(Collection<Object> entities, Map<String, String> additionalData, HttpServletRequest request) throws AbortLoadInterceptorException {
        getAuthenticatedUserProperties();
        for (Object entity : entities) {
            entity = afterSelectQueryInterceptor(entity, additionalData, request);
        }
        return entities;
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
    public Predicate beforeSelectQueryInterceptor(Predicate initialPredicate, Map<String, String> additionalData, HttpServletRequest request) throws AbortLoadInterceptorException {
        getAuthenticatedUserProperties();
        List<Utente> utentiPersona = userInfoService.getUtentiPersona(super.user);              
        BooleanExpression filterAziendaUtente = null;
        
        List<String> ambiti = new ArrayList();
        ambiti.add(InternautaConstants.Permessi.Ambiti.PICO.toString());
        ambiti.add(InternautaConstants.Permessi.Ambiti.DETE.toString());
        ambiti.add(InternautaConstants.Permessi.Ambiti.DELI.toString());
        
        if (utentiPersona != null && !utentiPersona.isEmpty()) {
            for (Utente up : utentiPersona) {
                try {
                    // I permessi di interesse sono quelli di tipo FLUSSO e con ambito PICO-DETE-DELI.
                    List<String> predicatiAzienda = permissionManager.getPermission(up, ambiti, InternautaConstants.Permessi.Tipi.FLUSSO.toString());
                    BooleanExpression booleanTemplate;
                    
                    // Creo un filtro che sarà True quando tra i permessi dell'utente ci sarà almeno una voce dei permessiNecessari della voce di menù.
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
        
        // Aggiungo il filtro al predicato. Se il filtro è vuoto allora nulla dev'essere visibile all'utente quindi il predicato di ritorno è una espressione False.
        return filterAziendaUtente != null ? filterAziendaUtente.and(initialPredicate): Expressions.FALSE.eq(Boolean.TRUE);
    }
    
    
//    private Utente getUtente(){
//        TokenBasedAuthentication authentication = (TokenBasedAuthentication) SecurityContextHolder.getContext().getAuthentication();
//        Utente user = (Utente) authentication.getPrincipal();
//        return user;
//    }
    

    
    private String getURLByIdAzienda(Azienda azienda) {
        String res = null;

        String[] paths = azienda.getPath();
        if (paths != null && paths.length > 0) {
            res = paths[0];
        }
        return res;
    }


    
    @Override
    public Object afterSelectQueryInterceptor(Object entity, Map<String, String> additionalData, HttpServletRequest request) throws AbortLoadInterceptorException {
        getAuthenticatedUserProperties();
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
            LOGGER.error("errore nella creazione del link", ex);
        }
        String fromURL = HTTPS + getURLByIdAzienda(user.getIdAzienda());
        String applicationURL = menu.getIdApplicazione().getBaseUrl() + "/" + menu.getIdApplicazione().getIndexPage();
        String assembledURL = destinationURL + LOGIN_SSO_URL + fromURL + SSO_TARGET + applicationURL + encode;
        menu.setOpenCommand(assembledURL);
        return menu;
    }

}
