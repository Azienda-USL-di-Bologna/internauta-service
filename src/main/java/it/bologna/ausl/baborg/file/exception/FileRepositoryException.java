package it.bologna.ausl.baborg.file.exception;

/**
 * Created by user on 27/06/2017.
 */
public class FileRepositoryException extends Exception {

    public FileRepositoryException(String message) {
        super(message);
    }

    public FileRepositoryException(String message, Throwable cause) {
        super(message, cause);
    }

    public FileRepositoryException(Throwable cause) {
        super(cause);
    }
}
