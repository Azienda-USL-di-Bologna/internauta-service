package it.bologna.ausl.internauta.utils.masterjobs.workers.jobs.calcolagerarchiaarchivio;

import com.fasterxml.jackson.annotation.JsonIgnore;
import it.bologna.ausl.internauta.utils.masterjobs.workers.jobs.JobWorkerData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author gusgus
 */
public class CalcolaGerarchiaArchivioJobWorkerData extends JobWorkerData {
    @JsonIgnore
    private static final Logger log = LoggerFactory.getLogger(CalcolaGerarchiaArchivioJobWorkerData.class);

    private Integer idArchivioRadice;

    public CalcolaGerarchiaArchivioJobWorkerData() {
    }

    public CalcolaGerarchiaArchivioJobWorkerData(Integer idArchivioRadice) {
        this.idArchivioRadice = idArchivioRadice;
    }

    public Integer getIdArchivioRadice() {
        return idArchivioRadice;
    }

    public void setIdArchivioRadice(Integer idArchivioRadice) {
        this.idArchivioRadice = idArchivioRadice;
    }
}
