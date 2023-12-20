package it.bologna.ausl.internauta.utils.masterjobs.workers.jobs.automatismifineanno;

import it.bologna.ausl.internauta.utils.masterjobs.workers.jobs.JobWorkerData;
import java.util.List;
import java.util.Map;

/**
 *
 * @author conte
 */

public class AutomatismiFineAnnoJobWorkerData extends JobWorkerData{
    private Integer idAzienda;
    private Integer idUtenteResponsabileFascicoloSpeciale;
    private Integer idVicarioFascicoloSpeciale;
    private Integer idClassificazioneFascSpeciale;
    private Map<String, String> nomeFascicoliSpeciali;
    
    public AutomatismiFineAnnoJobWorkerData() { }

    public AutomatismiFineAnnoJobWorkerData(Integer idAzienda, Integer idUtenteResponsabileFascicoloSpeciale, Integer idVicarioFascicoloSpeciale, Integer idClassificazioneFascSpeciale, Map<String, String> nomeFascicoliSpeciali) {
        this.idAzienda = idAzienda;
        this.idUtenteResponsabileFascicoloSpeciale = idUtenteResponsabileFascicoloSpeciale;
        this.idVicarioFascicoloSpeciale = idVicarioFascicoloSpeciale;
        this.idClassificazioneFascSpeciale = idClassificazioneFascSpeciale;
        this.nomeFascicoliSpeciali = nomeFascicoliSpeciali;
    }

    public Integer getIdAzienda() {
        return idAzienda;
    }

    public void setIdAzienda(Integer idAzienda) {
        this.idAzienda = idAzienda;
    }

    public Integer getIdUtenteResponsabileFascicoloSpeciale() {
        return idUtenteResponsabileFascicoloSpeciale;
    }

    public void setIdUtenteResponsabileFascicoloSpeciale(Integer idUtenteResponsabileFascicoloSpeciale) {
        this.idUtenteResponsabileFascicoloSpeciale = idUtenteResponsabileFascicoloSpeciale;
    }

    public Integer getIdVicarioFascicoloSpeciale() {
        return idVicarioFascicoloSpeciale;
    }

    public void setIdVicarioFascicoloSpeciale(Integer idVicarioFascicoloSpeciale) {
        this.idVicarioFascicoloSpeciale = idVicarioFascicoloSpeciale;
    }

    public Integer getIdClassificazioneFascSpeciale() {
        return idClassificazioneFascSpeciale;
    }

    public void setIdClassificazioneFascSpeciale(Integer idClassificazioneFascSpeciale) {
        this.idClassificazioneFascSpeciale = idClassificazioneFascSpeciale;
    }

    public Map<String, String> getNomeFascicoliSpeciali() {
        return nomeFascicoliSpeciali;
    }

    public void setNomeFascicoliSpeciali(Map<String, String> nomeFascicoliSpeciali) {
        this.nomeFascicoliSpeciali = nomeFascicoliSpeciali;
    }

}
