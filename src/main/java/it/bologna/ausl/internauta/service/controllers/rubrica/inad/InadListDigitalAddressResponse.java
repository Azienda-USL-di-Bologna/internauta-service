package it.bologna.ausl.internauta.service.controllers.rubrica.inad;

import java.time.ZonedDateTime;

/**
 *
 * @author Top
 * {
 *  "state": "PRESA_IN_CARICO",
 *  "message": "Richiesta elenco presa in carico",
 *  "id": "f8c3de3d-1fea-4d7c-a8b0-29f63c4c3454",
 *  "dateTimeRequest": "2017-07-21T17:32:28Z"
 * }
 */
public class InadListDigitalAddressResponse {
    
    public static enum StatoRichiestaListaDomiciliDigitali {
        PRESA_IN_CARICO,
        IN_ELABORAZIONE,
        DISPONIBILE
    }
    
    private StatoRichiestaListaDomiciliDigitali state;
    private String message;
    private String id;
    private ZonedDateTime dateTimeRequest;
    
    

    public StatoRichiestaListaDomiciliDigitali getStato() {
        return state;
    }

    public void setStato(StatoRichiestaListaDomiciliDigitali state) {
        this.state = state;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public ZonedDateTime getDateTimeRequest() {
        return dateTimeRequest;
    }

    public void setDateTimeRequest(ZonedDateTime dateTimeRequest) {
        this.dateTimeRequest = dateTimeRequest;
    }
    
    
    
}
