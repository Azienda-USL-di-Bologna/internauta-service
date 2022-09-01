package it.bologna.ausl.internauta.service.bridges.albo.exceptions;

/**
 *
 * @author gdm
 */
public class AlboBridgeException extends Exception {

     public AlboBridgeException(String message) {
        super(message);
    }

    public AlboBridgeException(String message, Throwable cause) {
        super(message, cause);
    }

    public AlboBridgeException(Throwable cause) {
        super(cause);
    }
}
