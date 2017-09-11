package it.nextsw.file.exception;

/**
 * Created by f.longhitano on 27/08/2017.
 */
public class FileRepositoryConnectionException extends Exception {

    public FileRepositoryConnectionException(String message) {
        super(message);
    }

    public FileRepositoryConnectionException(String message, Throwable cause) {
        super(message, cause);
    }
}
