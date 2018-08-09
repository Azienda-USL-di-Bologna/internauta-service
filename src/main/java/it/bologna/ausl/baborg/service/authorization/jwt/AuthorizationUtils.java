package it.bologna.ausl.baborg.service.authorization.jwt;

import it.bologna.ausl.baborg.service.authorization.UserInfoService;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import it.bologna.ausl.baborg.model.entities.Azienda;
import it.bologna.ausl.baborg.model.entities.AziendaParametriJson;
import it.bologna.ausl.baborg.model.entities.Ruolo;
import it.bologna.ausl.baborg.model.entities.Utente;
import it.bologna.ausl.baborg.service.authorization.TokenBasedAuthentication;
import it.bologna.ausl.baborg.service.exceptions.ObjectNotFoundException;
import it.bologna.ausl.baborg.service.repositories.UtenteRepository;
import java.io.IOException;
import java.util.Date;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import org.joda.time.DateTime;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.transaction.annotation.Transactional;

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
        USER_SSO_FIELD_VALUE
    }

    private final SignatureAlgorithm SIGNATURE_ALGORITHM = SignatureAlgorithm.HS256;

    @Autowired
    UtenteRepository utenteRepository;

    @Autowired
    UserInfoService userInfoService;

    @Autowired
    ObjectMapper objectMapper;
    
    @Value("${jwt.expires-seconds}")
    private Integer tokenExpireSeconds;
    

    private static final Logger logger = LoggerFactory.getLogger(AuthorizationUtils.class);

    /**
     * inserisce nel securityContext l'utente inserito nel token al momento del
     * login
     *
     * @param token il token
     * @param secretKey la chiave segreta per decifrare il token
     * @return i claims del token
     * @throws java.lang.ClassNotFoundException
     */
    @Transactional(rollbackFor = {Error.class})
    public Claims setInSecurityContext(String token, String secretKey) throws ClassNotFoundException {
        Claims claims = Jwts.parser().
                setSigningKey(secretKey).
                parseClaimsJws(token).
                getBody();

        Integer idUtente = Integer.parseInt(claims.getSubject());
        Utente user = userInfoService.loadUtente(idUtente);
        user.setRuoli(userInfoService.getRuoli(user));
        TokenBasedAuthentication authentication = new TokenBasedAuthentication(user);
        authentication.setToken(token);
        SecurityContextHolder.getContext().setAuthentication(authentication);
        return claims;
    }

    public ResponseEntity generateResponseEntityFromSAML(String path, String secretKey, HttpServletRequest request) throws IOException, ClassNotFoundException, ObjectNotFoundException {

        Azienda azienda = userInfoService.loadAziendaByPath(path);

        AziendaParametriJson aziendaParams = AziendaParametriJson.parse(objectMapper, azienda.getParametri());

        String ssoFieldValue = request.getAttribute(aziendaParams.getLoginSSOField()).toString();
        
        String[] loginDbFieldSplitted = aziendaParams.getLoginDBFieldBaborg().split("/");
        String entityClassName = loginDbFieldSplitted[0];
        String field = loginDbFieldSplitted[1];

        Class<?> entityClass = Class.forName(entityClassName);

        Utente user = userInfoService.loadUtente(entityClass, field, ssoFieldValue, azienda);
        user.setRuoli(userInfoService.getRuoli(user));
        if (user == null) {
            throw new ObjectNotFoundException("User not found");
        }

        user.setPasswordHash(null);

        DateTime currentDateTime = DateTime.now();
        
        return new ResponseEntity(
                new LoginController.LoginResponse(
                        Jwts.builder()
                                .setSubject(String.valueOf(user.getId()))
                                .claim(AuthorizationUtils.TokenClaims.COMPANY.name(), String.valueOf(azienda.getId()))
                                .claim(AuthorizationUtils.TokenClaims.SSO_LOGIN.name(), true)
                                .claim(AuthorizationUtils.TokenClaims.USER_ENTITY_CLASS.name(), entityClass)
                                .claim(AuthorizationUtils.TokenClaims.USER_FIELD.name(), field)
                                .claim(AuthorizationUtils.TokenClaims.USER_SSO_FIELD_VALUE.name(), ssoFieldValue)
                                .setIssuedAt(currentDateTime.toDate())
                                .setExpiration(currentDateTime.plusSeconds(tokenExpireSeconds).toDate())
                                .signWith(SIGNATURE_ALGORITHM, secretKey).compact(),
                        user.getUsername(),
                        user),
                HttpStatus.OK);
    }

    public ResponseEntity generateResponseEntityFromUsername() {
        return null;
    }
}
