package it.bologna.ausl.internauta.service.authorization.jwt.exceptions;

/**
 *
 * @author spritz
 */
public class BaborgAuthenticationException extends Exception {

    public BaborgAuthenticationException(String message) {
        super(message);
    }

    public BaborgAuthenticationException(String message, Throwable cause) {
        super(message, cause);
    }

    public BaborgAuthenticationException(Throwable cause) {
        super(cause);
    }
}
