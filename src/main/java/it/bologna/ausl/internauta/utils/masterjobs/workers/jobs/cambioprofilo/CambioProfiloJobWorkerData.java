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
    
    private String codiceFiscale;
    private String profiloNew;
    private String profiloOld;
    private String codiceAzienda;
    
    public CambioProfiloJobWorkerData() {
    }

    public String getCodiceFiscale() {
        return codiceFiscale;
    }

    public void setCodiceFiscale(String codiceFiscale) {
        this.codiceFiscale = codiceFiscale;
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

    public String getCodiceAzienda() {
        return codiceAzienda;
    }

    public void setCodiceAzienda(String codiceAzienda) {
        this.codiceAzienda = codiceAzienda;
    }
    
    
    
}
