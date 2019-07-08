package it.bologna.ausl.internauta.service.exceptions;

/**
 *
 * @author Top
 */
public class BadParamsException extends Exception {

    public BadParamsException(String message) {
        super(message);
    }

    public BadParamsException(String message, Throwable cause) {
        super(message, cause);
    }

    public BadParamsException(Throwable cause) {
        super(cause);
    }
    
}
