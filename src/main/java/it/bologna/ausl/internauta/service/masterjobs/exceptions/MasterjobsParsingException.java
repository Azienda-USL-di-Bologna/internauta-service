package it.bologna.ausl.internauta.service.masterjobs.exceptions;

/**
 *
 * @author gdm
 */
public class MasterjobsParsingException extends Exception {

    public MasterjobsParsingException(String message) {
        super(message);
    }

    public MasterjobsParsingException(String message, Throwable cause) {
        super(message, cause);
    }

    public MasterjobsParsingException(Throwable cause) {
        super(cause);
    }
}
