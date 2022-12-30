package it.bologna.ausl.internauta.utils.masterjobs.workers.jobs.calcolapersonevedentidoc;

import com.fasterxml.jackson.annotation.JsonIgnore;
import it.bologna.ausl.internauta.utils.masterjobs.workers.jobs.JobWorkerData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author gusgus
 */
public class CalcolaPersoneVedentiDocJobWorkerData extends JobWorkerData {
    @JsonIgnore
    private static final Logger log = LoggerFactory.getLogger(CalcolaPersoneVedentiDocJobWorkerData.class);

    private Integer idDoc;

    public CalcolaPersoneVedentiDocJobWorkerData() {
    }

    public CalcolaPersoneVedentiDocJobWorkerData(Integer idDoc) {
        this.idDoc = idDoc;
    }

    public Integer getIdDoc() {
        return idDoc;
    }

    public void setIdDoc(Integer idDoc) {
        this.idDoc = idDoc;
    }
}
