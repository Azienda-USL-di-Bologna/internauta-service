package it.bologna.ausl.internauta.service.exceptions;

/**
 *
 * @author Giuseppe Russo <g.russo@dilaxia.com>
 */
public class DownloaderUtilsException extends Exception {
     public DownloaderUtilsException(String message) {
        super(message);
    }

    public DownloaderUtilsException(String message, Throwable cause) {
        super(message, cause);
    }

    public DownloaderUtilsException(Throwable cause) {
        super(cause);
    }
}
