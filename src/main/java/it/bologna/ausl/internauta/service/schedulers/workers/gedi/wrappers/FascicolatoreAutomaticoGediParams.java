package it.bologna.ausl.internauta.service.schedulers.workers.gedi.wrappers;

import it.bologna.ausl.model.entities.baborg.Azienda;
import it.bologna.ausl.model.entities.baborg.Persona;
import it.bologna.ausl.model.entities.baborg.Utente;
import java.math.BigInteger;

/**
 *
 * @author Salo
 */
public class FascicolatoreAutomaticoGediParams {

    private Integer idOutbox;
    private Integer IdAzienda;
    private String cf;
    private String mittente;
    private String numerazioneGerarchica;
    private Integer idUtente;
    private Integer idPersona;

    public FascicolatoreAutomaticoGediParams() {
    }

    public FascicolatoreAutomaticoGediParams(Integer idOutbox, Integer IdAzienda, String cf, String mittente, String numerazioneGerarchica, Integer idUtente, Integer idPersona) {
        this.idOutbox = idOutbox;
        this.IdAzienda = IdAzienda;
        this.cf = cf;
        this.mittente = mittente;
        this.numerazioneGerarchica = numerazioneGerarchica;
        this.idUtente = idUtente;
        this.idPersona = idPersona;
    }

    public Integer getIdOutbox() {
        return idOutbox;
    }

    public void setIdOutbox(Integer idOutbox) {
        this.idOutbox = idOutbox;
    }

    public Integer getIdAzienda() {
        return IdAzienda;
    }

    public void setIdAzienda(Integer idAzienda) {
        this.IdAzienda = idAzienda;
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

    public Integer getUtente() {
        return idUtente;
    }

    public void setUtente(Integer idUtente) {
        this.idUtente = idUtente;
    }

    public Integer getPersona() {
        return idPersona;
    }

    public void setPersona(Integer idPersona) {
        this.idPersona = idPersona;
    }

    @Override
    public String toString() {
        return "FascicolatoreAutomaticoGediParams{" + "idOutbox=" + idOutbox + ", idAzienda=" + IdAzienda + ", cf=" + cf + ", mittente=" + mittente + ", numerazioneGerarchica=" + numerazioneGerarchica + '}';
    }

}
