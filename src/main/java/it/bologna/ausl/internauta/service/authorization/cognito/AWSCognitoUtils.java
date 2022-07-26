package it.bologna.ausl.internauta.service.authorization.cognito;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.bologna.ausl.blackbox.exceptions.BlackBoxPermissionException;
import it.bologna.ausl.internauta.service.authorization.jwt.AuthorizationUtils;
import it.bologna.ausl.internauta.service.authorization.jwt.LoginController;
import it.bologna.ausl.internauta.service.exceptions.ObjectNotFoundException;
import it.bologna.ausl.internauta.service.exceptions.SSOException;
import java.io.IOException;
import java.net.URI;
import java.util.Base64;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

/**
 *
 * @author spritz
 */
@Component
public class AWSCognitoUtils {

    @Autowired
    private AuthorizationUtils authorizationUtils;

    @Value("${jwt.cognito.domain}")
    private String cognitoDomain;

    @Value("${jwt.cognito.client_id}")
    private String clientID;

    @Value("${jwt.cognito.redirect_uri}")
    private String redirectURI;

    @Value("${jwt.cognito.secret_id}")
    private String secretID;

    @Value("${jwt.cognito.oauth2}")
    private String urlOauthToken;

    @Autowired
    private ObjectMapper objectMapper;

    private final String GRANT_TYPE = "authorization_code";

    private static final Logger log = LoggerFactory.getLogger(AWSCognitoUtils.class);

    public ResponseEntity callCognitoUI() {
        String uri = String.format("%s/login?response_type=code&client_id=%s&redirect_uri=%s", cognitoDomain, clientID, redirectURI);
        HttpHeaders responseHeaders = new HttpHeaders();
        responseHeaders.set("Content-Type", "application/x-www-form-urlencoded");
        responseHeaders.set("redirect-url", URI.create(uri).toString());
        return ResponseEntity.status(HttpStatus.CREATED)
                .headers(responseHeaders)
                .location(URI.create(uri))
                .build();
    }

    public CognitoJWT getCognitoJWT(String cognitoCode) throws Exception {

        String clearAuth = String.format("%s:%s", clientID, secretID);
        log.info("clearAuth: " + clearAuth);
        String encodedAuth = Base64.getEncoder().encodeToString(clearAuth.getBytes());
        log.info("encodedAuth: " + encodedAuth);
        String auth = String.format("Basic %s", encodedAuth);
        log.info("auth: " + auth);

        OkHttpClient client = new OkHttpClient();
        RequestBody formBody = new FormBody.Builder()
                .add("grant_type", GRANT_TYPE)
                .add("client_id", clientID)
                .add("redirect_uri", redirectURI)
                .add("code", cognitoCode)
                .build();

        Request request = new Request.Builder()
                .addHeader("Authorization", auth)
                .addHeader("Content-Type", "application/x-www-form-urlencoded")
                .url(urlOauthToken)
                .post(formBody)
                .build();

        try {
            Response response = client.newCall(request).execute();
            log.info("cognito_response: " + response);
            CognitoJWT cognitoJWT = new CognitoJWT();

            cognitoJWT.setStatusCode(response.code());
            log.info("cognito_response_status: " + cognitoJWT.getStatusCode());
            String body = response.body().string();
            log.info(body);
            Map<String, Object> json = objectMapper.readValue(body, new TypeReference<Map<String, Object>>() {
            });

            cognitoJWT.setIdToken((String) json.get("id_token"));
            cognitoJWT.setAccessToken((String) json.get("access_token"));
            cognitoJWT.setTokenType((String) json.get("token_type"));

            log.info("token: " + cognitoJWT.getIdToken() + " access:" + cognitoJWT.getAccessToken());
            return cognitoJWT;
        } catch (Throwable e) {
            log.error("errore CognitoJWT", e);
            throw new Exception("errore CognitoJWT");
        }
    }

    public ResponseEntity<LoginController.LoginResponse> login(String azienda, String hostname, String secretKey, HttpServletRequest request, String applicazione, String cf) throws IOException, ClassNotFoundException {
        ResponseEntity res;
        try {
            log.info("azienda: " + azienda);
            log.info("hostname: " + hostname);
            res = authorizationUtils.generateResponseEntityFromSAML(azienda, hostname, secretKey, request, cf, null, applicazione, false, true, true);
        } catch (ObjectNotFoundException | BlackBoxPermissionException ex) {
            log.error("errore nel login", ex);
            res = new ResponseEntity(HttpStatus.FORBIDDEN);
        } catch (SSOException ex) {
            res = new ResponseEntity(HttpStatus.UNPROCESSABLE_ENTITY);
        }
        return res;
    }
}
