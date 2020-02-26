package it.bologna.ausl.internauta.service.exceptions;

/**
 *
 * @author gdm
 */
public class SSOException extends Exception {

    public SSOException(String message) {
        super(message);
    }

    public SSOException(String message, Throwable cause) {
        super(message, cause);
    }

    public SSOException(Throwable cause) {
        super(cause);
    }
}