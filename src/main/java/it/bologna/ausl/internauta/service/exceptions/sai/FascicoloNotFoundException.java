package it.bologna.ausl.internauta.service.exceptions.sai;

/**
 *
 * @author Salo
 */
public class FascicoloNotFoundException extends SAIException {

    public FascicoloNotFoundException(String message) {
        super(message);
    }

    public FascicoloNotFoundException(String message, Throwable t) {
        super(message, t);
    }

}
