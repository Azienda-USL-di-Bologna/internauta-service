package it.bologna.ausl.internauta.utils.masterjobs.workers.jobs.calcolopermessiarchivio;

import com.fasterxml.jackson.annotation.JsonIgnore;
import it.bologna.ausl.internauta.utils.masterjobs.workers.jobs.JobWorkerData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author mido
 */
public class CalcoloPermessiArchivioJobWorkerData extends JobWorkerData {
    @JsonIgnore
    private static final Logger log = LoggerFactory.getLogger(CalcoloPermessiArchivioJobWorkerData.class);

    private Integer idArchivio;

    public CalcoloPermessiArchivioJobWorkerData() {
    }

    public CalcoloPermessiArchivioJobWorkerData(Integer idArchivio) {
        this.idArchivio = idArchivio;
    }

    public Integer getIdArchivio() {
        return idArchivio;
    }

    public void setIdArchivio(Integer idArchivio) {
        this.idArchivio = idArchivio;
    }
}
