package it.bologna.ausl.internauta.utils.masterjobs.workers.jobs.sendmail;

import com.fasterxml.jackson.annotation.JsonIgnore;
import it.bologna.ausl.internauta.utils.masterjobs.workers.jobs.JobWorkerData;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.multipart.MultipartFile;

/**
 *
 * @author gdm
 */
public class SendAttivitaMailJobWorkerData extends JobWorkerData {
    @JsonIgnore
    private static final Logger log = LoggerFactory.getLogger(SendAttivitaMailJobWorkerData.class);

    private Integer idAttivita;
    private Integer idAzienda;
    private Integer idPersona;
//    private String fromName;
    private String oggettoAttivita;
    private List<String> to;
//    private String body;
//    private List<String> cc;
//    private List<String> bcc;
//    private List<MultipartFile> attachments;
//    private List<String> replyTo;

    public SendAttivitaMailJobWorkerData() {
    }

    public SendAttivitaMailJobWorkerData(Integer idAttivita, Integer idAzienda, Integer idPersona, String oggettoAttivita, List<String> to) {
        this.idAttivita = idAttivita;
        this.idAzienda = idAzienda;
        this.idPersona = idPersona;
        this.oggettoAttivita = oggettoAttivita;
        this.to = to;
    }

    public Integer getIdAttivita() {
        return idAttivita;
    }

    public void setIdAttivita(Integer idAttivita) {
        this.idAttivita = idAttivita;
    }
    
    public Integer getIdAzienda() {
        return idAzienda;
    }

    public void setIdAzienda(Integer idAzienda) {
        this.idAzienda = idAzienda;
    }

    public Integer getIdPersona() {
        return idPersona;
    }

    public void setIdPersona(Integer idPersona) {
        this.idPersona = idPersona;
    }

    public String getOggettoAttivita() {
        return oggettoAttivita;
    }

    public void setOggettoAttivita(String oggettoAttivita) {
        this.oggettoAttivita = oggettoAttivita;
    }

    public List<String> getTo() {
        return to;
    }

    public void setTo(List<String> to) {
        this.to = to;
    }
}
