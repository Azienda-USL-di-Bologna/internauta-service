package it.bologna.ausl.internauta.service.controllers.tip.exceptions;

/**
 *
 * @author gdm
 */
public class TipImportBadDataException  extends RuntimeException {
    
    public TipImportBadDataException(String message) {
        super(message);
    }

    public TipImportBadDataException(String message, Throwable cause) {
        super(message, cause);
    }

    public TipImportBadDataException(Throwable cause) {
        super(cause);
    }
}
