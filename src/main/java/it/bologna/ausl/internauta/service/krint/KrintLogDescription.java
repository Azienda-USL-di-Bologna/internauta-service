package it.bologna.ausl.internauta.service.krint;

import java.time.LocalDateTime;

/**
 *
 * @author gusgus
 */
public class KrintLogDescription {
    private String descrizioneOperazione;
    private Integer idUtente; 
    private String descrizioneUtente;
    private String idOggetto; 
    private String tipoOggetto;
    private String descrizioneOggetto; 
    private LocalDateTime dataoraOperazione;

    public String getDescrizioneOperazione() {
        return descrizioneOperazione;
    }

    public void setDescrizioneOperazione(String descrizioneOperazione) {
        this.descrizioneOperazione = descrizioneOperazione;
    }

    public Integer getIdUtente() {
        return idUtente;
    }

    public void setIdUtente(Integer idUtente) {
        this.idUtente = idUtente;
    }

    public String getDescrizioneUtente() {
        return descrizioneUtente;
    }

    public void setDescrizioneUtente(String descrizioneUtente) {
        this.descrizioneUtente = descrizioneUtente;
    }

    public String getIdOggetto() {
        return idOggetto;
    }

    public void setIdOggetto(String idOggetto) {
        this.idOggetto = idOggetto;
    }

    public String getTipoOggetto() {
        return tipoOggetto;
    }

    public void setTipoOggetto(String tipoOggetto) {
        this.tipoOggetto = tipoOggetto;
    }

    public String getDescrizioneOggetto() {
        return descrizioneOggetto;
    }

    public void setDescrizioneOggetto(String descrizioneOggetto) {
        this.descrizioneOggetto = descrizioneOggetto;
    }

    public LocalDateTime getDataoraOperazione() {
        return dataoraOperazione;
    }

    public void setDataoraOperazione(LocalDateTime dataoraOperazione) {
        this.dataoraOperazione = dataoraOperazione;
    }
    
}
