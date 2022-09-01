package it.bologna.ausl.internauta.service.scrivania.anteprima;

import java.io.Serializable;

/**
 *
 * @author gdm
 */
public class BabelDownloaderResponseBody implements Serializable {
    public static enum Status {OK, FILE_NOT_FOUND, USER_NOT_FOUND, FORBIDDEN, GENERAL_ERROR, BAD_REQUEST}
    
    private String message;
    private Status status;
    private String url;

    public BabelDownloaderResponseBody() {
    }

    public BabelDownloaderResponseBody(String message, Status status, String url) {
        this.message = message;
        this.status = status;
        this.url = url;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }
}
