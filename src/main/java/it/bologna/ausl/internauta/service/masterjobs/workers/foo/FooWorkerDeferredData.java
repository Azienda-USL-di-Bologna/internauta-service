package it.bologna.ausl.internauta.service.masterjobs.workers.foo;

import com.fasterxml.jackson.annotation.JsonIgnore;
import it.bologna.ausl.internauta.service.masterjobs.workers.WorkerData;
import it.bologna.ausl.internauta.service.masterjobs.workers.WorkerDeferredData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author gdm
 */
public class FooWorkerDeferredData extends WorkerDeferredData {
    @JsonIgnore
    private static final Logger log = LoggerFactory.getLogger(FooWorkerDeferredData.class);
    
    @JsonIgnore
    private String name = "Foo";
    
    public FooWorkerDeferredData() {
    }
    
    @Override
    public WorkerData toWorkerData() {
        FooWorkerData wd = new FooWorkerData();
        wd.setName("erano i deferred data");
        wd.setParams2("params 1 deferred");
        return wd;
    }
}
