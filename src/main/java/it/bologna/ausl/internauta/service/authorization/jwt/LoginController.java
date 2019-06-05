package it.bologna.ausl.internauta.service.authorization.jwt;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JavaType;
import it.bologna.ausl.internauta.service.authorization.UserInfoService;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import it.bologna.ausl.blackbox.exceptions.BlackBoxPermissionException;
import it.bologna.ausl.model.entities.baborg.Utente;
import it.bologna.ausl.internauta.service.exceptions.ObjectNotFoundException;
import it.bologna.ausl.internauta.service.repositories.baborg.AziendaRepository;
import it.bologna.ausl.internauta.service.repositories.baborg.UtenteRepository;
import it.bologna.ausl.internauta.service.utils.HttpSessionData;
import it.bologna.ausl.internauta.service.utils.InternautaConstants;
import it.bologna.ausl.internauta.service.utils.ProjectionBeans;
import it.bologna.ausl.model.entities.baborg.Azienda;
import it.bologna.ausl.model.entities.baborg.Persona;
import it.bologna.ausl.model.entities.baborg.projections.generated.UtenteWithIdPersona;
import it.bologna.ausl.model.entities.baborg.projections.generated.UtenteWithPlainFields;
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
import it.bologna.ausl.model.entities.configuration.ImpostazioniApplicazioni;
import java.util.List;
import org.springframework.util.StringUtils;
import it.bologna.ausl.model.entities.baborg.projections.CustomUtenteLogin;
import java.util.logging.Level;

/**
 *
 * @author gdm
 */
@RestController
//@RequestMapping(value = "${custom.mapping.url.login}")
public class LoginController {

    private static final Logger logger = LoggerFactory.getLogger(LoginController.class);

    private final SignatureAlgorithm SIGNATURE_ALGORITHM = SignatureAlgorithm.HS256;

    private final String IMPERSONATE_USER = "utenteImpersonato";
    private final String APPLICAZIONE = "applicazione";

    @Value("${jwt.secret}")
    private String secretKey;

    @Value("${jwt.expires-seconds}")
    private Integer tokenExpireSeconds;

    @Value("${jwt.saml.enabled:false}")
    private boolean samlEnabled;

    @Autowired
    AuthorizationUtils authorizationUtils;

    @Autowired
    UserInfoService userInfoService;

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    CommonUtils commonUtils;

    @Autowired
    UtenteRepository utenteRepository;

    @Autowired
    AziendaRepository aziendaRepository;

    @Autowired
    ProjectionBeans projectionBeans;

    @Autowired
    ProjectionFactory factory;
    
    @Autowired
    HttpSessionData httpSessionData;

