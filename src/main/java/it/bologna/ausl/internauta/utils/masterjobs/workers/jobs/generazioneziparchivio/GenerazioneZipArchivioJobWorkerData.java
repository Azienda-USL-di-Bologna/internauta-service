package it.bologna.ausl.internauta.utils.masterjobs.workers.jobs.generazioneziparchivio;

import it.bologna.ausl.internauta.utils.masterjobs.workers.jobs.JobWorkerData;
import it.bologna.ausl.model.entities.baborg.Persona;
import it.bologna.ausl.model.entities.scripta.Archivio;

/**
 *
 * @author conte
 */
public class GenerazioneZipArchivioJobWorkerData extends JobWorkerData{
    
    private Integer idPersona;
    private Integer idArchivio;
    private String downloadUrl;
//    private String uploadUrl;
//    private String note;

    public GenerazioneZipArchivioJobWorkerData() { }

    public GenerazioneZipArchivioJobWorkerData(Integer idPersona, Integer idArchivio, String downloadUrl) {
        this.idPersona = idPersona;
        this.idArchivio = idArchivio;
        this.downloadUrl = downloadUrl;
    }
    
    public Integer getIdPersona() {
        return idPersona;
    }

    public void setIdPersona(Integer idPersona) {
        this.idPersona = idPersona;
    }

    public Integer getIdArchivio() {
        return idArchivio;
    }

    public void setIdArchivio(Integer idArchivio) {
        this.idArchivio = idArchivio;
    }

    public String getDownloadUrl() {
        return downloadUrl;
    }

    public void setDownloadUrl(String downloadUrl) {
        this.downloadUrl = downloadUrl;
    }
}
