package it.bologna.ausl.internauta.service.exceptions;

/**
 *
 * @author gusgus
 */
public class InternautaRuntimeExceptionContainer extends RuntimeException {
    
    private Exception exception;

    public InternautaRuntimeExceptionContainer(Exception exception) {
        super();
        this.exception = exception;
    }

    public Exception getException() {
        return exception;
    }
}
