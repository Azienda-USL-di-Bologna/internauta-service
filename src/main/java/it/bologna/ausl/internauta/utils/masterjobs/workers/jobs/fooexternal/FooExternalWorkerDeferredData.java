package it.bologna.ausl.internauta.utils.masterjobs.workers.jobs.fooexternal;

import com.fasterxml.jackson.annotation.JsonIgnore;
import it.bologna.ausl.internauta.utils.masterjobs.workers.jobs.JobWorkerData;
import it.bologna.ausl.internauta.utils.masterjobs.workers.jobs.JobWorkerDeferredData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author gdm
 */
public class FooExternalWorkerDeferredData extends JobWorkerDeferredData {
    @JsonIgnore
    private static final Logger log = LoggerFactory.getLogger(FooExternalWorkerDeferredData.class);
  
    public FooExternalWorkerDeferredData() {
    }
    
    @Override
    public JobWorkerData toWorkerData() {
        FooExternalWorkerData wd = new FooExternalWorkerData();
        wd.setName("erano i deferred data");
        wd.setParams2("params 1 deferred");
        return wd;
    }
}
