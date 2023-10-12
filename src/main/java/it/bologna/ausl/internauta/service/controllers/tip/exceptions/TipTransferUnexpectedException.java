package it.bologna.ausl.internauta.service.controllers.tip.exceptions;

/**
 *
 * @author gdm
 */
public class TipTransferUnexpectedException  extends RuntimeException {
    
    public TipTransferUnexpectedException(String message) {
        super(message);
    }

    public TipTransferUnexpectedException(String message, Throwable cause) {
        super(message, cause);
    }

    public TipTransferUnexpectedException(Throwable cause) {
        super(cause);
    }
}
