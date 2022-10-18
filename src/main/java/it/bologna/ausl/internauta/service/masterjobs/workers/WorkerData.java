package it.bologna.ausl.internauta.service.masterjobs.workers;

import com.fasterxml.jackson.annotation.JsonTypeInfo;

/**
 *
 * @author gdm
 * chiunque deve estendere questa classe deve anche creare un costruttore vuoto
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
