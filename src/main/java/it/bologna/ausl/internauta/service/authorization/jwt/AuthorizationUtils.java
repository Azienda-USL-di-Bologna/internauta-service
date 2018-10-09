package it.bologna.ausl.internauta.service.authorization.jwt;

import it.bologna.ausl.internauta.service.authorization.UserInfoService;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import it.bologna.ausl.model.entities.baborg.Utente;
import it.bologna.ausl.internauta.service.authorization.TokenBasedAuthentication;
import it.bologna.ausl.internauta.service.exceptions.ObjectNotFoundException;
import it.bologna.ausl.internauta.service.repositories.baborg.UtenteRepository;
import it.bologna.ausl.internauta.service.repositories.scrivania.CounterRepository;
import it.bologna.ausl.model.entities.baborg.Azienda;
import it.bologna.ausl.model.entities.baborg.AziendaParametriJson;
import it.bologna.ausl.model.entities.baborg.Ruolo;
import it.bologna.ausl.model.entities.baborg.projections.CustomUtenteWithIdPersonaAndIdAzienda;
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
        REAL_USER,
        ID_SESSION_LOG
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

    @Autowired
    CounterRepository counterRepository;

    @Autowired
    ProjectionFactory factory;

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
        Integer idSessionLog = Integer.parseInt((String) claims.get(AuthorizationUtils.TokenClaims.ID_SESSION_LOG.name()));
        Utente user = userInfoService.loadUtente(idUtente);
        user.setRuoli(userInfoService.getRuoli(user));
        TokenBasedAuthentication authentication = new TokenBasedAuthentication(user);
        authentication.setToken(token);
        authentication.setIdSessionLog(idSessionLog);
        SecurityContextHolder.getContext().setAuthentication(authentication);
        return claims;
    }

    public ResponseEntity generateResponseEntityFromSAML(String path, String secretKey, HttpServletRequest request, String ssoFieldValue, String utenteImpersonatoStr) throws IOException, ClassNotFoundException, ObjectNotFoundException {

        Utente impersonatedUser = null;
        boolean isSuperDemiurgo = false;

        Azienda azienda = userInfoService.loadAziendaByPath(path);

        AziendaParametriJson aziendaParams = AziendaParametriJson.parse(objectMapper, azienda.getParametri());

        if (ssoFieldValue == null) {
            ssoFieldValue = request.getAttribute(aziendaParams.getLoginSSOField()).toString();
        }

        String[] loginDbFieldSplitted = aziendaParams.getLoginDBFieldBaborg().split("/");
        String entityClassName = loginDbFieldSplitted[0];
        String field = loginDbFieldSplitted[1];

        Class<?> entityClass = Class.forName(entityClassName);

        // carica l'utente vero che si è loggato con SSO
        Utente user = userInfoService.loadUtente(entityClass, field, ssoFieldValue, azienda);

        if (user == null) {
            throw new ObjectNotFoundException("User not found");
        }

        // prendi ID dell'utente reale
        String realUserSubject = String.valueOf(user.getId());
        // rimuovi l'informazione sulla password
        user.setPasswordHash(null);

        user.setRuoli(userInfoService.getRuoli(user));

        if (user == null) {
            throw new ObjectNotFoundException("User not found");
        }

        user.setPasswordHash(null);
        CustomUtenteWithIdPersonaAndIdAzienda userLoginSSOWithPersonaAndAzienda = factory.createProjection(CustomUtenteWithIdPersonaAndIdAzienda.class, user);

        // controlla se è stato passato il parametro di utente impersonato
        if (utenteImpersonatoStr != null && !utenteImpersonatoStr.equals("")) {
            // solo se l'utente reale è super demiurgo allora può fare il cambia utente
            List<Ruolo> ruoli = user.getRuoli();

            for (Ruolo ruolo : ruoli) {
                Ruolo.CodiciRuolo codiceRuolo = (Ruolo.CodiciRuolo) ruolo.getNomeBreve();
                if (codiceRuolo == Ruolo.CodiciRuolo.SD) {
                    isSuperDemiurgo = true;
                    break;
                }
            }

            if (isSuperDemiurgo) {
                logger.info(String.format("utente %s ha ruolo SD", realUserSubject));
                impersonatedUser = userInfoService.loadUtente(entityClass, field, utenteImpersonatoStr, azienda);
                impersonatedUser.setPasswordHash(null);

                CustomUtenteWithIdPersonaAndIdAzienda impersonatedUserWithPersonaAndAzienda = factory.createProjection(CustomUtenteWithIdPersonaAndIdAzienda.class, impersonatedUser);

                String impersonateUserSubject = String.valueOf(impersonatedUser.getId());

                // se utente reale = utente impersonato allora non si fa il cambia utente
                if (realUserSubject.equals(impersonateUserSubject)) {
                    logger.info(String.format("utente reale %s == utente impersonato %s", realUserSubject, impersonateUserSubject));
                    user = null;
                }

                // ritorna utente impersonato con informazioni dell'utente reale
                return new ResponseEntity(
                        generateLoginResponse(impersonatedUserWithPersonaAndAzienda, userLoginSSOWithPersonaAndAzienda, azienda, entityClass, field, utenteImpersonatoStr, secretKey),
                        HttpStatus.OK);
            } else {
                // ritorna l'utente stesso perchè non ha i permessi per fare il cambia utente
                logger.info(String.format("utente %s non ha ruolo SD, ritorna se stesso nel token", realUserSubject));
                return new ResponseEntity(
                        generateLoginResponse(userLoginSSOWithPersonaAndAzienda, null, azienda, entityClass, field, ssoFieldValue, secretKey),
                        HttpStatus.OK);
            }

        } else {
            // ritorna l'utente reale perchè non è stato passato l'utente impersonato
            return new ResponseEntity(
                    generateLoginResponse(userLoginSSOWithPersonaAndAzienda, null, azienda, entityClass, field, ssoFieldValue, secretKey),
                    HttpStatus.OK);
        }
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

    public ResponseEntity generateResponseEntityFromUsername() {
        return null;
    }

    private LoginController.LoginResponse generateLoginResponse(
            CustomUtenteWithIdPersonaAndIdAzienda currentUser,
            CustomUtenteWithIdPersonaAndIdAzienda realUser,
            Azienda azienda,
            Class<?> entityClass,
            String field,
            String ssoFieldValue,
            String secretKey) {
        DateTime currentDateTime = DateTime.now();

        String realUserStr = null;

        if (realUser != null) {
            realUserStr = String.valueOf(realUser.getId());
        } else {
            realUserStr = String.valueOf(currentUser.getId());
        }

        return new LoginController.LoginResponse(
                Jwts.builder()
                        .setSubject(String.valueOf(currentUser.getId()))
                        .claim(AuthorizationUtils.TokenClaims.COMPANY.name(), String.valueOf(azienda.getId()))
                        .claim(AuthorizationUtils.TokenClaims.SSO_LOGIN.name(), true)
                        .claim(AuthorizationUtils.TokenClaims.USER_ENTITY_CLASS.name(), entityClass)
                        .claim(AuthorizationUtils.TokenClaims.USER_FIELD.name(), field)
                        .claim(AuthorizationUtils.TokenClaims.USER_SSO_FIELD_VALUE.name(), ssoFieldValue)
                        .claim(AuthorizationUtils.TokenClaims.ID_SESSION_LOG.name(), String.valueOf(createIdSessionLog().getId()))
                        .claim(AuthorizationUtils.TokenClaims.REAL_USER.name(), realUserStr)
                        .setIssuedAt(currentDateTime.toDate())
                        .setExpiration(tokenExpireSeconds > 0 ? currentDateTime.plusSeconds(tokenExpireSeconds).toDate() : null)
                        .signWith(SIGNATURE_ALGORITHM, secretKey).compact(),
                currentUser.getUsername(),
                currentUser);
    }

    public Counter createIdSessionLog() {
        Counter counter = new Counter();
        counter.setOggetto("generate_after_login");

        return counterRepository.save(counter);

    }
}
