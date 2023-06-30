package it.bologna.ausl.internauta.utils.masterjobs.workers.jobs.cambioprofilo;

import com.fasterxml.jackson.annotation.JsonIgnore;
import it.bologna.ausl.internauta.utils.masterjobs.workers.jobs.JobWorkerData;
import it.bologna.ausl.internauta.utils.masterjobs.workers.jobs.calcolagerarchiaarchivio.CalcolaGerarchiaArchivioJobWorkerData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author mido
 */
public class CambioProfiloJobWorkerData extends JobWorkerData {
    
    @JsonIgnore
    private static final Logger log = LoggerFactory.getLogger(CalcolaGerarchiaArchivioJobWorkerData.class);
    
    private Integer idPersona;
    private String profiloNew;
    private String profiloOld;
    private Integer idAzienda;
    
    public CambioProfiloJobWorkerData() {
    }

    public Integer getIdPersona() {
        return idPersona;
    }

    public void setIdPersona(Integer idPersona) {
        this.idPersona = idPersona;
    }

    public String getProfiloNew() {
        return profiloNew;
    }

    public void setProfiloNew(String profiloNew) {
        this.profiloNew = profiloNew;
    }

    public String getProfiloOld() {
        return profiloOld;
    }

    public void setProfiloOld(String profiloOld) {
        this.profiloOld = profiloOld;
    }

    public Integer getIdAzienda() {
        return idAzienda;
    }

    public void setIdAzienda(Integer idAzienda) {
        this.idAzienda = idAzienda;
    }
    
}
