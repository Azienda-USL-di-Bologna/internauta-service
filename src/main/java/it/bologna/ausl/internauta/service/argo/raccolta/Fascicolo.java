/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.bologna.ausl.internauta.service.argo.raccolta;

import java.util.Objects;

/**
 *
 * @author Matteo Next
 */
public class Fascicolo {
    
    private String numerazioneGerarchica;

    public String getNumerazioneGerarchica() {
        return numerazioneGerarchica;
    }

    public void setNumerazioneGerarchica(String numerazioneGerarchica) {
        this.numerazioneGerarchica = numerazioneGerarchica;
    }
    
    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Fascicolo) {
            return (((Fascicolo) obj).getNumerazioneGerarchica() == null ? this.numerazioneGerarchica == null : ((Fascicolo) obj).getNumerazioneGerarchica().equals(this.numerazioneGerarchica));
        }
        return false;
    }
    
}
