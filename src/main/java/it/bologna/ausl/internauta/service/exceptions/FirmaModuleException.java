package it.bologna.ausl.internauta.service.exceptions;

/**
 *
 * @author chiara
 */
public class FirmaModuleException extends Exception {

    public FirmaModuleException(String message) {
        super(message);
    }

    public FirmaModuleException(String message, Throwable cause) {
        super(message, cause);
    }

    public FirmaModuleException(Throwable cause) {
        super(cause);
    }
    
}
