package it.bologna.ausl.internauta.service.exceptions;

/**
 *
 * @author gusgus
 */
public class LambdaUncheckedException extends RuntimeException {

    public LambdaUncheckedException(String message) {
        super(message);
    }

    public LambdaUncheckedException(String message, Throwable cause) {
        super(message, cause);
    }

    public LambdaUncheckedException(Throwable cause) {
        super(cause);
    }
    
}
