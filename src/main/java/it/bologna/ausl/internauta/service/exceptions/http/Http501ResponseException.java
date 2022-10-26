package it.bologna.ausl.internauta.service.exceptions.http;

import org.springframework.http.HttpStatus;

/**
 *
 * @author gusgus
 */
public class Http501ResponseException extends HttpInternautaResponseException {

    public Http501ResponseException(String code, String message, Throwable cause) {
        super(HttpStatus.NOT_IMPLEMENTED, code, message, cause);
    }

    public Http501ResponseException(String code, String message) {
        super(HttpStatus.NOT_IMPLEMENTED, code, message);
    }

    public Http501ResponseException(String code, Throwable cause) {
        super(HttpStatus.NOT_IMPLEMENTED, code, cause);
    }
}
