package it.bologna.ausl.internauta.service.exceptions;

/**
 *
 * @author mdormdor
 */
public class GruppiException extends Exception {

    public GruppiException(String message) {
        super(message);
    }

    public GruppiException(String message, Throwable cause) {
        super(message, cause);
    }

    public GruppiException(Throwable cause) {
        super(cause);
    }
    
}
