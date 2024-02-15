package it.bologna.ausl.internauta.service.exceptions;

/**
 *
 * @author Top
 */
public class BadParamsException extends Exception {

    public String reason = null;
    
    public BadParamsException(String message) {
        super(message);
    }
    
    public BadParamsException(String message, String reason) {
        this(message);
        this.reason = reason;
    }
    

    public BadParamsException(String message, Throwable cause) {
        super(message, cause);
    }
    
    public BadParamsException(String message, String reason, Throwable cause) {
        this(message, cause);
        this.reason = reason;
    }

    public BadParamsException(Throwable cause) {
        super(cause);
    }
    
    public BadParamsException(Throwable cause, String reason) {
        this(cause);
        this.reason = reason;
    }

    public String getReason() {
        return reason;
    }
}
