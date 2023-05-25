package it.bologna.ausl.internauta.utils.masterjobs.workers.jobs.eliminaarchiviazioni;

import it.bologna.ausl.internauta.utils.masterjobs.workers.jobs.JobWorkerData;

/**
 *
 * @author conte
 */
public class EliminaArchiviazioniJobWorkerData extends JobWorkerData{
    private Integer idAzienda;
    private Integer tempoEliminaArchiviazioni;
    private String note;

    public EliminaArchiviazioniJobWorkerData() { }

    public EliminaArchiviazioniJobWorkerData(Integer idAzienda, Integer tempoEliminaArchiviazioni, String note) {
        this.idAzienda = idAzienda;
        this.tempoEliminaArchiviazioni = tempoEliminaArchiviazioni;
        this.note = note;
    }

    public Integer getIdAzienda() {
        return idAzienda;
    }

    public void setIdAzienda(Integer idAzienda) {
        this.idAzienda = idAzienda;
    }

    public Integer getTempoEliminaArchiviazioni() {
        return tempoEliminaArchiviazioni;
    }

    public void setTempoEliminaArchiviazioni(Integer tempoEliminaArchiviazioni) {
        this.tempoEliminaArchiviazioni = tempoEliminaArchiviazioni;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }
    
}
