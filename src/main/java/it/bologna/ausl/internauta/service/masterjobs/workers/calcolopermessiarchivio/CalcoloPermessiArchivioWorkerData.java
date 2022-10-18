package it.bologna.ausl.internauta.service.masterjobs.workers.calcolopermessiarchivio;

import com.fasterxml.jackson.annotation.JsonIgnore;
import it.bologna.ausl.internauta.service.masterjobs.workers.WorkerData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author mido
 */
public class CalcoloPermessiArchivioWorkerData extends WorkerData {
    @JsonIgnore
    private static final Logger log = LoggerFactory.getLogger(CalcoloPermessiArchivioWorkerData.class);

    private Integer idArchivio;

    public CalcoloPermessiArchivioWorkerData() {
    }

    public CalcoloPermessiArchivioWorkerData(Integer idArchivio) {
        this.idArchivio=idArchivio;
    }

    public Integer getIdArchivio() {
        return idArchivio;
    }

    public void setIdArchivio(Integer idArchivio) {
        this.idArchivio = idArchivio;
    }

   

    
}