    @RequestMapping(value = "${security.login.path}", method = RequestMethod.POST)
    public ResponseEntity<LoginResponse> loginPOST(@RequestBody final UserLogin userLogin, javax.servlet.http.HttpServletRequest request) throws NoSuchAlgorithmException, InvalidKeySpecException, JsonProcessingException, IOException {
        String hostname = commonUtils.getHostname(request);

        logger.debug("login username: " + userLogin.username);
        logger.debug("login username: " + userLogin.password);
        logger.debug("login username: " + userLogin.realUser);
        logger.debug("login username: " + userLogin.applicazione);

        
        userInfoService.loadUtenteRemoveCache(userLogin.username, hostname, userLogin.applicazione);       
        Utente utente = userInfoService.loadUtente(userLogin.username, hostname, userLogin.applicazione);
        if (utente == null) {
            return new ResponseEntity(HttpStatus.FORBIDDEN);
        }
        if (!PasswordHashUtils.validatePassword(userLogin.password, utente.getPasswordHash())) {
            return new ResponseEntity(HttpStatus.FORBIDDEN);
        }
        userInfoService.getRuoliRemoveCache(utente);
        // TODO: permessi
        userInfoService.getPermessiDiFlussoRemoveCache(utente);
        userInfoService.loadUtenteRemoveCache(utente.getId(), userLogin.applicazione);
        userInfoService.getUtentiPersonaByUtenteRemoveCache(utente);
        if (StringUtils.hasText(userLogin.realUser)) {
            // TODO: controllare che l'utente possa fare il cambia utente
            userInfoService.loadUtenteRemoveCache(userLogin.realUser, hostname, userLogin.applicazione);       
            Utente utenteReale = userInfoService.loadUtente(userLogin.realUser, hostname, userLogin.applicazione);
            userInfoService.getRuoliRemoveCache(utenteReale);
            // TODO: permessi
            userInfoService.getPermessiDiFlussoRemoveCache(utenteReale);
            userInfoService.loadUtenteRemoveCache(utenteReale.getId(), userLogin.applicazione);
            userInfoService.getUtentiPersonaByUtenteRemoveCache(utenteReale);
            utente.setUtenteReale(utenteReale);
        }
        

        
        
        CustomUtenteLogin utenteWithPersona = factory.createProjection(CustomUtenteLogin.class, utente);

        String idSessionLog = String.valueOf(authorizationUtils.createIdSessionLog().getId());
        
        // mi metto in sessione l'utente_loggato e l'id_session_log, mi servirà in altri punti nella procedura di login, 
        // in particolare in projection custom
        httpSessionData.putData(InternautaConstants.HttpSessionData.Keys.UtenteLogin, utente);
        httpSessionData.putData(InternautaConstants.HttpSessionData.Keys.IdSessionLog, idSessionLog);
//        utente.setRuoli(userInfoService.getRuoli(utente));

        DateTime currentDateTime = DateTime.now();
        String token = Jwts.builder()
                .setSubject(String.valueOf(utente.getId()))
                .claim(AuthorizationUtils.TokenClaims.SSO_LOGIN.name(), false)
                .claim(AuthorizationUtils.TokenClaims.COMPANY.name(), utente.getIdAzienda().getId())
                .claim(AuthorizationUtils.TokenClaims.ID_SESSION_LOG.name(), idSessionLog)
                .setIssuedAt(currentDateTime.toDate())
                .setExpiration(tokenExpireSeconds > 0 ? currentDateTime.plusSeconds(tokenExpireSeconds).toDate() : null)
                .signWith(SIGNATURE_ALGORITHM, secretKey)
                .compact();
        
        
        

//        utente.setPasswordHash(null);
        return new ResponseEntity(
                new LoginResponse(
                        token,
                        utente.getUsername(),
                        utenteWithPersona),
                HttpStatus.OK);
    }

    @RequestMapping(value = "${security.login.path}", method = RequestMethod.GET)
    public ResponseEntity<LoginResponse> loginGET(HttpServletRequest request) throws NoSuchAlgorithmException, InvalidKeySpecException, IOException, ClassNotFoundException {

        String impersonateUser = request.getParameter(IMPERSONATE_USER);
        String applicazione = request.getParameter(APPLICAZIONE);
        logger.info("impersonate user: " + impersonateUser);
        logger.info("applicazione: " + applicazione);

        //LOGIN SAML
        if (!samlEnabled) {
            return new ResponseEntity("SAML authentication not enabled", HttpStatus.UNAUTHORIZED);
        }
        logger.debug("SAML Authentication is enabled");

        String hostname = commonUtils.getHostname(request);

        ResponseEntity res;
        try {
            res = authorizationUtils.generateResponseEntityFromSAML(hostname, secretKey, request, null, impersonateUser, applicazione);
        } catch (ObjectNotFoundException | BlackBoxPermissionException ex) {
            logger.error("errore nel login", ex);
            res = new ResponseEntity(HttpStatus.FORBIDDEN);
        }
        return res;
    }

    @SuppressWarnings("unused")
    public static class UserLogin {

        public String username;
        public String realUser;
        public String password;
        public String applicazione;
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
