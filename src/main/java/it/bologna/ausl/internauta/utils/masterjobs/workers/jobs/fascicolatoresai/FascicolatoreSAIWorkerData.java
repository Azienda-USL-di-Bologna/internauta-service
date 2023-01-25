package it.bologna.ausl.internauta.utils.masterjobs.workers.jobs.fascicolatoresai;

import com.fasterxml.jackson.annotation.JsonIgnore;
import it.bologna.ausl.internauta.utils.masterjobs.workers.jobs.JobWorkerData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author gdm
 */
public class FascicolatoreSAIWorkerData extends JobWorkerData {
    @JsonIgnore
    private static final Logger log = LoggerFactory.getLogger(FascicolatoreSAIWorker.class);
    
    private Integer idOutbox;
    private Integer idAzienda;
    private String cf;
    private String mittente;
    private String numerazioneGerarchica;
    private Integer idUtente;
    private Integer idPersona;

    public FascicolatoreSAIWorkerData() {
    }

    public FascicolatoreSAIWorkerData(Integer idOutbox, Integer idAzienda, String cf, String mittente, String numerazioneGerarchica, Integer idUtente, Integer idPersona) {
        this.idOutbox = idOutbox;
        this.idAzienda = idAzienda;
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
        return idAzienda;
    }

    public void setIdAzienda(Integer IdAzienda) {
        this.idAzienda = IdAzienda;
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

    public Integer getIdUtente() {
        return idUtente;
    }

    public void setIdUtente(Integer idUtente) {
        this.idUtente = idUtente;
    }

    public Integer getIdPersona() {
        return idPersona;
    }

    public void setIdPersona(Integer idPersona) {
        this.idPersona = idPersona;
    }
}
