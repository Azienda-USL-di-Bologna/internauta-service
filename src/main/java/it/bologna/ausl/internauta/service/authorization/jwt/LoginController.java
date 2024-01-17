package it.bologna.ausl.internauta.service.authorization.jwt;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.bologna.ausl.internauta.service.authorization.UserInfoService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Header;
import io.jsonwebtoken.Jwt;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import it.bologna.ausl.blackbox.exceptions.BlackBoxPermissionException;
import it.bologna.ausl.internauta.service.authorization.AuthenticatedSessionData;
import it.bologna.ausl.internauta.service.authorization.AuthenticatedSessionDataBuilder;
import it.bologna.ausl.internauta.service.authorization.cognito.AWSCognitoUtils;
import it.bologna.ausl.internauta.service.authorization.cognito.AwsCognitoJwtParserUtils;
import it.bologna.ausl.internauta.service.authorization.cognito.CognitoJWT;
import it.bologna.ausl.model.entities.baborg.Utente;
import it.bologna.ausl.internauta.service.exceptions.ObjectNotFoundException;
import it.bologna.ausl.internauta.service.exceptions.SSOException;
import it.bologna.ausl.internauta.service.exceptions.intimus.IntimusSendCommandException;
import it.bologna.ausl.internauta.service.utils.CacheUtilities;
import it.bologna.ausl.internauta.service.schedulers.workers.logoutmanager.LogoutManagerWorker;
import it.bologna.ausl.internauta.service.utils.CachedEntities;
import it.bologna.ausl.internauta.service.utils.HttpSessionData;
import it.bologna.ausl.internauta.service.utils.InternautaConstants;
import it.bologna.ausl.model.entities.baborg.Azienda;
import it.bologna.ausl.model.entities.baborg.Persona;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import it.nextsw.common.utils.CommonUtils;
import it.nextsw.common.utils.PasswordHashUtils;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import javax.servlet.http.HttpServletRequest;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.projection.ProjectionFactory;
import org.springframework.http.HttpStatus;
import java.util.List;
import org.springframework.util.StringUtils;
import it.bologna.ausl.model.entities.configurazione.Applicazione.Applicazioni;
import java.time.ZonedDateTime;
import java.util.Date;
import org.springframework.web.bind.annotation.RequestParam;
import it.bologna.ausl.model.entities.baborg.projections.utente.UtenteLoginCustom;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.codec.digest.DigestUtils;

/**
 *
 * @author gdm
 */
@RestController
//@RequestMapping(value = "${custom.mapping.url.login}")
public class LoginController {

    private static final Logger logger = LoggerFactory.getLogger(LoginController.class);

    private final SignatureAlgorithm SIGNATURE_ALGORITHM = SignatureAlgorithm.HS256;

    private final String IMPERSONATE_USER = "impersonatedUser";
    private final String APPLICATION = "application";
    private final String AZIENDA = "azienda";
    private final String PASS_TOKEN = "passToken";
    private final String NEW_USER_ACCESS = "newUserAccess";
    private final String COGNITO_CODE = "code";

//    @Value("${jwt.secret}")
//    private String secretKey;
//
//    @Value("${jwt.expires-seconds}")
//    private Integer tokenExpireSeconds;
//
//    @Value("${jwt.passtoken-expires-seconds}")
//    private Integer passTokenExpireSeconds;
//
//    @Value("${jwt.saml.enabled:false}")
//    private boolean samlEnabled;
//
//    @Value("${jwt.cognito.enabled:false}")
//    private boolean cognitoEnabled;

    @Autowired
    private AuthorizationUtils authorizationUtils;

    @Autowired
    private UserInfoService userInfoService;

    @Autowired
    private CommonUtils commonUtils;

    @Autowired
    private CacheUtilities cacheUtilities;
    
    @Autowired
    private CachedEntities cachedEntities;

    @Autowired
    private ProjectionFactory factory;

    @Autowired
    private HttpSessionData httpSessionData;

    @Autowired
    private LogoutManagerWorker logoutManagerWorker;

    @Autowired
    private AuthenticatedSessionDataBuilder authenticatedSessionDataBuilder;

    @Autowired
    private AwsCognitoJwtParserUtils cognitoJwtParserUtils;
    
    @Autowired
    private LoginConfig loginConfig;
    
