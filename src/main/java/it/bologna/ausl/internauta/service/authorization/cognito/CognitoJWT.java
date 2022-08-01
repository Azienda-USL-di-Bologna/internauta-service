package it.bologna.ausl.internauta.service.authorization.cognito;

import org.springframework.stereotype.Component;

/**
 *
 * @author andrae
 */
@Component
public class CognitoJWT {

    private Integer statusCode;
    private String accessToken;
    private String idToken;
    private String tokenType;
    private Integer expiresIn;

    public CognitoJWT() {
    }

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public String getIdToken() {
        return idToken;
    }

    public void setIdToken(String idToken) {
        this.idToken = idToken;
    }

    public String getTokenType() {
        return tokenType;
    }

    public void setTokenType(String tokenType) {
        this.tokenType = tokenType;
    }

    public Integer getExpiresIn() {
        return expiresIn;
    }

    public void setExpiresIn(Integer expiresIn) {
        this.expiresIn = expiresIn;
    }

    public Integer getStatusCode() {
        return statusCode;
    }

    public void setStatusCode(Integer statusCode) {
        this.statusCode = statusCode;
    }

}
