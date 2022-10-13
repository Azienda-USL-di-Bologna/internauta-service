package it.bologna.ausl.internauta.service.masterjobs;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

/**
 *
 * @author gdm
 */
//@Component
public class NOTUSED_MasterjobsWaitQueueData extends MasterjobsQueueData {
    private String destinationQueue;
    
    @JsonIgnore
    private ObjectMapper objectMapper;
    
    public NOTUSED_MasterjobsWaitQueueData(MasterjobsQueueData queueData, String destinationQueue){
        this.setSet(queueData.getSet());
        this.setJobs(queueData.getJobs());
        this.setObjectMapper(queueData.getObjectMapper());
        this.destinationQueue = destinationQueue;
    }

    public String getDestinationQueue() {
        return destinationQueue;
    }

    public void setDestinationQueue(String destinationQueue) {
        this.destinationQueue = destinationQueue;
    }
}
