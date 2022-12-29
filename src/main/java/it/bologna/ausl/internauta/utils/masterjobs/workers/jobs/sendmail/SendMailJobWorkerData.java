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
public class SendMailJobWorkerData extends JobWorkerData {
    @JsonIgnore
    private static final Logger log = LoggerFactory.getLogger(SendMailJobWorkerData.class);

    private Integer idAzienda;
    private String fromName;
    private String Subject;
    private List<String> To;
    private String body;
    private List<String> cc;
    private List<String> bcc;
    private MultipartFile[] attachments;
    private List<String> replyTo;

    public SendMailJobWorkerData() {
    }

    public SendMailJobWorkerData(Integer idAzienda, String fromName, String Subject, List<String> To, String body, List<String> cc, List<String> bcc, MultipartFile[] attachments, List<String> replyTo) {
        this.idAzienda = idAzienda;
        this.fromName = fromName;
        this.Subject = Subject;
        this.To = To;
        this.body = body;
        this.cc = cc;
        this.bcc = bcc;
        this.attachments = attachments;
        this.replyTo = replyTo;
    }
    
    public Integer getIdAzienda() {
        return idAzienda;
    }

    public void setIdAzienda(Integer idAzienda) {
        this.idAzienda = idAzienda;
    }

    public String getFromName() {
        return fromName;
    }

    public void setFromName(String fromName) {
        this.fromName = fromName;
    }

    public String getSubject() {
        return Subject;
    }

    public void setSubject(String Subject) {
        this.Subject = Subject;
    }

    public List<String> getTo() {
        return To;
    }

    public void setTo(List<String> To) {
        this.To = To;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public List<String> getCc() {
        return cc;
    }

    public void setCc(List<String> cc) {
        this.cc = cc;
    }

    public List<String> getBcc() {
        return bcc;
    }

    public void setBcc(List<String> bcc) {
        this.bcc = bcc;
    }

    public MultipartFile[] getAttachments() {
        return attachments;
    }

    public void setAttachments(MultipartFile[] attachments) {
        this.attachments = attachments;
    }

    public List<String> getReplyTo() {
        return replyTo;
    }

    public void setReplyTo(List<String> replyTo) {
        this.replyTo = replyTo;
    }
}
