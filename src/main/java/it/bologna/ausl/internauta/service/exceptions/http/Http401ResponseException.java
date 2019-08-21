package it.bologna.ausl.internauta.service.exceptions.http;

import org.springframework.http.HttpStatus;

/**
 *
 * @author spritz
 */
public class Http401ResponseException extends HttpInternautaResponseException {

    public Http401ResponseException(String code, String message, Throwable cause) {
        super(HttpStatus.UNAUTHORIZED, code, message, cause);
    }

    public Http401ResponseException(String code, String message) {
        super(HttpStatus.UNAUTHORIZED, code, message);
    }

    public Http401ResponseException(String code, Throwable cause) {
        super(HttpStatus.UNAUTHORIZED, code, cause);
    }
}
