package it.bologna.ausl.internauta.utils.masterjobs.workers.jobs.updatecontattiinad;

import com.fasterxml.jackson.annotation.JsonIgnore;
import it.bologna.ausl.internauta.utils.masterjobs.workers.jobs.JobWorkerData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author gdm
 */
public class UpdateContattiIfPossibleInadJobWorkerData extends JobWorkerData {
    
    @JsonIgnore
    private static final Logger LOGGER = LoggerFactory.getLogger(UpdateContattiIfPossibleInadJobWorkerData.class);

    private String idRequest;

    public UpdateContattiIfPossibleInadJobWorkerData() {
    }

    public UpdateContattiIfPossibleInadJobWorkerData(String idRequest) {
        this.idRequest = idRequest;
    }

    public String getIdRequest() {
        return idRequest;
    }

    public void setIdRequest(String idRequest) {
        this.idRequest = idRequest;
    }
}
