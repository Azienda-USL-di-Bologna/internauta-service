package it.nextsw.file.exception;

/**
 * Created by user on 27/06/2017.
 */
public class NextFileException extends Exception {

    public NextFileException(String message) {
        super(message);
    }

    public NextFileException(String message, Throwable cause) {
        super(message, cause);
    }

    public NextFileException(Throwable cause) {
        super(cause);
    }
}
