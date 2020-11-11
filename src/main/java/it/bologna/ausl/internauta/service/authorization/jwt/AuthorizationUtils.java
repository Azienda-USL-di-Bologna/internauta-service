package it.bologna.ausl.internauta.service.authorization.jwt;

import com.fasterxml.jackson.core.JsonProcessingException;
import it.bologna.ausl.internauta.service.authorization.UserInfoService;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import it.bologna.ausl.blackbox.exceptions.BlackBoxPermissionException;
import it.bologna.ausl.model.entities.baborg.Utente;
import it.bologna.ausl.internauta.service.authorization.TokenBasedAuthentication;
import it.bologna.ausl.internauta.service.exceptions.ObjectNotFoundException;
import it.bologna.ausl.internauta.service.exceptions.SSOException;
import it.bologna.ausl.internauta.service.utils.CacheUtilities;
import it.bologna.ausl.internauta.service.repositories.baborg.UtenteRepository;
import it.bologna.ausl.internauta.service.repositories.logs.CounterRepository;
import it.bologna.ausl.internauta.service.repositories.tools.UserAccessRepository;
import it.bologna.ausl.internauta.service.utils.CachedEntities;
import it.bologna.ausl.internauta.service.utils.HttpSessionData;
import it.bologna.ausl.internauta.service.utils.InternautaConstants;
import it.bologna.ausl.model.entities.baborg.Azienda;
import it.bologna.ausl.model.entities.baborg.AziendaParametriJson;
import it.bologna.ausl.model.entities.baborg.Persona;
import it.bologna.ausl.model.entities.baborg.Ruolo;
import it.bologna.ausl.model.entities.logs.Counter;
import java.io.IOException;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.projection.ProjectionFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import it.bologna.ausl.model.entities.baborg.projections.CustomUtenteLogin;
import it.bologna.ausl.model.entities.configuration.Applicazione.Applicazioni;
import it.bologna.ausl.model.entities.tools.UserAccess;
import java.util.Arrays;
import org.springframework.util.StringUtils;

/**
 *
 * @author gdm
 */
@Component
public class AuthorizationUtils {

    public enum TokenClaims {
        COMPANY,
        SSO_LOGIN,
        USER_ENTITY_CLASS,
        USER_FIELD,
        USER_SSO_FIELD_VALUE,
        REAL_USER_SSO_FIELD_VALUE,
        USERNAME,
        REAL_USER,
        REAL_USER_USERNAME,
        ID_SESSION_LOG,
        FROM_INTERNET
    }

    private final SignatureAlgorithm SIGNATURE_ALGORITHM = SignatureAlgorithm.HS256;

    @Autowired
    UserAccessRepository userAccessRepository;

    @Autowired
    UtenteRepository utenteRepository;

    @Autowired
    UserInfoService userInfoService;

    @Autowired
    CachedEntities cachedEntities;

    @Autowired
    ObjectMapper objectMapper;

    @Value("${jwt.expires-seconds}")
    private Integer tokenExpireSeconds;

    @Autowired
    CounterRepository counterRepository;

    @Autowired
    ProjectionFactory factory;

    @Autowired
    HttpSessionData httpSessionData;

    @Autowired
    CacheUtilities cacheUtilities;

    private static final Logger logger = LoggerFactory.getLogger(AuthorizationUtils.class);

