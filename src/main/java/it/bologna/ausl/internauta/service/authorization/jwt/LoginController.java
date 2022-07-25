package it.bologna.ausl.internauta.service.authorization.jwt;

import com.fasterxml.jackson.core.JsonProcessingException;
import it.bologna.ausl.internauta.service.authorization.UserInfoService;
import io.jsonwebtoken.Claims;
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
import it.bologna.ausl.internauta.service.utils.HttpSessionData;
import it.bologna.ausl.internauta.service.utils.InternautaConstants;
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

    @Value("${jwt.secret}")
    private String secretKey;

    @Value("${jwt.expires-seconds}")
    private Integer tokenExpireSeconds;

    @Value("${jwt.passtoken-expires-seconds}")
    private Integer passTokenExpireSeconds;

    @Value("${jwt.saml.enabled:false}")
    private boolean samlEnabled;

    @Value("${jwt.cognito.enabled:false}")
    private boolean cognitoEnabled;

    @Autowired
    private AuthorizationUtils authorizationUtils;

    @Autowired
    private UserInfoService userInfoService;

    @Autowired
    private CommonUtils commonUtils;

    @Autowired
    private CacheUtilities cacheUtilities;

    @Autowired
    private ProjectionFactory factory;

    @Autowired
    private HttpSessionData httpSessionData;

    @Autowired
    private LogoutManagerWorker logoutManagerWorker;

    @Autowired
    private AuthenticatedSessionDataBuilder authenticatedSessionDataBuilder;

    @Autowired
    private AWSCognitoUtils cognitoUtils;

    @Autowired
    private AwsCognitoJwtParserUtils cognitoJwtParserUtils;

    @Value("${jwt.cognito.domain}")
    private String cognitoDomain;

    @Value("${jwt.cognito.client_id}")
    private String clientID;

    @Value("${jwt.cognito.redirect_uri}")
    private String redirectURI;

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
                .setExpiration(tokenExpireSeconds > 0 ? Date.from(currentDateTime.plusSeconds(passTokenExpireSeconds).toInstant()) : null)
                .signWith(SIGNATURE_ALGORITHM, secretKey).compact();
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

        if (userLogin.passToken != null) {
            logger.info("c'è il passToken, agisco di conseguenza...");
            try {
                Claims claims = Jwts.parser().
                        setSigningKey(secretKey).
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
        }

        userInfoService.loadUtenteRemoveCache(userLogin.username, hostname);
        Utente utente = userInfoService.loadUtente(userLogin.username, hostname);
        if (utente == null) {
            return new ResponseEntity(HttpStatus.FORBIDDEN);
        }

        if (utente.getIdAzienda().getCodice().equals("050109")) {
            String md5DaCalcolare = userLogin.username + "/" + userLogin.password;
            if (!DigestUtils.md5Hex(md5DaCalcolare).toUpperCase().equals(utente.getPasswordHash().toUpperCase())) {
                return new ResponseEntity(HttpStatus.FORBIDDEN);
            }

        } else {

            if (!PasswordHashUtils.validatePassword(userLogin.password, utente.getPasswordHash())) {
                return new ResponseEntity(HttpStatus.FORBIDDEN);
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
        userInfoService.getPermessiPecRemoveCache(utente.getIdPersona());
        userInfoService.getStruttureDelSegretarioRemoveCache(utente.getIdPersona());

        String realUserId = null;
        if (StringUtils.hasText(userLogin.realUser)) {
            // TODO: controllare che l'utente possa fare il cambia utente
            userInfoService.loadUtenteRemoveCache(userLogin.realUser, hostname);
            Utente utenteReale = userInfoService.loadUtente(userLogin.realUser, hostname);
            //userInfoService.getRuoliRemoveCache(utenteReale);
            cacheUtilities.cleanCacheRuoliUtente(utenteReale.getId(), utenteReale.getIdPersona().getId());
            cacheUtilities.cleanCachePermessiUtente(utenteReale.getId());
            // TODO: permessi
            userInfoService.getPermessiDiFlussoRemoveCache(utenteReale);
            userInfoService.loadUtenteRemoveCache(utenteReale.getId());
            userInfoService.getUtentiPersonaByUtenteRemoveCache(utenteReale);
            userInfoService.getUtentiPersonaRemoveCache(utenteReale.getIdPersona());
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
                .setExpiration(tokenExpireSeconds > 0 ? currentDateTime.plusSeconds(tokenExpireSeconds).toDate() : null)
                .signWith(SIGNATURE_ALGORITHM, secretKey)
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

        //LOGIN SAML
        if (!samlEnabled) {
            if (cognitoEnabled) {
                if (cognitoCode != null && !cognitoCode.equals("")) {
                    logger.info("cognito_code: " + cognitoCode);
                    // TODO: validate JWT
                    CognitoJWT cognitoJWT = cognitoUtils.getCognitoJWT(cognitoCode);
                    String codiceFiscaleRealUser = cognitoJwtParserUtils.getClaim(cognitoJWT.getIdToken(), "custom:codice_fiscale");
                    logger.info("codice_fiscale_utente_richiesto: " + codiceFiscaleRealUser);
                    return cognitoUtils.login(azienda, hostname, secretKey, request, applicazione, codiceFiscaleRealUser);
                } else {
                    return cognitoUtils.callCognitoUI();

                }
            }
            return new ResponseEntity("SAML and COGNITO authentication not enabled", HttpStatus.UNPROCESSABLE_ENTITY);
        }
        logger.debug("SAML Authentication is enabled");

        String ssoFieldValue = null;
        Boolean fromInternet = null;
        if (StringUtils.hasText(passToken)) {
            logger.info("c'è il passToken, agisco di conseguenza...");
            try {
                Claims claims = Jwts.parser().
                        setSigningKey(secretKey).
                        parseClaimsJws(passToken).
                        getBody();

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
        }

        ResponseEntity res;
//      Create a boolean to manage writes to DB of real new LOG IN
        Boolean writeUserAccess = false;
        try {
            writeUserAccess = Boolean.valueOf(newUserAccessString) && applicazione.equals(Applicazioni.scrivania.toString()) && StringUtils.isEmpty(impersonateUser);
            logger.info("writeUserAccess: " + writeUserAccess);
            res = authorizationUtils.generateResponseEntityFromSAML(azienda, hostname, secretKey, request, ssoFieldValue, impersonateUser, applicazione, fromInternet, writeUserAccess);
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
