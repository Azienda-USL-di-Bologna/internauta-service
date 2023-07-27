package it.bologna.ausl.internauta.utils.masterjobs.workers.jobs.generazioneziparchivio;

import it.bologna.ausl.internauta.utils.masterjobs.workers.jobs.JobWorkerData;
import it.bologna.ausl.model.entities.baborg.Persona;
import it.bologna.ausl.model.entities.scripta.Archivio;

/**
 *
 * @author conte
 */
public class GenerazioneZipArchivioJobWorkerData extends JobWorkerData{
    
    private Persona persona;
    private Archivio archivio;
    private String downloadUrl;
    private String uploadUrl;
    private String note;

    public GenerazioneZipArchivioJobWorkerData() { }

    public GenerazioneZipArchivioJobWorkerData(Persona persona, Archivio archivio, String downloadUrl, String uploadUrl, String note) {
        this.persona = persona;
        this.archivio = archivio;
        this.downloadUrl = downloadUrl;
        this.uploadUrl = uploadUrl;
        this.note = note;
    }


    public Persona getPersona() {
        return persona;
    }

    public void setPersona(Persona persona) {
        this.persona = persona;
    }

    public Archivio getArchivio() {
        return archivio;
    }

    public void setArchivio(Archivio archivio) {
        this.archivio = archivio;
    }

    public String getDownloadUrl() {
        return downloadUrl;
    }

    public void setDownloadUrl(String downloadUrl) {
        this.downloadUrl = downloadUrl;
    }

    public String getUploadUrl() {
        return uploadUrl;
    }

    public void setUploadUrl(String uploadUrl) {
        this.uploadUrl = uploadUrl;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }
    
}