    @Autowired
    private ObjectMapper objectMapper;

    @RequestMapping(value = "${internauta.security.passtoken-path}", method = RequestMethod.GET)
    public ResponseEntity<String> passTokenGenerator() throws BlackBoxPermissionException {
        
        AuthenticatedSessionData authenticatedUserProperties = authenticatedSessionDataBuilder.getAuthenticatedUserProperties();
        Utente user = authenticatedUserProperties.getUser();
        Utente realUser = authenticatedUserProperties.getRealUser();
        String realUserStr = null;
        String realUserUsernameStr = null;
        String realUserSSOFieldValue = null;
        if (realUser != null) {
            realUserStr = String.valueOf(realUser.getId());
        }
        if (realUser != null) {
            realUserUsernameStr = String.valueOf(realUser.getUsername());
        }
        if (realUser != null) {
            realUserSSOFieldValue = String.valueOf(realUser.getIdPersona().getCodiceFiscale());
        }
        Integer idAzienda = realUser != null ? realUser.getIdAzienda().getId():  user.getIdAzienda().getId();
        loginConfig.readConfig(idAzienda);
        
        ZonedDateTime currentDateTime = ZonedDateTime.now();
        String token = Jwts.builder()
                .setSubject(String.valueOf(user.getId()))
                .claim(AuthorizationUtils.TokenClaims.USERNAME.name(), user.getUsername())
                .claim(AuthorizationUtils.TokenClaims.USER_SSO_FIELD_VALUE.name(), user.getIdPersona().getCodiceFiscale())
                .claim(AuthorizationUtils.TokenClaims.REAL_USER.name(), realUserStr)
                .claim(AuthorizationUtils.TokenClaims.REAL_USER_USERNAME.name(), realUserUsernameStr)
                .claim(AuthorizationUtils.TokenClaims.REAL_USER_SSO_FIELD_VALUE.name(), realUserSSOFieldValue)
                .claim(AuthorizationUtils.TokenClaims.FROM_INTERNET.name(), authenticatedUserProperties.isFromInternet())
                .setIssuedAt(Date.from(currentDateTime.toInstant()))
                .setExpiration(loginConfig.getJwtExpiresSeconds()> 0 ? Date.from(currentDateTime.plusSeconds(loginConfig.getPasstokenExpiresSeconds()).toInstant()) : null)
                .signWith(SIGNATURE_ALGORITHM, loginConfig.getJwtSecret()).compact();
        return new ResponseEntity(token, HttpStatus.OK);
    }

    @RequestMapping(value = "${security.logout.path}", method = RequestMethod.GET)
    public ResponseEntity<LoginResponse> logout(@RequestParam("redirectUrl") String redirectUrl) throws BlackBoxPermissionException, IOException, IntimusSendCommandException {

        // voglio fare il logout dell'utente reale su tutte le sue aziende
        Persona persona = getPersonaReale();
        try {
            this.logoutManagerWorker.sendLogoutCommand(persona, redirectUrl);
        } catch (Exception ex) {
            String errorMessage = String.format("errore nell'invio del comando di logout alla persona: %s", persona.getCodiceFiscale());
            logger.error(errorMessage, ex);
            return new ResponseEntity(errorMessage, HttpStatus.INTERNAL_SERVER_ERROR);
        }
        return new ResponseEntity(HttpStatus.OK);
    }

    @RequestMapping(value = "${security.refresh-session.path}", method = RequestMethod.GET)
    public ResponseEntity<LoginResponse> refresh(@RequestParam("redirectUrl") String redirectUrl) throws BlackBoxPermissionException {

        // voglio refreshare l'utente reale
        Persona persona = getPersonaReale();
        try {
            this.logoutManagerWorker.addOrRefreshPersona(persona, redirectUrl);
        } catch (Exception ex) {
            String errorMessage = String.format("errore nell'aggiornamento della data di unltimo refresh della persona: %s con codice fiscale: %s", persona.getId(), persona.getCodiceFiscale());
            logger.error(errorMessage, ex);
            return new ResponseEntity(errorMessage, HttpStatus.INTERNAL_SERVER_ERROR);
        }
        return new ResponseEntity(HttpStatus.OK);
    }

