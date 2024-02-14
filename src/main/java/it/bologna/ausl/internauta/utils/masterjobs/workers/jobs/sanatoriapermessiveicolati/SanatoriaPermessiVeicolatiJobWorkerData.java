package it.bologna.ausl.internauta.utils.masterjobs.workers.jobs.sanatoriapermessiveicolati;

import com.fasterxml.jackson.annotation.JsonIgnore;
import it.bologna.ausl.internauta.utils.masterjobs.workers.jobs.JobWorkerData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Michele D'Onza
 */
public class SanatoriaPermessiVeicolatiJobWorkerData extends JobWorkerData {
    @JsonIgnore
    private static final Logger log = LoggerFactory.getLogger(SanatoriaPermessiVeicolatiJobWorkerData.class);
    
    boolean AspettaRibaltone;

    public boolean isAspettaRibaltone() {
        return AspettaRibaltone;
    }

    public void setAspettaRibaltone(boolean AspettaRibaltone) {
        this.AspettaRibaltone = AspettaRibaltone;
    }
    
    
}
