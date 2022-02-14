package it.bologna.ausl.internauta.service.exceptions.scripta;


/**
 *
 * @author gusgus
 */
public class AllegatoException extends Exception {
    
    public AllegatoException(String message, Throwable cause) {
        super(message, cause);
    }

    public AllegatoException(String message) {
        super(message);
    }

    public AllegatoException(Throwable cause) {
        super(cause);
    }
}