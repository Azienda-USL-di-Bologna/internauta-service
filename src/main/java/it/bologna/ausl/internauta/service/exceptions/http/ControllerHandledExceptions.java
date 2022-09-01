package it.bologna.ausl.internauta.service.exceptions.http;

import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 *
 * @author spritz
 */
public interface ControllerHandledExceptions {

    /**
     * Errore 400 - Bad request
     * @param ex eccezione 400
     * @return mappa contenente  HttpStatus, message, code dell'eccezione
     */
    @ExceptionHandler(Http400ResponseException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public default Map<String, Object> handleHttp400ResponseException(Http400ResponseException ex) {
        return ex.getResponseBody();
    }

    /**
     * Errore 401 - Unauthorized
     * @param ex eccezione 401
     * @return mappa contenente  HttpStatus, message, code dell'eccezione
     */
    @ExceptionHandler(Http401ResponseException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public default Map<String, Object> handleHttp401ResponseException(Http401ResponseException ex) {
        return ex.getResponseBody();
    }
    
    /**
     * Errore 403 - Forbidden
     * @param ex eccezione 403
     * @return mappa contenente  HttpStatus, message, code dell'eccezione
     */
    @ExceptionHandler(Http403ResponseException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public default Map<String, Object> handleHttp403ResponseException(Http403ResponseException ex) {
        return ex.getResponseBody();
    }

    /**
     * Errore 404 - Not Found
     * @param ex eccezione 404
     * @return mappa contenente  HttpStatus, message, code dell'eccezione
     */
    @ExceptionHandler(Http404ResponseException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public default Map<String, Object> handleHttp404ResponseException(Http404ResponseException ex) {
        return ex.getResponseBody();
    }

    /**
     * Errore 409 - Conflict
     * @param ex eccezione 409
     * @return mappa contenente  HttpStatus, message, code dell'eccezione
     */
    @ExceptionHandler(Http409ResponseException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public default Map<String, Object> handleHttp409ResponseException(Http409ResponseException ex) {
        return ex.getResponseBody();
    }
    
    /**
     * Errore 500 - Internal Server Error
     * @param ex eccezione 500
     * @return mappa contenente  HttpStatus, message, code dell'eccezione
     */
    @ExceptionHandler(Http500ResponseException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public default Map<String, Object> handleHttp500ResponseException(Http500ResponseException ex) {
        return ex.getResponseBody();
    }
}
