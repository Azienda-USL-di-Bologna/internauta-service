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
public class CalcolaPersoneVedentiDaArchiviRadiceJobWorkerData extends JobWorkerData {
    @JsonIgnore
    private static final Logger log = LoggerFactory.getLogger(CalcolaPersoneVedentiDaArchiviRadiceJobWorkerData.class);

    private Set<Integer> idArchiviRadice;

    public CalcolaPersoneVedentiDaArchiviRadiceJobWorkerData() {
    }

    public CalcolaPersoneVedentiDaArchiviRadiceJobWorkerData(Set<Integer> idArchiviRadice) {
        this.idArchiviRadice = idArchiviRadice;
    }

    public Set<Integer> getidArchiviRadice() {
        return idArchiviRadice;
    }

    public void setidArchiviRadice(Set<Integer> idArchiviRadice) {
        this.idArchiviRadice = idArchiviRadice;
    }

}
