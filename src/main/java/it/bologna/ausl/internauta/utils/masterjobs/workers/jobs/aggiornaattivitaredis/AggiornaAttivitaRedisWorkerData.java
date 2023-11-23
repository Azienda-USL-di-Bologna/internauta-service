package it.bologna.ausl.internauta.utils.masterjobs.workers.jobs.aggiornaattivitaredis;

import com.fasterxml.jackson.annotation.JsonIgnore;
import it.bologna.ausl.internauta.utils.masterjobs.workers.jobs.JobWorkerData;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author conte
 */
public class AggiornaAttivitaRedisWorkerData extends JobWorkerData {
    @JsonIgnore
    private static final Logger log = LoggerFactory.getLogger(AggiornaAttivitaRedisWorkerData.class);

    private Integer idAttivita;
    private Integer idPersona;
    private String operation;

    public AggiornaAttivitaRedisWorkerData() {
    }

    public AggiornaAttivitaRedisWorkerData(Integer idAttivita, Integer idPersona, String operation) {
        this.idAttivita = idAttivita;
        this.idPersona = idPersona;
        this.operation = operation;
    }

    public Integer getIdAttivita() {
        return idAttivita;
    }

    public void setIdAttivita(Integer idAttivita) {
        this.idAttivita = idAttivita;
    }

    public Integer getIdPersona() {
        return idPersona;
    }

    public void setIdPersona(Integer idPersona) {
        this.idPersona = idPersona;
    }

    public String getOperation() {
        return operation;
    }

    public void setOperation(String operation) {
        this.operation = operation;
    }
    
    
}
