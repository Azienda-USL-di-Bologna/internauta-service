package it.bologna.ausl.internauta.service.authorization.cognito;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
public class CognitoException extends Exception {

    private HttpStatus httpStatus;

    public CognitoException() {
    }

    public CognitoException(HttpStatus httpStatus, String errorReason, String message) {
        super(errorReason + message);
    }
}
