package it.bologna.ausl.internauta.service.exceptions.sai;

/**
 *
 * @author Salo
 */
public class FascicoloPadreNotDefinedException extends SAIException {

    public FascicoloPadreNotDefinedException(String message) {
        super(message);
    }

    public FascicoloPadreNotDefinedException(String message, Throwable t) {
        super(message, t);
    }

}
