package it.bologna.ausl.internauta.utils.masterjobs.workers.jobs.calcolopermessiarchivio;

import com.fasterxml.jackson.annotation.JsonIgnore;
import it.bologna.ausl.internauta.utils.masterjobs.workers.jobs.JobWorkerData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author gusgus
 */
public class CalcoloPermessiArchivioJobWorkerData extends JobWorkerData {
    @JsonIgnore
    private static final Logger log = LoggerFactory.getLogger(CalcoloPermessiArchivioJobWorkerData.class);

    private Integer idArchivio;
    private Boolean queueJobCalcolaPersoneVedentiDoc = false;

    public CalcoloPermessiArchivioJobWorkerData() {
    }

    public CalcoloPermessiArchivioJobWorkerData(Integer idArchivio) {
        this.idArchivio = idArchivio;
    }

    public CalcoloPermessiArchivioJobWorkerData(Integer idArchivio, Boolean queueJobCalcolaPersoneVedentiDoc) {
        this.idArchivio = idArchivio;
        this.queueJobCalcolaPersoneVedentiDoc = queueJobCalcolaPersoneVedentiDoc;
    }

    public Integer getIdArchivio() {
        return idArchivio;
    }

    public void setIdArchivio(Integer idArchivio) {
        this.idArchivio = idArchivio;
    }

    public Boolean getQueueJobCalcolaPersoneVedentiDoc() {
        return queueJobCalcolaPersoneVedentiDoc;
    }

    public void setQueueJobCalcolaPersoneVedentiDoc(Boolean queueJobCalcolaPersoneVedentiDoc) {
        this.queueJobCalcolaPersoneVedentiDoc = queueJobCalcolaPersoneVedentiDoc;
    }
}
