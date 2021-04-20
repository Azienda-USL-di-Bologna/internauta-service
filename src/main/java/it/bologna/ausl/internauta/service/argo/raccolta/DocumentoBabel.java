/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.bologna.ausl.internauta.service.argo.raccolta;

/**
 *
 * @author Matteo Next
 */
public class DocumentoBabel {
    
    private String codiceRegistro;
    
    private String numeroRegistro;
    
    private Integer annoRegistro;
    
    public String getCodiceRegistro() {
        return codiceRegistro;
    }

    public void setCodiceRegistro(String codiceRegistro) {
        this.codiceRegistro = codiceRegistro;
    }

    public String getNumeroRegistro() {
        return numeroRegistro;
    }

    public void setNumeroRegistro(String numeroRegistro) {
        this.numeroRegistro = numeroRegistro;
    }

    public Integer getAnnoRegistro() {
        return annoRegistro;
    }

    public void setAnnoRegistro(Integer annoRegistro) {
        this.annoRegistro = annoRegistro;
    }
    
    public String getCodiceBabel() {
        
        return codiceRegistro + numeroRegistro + "/" + annoRegistro.toString();
    }
}
