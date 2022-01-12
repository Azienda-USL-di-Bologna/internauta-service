package it.bologna.ausl.internauta.service.exceptions.sai;

/**
 *
 * @author Salo
 */
public class TooManyObjectsException extends SAIException {

    public TooManyObjectsException(String message) {
        super(message);
    }

    public TooManyObjectsException(String message, Throwable t) {
        super(message, t);
    }

}