    /**
     * inserisce nel securityContext l'utente inserito nel token al momento del
     * login
     *
     * @param token il token
     * @param secretKey la chiave segreta per decifrare il token
     * @param idApplicazione
     * @return i claims del token
     * @throws java.lang.ClassNotFoundException
     */
    @Transactional(rollbackFor = {Error.class})
    public Claims setInSecurityContext(String token, String secretKey, String idApplicazione) throws ClassNotFoundException, BlackBoxPermissionException {
        Claims claims = Jwts.parser().
                setSigningKey(secretKey).
                parseClaimsJws(token).
                getBody();

        Integer userId = Integer.parseInt(claims.getSubject());
        Integer realUserId = null;
        Object realUserString = claims.get(AuthorizationUtils.TokenClaims.REAL_USER.name());
        if (realUserString != null && !((String) realUserString).isEmpty()) {
            realUserId = Integer.parseInt((String) realUserString);
        }
        Integer idSessionLog = Integer.parseInt((String) claims.get(AuthorizationUtils.TokenClaims.ID_SESSION_LOG.name()));
        Utente user = userInfoService.loadUtente(userId);
        logger.info("user: " + (user != null ? user.getId() : "null"));
        user.setMappaRuoli(userInfoService.getRuoliPerModuli(user, null));
        logger.info("ruoli user: ");
        try {
            logger.info(objectMapper.writeValueAsString(user.getMappaRuoli()));
        } catch (JsonProcessingException ex) {
            logger.warn("Errore nella stampa dei ruoli", ex);
        }
        user.setRuoliUtentiPersona(userInfoService.getRuoliUtentiPersona(user.getIdPersona(), true));
//        user.setRuoliUtentiPersona(userInfoService.getRuoliUtentiPersona(user, true));
        user.setPermessiDiFlusso(userInfoService.getPermessiDiFlusso(user));
        user.setPermessiDiFlussoByCodiceAzienda(userInfoService.getPermessiDiFlussoByCodiceAzienda(user));
        boolean fromInternet = false;
        Object fromInternetObj = claims.get(AuthorizationUtils.TokenClaims.FROM_INTERNET.name());
        logger.info("fromInternetObj: " + fromInternetObj);
        if (fromInternetObj != null) {
            fromInternet = claims.get(AuthorizationUtils.TokenClaims.FROM_INTERNET.name(), Boolean.class);
            logger.info("fromInternet boolean: " + fromInternet);
        }
        if (realUserId != null && !realUserId.equals(userId)) {
            Utente realUser = userInfoService.loadUtente(realUserId);
            insertInContext(realUser, user, idSessionLog, token, idApplicazione, fromInternet);
        } else {
            insertInContext(user, idSessionLog, token, idApplicazione, fromInternet);
        }
        return claims;
    }

    public void insertInContext(Utente user, Integer idSessionLog, String token, String idApplicazione, boolean fromInternet) {
        insertInContext(null, user, idSessionLog, token, idApplicazione, fromInternet);
    }

