package it.bologna.ausl.internauta.service.authorization.jwt;

import com.fasterxml.jackson.core.JsonProcessingException;
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
import it.bologna.ausl.model.entities.baborg.Ruolo;
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
import it.bologna.ausl.model.entities.baborg.projections.CustomUtenteLogin;

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

    private boolean isSD(Utente user) {
        user.setRuoli(userInfoService.getRuoli(user, null));
        List<Ruolo> ruoli = user.getRuoli();
        Boolean isSD = ruoli.stream().anyMatch(p -> p.getNomeBreve() == Ruolo.CodiciRuolo.SD);
        return isSD;
    }

    @RequestMapping(value = "${security.login.path}", method = RequestMethod.POST)
    public ResponseEntity<LoginResponse> loginPOST(@RequestBody final UserLogin userLogin, javax.servlet.http.HttpServletRequest request) throws NoSuchAlgorithmException, InvalidKeySpecException, JsonProcessingException, IOException, BlackBoxPermissionException {
        String hostname = commonUtils.getHostname(request);

        logger.debug("login username: " + userLogin.username);
        logger.debug("login password: " + userLogin.password);
        logger.debug("login realUser: " + userLogin.realUser);
        logger.debug("login applicazione: " + userLogin.application);

        userInfoService.loadUtenteRemoveCache(userLogin.username, hostname);
        Utente utente = userInfoService.loadUtente(userLogin.username, hostname);
        if (utente == null) {
            return new ResponseEntity(HttpStatus.FORBIDDEN);
        }
        if (!PasswordHashUtils.validatePassword(userLogin.password, utente.getPasswordHash())) {
            return new ResponseEntity(HttpStatus.FORBIDDEN);
        }

        userInfoService.getRuoliRemoveCache(utente);
        // TODO: permessi
        userInfoService.getPermessiDiFlussoRemoveCache(utente);
        userInfoService.loadUtenteRemoveCache(utente.getId());
        userInfoService.getUtentiPersonaByUtenteRemoveCache(utente);
        userInfoService.getUtentiPersonaRemoveCache(utente.getIdPersona());
        userInfoService.getPermessiPecRemoveCache(utente.getIdPersona());
        
        if (StringUtils.hasText(userLogin.realUser)) {
            // TODO: controllare che l'utente possa fare il cambia utente
            userInfoService.loadUtenteRemoveCache(userLogin.realUser, hostname);
            Utente utenteReale = userInfoService.loadUtente(userLogin.realUser, hostname);
            userInfoService.getRuoliRemoveCache(utenteReale);
            // TODO: permessi
            userInfoService.getPermessiDiFlussoRemoveCache(utenteReale);
            userInfoService.loadUtenteRemoveCache(utenteReale.getId());
            userInfoService.getUtentiPersonaByUtenteRemoveCache(utenteReale);
            userInfoService.getUtentiPersonaRemoveCache(utenteReale.getIdPersona());
            userInfoService.getPermessiDelegaRemoveCache(utenteReale);
            List<Integer> permessiDelega = userInfoService.getPermessiDelega(utenteReale);
            boolean isSuperDemiurgo = isSD(utenteReale);
            boolean isDelegato = permessiDelega != null && !permessiDelega.isEmpty() && permessiDelega.contains(utente.getId());

            if (!isSuperDemiurgo && !isDelegato) {
                return new ResponseEntity("Non puoi cambiare utente!", HttpStatus.UNAUTHORIZED);
            }

            utente.setUtenteReale(utenteReale);
        }
        
        
        CustomUtenteLogin utenteWithPersona = factory.createProjection(CustomUtenteLogin.class, utente);

        Integer idSessionLog = authorizationUtils.createIdSessionLog().getId();
        String idSessionLogString = String.valueOf(idSessionLog);
        
        // mi metto in sessione l'utente_loggato e l'id_session_log, mi servirà in altri punti nella procedura di login, 
        // in particolare in projection custom
        httpSessionData.putData(InternautaConstants.HttpSessionData.Keys.UtenteLogin, utente);
        httpSessionData.putData(InternautaConstants.HttpSessionData.Keys.IdSessionLog, idSessionLogString);
//        utente.setRuoli(userInfoService.getRuoli(utente));
        DateTime currentDateTime = DateTime.now();
        String token = Jwts.builder()
                .setSubject(String.valueOf(utente.getId()))
                .claim(AuthorizationUtils.TokenClaims.SSO_LOGIN.name(), false)
                .claim(AuthorizationUtils.TokenClaims.COMPANY.name(), utente.getIdAzienda().getId())
                .claim(AuthorizationUtils.TokenClaims.ID_SESSION_LOG.name(), idSessionLogString)
                .setIssuedAt(currentDateTime.toDate())
                .setExpiration(tokenExpireSeconds > 0 ? currentDateTime.plusSeconds(tokenExpireSeconds).toDate() : null)
                .signWith(SIGNATURE_ALGORITHM, secretKey)
                .compact();
        
        authorizationUtils.insertInContext(utente.getUtenteReale(), utente, idSessionLog, token, userLogin.application);

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
        String applicazione = request.getParameter(APPLICATION);
        String azienda = request.getParameter(AZIENDA);
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
            res = authorizationUtils.generateResponseEntityFromSAML(azienda, hostname, secretKey, request, null, impersonateUser, applicazione);
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
        public String application;
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
