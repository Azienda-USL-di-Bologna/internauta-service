package it.bologna.ausl.internauta.service.masterjobs.workers;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.bologna.ausl.internauta.service.masterjobs.exceptions.MasterjobsParsingException;
import static it.bologna.ausl.internauta.service.masterjobs.workers.WorkerData.getClassNameKey;
import java.util.Map;

/**
 *
 * @author gdm
 */
@JsonTypeInfo(
    use = JsonTypeInfo.Id.CLASS,
    include = JsonTypeInfo.As.PROPERTY,
    property = "@class")
public abstract class WorkerDeferredData implements WorkerDataInterface {

    public abstract WorkerData toWorkerData();
    

}