    /**
     * ritorna la persona reale connessa: cioè la persona derivata dall'utente
     * connesso in caso di login senza cambia utente; la persona associata
     * all'utente reale nel caso di logon con cambio utente
     */
    private Persona getPersonaReale() throws BlackBoxPermissionException {
        // leggo l'utente connesso dalla sessione
        AuthenticatedSessionData authenticatedUserProperties = authenticatedSessionDataBuilder.getAuthenticatedUserProperties();
        Utente user = authenticatedUserProperties.getUser();
        Utente realUser = authenticatedUserProperties.getRealUser();
        Persona persona;
        if (realUser != null) {
            persona = realUser.getIdPersona();
        } else {
            persona = user.getIdPersona();
        }

        return persona;
    }

    @RequestMapping(value = "${security.login.path}", method = RequestMethod.POST)
    public ResponseEntity<LoginResponse> loginPOST(@RequestBody final UserLogin userLogin, javax.servlet.http.HttpServletRequest request) throws NoSuchAlgorithmException, InvalidKeySpecException, JsonProcessingException, IOException, BlackBoxPermissionException {
        String hostname = commonUtils.getHostname(request);

        logger.info("login username: " + userLogin.username);
        logger.info("login password: " + userLogin.password);
        logger.info("login realUser: " + userLogin.realUser);
        logger.info("login applicazione: " + userLogin.application);
        logger.info("passToken: " + userLogin.passToken);
        logger.info("login, is a new user access ?: " + userLogin.newUserAccess);
        logger.info("hostname: " + hostname);

        if (StringUtils.hasText(userLogin.passToken)) {
            logger.info("c'è il passToken, agisco di conseguenza...");
            try {
                /* 
                devo estrarre l'azienda dal passtoken per leggere i parametri dai parametri_azienda,
                per farlo devo leggere l'utente dai claims per poter poi estrarre l'azienda.
                Una volta letti i parametri potrò leggere il jwtSecret per poter controllare la firma del passtoken
                */
                Integer idAzienda = authorizationUtils.getIdAziendaFromPassTken(userLogin.passToken);
                loginConfig.readConfig(idAzienda);
                Claims claims = Jwts.parser().
                    setSigningKey(loginConfig.getJwtSecret()).
                    parseClaimsJws(userLogin.passToken).
                    getBody();

                Object usernameObj = claims.get(AuthorizationUtils.TokenClaims.USERNAME.name());
                Object realUserUsernameObj = claims.get(AuthorizationUtils.TokenClaims.REAL_USER_USERNAME.name());
                userLogin.username = usernameObj.toString();
                if (realUserUsernameObj != null) {
                    userLogin.realUser = realUserUsernameObj.toString();
                }
            } catch (Exception ex) {
                return new ResponseEntity("passToken non valido", HttpStatus.FORBIDDEN);
            }
        } else if (StringUtils.hasText(hostname)) {
            /* 
            se non c'è il passtoken recupero l'azienda dall'hostname, 
            il caso in cui l'applicazione è accessibile sia da internet che non e faccio il login post non è gestito e non saprei neanche come farlo
            */
            Integer idAzienda = null;
            Azienda aziendaFromPath = cachedEntities.getAziendaFromPath(hostname);
            if (aziendaFromPath != null) {
                idAzienda = aziendaFromPath.getId();
            }
            // se non riesco a leggere l'azienda, la lettura dei parametri non verrà effettuata e saranno usati quelli dell'application.properties
            if (idAzienda != null) {
                loginConfig.readConfig(idAzienda);
            }
        }

        userInfoService.loadUtenteRemoveCache(userLogin.username, hostname, true);
        Utente utente = userInfoService.loadUtente(userLogin.username, hostname, true);
        if (utente == null) {
            return new ResponseEntity(HttpStatus.FORBIDDEN);
        }

        // caso particolare solo per verona
        if (!StringUtils.hasText(userLogin.passToken) && utente.getIdAzienda().getCodice().equals("050109")) {
            String md5DaCalcolare = userLogin.username + "/" + userLogin.password;
            if (!DigestUtils.md5Hex(md5DaCalcolare).toUpperCase().equals(utente.getPasswordHash().toUpperCase())) {
                return new ResponseEntity(HttpStatus.FORBIDDEN);
            }
        } else {
            if (!StringUtils.hasText(userLogin.passToken)) {
                if (!PasswordHashUtils.validatePassword(userLogin.password, utente.getPasswordHash())) {
                    return new ResponseEntity(HttpStatus.FORBIDDEN);
                }
            }
        }

        //userInfoService.getRuoliRemoveCache(utente);
        cacheUtilities.cleanCacheRuoliUtente(utente.getId(), utente.getIdPersona().getId());

//        userInfoService.getPermessiDiFlussoRemoveCache(utente);
//        userInfoService.getPermessiDiFlussoRemoveCache(utente, null, true);
//        userInfoService.getPermessiDiFlussoRemoveCache(utente, null, false);
//        userInfoService.getPermessiDiFlussoRemoveCache(utente);
        cacheUtilities.cleanCachePermessiUtente(utente.getId());

        userInfoService.loadUtenteRemoveCache(utente.getId());
        userInfoService.getUtentiPersonaByUtenteRemoveCache(utente);
        userInfoService.getUtentiPersonaRemoveCache(utente.getIdPersona());
        userInfoService.getUtenteStrutturaListRemoveCache(utente, true);
        userInfoService.getUtenteStrutturaListRemoveCache(utente, false);
        userInfoService.getPermessiGediByCodiceAziendaRemoveCache(utente.getIdPersona());
        userInfoService.getPermessiPecRemoveCache(utente.getIdPersona());
        userInfoService.getStruttureDelSegretarioRemoveCache(utente.getIdPersona());

        String realUserId = null;
        if (StringUtils.hasText(userLogin.realUser)) {
            // TODO: controllare che l'utente possa fare il cambia utente
            userInfoService.loadUtenteRemoveCache(userLogin.realUser, hostname, false);
            Utente utenteReale = userInfoService.loadUtente(userLogin.realUser, hostname, false);
            //userInfoService.getRuoliRemoveCache(utenteReale);
            cacheUtilities.cleanCacheRuoliUtente(utenteReale.getId(), utenteReale.getIdPersona().getId());
            cacheUtilities.cleanCachePermessiUtente(utenteReale.getId());
            // TODO: permessi
            userInfoService.getPermessiDiFlussoRemoveCache(utenteReale);
            userInfoService.loadUtenteRemoveCache(utenteReale.getId());
            userInfoService.getUtentiPersonaByUtenteRemoveCache(utenteReale);
            userInfoService.getUtentiPersonaRemoveCache(utenteReale.getIdPersona());
            userInfoService.getPermessiGediByCodiceAziendaRemoveCache(utenteReale.getIdPersona());
            userInfoService.getUtenteStrutturaListRemoveCache(utenteReale, true);
            userInfoService.getUtenteStrutturaListRemoveCache(utenteReale, false);
            userInfoService.getStruttureDelSegretarioRemoveCache(utenteReale.getIdPersona());
//            userInfoService.getPermessiDelegaRemoveCache(utenteReale);
            List<Integer> permessiAvatar = userInfoService.getPermessiAvatar(utenteReale);
            boolean isSD = userInfoService.isSD(utenteReale);
            boolean isSDImpersonato = userInfoService.isSD(utente);
            boolean isCI = userInfoService.isCI(utenteReale);
            boolean isCA = userInfoService.isCA(utenteReale);
            boolean isAvatarato = permessiAvatar != null && !permessiAvatar.isEmpty() && permessiAvatar.contains(utente.getId());

            if (!isSD
                    && ((isCI || isCA) && isSDImpersonato)
                    && !isCI
                    && // se sei CA puoi cambiare utente solo se l'utente è parte dei un'azienda di cui sei CA
                    (!isCA || !authorizationUtils.isCAOfAziendaUtenteImpersonato(utenteReale, utente))
                    && !isAvatarato) {
                return new ResponseEntity("Non puoi cambiare utente!", HttpStatus.UNAUTHORIZED);
            }

            utente.setUtenteReale(utenteReale);
            realUserId = String.valueOf(utenteReale.getId());
        }

        Integer idSessionLog = authorizationUtils.createIdSessionLog().getId();
        String idSessionLogString = String.valueOf(idSessionLog);

        // mi metto in sessione l'utente_loggato e l'id_session_log, mi servirà in altri punti nella procedura di login,
        // in particolare in projection custom
        httpSessionData.putData(InternautaConstants.HttpSessionData.Keys.UtenteLogin, utente);
        httpSessionData.putData(InternautaConstants.HttpSessionData.Keys.IdSessionLog, idSessionLog);
//        utente.setRuoli(userInfoService.getRuoli(utente));
        DateTime currentDateTime = DateTime.now();
        String token = Jwts.builder()
                .setSubject(String.valueOf(utente.getId()))
                .claim(AuthorizationUtils.TokenClaims.REAL_USER.name(), realUserId)
                .claim(AuthorizationUtils.TokenClaims.SSO_LOGIN.name(), false)
                .claim(AuthorizationUtils.TokenClaims.COMPANY.name(), utente.getIdAzienda().getId())
                .claim(AuthorizationUtils.TokenClaims.ID_SESSION_LOG.name(), idSessionLogString)
                .setIssuedAt(currentDateTime.toDate())
                .setExpiration(loginConfig.getJwtExpiresSeconds() > 0 ? currentDateTime.plusSeconds(loginConfig.getJwtExpiresSeconds()).toDate() : null)
                .signWith(SIGNATURE_ALGORITHM, loginConfig.getJwtSecret())
                .compact();

        authorizationUtils.insertInContext(utente.getUtenteReale(), utente, idSessionLog, token, userLogin.application, false);

        UtenteLoginCustom utenteWithPersona = factory.createProjection(UtenteLoginCustom.class, utente);

//        utente.setPasswordHash(null);
        return new ResponseEntity(
                new LoginResponse(
                        token,
                        utente.getUsername(),
                        utenteWithPersona),
                HttpStatus.OK);
    }

