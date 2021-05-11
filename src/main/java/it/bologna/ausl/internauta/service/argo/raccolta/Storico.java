
package it.bologna.ausl.internauta.service.argo.raccolta;

/**
 *
 * @author Matteo Next
 */
public class Storico {
    
    private String utente;
    
    private String motivazione;
    
    private String stato;
    
    private String data; 

    public String getUtente() {
        return utente;
    }

    public void setUtente(String utente) {
        this.utente = utente;
    }

    public String getMotivazione() {
        return motivazione;
    }

    public void setMotivazione(String motivazione) {
        this.motivazione = motivazione;
    }

    public String getStato() {
        return stato;
    }

    public void setStato(String stato) {
        this.stato = stato;
    }

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }

    public Storico() {
    }

    public Storico(String utente, String motivazione, String stato, String data) {
        this.utente = utente;
        this.motivazione = motivazione;
        this.stato = stato;
        this.data = data;
    }
    
    
    
}
