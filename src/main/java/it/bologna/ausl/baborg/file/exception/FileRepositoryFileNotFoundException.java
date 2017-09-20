package it.bologna.ausl.baborg.file.exception;

/**
 * Created by user on 27/06/2017.
 */
public class FileRepositoryFileNotFoundException extends Exception {

    public FileRepositoryFileNotFoundException(String message) {
        super(message);
    }

    public FileRepositoryFileNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }

    public FileRepositoryFileNotFoundException(Throwable cause) {
        super(cause);
    }
}
