package it.bologna.ausl.internauta.utils.masterjobs.workers.jobs.managecambiassociazioni;

import com.fasterxml.jackson.annotation.JsonIgnore;
import it.bologna.ausl.internauta.utils.masterjobs.workers.jobs.JobWorkerData;
import java.time.ZonedDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author gdm
 */
public class ManageCambiAssociazioniJobWorkerData extends JobWorkerData {
    @JsonIgnore
    private static final Logger log = LoggerFactory.getLogger(ManageCambiAssociazioniJobWorkerData.class);

    private ZonedDateTime dataRiferimento;

    public ManageCambiAssociazioniJobWorkerData() {
    }
    
    public ManageCambiAssociazioniJobWorkerData(ZonedDateTime dataRiferimento) {
        this.dataRiferimento = dataRiferimento;
        
    }

    public ZonedDateTime getDataRiferimento() {
        return dataRiferimento;
    }

    public void setDataRiferimento(ZonedDateTime dataRiferimento) {
        this.dataRiferimento = dataRiferimento;
    }
    
    
}
