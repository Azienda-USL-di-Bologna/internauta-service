package it.nextsw.messagedelivery.exception;

/**
 * Created by user on 27/06/2017.
 */
public class SendNextMessageException extends Exception {

    public SendNextMessageException(String message) {
        super(message);
    }

    public SendNextMessageException(String message, Throwable cause) {
        super(message, cause);
    }
}
