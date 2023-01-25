package it.bologna.ausl.internauta.service.exceptions.sai;

/**
 *
 * @author Salo
 */
public class SottoDocumentoNotFoundException extends SAIException {

    public SottoDocumentoNotFoundException(String message) {
        super(message);
    }

    public SottoDocumentoNotFoundException(String message, Throwable t) {
        super(message, t);
    }

}
