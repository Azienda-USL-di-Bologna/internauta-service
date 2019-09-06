package it.bologna.ausl.internauta.service.exceptions.intimus;

/**
 *
 * @author gdm
 */
public class IntimusSendCommandException extends Exception {

    public IntimusSendCommandException(String message) {
        super(message);
    }

    public IntimusSendCommandException(String message, Throwable cause) {
        super(message, cause);
    }

    public IntimusSendCommandException(Throwable cause) {
        super(cause);
    }
}