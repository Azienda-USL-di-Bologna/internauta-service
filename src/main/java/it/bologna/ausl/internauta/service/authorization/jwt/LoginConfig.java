package it.bologna.ausl.internauta.service.authorization.jwt;

import com.fasterxml.jackson.core.type.TypeReference;
import it.bologna.ausl.internauta.utils.parameters.manager.ParametriAziendeReader;
import it.bologna.ausl.model.entities.configurazione.ParametroAziende;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 *
 * @author gdm
 */
@Component
public class LoginConfig {

    private static final Logger log = LoggerFactory.getLogger(LoginConfig.class);
    
    public enum ConfigSource {
        APPLICATION_PROPERTIES, PARAMETRI_AZIENDE
    }
    
    @Value("${jwt.saml.config-source:APPLICATION_PROPERTIES}")
    private ConfigSource configSource;
    
    @Value("${jwt.saml.enabled:false}")
    private boolean samlEnabled;
    
    @Value("${jwt.saml.company-identification-field:companyName}")
    private String samlCompanyIdentificationField;
    
    @Value("${jwt.secret:QUdvMWJHUEJZem5CTFBQalYxNmEzb3FWRkZPM3hqbXV5Y25TMHBTZXZaQk5ScHVJUFhMNzAwQllCY2dYNnJpaw==}")
    private String jwtSecret;
    
    @Value("${jwt.cookie:AUTH-TOKEN}")
    private String jwtCookie;
    
    @Value("${jwt.header:authorization}")
    private String jwtHeader;
    
    @Value("${jwt.expires-seconds:432000}")
    private Integer jwtExpiresSeconds;
    
    @Value("${jwt.passtoken-expires-seconds:60}")
    private Integer passtokenExpiresSeconds;
    
    @Value("${jwt.cognito.enabled:false}")
    private boolean cognitoEnabled;
    
    @Value("${jwt.cognito.domain:https://region.amazoncognito.com}")
    private String cognitoDomain;

    @Value("${jwt.cognito.client_id:2clientID}")
    private String cognitoClientID;

    @Value("${jwt.cognito.redirect_uri:https://redirect_url}")
    private String cognitoRedirectURI;

    @Value("${jwt.cognito.secret_id:12345678}")
    private String cognitoSecretID;

    @Value("${jwt.cognito.oauth2:https://region.amazoncognito.com/oauth2/token}")
    private String cognitoUrlOauthToken;
    
    @Autowired
    private ParametriAziendeReader parametriAziendeReader;
    
    /**
     * di fatto non fa nulla
     */
    public void readConfig() {
        readConfig(null) ;
    }
    
