package it.bologna.ausl.internauta.service.authorization.jwt;

import it.bologna.ausl.internauta.service.authorization.UserInfoService;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import it.bologna.ausl.model.entities.baborg.Utente;
import it.bologna.ausl.internauta.service.exceptions.ObjectNotFoundException;
import it.bologna.ausl.internauta.service.repositories.baborg.AziendaRepository;
import it.bologna.ausl.internauta.service.repositories.baborg.UtenteRepository;
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
import org.springframework.http.HttpStatus;

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

    @RequestMapping(value = "${security.login.path}", method = RequestMethod.POST)
    public ResponseEntity<LoginResponse> loginPOST(@RequestBody final UserLogin userLogin, javax.servlet.http.HttpServletRequest request) throws NoSuchAlgorithmException, InvalidKeySpecException {
        String hostname = commonUtils.getHostname(request);

        logger.debug("login username: " + userLogin.username);
        logger.debug("login username: " + userLogin.password);

        Utente utente = userInfoService.loadUtente(userLogin.username, hostname);
        utente.setRuoli(userInfoService.getRuoli(utente));
        if (utente == null) {
            return new ResponseEntity(HttpStatus.FORBIDDEN);
        }
        if (!PasswordHashUtils.validatePassword(userLogin.password, utente.getPasswordHash())) {
            return new ResponseEntity(HttpStatus.FORBIDDEN);
        }
        DateTime currentDateTime = DateTime.now();
        String token = Jwts.builder()
                .setSubject(String.valueOf(utente.getId()))
                .claim(AuthorizationUtils.TokenClaims.SSO_LOGIN.name(), false)
                .claim(AuthorizationUtils.TokenClaims.COMPANY.name(), utente.getIdAzienda().getId())
                .setIssuedAt(currentDateTime.toDate())
                .setExpiration(tokenExpireSeconds > 0 ? currentDateTime.plusSeconds(tokenExpireSeconds).toDate() : null)
                .signWith(SIGNATURE_ALGORITHM, secretKey)
                .compact();

        utente.setPasswordHash(null);
        return new ResponseEntity(
                new LoginResponse(
                        token,
                        utente.getUsername(),
                        utente),
                HttpStatus.OK);
    }

    @RequestMapping(value = "${security.login.path}", method = RequestMethod.GET)
    public ResponseEntity<LoginResponse> loginGET(HttpServletRequest request) throws NoSuchAlgorithmException, InvalidKeySpecException, IOException, ClassNotFoundException {

        String impersonateUser = request.getParameter(IMPERSONATE_USER);
        logger.info("impersonate user: " + impersonateUser);

        //LOGIN SAML
        if (!samlEnabled) {
            return new ResponseEntity("SAML authentication not enabled", HttpStatus.UNAUTHORIZED);
        }
        logger.debug("SAML Authentication is enabled");

        String hostname = commonUtils.getHostname(request);

        ResponseEntity res;
        try {
            res = authorizationUtils.generateResponseEntityFromSAML(hostname, secretKey, request, null, impersonateUser);
        } catch (ObjectNotFoundException ex) {
            logger.error("errore nel login", ex);
            res = new ResponseEntity(HttpStatus.FORBIDDEN);
        }
        return res;
    }

    @SuppressWarnings("unused")
    public static class UserLogin {

        public String username;
        public String password;
    }

    @SuppressWarnings("unused")
    public static class LoginResponse {

        public String token;
        public String username;
        public Utente userInfo;

        public LoginResponse(final String token, final String username, Utente userInfo) {
            this.token = token;
            this.username = username;
            this.userInfo = userInfo;
        }
    }
}