    @RequestMapping(value = "${security.login.path}", method = RequestMethod.GET)
//    @Transactional(rollbackFor = Throwable.class)
    public ResponseEntity<LoginResponse> loginGET(HttpServletRequest request, HttpServletResponse response) throws NoSuchAlgorithmException, InvalidKeySpecException, IOException, ClassNotFoundException, Exception {

        String impersonateUser = request.getParameter(IMPERSONATE_USER);
        String applicazione = request.getParameter(APPLICATION);
        String azienda = request.getParameter(AZIENDA);
        String passToken = request.getParameter(PASS_TOKEN);
        String newUserAccessString = request.getParameter(NEW_USER_ACCESS);
        String cognitoCode = request.getParameter(COGNITO_CODE);
        String hostname = commonUtils.getHostname(request);

        logger.info("impersonate user: " + impersonateUser);
        logger.info("applicazione: " + applicazione);
        logger.info("azienda: " + azienda);
        logger.info("passToken: " + passToken);
        logger.info("is a new user access ?" + newUserAccessString);
        logger.info("hostname: " + hostname);

        String ssoFieldValue = null;
        Boolean fromInternet = authorizationUtils.fromInternet(request);
        if (StringUtils.hasText(passToken)) {
            logger.info("c'è il passToken, agisco di conseguenza...");
            try {
                /* 
                devo estrarre l'azienda dal passtoken per leggere i parametri dai parametri_azienda,
                per farlo devo leggere l'utente dai claims per poter poi estrarre l'azienda.
                Una volta letti i parametri potrò leggere il jwtSecret per poter controllare la firma del passtoken
                */
                Integer idAzienda = authorizationUtils.getIdAziendaFromPassTken(passToken);
                loginConfig.readConfig(idAzienda);
                
                // controlla la firma, se non è valida da eccozione
                Claims claims = Jwts.parser().setSigningKey(loginConfig.getJwtSecret()).parseClaimsJws(passToken).getBody();

                Object userSSOFieldValueObj = claims.get(AuthorizationUtils.TokenClaims.USER_SSO_FIELD_VALUE.name());
                Object realUserSSOFieldValueObj = claims.get(AuthorizationUtils.TokenClaims.REAL_USER_SSO_FIELD_VALUE.name());
                Object fromInternetObj = claims.get(AuthorizationUtils.TokenClaims.FROM_INTERNET.name());
                if (fromInternetObj != null && !fromInternetObj.toString().equals("")) {
                    fromInternet = Boolean.parseBoolean(fromInternetObj.toString());
                }

                if (realUserSSOFieldValueObj != null) {
//                   impersonateUser = realUserSSOFieldValueObj.toString();
                    ssoFieldValue = realUserSSOFieldValueObj.toString();
                    if (StringUtils.isEmpty(impersonateUser)) {
                        impersonateUser = userSSOFieldValueObj.toString();
                    }
                } else {
                    ssoFieldValue = userSSOFieldValueObj.toString();
                }
            } catch (Exception ex) {
                logger.error("passToken non valido", ex);
                return new ResponseEntity("passToken non valido", HttpStatus.FORBIDDEN);
            }
        } else if (StringUtils.hasText(hostname)) { 
            /* 
            se non c'è il passtoken recupero l'azienda dall'hostname, ma nel caso mi trovi su internet l'hostname è lo stesso per tutte le aziende,
            per cui in quel caso prendo l'azienda di default della persona
            */
            Integer idAzienda = null;
            Azienda aziendaFromPath = cachedEntities.getAziendaFromPath(hostname);
            if (aziendaFromPath != null) {
                idAzienda = aziendaFromPath.getId();
            } else if (fromInternet) {
                ssoFieldValue = request.getAttribute("CodiceFiscale").toString();
                Persona person = cachedEntities.getPersonaFromCodiceFiscale(ssoFieldValue);
                if (person != null) {
                    idAzienda = person.getIdAziendaDefault().getId();
                }
            }
            // se non riesco a leggere l'azienda, la lettura dei parametri non verrà effettuata e saranno usati quelli dell'application.properties
            if (idAzienda != null) {
                loginConfig.readConfig(idAzienda);
            }
        }
        
        //LOGIN SAML
        if (!loginConfig.isSamlEnabled() && !StringUtils.hasText(passToken)) {
            if (loginConfig.isCognitoEnabled()) {
                AWSCognitoUtils cognitoUtils = new AWSCognitoUtils(authorizationUtils, objectMapper, loginConfig);
                if (cognitoCode != null && !cognitoCode.equals("")) {
                    logger.info("cognito_code: " + cognitoCode);
                    // TODO: validate JWT
                    CognitoJWT cognitoJWT = cognitoUtils.getCognitoJWT(cognitoCode);
                    String codiceFiscaleRealUser = cognitoJwtParserUtils.getClaim(cognitoJWT.getIdToken(), "custom:codice_fiscale");
                    logger.info("codice_fiscale_utente_richiesto: " + codiceFiscaleRealUser);
                    return cognitoUtils.login(azienda, hostname, loginConfig.getJwtSecret(), request, applicazione, codiceFiscaleRealUser);
                } else {
                    return cognitoUtils.callCognitoUI();

                }
            }
            return new ResponseEntity("SAML and COGNITO authentication not enabled", HttpStatus.UNPROCESSABLE_ENTITY);
        }
        logger.debug("SAML Authentication is enabled");

        

        ResponseEntity res;
//      Create a boolean to manage writes to DB of real new LOG IN
        Boolean writeUserAccess;
        try {
            writeUserAccess = Boolean.parseBoolean(newUserAccessString) && applicazione.equals(Applicazioni.scrivania.toString()) && !StringUtils.hasText(impersonateUser);
            logger.info("writeUserAccess: " + writeUserAccess);
            res = authorizationUtils.generateResponseEntityFromSAML(azienda, hostname, loginConfig.getJwtSecret(), request, ssoFieldValue, impersonateUser, applicazione, fromInternet, writeUserAccess, false);
        } catch (ObjectNotFoundException | BlackBoxPermissionException ex) {
            logger.error("errore nel login", ex);
            res = new ResponseEntity(HttpStatus.FORBIDDEN);
        } catch (SSOException ex) {
            res = new ResponseEntity(HttpStatus.UNPROCESSABLE_ENTITY);
        }
        return res;
    }

    @SuppressWarnings("unused")
    public static class UserLogin {

        public String username;
        public String realUser;
        public String password;
        public String application;
        public String passToken;
        public Boolean newUserAccess;
    }

    @SuppressWarnings("unused")
    public static class LoginResponse {

        public String token;
        public String username;
        public Object userInfo;

        public LoginResponse(final String token, final String username, Object userInfo) {
            this.token = token;
            this.username = username;
            this.userInfo = userInfo;
        }
    }
}