    /**
     * richiamare questo metodo per leggere i la configurazione del login di un azienda.
     * Ha effetto solo nel caso in cui nell'application.properties è impostato il parametro jwt.saml.config-source a PARAMETRI_AZIENDE
     * @param idAzienda 
     */
    public void readConfig(Integer idAzienda) {
        if (idAzienda != null && configSource == ConfigSource.PARAMETRI_AZIENDE) {
            List<ParametroAziende> parameters = parametriAziendeReader.getParameters(ParametriAziendeReader.ParametriAzienda.loginConfig, new Integer[]{idAzienda});
            if (parameters != null && !parameters.isEmpty())  {
                Map<String, Object> loginConfig = parametriAziendeReader.getValue(parameters.get(0), new TypeReference<Map<String, Object>>(){});
                if (loginConfig != null && !loginConfig.isEmpty()) {
                    try {
                        samlEnabled = (boolean) loginConfig.get("jwt.saml.enabled");
                    } catch (Exception e) {
                        log.warn(String.format("parametro \"%s\" non trovato, verrà usato quello dell'application.properties", "jwt.saml.enabled"));
                    }
                    try {
                        samlCompanyIdentificationField = (String) loginConfig.get("jwt.saml.company-identification-field");
                        if (!StringUtils.hasText(samlCompanyIdentificationField)) {
                            throw new LoginConfigException("parametro vuoto o nullo");
                        }
                    } catch (Exception e) {
                        log.warn(String.format("parametro \"%s\" non trovato, verrà usato quello dell'application.properties", "jwt.saml.company-identification-field"));
                    }
                    try {
                        jwtSecret = (String) loginConfig.get("jwt.secret");
                        if (!StringUtils.hasText(jwtSecret)) {
                            throw new LoginConfigException("parametro vuoto o nullo");
                        }
                    } catch (Exception e) {
                        log.warn(String.format("parametro \"%s\" non trovato, verrà usato quello dell'application.properties", "jwt.secret"));
                    }
                    try {
                        jwtCookie = (String) loginConfig.get("jwt.cookie");
                        if (!StringUtils.hasText(jwtCookie)) {
                            throw new LoginConfigException("parametro vuoto o nullo");
                        }
                    } catch (Exception e) {
                        log.warn(String.format("parametro \"%s\" non trovato, verrà usato quello dell'application.properties", "jwt.cookie"));
                    }
                    try {
                        jwtHeader = (String) loginConfig.get("jwt.header");
                        if (!StringUtils.hasText(jwtHeader)) {
                            throw new LoginConfigException("parametro vuoto o nullo");
                        }
                    } catch (Exception e) {
                        log.warn(String.format("parametro \"%s\" non trovato, verrà usato quello dell'application.properties", "jwt.header"));
                    }
                    try {
                        jwtExpiresSeconds = (Integer) loginConfig.get("jwt.expires-seconds");
                        if (jwtExpiresSeconds == null) {
                            throw new LoginConfigException("parametro vuoto o nullo");
                        }
                    } catch (Exception e) {
                        log.warn(String.format("parametro \"%s\" non trovato, verrà usato quello dell'application.properties", "jwt.expires-seconds"));
                    }
                    try {
                        passtokenExpiresSeconds =(Integer) loginConfig.get("jwt.passtoken-expires-seconds");
                        if (passtokenExpiresSeconds == null) {
                            throw new LoginConfigException("parametro vuoto o nullo");
                        }
                    } catch (Exception e) {
                        log.warn(String.format("parametro \"%s\" non trovato, verrà usato quello dell'application.properties", "jwt.passtoken-expires-seconds"));
                    }
                    try {
                        cognitoEnabled = (boolean) loginConfig.get("jwt.cognito.enabled");
                    } catch (Exception e) {
                        log.warn(String.format("parametro \"%s\" non trovato, verrà usato quello dell'application.properties", "jwt.cognito.enabled"));
                    }
                    try {
                        cognitoDomain = (String) loginConfig.get("jwt.cognito.domain");
                        if (!StringUtils.hasText(cognitoDomain)) {
                            throw new LoginConfigException("parametro vuoto o nullo");
                        }
                    } catch (Exception e) {
                        log.warn(String.format("parametro \"%s\" non trovato, verrà usato quello dell'application.properties", "jwt.cognito.domain"));
                    }
                    try {
                        cognitoClientID = (String) loginConfig.get("jwt.cognito.client_id");
                        if (!StringUtils.hasText(cognitoClientID)) {
                            throw new LoginConfigException("parametro vuoto o nullo");
                        }
                    } catch (Exception e) {
                        log.warn(String.format("parametro \"%s\" non trovato, verrà usato quello dell'application.properties", "jwt.cognito.client_id"));
                    }
                    try {
                        cognitoRedirectURI = (String) loginConfig.get("jwt.cognito.redirect_uri");
                        if (!StringUtils.hasText(cognitoRedirectURI)) {
                            throw new LoginConfigException("parametro vuoto o nullo");
                        }
                    } catch (Exception e) {
                        log.warn(String.format("parametro \"%s\" non trovato, verrà usato quello dell'application.properties", "jwt.cognito.redirect_uri"));
                    }
                    try {
                        cognitoSecretID = (String) loginConfig.get("jwt.cognito.secret_id");
                        if (!StringUtils.hasText(cognitoSecretID)) {
                            throw new LoginConfigException("parametro vuoto o nullo");
                        }
                    } catch (Exception e) {
                        log.warn(String.format("parametro \"%s\" non trovato, verrà usato quello dell'application.properties", "jwt.cognito.secret_id"));
                    }
                    try {
                        cognitoUrlOauthToken =  (String) loginConfig.get("jwt.cognito.oauth2");
                        if (!StringUtils.hasText(cognitoUrlOauthToken)) {
                            throw new LoginConfigException("parametro vuoto o nullo");
                        }
                    } catch (Exception e) {
                        log.warn(String.format("parametro \"%s\" non trovato, verrà usato quello dell'application.properties", "jwt.cognito.oauth2"));
                    }
                } else {
                    log.warn(String.format(
                            "è stato indicato di usare i loginParams da PARAMETRI_AZIENDE, ma il parametro non è stato trovato o è vuoto per l'azienda %s. Saranno usati quelli dell'application.properties", 
                            idAzienda));
                }
            }
        }
    }

    public ConfigSource getConfigSource() {
        return configSource;
    }

    public boolean isSamlEnabled() {
        return samlEnabled;
    }

    public String getSamlCompanyIdentificationField() {
        return samlCompanyIdentificationField;
    }

    public String getJwtSecret() {
        return jwtSecret;
    }

    public String getJwtCookie() {
        return jwtCookie;
    }

    public String getJwtHeader() {
        return jwtHeader;
    }

    public Integer getJwtExpiresSeconds() {
        return jwtExpiresSeconds;
    }

    public Integer getPasstokenExpiresSeconds() {
        return passtokenExpiresSeconds;
    }

    public boolean isCognitoEnabled() {
        return cognitoEnabled;
    }

    public String getCognitoDomain() {
        return cognitoDomain;
    }

    public String getCognitoClientID() {
        return cognitoClientID;
    }

    public String getCognitoRedirectURI() {
        return cognitoRedirectURI;
    }

    public String getCognitoSecretID() {
        return cognitoSecretID;
    }

    public String getCognitoUrlOauthToken() {
        return cognitoUrlOauthToken;
    }

    public ParametriAziendeReader getParametriAziendeReader() {
        return parametriAziendeReader;
    }
    
    public class LoginConfigException extends Exception {

        public LoginConfigException() {
        }
        
        public LoginConfigException(String string) {
            super(string);
        }

        public LoginConfigException(String string, Throwable thrwbl) {
            super(string, thrwbl);
        }

        public LoginConfigException(Throwable thrwbl) {
            super(thrwbl);
        }
        
    }
}
