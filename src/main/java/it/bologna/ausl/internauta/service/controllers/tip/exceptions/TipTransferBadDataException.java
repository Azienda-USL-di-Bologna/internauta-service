package it.bologna.ausl.internauta.service.controllers.tip.exceptions;

/**
 *
 * @author gdm
 */
public class TipTransferBadDataException  extends Exception {
    
    public TipTransferBadDataException(String message) {
        super(message);
    }

    public TipTransferBadDataException(String message, Throwable cause) {
        super(message, cause);
    }

    public TipTransferBadDataException(Throwable cause) {
        super(cause);
    }
}
