package it.bologna.ausl.internauta.service.krint;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;
import org.springframework.format.annotation.DateTimeFormat;

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
    @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime dataoraOperazione;
    
    @JsonProperty("descrizione_operazione")
    public String getDescrizioneOperazione() {
        return descrizioneOperazione;
    }
    
    @JsonProperty("descrizione_operazione")
    public void setDescrizioneOperazione(String descrizioneOperazione) {
        this.descrizioneOperazione = descrizioneOperazione;
    }
    
    @JsonProperty("id_utente")
    public Integer getIdUtente() {
        return idUtente;
    }
    
    @JsonProperty("id_utente")
    public void setIdUtente(Integer idUtente) {
        this.idUtente = idUtente;
    }

    @JsonProperty("descrizione_utente")
    public String getDescrizioneUtente() {
        return descrizioneUtente;
    }

    @JsonProperty("descrizione_utente")
    public void setDescrizioneUtente(String descrizioneUtente) {
        this.descrizioneUtente = descrizioneUtente;
    }

    @JsonProperty("id_oggetto")
    public String getIdOggetto() {
        return idOggetto;
    }

    @JsonProperty("id_oggetto")
    public void setIdOggetto(String idOggetto) {
        this.idOggetto = idOggetto;
    }

    @JsonProperty("tipo_oggetto")
    public String getTipoOggetto() {
        return tipoOggetto;
    }

    @JsonProperty("tipo_oggetto")
    public void setTipoOggetto(String tipoOggetto) {
        this.tipoOggetto = tipoOggetto;
    }

    @JsonProperty("descrizione_oggetto")
    public String getDescrizioneOggetto() {
        return descrizioneOggetto;
    }

    @JsonProperty("descrizione_oggetto")
    public void setDescrizioneOggetto(String descrizioneOggetto) {
        this.descrizioneOggetto = descrizioneOggetto;
    }

    @JsonProperty("dataora_operazione")
    public LocalDateTime getDataoraOperazione() {
        return dataoraOperazione;
    }

    @JsonProperty("dataora_operazione")
    public void setDataoraOperazione(LocalDateTime dataoraOperazione) {
        this.dataoraOperazione = dataoraOperazione;
    }
    
}
