package it.bologna.ausl.internauta.service.controllers.rubrica.inad;

import java.time.ZonedDateTime;
import java.util.List;

/**
 *
 * @author MicheleD'Onza
 * classe che mi gestisce le risposte dell'inad
 * 
 * {
        "codiceFiscale": "RRANGL74M28R701V",
        "since": "2017-07-21T17:32:28Z",
        "digitalAddress": [
            {
                "digitalAddress": "example@pec.it",
                "practicedProfession": "Avvocato",
                "usageInfo": {
                    "motivation": "CESSAZIONE_VOLONTARIA",
                    "dateEndValidity": "2017-07-21T17:32:28Z"
                }
            }
        ]
    }
 */
public class InadExtractResponse {
    
    private String codiceFiscale;
    private ZonedDateTime since;
    private List<DigitalAddress> digitalAddresses;

    public String getCodiceFiscale() {
        return codiceFiscale;
    }

    public void setCodiceFiscale(String codiceFiscale) {
        this.codiceFiscale = codiceFiscale;
    }

    public ZonedDateTime getSince() {
        return since;
    }

    public void setSince(ZonedDateTime since) {
        this.since = since;
    }

    public List<DigitalAddress> getDigitalAddresses() {
        return digitalAddresses;
    }

    public void setDigitalAddresses(List<DigitalAddress> digitalAddresses) {
        this.digitalAddresses = digitalAddresses;
    }    
    
}
