package it.bologna.ausl.internauta.service.masterjobs.workers;

import com.fasterxml.jackson.annotation.JsonTypeInfo;

/**
 *
 * @author gdm
 */
@JsonTypeInfo(
        use = JsonTypeInfo.Id.CLASS,
        include = JsonTypeInfo.As.PROPERTY,
        property = "@class")
public abstract class WorkerData implements WorkerDataInterface {

    public static String getClassNameKey() {
        return "@class";
    }
}
