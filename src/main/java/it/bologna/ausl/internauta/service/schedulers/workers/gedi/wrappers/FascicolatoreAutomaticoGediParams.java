package it.bologna.ausl.internauta.service.schedulers.workers.gedi.wrappers;

import it.bologna.ausl.model.entities.baborg.Azienda;
import it.bologna.ausl.model.entities.baborg.Persona;
import it.bologna.ausl.model.entities.baborg.Utente;

/**
 *
 * @author Salo
 */
public class FascicolatoreAutomaticoGediParams {

    private Integer idOutbox;
    private Azienda azienda;
    private String cf;
    private String mittente;
    private String numerazioneGerarchica;
    private Utente utente;
    private Persona persona;

    public FascicolatoreAutomaticoGediParams() {
    }

    public FascicolatoreAutomaticoGediParams(Integer idOutbox, Azienda azienda, String cf, String mittente, String numerazioneGerarchica, Utente utente, Persona persona) {
        this.idOutbox = idOutbox;
        this.azienda = azienda;
        this.cf = cf;
        this.mittente = mittente;
        this.numerazioneGerarchica = numerazioneGerarchica;
        this.utente = utente;
        this.persona = persona;
    }

    public Integer getIdOutbox() {
        return idOutbox;
    }

    public void setIdOutbox(Integer idOutbox) {
        this.idOutbox = idOutbox;
    }

    public Azienda getIdAzienda() {
        return azienda;
    }

    public void setIdAzienda(Azienda idAzienda) {
        this.azienda = idAzienda;
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

    public Utente getUtente() {
        return utente;
    }

    public void setUtente(Utente utente) {
        this.utente = utente;
    }

    public Persona getPersona() {
        return persona;
    }

    public void setPersona(Persona persona) {
        this.persona = persona;
    }

    @Override
    public String toString() {
        return "FascicolatoreAutomaticoGediParams{" + "idOutbox=" + idOutbox + ", idAzienda=" + azienda + ", cf=" + cf + ", mittente=" + mittente + ", numerazioneGerarchica=" + numerazioneGerarchica + '}';
    }

}
