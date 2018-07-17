package it.bologna.ausl.baborg.service.exceptions;

/**
 *
 * @author spritz
 */
public class ObjectNotFoundException extends Exception {

    public ObjectNotFoundException(String message) {
        super(message);
    }

    public ObjectNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }

    public ObjectNotFoundException(Throwable cause) {
        super(cause);
    }
}
