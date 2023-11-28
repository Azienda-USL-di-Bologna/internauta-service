package it.bologna.ausl.internauta.utils.masterjobs.workers.jobs.updatecontattiinad;

import com.fasterxml.jackson.annotation.JsonIgnore;
import it.bologna.ausl.internauta.utils.masterjobs.workers.jobs.JobWorkerData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author gdm
 */
public class UpdateContattiInadJobWorkerData extends JobWorkerData {
    
    @JsonIgnore
    private static final Logger LOGGER = LoggerFactory.getLogger(UpdateContattiInadJobWorkerData.class);
    
    Integer numeroContattiDaAggiornare;
    Integer idAzienda;
    
    public UpdateContattiInadJobWorkerData() {
    }

    public UpdateContattiInadJobWorkerData(Integer numeroContattiDaAggiornare, Integer idAzienda) {
        this.numeroContattiDaAggiornare = numeroContattiDaAggiornare;
        this.idAzienda = idAzienda;
    }
    
    public Integer getNumeroContattiDaAggiornare() {
        return numeroContattiDaAggiornare;
    }

    public void setNumeroContattiDaAggiornare(Integer numeroContattiDaAggiornare) {
        this.numeroContattiDaAggiornare = numeroContattiDaAggiornare;
    }

    public Integer getIdAzienda() {
        return idAzienda;
    }

    public void setIdAzienda(Integer idAzienda) {
        this.idAzienda = idAzienda;
    }
    
}
