package it.bologna.ausl.internauta.utils.masterjobs.workers.jobs.calcolapersonevedentidaarchivi;

import com.fasterxml.jackson.annotation.JsonIgnore;
import it.bologna.ausl.internauta.utils.masterjobs.workers.jobs.JobWorkerData;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author gusgus
 */
public class CalcolaPersoneVedentiDaArchiviJobWorkerData extends JobWorkerData {
    @JsonIgnore
    private static final Logger log = LoggerFactory.getLogger(CalcolaPersoneVedentiDaArchiviJobWorkerData.class);

    private Set<Integer> idArchivi;

    public CalcolaPersoneVedentiDaArchiviJobWorkerData() {
    }

    public CalcolaPersoneVedentiDaArchiviJobWorkerData(Set<Integer> idArchivi) {
        this.idArchivi = idArchivi;
    }

    public Set<Integer> getIdArchivi() {
        return idArchivi;
    }

    public void setIdArchivi(Set<Integer> idArchivi) {
        this.idArchivi = idArchivi;
    }

}
