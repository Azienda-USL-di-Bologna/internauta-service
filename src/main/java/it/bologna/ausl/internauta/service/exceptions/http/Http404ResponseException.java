package it.bologna.ausl.internauta.service.exceptions.http;

import org.springframework.http.HttpStatus;

/**
 *
 * @author spritz
 */
public class Http404ResponseException extends HttpInternautaResponseException {

    public Http404ResponseException(String code, String message, Throwable cause) {
        super(HttpStatus.NOT_FOUND, code, message, cause);
    }

    public Http404ResponseException(String code, String message) {
        super(HttpStatus.NOT_FOUND, code, message);
    }

    public Http404ResponseException(String code, Throwable cause) {
        super(HttpStatus.NOT_FOUND, code, cause);
    }
}