    public void insertInContext(Utente realUser, Utente user, Integer idSessionLog, String token, String idApplicazione, boolean fromInternet) {
        logger.info("insertInContext fromInternet: " + fromInternet);
        TokenBasedAuthentication authentication;
        Applicazioni applicazione = Applicazioni.valueOf(idApplicazione);
        if (realUser != null) {
            authentication = new TokenBasedAuthentication(user, realUser, applicazione, fromInternet);
        } else {
            authentication = new TokenBasedAuthentication(user, applicazione, fromInternet);
        }
        authentication.setToken(token);
        authentication.setIdSessionLog(idSessionLog);
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    /**
     *
     * @param idAzienda se è presente viene usato per caricare l'azienda,
     * altrimenti usa il parametro path
     * @param path il path che identifica l'azienda dalla quale è partito il
     * login (es. babel-auslbo.avec.emr.it)
     * @param secretKey
     * @param request
     * @param ssoFieldValue
     * @param utenteImpersonatoStr
     * @param applicazione
     * @param fromInternetLogin
     * @return
     * @throws IOException
     * @throws ClassNotFoundException
     * @throws ObjectNotFoundException
     * @throws BlackBoxPermissionException
     */
    public ResponseEntity generateResponseEntityFromSAML(String idAzienda, String path, String secretKey, HttpServletRequest request, String ssoFieldValue, String utenteImpersonatoStr, String applicazione, Boolean fromInternetLogin, Boolean writeUserAccess) throws IOException, ClassNotFoundException, ObjectNotFoundException, BlackBoxPermissionException, SSOException {

        ResponseEntity res;

        if (fromInternetLogin == null) {
            fromInternetLogin = fromInternet(request);
        }
        logger.info("idAzienda: " + objectMapper.writeValueAsString(idAzienda));
        logger.info("path: " + objectMapper.writeValueAsString(path));
        logger.info("fromInternet: " + fromInternetLogin);
        Azienda aziendaRealUser = null;
        if (fromInternetLogin) {
            if (StringUtils.isEmpty(ssoFieldValue)) {
                if (!StringUtils.isEmpty(request.getAttribute("CodiceFiscale"))) {
                    ssoFieldValue = request.getAttribute("CodiceFiscale").toString();
                } else {
                    throw new SSOException("ssoFieldValue is empty");
                }
            }
            Persona realPerson = cachedEntities.getPersonaFromCodiceFiscale(ssoFieldValue);
            if (realPerson != null) {
                aziendaRealUser = cachedEntities.getAzienda(realPerson.getIdAziendaDefault().getId());
            }
        } else {
            if (StringUtils.isEmpty(path)) {
                throw new ObjectNotFoundException("impossibile stabilire l'azienda dell'utente, il campo \"path\" è vuoto");
            }
            aziendaRealUser = cachedEntities.getAziendaFromPath(path);
        }

        Utente impersonatedUser;
        boolean isSuperDemiurgo = false;
        Azienda aziendaImpersonatedUser = (idAzienda == null || aziendaRealUser.getId() == Integer.parseInt(idAzienda)
                ? aziendaRealUser
                : cachedEntities.getAzienda(Integer.parseInt(idAzienda)));

        //userInfoService.loadAziendaByPathRemoveCache(path);
        AziendaParametriJson aziendaRealUserParams = AziendaParametriJson.parse(objectMapper, aziendaRealUser.getParametri());
        //AziendaParametriJson aziendaImpersonatedUserParams = AziendaParametriJson.parse(objectMapper, aziendaImpersonatedUser.getParametri());

        if (ssoFieldValue == null) {
            if (!StringUtils.isEmpty(request.getAttribute(aziendaRealUserParams.getLoginSSOField()))) {
                ssoFieldValue = request.getAttribute(aziendaRealUserParams.getLoginSSOField()).toString();
            } else {
                throw new SSOException("ssoFieldValue is empty");
            }
        }

        String[] loginDbFieldSplitted = aziendaRealUserParams.getLoginDBFieldBaborg().split("/");
        String entityClassName = loginDbFieldSplitted[0];
        String field = loginDbFieldSplitted[1];

        Class<?> entityClass = Class.forName(entityClassName);

        // carica l'utente vero che si è loggato con SSO
        userInfoService.loadUtenteRemoveCache(entityClass, field, ssoFieldValue, aziendaRealUser, true);
        Utente user = userInfoService.loadUtente(entityClass, field, ssoFieldValue, aziendaRealUser, true);
        if (user == null) {
            throw new ObjectNotFoundException("User not found");
        }
        userInfoService.loadUtenteRemoveCache(user.getId());
        userInfoService.getUtentiPersonaByUtenteRemoveCache(user);
        userInfoService.getUtentiPersonaRemoveCache(user.getIdPersona());
        userInfoService.getUtenteStrutturaListRemoveCache(user, true);
        userInfoService.getUtenteStrutturaListRemoveCache(user, false);
        cacheUtilities.cleanCacheRuoliUtente(user.getId(), user.getIdPersona().getId());
        // TODO: rimuovere permessi cache
        userInfoService.getPermessiDiFlussoRemoveCache(user);
        userInfoService.getPermessiPecRemoveCache(user.getIdPersona());
        // prendi ID dell'utente reale
        String realUserSubject = String.valueOf(user.getId());

        user.setMappaRuoli(userInfoService.getRuoliPerModuli(user, null));
        user.setPermessiDiFlusso(userInfoService.getPermessiDiFlusso(user));
        userInfoService.getPermessiDelegaRemoveCache(user);
        logger.info("realUser: " + objectMapper.writeValueAsString(user));
        logger.info("aziendaRealUserLoaded: " + (aziendaRealUser != null ? aziendaRealUser.getId().toString() : "null"));
        logger.info("impersonatedUser: " + utenteImpersonatoStr);
        logger.info("aziendaImpersonatedUserLoaded: " + (aziendaImpersonatedUser != null ? aziendaImpersonatedUser.getId().toString() : "null"));
        List<Integer> permessiDelega = userInfoService.getPermessiDelega(user);
        logger.info("permessiDelega: " + Arrays.toString(permessiDelega.toArray()));

        if (user == null) {
            throw new ObjectNotFoundException("User not found");
        }
        // controlla se è stato passato il parametro di utente impersonato
        if (StringUtils.hasText(utenteImpersonatoStr)) {
            // solo se l'utente reale è super demiurgo allora può fare il cambia utente
            List<Ruolo> ruoli = user.getMappaRuoli().get(Ruolo.ModuliRuolo.GENERALE.toString());

            for (Ruolo ruolo : ruoli) {
                Ruolo.CodiciRuolo codiceRuolo = (Ruolo.CodiciRuolo) ruolo.getNomeBreve();
                if (codiceRuolo == Ruolo.CodiciRuolo.SD) {
                    isSuperDemiurgo = true;
                    break;
                }
            }

            userInfoService.loadUtenteRemoveCache(entityClass, field, utenteImpersonatoStr, aziendaImpersonatedUser, false);
            impersonatedUser = userInfoService.loadUtente(entityClass, field, utenteImpersonatoStr, aziendaImpersonatedUser, false);
            logger.info("loadedImpersonateUser: " + (impersonatedUser != null ? impersonatedUser.getId().toString() : "null"));
            userInfoService.loadUtenteRemoveCache(impersonatedUser.getId());
            userInfoService.getUtentiPersonaByUtenteRemoveCache(impersonatedUser);
            userInfoService.getUtentiPersonaRemoveCache(impersonatedUser.getIdPersona());
            userInfoService.getUtenteStrutturaListRemoveCache(impersonatedUser, true);
            userInfoService.getUtenteStrutturaListRemoveCache(impersonatedUser, false);
            cacheUtilities.cleanCacheRuoliUtente(impersonatedUser.getId(), impersonatedUser.getIdPersona().getId());

//            userInfoService.getPermessiDiFlussoRemoveCache(impersonatedUser);
//            userInfoService.getPermessiDiFlussoRemoveCache(impersonatedUser, null, false);
//            userInfoService.getPermessiDiFlussoRemoveCache(impersonatedUser, null, true);
            cacheUtilities.cleanCachePermessiUtente(impersonatedUser.getId());

            impersonatedUser.setUtenteReale(user);

            boolean isDelegato = permessiDelega != null && !permessiDelega.isEmpty() && permessiDelega.contains(impersonatedUser.getId());

            logger.info("isSuperDemiurgo: " + isSuperDemiurgo);
            logger.info("isDelegato: " + isDelegato);
            if (isSuperDemiurgo || isDelegato) {
                logger.info(String.format("utente %s ha ruolo SD", realUserSubject));

                // mi metto in sessione l'utente loggato, mi servirà in altri punti nella procedura di login, in particolare in projection custom
                httpSessionData.putData(InternautaConstants.HttpSessionData.Keys.UtenteLogin, impersonatedUser);
//                impersonatedUser.setPasswordHash(null);

//                CustomUtenteLogin impersonatedUserWithPersonaAndAzienda = factory.createProjection(CustomUtenteLogin.class, impersonatedUser);
                String impersonateUserSubject = String.valueOf(impersonatedUser.getId());

                // se utente reale = utente impersonato allora non si fa il cambia utente
                if (realUserSubject.equals(impersonateUserSubject)) {
                    logger.info(String.format("utente reale %s == utente impersonato %s", realUserSubject, impersonateUserSubject));
//                    user = null;
                }

                // ritorna utente impersonato con informazioni dell'utente reale
                res = new ResponseEntity(
                        generateLoginResponse(impersonatedUser, user, aziendaImpersonatedUser, entityClass, field, utenteImpersonatoStr, secretKey, applicazione, fromInternetLogin),
                        HttpStatus.OK);
            } else {
                // mi metto in sessione l'utente loggato, mi servirà in altri punti nella procedura di login, in particolare in projection custm
                httpSessionData.putData(InternautaConstants.HttpSessionData.Keys.UtenteLogin, user);
                // ritorna l'utente stesso perchè non ha i permessi per fare il cambia utente
                logger.info(String.format("utente %s non ha ruolo SD, ritorna se stesso nel token", realUserSubject));
                res = new ResponseEntity(
                        generateLoginResponse(user, null, aziendaRealUser, entityClass, field, ssoFieldValue, secretKey, applicazione, fromInternetLogin),
                        HttpStatus.OK);
            }
        } else {
            httpSessionData.putData(InternautaConstants.HttpSessionData.Keys.UtenteLogin, user);

            // ritorna l'utente reale perchè non è stato passato l'utente impersonato
            res = new ResponseEntity(
                    generateLoginResponse(user, null, aziendaRealUser, entityClass, field, ssoFieldValue, secretKey, applicazione, fromInternetLogin),
                    HttpStatus.OK);
        }
        if (writeUserAccess) {
//          write information to DB about real new LOG IN 
            this.writeNewUserAccess(user, fromInternetLogin, applicazione, aziendaRealUser.getCodice());
        }
        return res;

        //        DateTime currentDateTime = DateTime.now();
        //        return new ResponseEntity(
        //                new LoginController.LoginResponse(
        //                        Jwts.builder()
        //                                .setSubject(String.valueOf(user.getId()))
        //                                .claim(AuthorizationUtils.TokenClaims.COMPANY.name(), String.valueOf(azienda.getId()))
        //                                .claim(AuthorizationUtils.TokenClaims.SSO_LOGIN.name(), true)
        //                                .claim(AuthorizationUtils.TokenClaims.USER_ENTITY_CLASS.name(), entityClass)
        //                                .claim(AuthorizationUtils.TokenClaims.USER_FIELD.name(), field)
        //                                .claim(AuthorizationUtils.TokenClaims.USER_SSO_FIELD_VALUE.name(), ssoFieldValue)
        //                                .setIssuedAt(currentDateTime.toDate())
        //                                .setExpiration(tokenExpireSeconds > 0 ? currentDateTime.plusSeconds(tokenExpireSeconds).toDate() : null)
        //                                .signWith(SIGNATURE_ALGORITHM, secretKey).compact(),
        //                        user.getUsername(),
        //                        user),
        //                HttpStatus.OK);
        //    }
    }

//  funtion that calls the repository needed to write to DB info about real new LOG IN from Scrivania
    private void writeNewUserAccess(Utente realUser, Boolean fromInternet, String applicazione, String codiceAzienda) {
        UserAccess userAccess = new UserAccess(realUser.getId(), realUser.getIdPersona().getCodiceFiscale(), realUser.getIdPersona().getDescrizione(), fromInternet, applicazione, codiceAzienda);
        userAccessRepository.save(userAccess);
    }

    private boolean fromInternet(HttpServletRequest request) {
        try {
            String internet = request.getAttribute("internet").toString();
            logger.info("letto dalla sessione request.getAttribute(\"internet\"): " + request.getAttribute("internet"));
            return Boolean.parseBoolean(internet);
        } catch (Exception ex) {
            logger.info("nel catch di fromInternet()");
            return false;
        }
    }

    public ResponseEntity generateResponseEntityFromUsername() {
        return null;
    }

    private LoginController.LoginResponse generateLoginResponse(
            Utente currentUser,
            Utente realUser,
            Azienda azienda,
            Class<?> entityClass,
            String field,
            String ssoFieldValue,
            String secretKey,
            String applicazione,
            boolean fromInternet) {
        DateTime currentDateTime = DateTime.now();

        logger.info("generateLoginResponse fromInternet: " + fromInternet);
        String realUserStr = null;
        String realUserCfStr = null;
        if (realUser != null) {
            realUserStr = String.valueOf(realUser.getId());
            realUserCfStr = realUser.getIdPersona().getCodiceFiscale();
        }
        Integer idSessionLog = createIdSessionLog().getId();
        String idSessionLogString = String.valueOf(idSessionLog);
        String token = Jwts.builder()
                .setSubject(String.valueOf(currentUser.getId()))
                .claim(AuthorizationUtils.TokenClaims.COMPANY.name(), String.valueOf(azienda.getId()))
                .claim(AuthorizationUtils.TokenClaims.SSO_LOGIN.name(), true)
                .claim(AuthorizationUtils.TokenClaims.USER_ENTITY_CLASS.name(), entityClass)
                .claim(AuthorizationUtils.TokenClaims.USER_FIELD.name(), field)
                .claim(AuthorizationUtils.TokenClaims.REAL_USER_SSO_FIELD_VALUE.name(), realUserCfStr)
                .claim(AuthorizationUtils.TokenClaims.USER_SSO_FIELD_VALUE.name(), ssoFieldValue)
                .claim(AuthorizationUtils.TokenClaims.ID_SESSION_LOG.name(), idSessionLogString)
                .claim(AuthorizationUtils.TokenClaims.REAL_USER.name(), realUserStr)
                .claim(AuthorizationUtils.TokenClaims.FROM_INTERNET.name(), fromInternet)
                .setIssuedAt(currentDateTime.toDate())
                .setExpiration(tokenExpireSeconds > 0 ? currentDateTime.plusSeconds(tokenExpireSeconds).toDate() : null)
                .signWith(SIGNATURE_ALGORITHM, secretKey).compact();
        httpSessionData.putData(InternautaConstants.HttpSessionData.Keys.IdSessionLog, idSessionLog);
        if (realUser != null) {
            insertInContext(realUser, currentUser, idSessionLog, token, applicazione, fromInternet);
        } else {
            insertInContext(currentUser, idSessionLog, field, applicazione, fromInternet);
        }

        CustomUtenteLogin customUtenteLogin = factory.createProjection(CustomUtenteLogin.class, currentUser);

        return new LoginController.LoginResponse(
                token, currentUser.getUsername(), customUtenteLogin);
    }

    public Counter createIdSessionLog() {
        Counter counter = new Counter();
        counter.setOggetto("generate_after_login");

        return counterRepository.save(counter);

    }
}
