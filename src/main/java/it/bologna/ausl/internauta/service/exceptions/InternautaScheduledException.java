package it.bologna.ausl.internauta.service.exceptions;

/**
 *
 * @author gdm
 */
public class InternautaScheduledException extends Exception {

    public InternautaScheduledException(String message) {
        super(message);
    }

    public InternautaScheduledException(String message, Throwable cause) {
        super(message, cause);
    }

    public InternautaScheduledException(Throwable cause) {
        super(cause);
    }
    
}
