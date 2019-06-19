package it.bologna.ausl.internauta.service.authorization.utils;

import java.io.Serializable;


/**
 *
 * @author gusgus
 */
public class UtenteProcton implements Serializable {
    
    private String idUtente;
    private String idStruttura;
    

    public UtenteProcton() {
    }

    public String getIdUtente() {
        return idUtente;
    }

    public void setIdUtente(String idUtente) {
        this.idUtente = idUtente;
    }

    public String getIdStruttura() {
        return idStruttura;
    }

    public void setIdStruttura(String idStruttura) {
        this.idStruttura = idStruttura;
    }
    
}
