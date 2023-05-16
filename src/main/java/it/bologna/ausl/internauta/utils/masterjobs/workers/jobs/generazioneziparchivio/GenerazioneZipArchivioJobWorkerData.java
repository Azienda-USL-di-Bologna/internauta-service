package it.bologna.ausl.internauta.utils.masterjobs.workers.jobs.generazioneziparchivio;

import it.bologna.ausl.internauta.utils.masterjobs.workers.jobs.eliminaarchiviazioni.*;
import it.bologna.ausl.internauta.utils.masterjobs.workers.jobs.JobWorkerData;
import it.bologna.ausl.model.entities.baborg.Persona;
import it.bologna.ausl.model.entities.scripta.Archivio;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;

/**
 *
 * @author conte
 */
public class GenerazioneZipArchivioJobWorkerData extends JobWorkerData{
    
    private Persona persona;
    private Archivio archivio;
    private HttpServletRequest request;
    private String note;

    public GenerazioneZipArchivioJobWorkerData() { }

    public GenerazioneZipArchivioJobWorkerData(Persona persona, Archivio archivio, HttpServletRequest request, String note) {
        this.persona = persona;
        this.archivio = archivio;
        this.request = request;
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

    public HttpServletRequest getRequest() {
        return request;
    }

    public void setRequest(HttpServletRequest request) {
        this.request = request;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }
    
}
