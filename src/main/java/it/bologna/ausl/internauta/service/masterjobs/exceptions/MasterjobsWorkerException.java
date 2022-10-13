package it.bologna.ausl.internauta.service.masterjobs.exceptions;

/**
 *
 * @author gdm
 */
public class MasterjobsWorkerException extends Exception {

    public MasterjobsWorkerException(String message) {
        super(message);
    }

    public MasterjobsWorkerException(String message, Throwable cause) {
        super(message, cause);
    }

    public MasterjobsWorkerException(Throwable cause) {
        super(cause);
    }
}
