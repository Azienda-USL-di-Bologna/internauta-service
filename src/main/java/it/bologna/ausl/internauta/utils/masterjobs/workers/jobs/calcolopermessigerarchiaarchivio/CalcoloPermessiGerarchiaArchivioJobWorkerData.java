package it.bologna.ausl.internauta.utils.masterjobs.workers.jobs.calcolopermessigerarchiaarchivio;

import com.fasterxml.jackson.annotation.JsonIgnore;
import it.bologna.ausl.internauta.utils.masterjobs.workers.jobs.JobWorkerData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author mido
 */
public class CalcoloPermessiGerarchiaArchivioJobWorkerData extends JobWorkerData {
    @JsonIgnore
    private static final Logger log = LoggerFactory.getLogger(CalcoloPermessiGerarchiaArchivioJobWorkerData.class);

    private Integer idArchivioRadice;

    public CalcoloPermessiGerarchiaArchivioJobWorkerData() {
    }

    public CalcoloPermessiGerarchiaArchivioJobWorkerData(Integer idArchivioRadice) {
        this.idArchivioRadice = idArchivioRadice;
    }

    public Integer getIdArchivioRadice() {
        return idArchivioRadice;
    }

    public void setIdArchivioRadice(Integer idArchivioRadice) {
        this.idArchivioRadice = idArchivioRadice;
    }

}
