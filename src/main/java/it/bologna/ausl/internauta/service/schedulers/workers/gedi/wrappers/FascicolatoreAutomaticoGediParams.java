/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.bologna.ausl.internauta.service.schedulers.workers.gedi.wrappers;

/**
 *
 * @author Salo
 */
public class FascicolatoreAutomaticoGediParams {

    private Integer idOutbox;
    private Integer idAzienda;
    private String cf;
    private String mittente;
    private String numerazioneGerarchica;

    public FascicolatoreAutomaticoGediParams() {
    }

    public FascicolatoreAutomaticoGediParams(Integer idOutbox, Integer idAzienda, String cf, String mittente, String numerazioneGerarchica) {
        this.idOutbox = idOutbox;
        this.idAzienda = idAzienda;
        this.cf = cf;
        this.mittente = mittente;
        this.numerazioneGerarchica = numerazioneGerarchica;
    }

    public Integer getIdOutbox() {
        return idOutbox;
    }

    public void setIdOutbox(Integer idOutbox) {
        this.idOutbox = idOutbox;
    }

    public Integer getIdAzienda() {
        return idAzienda;
    }

    public void setIdAzienda(Integer idAzienda) {
        this.idAzienda = idAzienda;
    }

    public String getCf() {
        return cf;
    }

    public void setCf(String cf) {
        this.cf = cf;
    }

    public String getMittente() {
        return mittente;
    }

    public void setMittente(String mittente) {
        this.mittente = mittente;
    }

    public String getNumerazioneGerarchica() {
        return numerazioneGerarchica;
    }

    public void setNumerazioneGerarchica(String numerazioneGerarchica) {
        this.numerazioneGerarchica = numerazioneGerarchica;
    }

    @Override
    public String toString() {
        return "FascicolatoreAutomaticoGediParams{" + "idOutbox=" + idOutbox + ", idAzienda=" + idAzienda + ", cf=" + cf + ", mittente=" + mittente + ", numerazioneGerarchica=" + numerazioneGerarchica + '}';
    }

}
