package it.bologna.ausl.internauta.service.controllers.tip.exceptions;

/**
 *
 * @author gdm
 */
public class TipTransferException  extends Exception {
    
    public TipTransferException(String message) {
        super(message);
    }

    public TipTransferException(String message, Throwable cause) {
        super(message, cause);
    }

    public TipTransferException(Throwable cause) {
        super(cause);
    }
}
