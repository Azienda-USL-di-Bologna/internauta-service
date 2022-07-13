package it.bologna.ausl.internauta.service.authorization.cognito;

import org.springframework.http.HttpStatus;

public class APICognitoRestUtils {

    public static boolean isHTTPError(HttpStatus statusCode) {
        return statusCode.is4xxClientError() || statusCode.is5xxServerError();
    }
}
