package it.bologna.ausl.internauta.utils.masterjobs.workers.jobs.fascicolatoresai;

import com.fasterxml.jackson.annotation.JsonIgnore;
import it.bologna.ausl.internauta.utils.masterjobs.workers.jobs.JobWorkerData;
import it.bologna.ausl.internauta.utils.masterjobs.workers.jobs.JobWorkerDeferredData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author gdm
 */
public class FascicolatoreSAIWorkerDeferredData extends JobWorkerDeferredData {
    @JsonIgnore
    private static final Logger log = LoggerFactory.getLogger(FascicolatoreSAIWorkerDeferredData.class);
  
    public FascicolatoreSAIWorkerDeferredData() {
    }

    @Override
    public JobWorkerData toWorkerData() {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }
    
    
}
