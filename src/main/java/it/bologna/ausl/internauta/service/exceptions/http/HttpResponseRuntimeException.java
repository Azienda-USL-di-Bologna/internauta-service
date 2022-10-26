package it.bologna.ausl.internauta.service.exceptions.http;

/**
 *
 * @author gusgus
 */
public class HttpResponseRuntimeException extends RuntimeException {
    
    private HttpInternautaResponseException httpInternautaResponseException;

    public HttpResponseRuntimeException(HttpInternautaResponseException httpInternautaResponseException) {
        super();
        this.httpInternautaResponseException = httpInternautaResponseException;
    }

    public HttpInternautaResponseException getHttpInternautaResponseException() {
        return httpInternautaResponseException;
    }
}
