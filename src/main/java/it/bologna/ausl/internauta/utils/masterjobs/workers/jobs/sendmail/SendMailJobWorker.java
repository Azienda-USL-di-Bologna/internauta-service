package it.bologna.ausl.internauta.utils.masterjobs.workers.jobs.sendmail;

import it.bologna.ausl.internauta.service.utils.SimpleMailSenderUtility;
import it.bologna.ausl.internauta.utils.masterjobs.annotations.MasterjobsWorker;
import it.bologna.ausl.internauta.utils.masterjobs.exceptions.MasterjobsWorkerException;
import it.bologna.ausl.internauta.utils.masterjobs.workers.jobs.JobWorker;
import it.bologna.ausl.internauta.utils.masterjobs.workers.jobs.JobWorkerResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Invia una mail leggendo i parametri aziendali
 * @author gdm
 */
@MasterjobsWorker
public class SendMailJobWorker extends JobWorker<SendMailJobWorkerData, JobWorkerResult> {
    private static Logger log = LoggerFactory.getLogger(SendMailJobWorker.class);
    
    @Autowired
    private SimpleMailSenderUtility simpleMailSenderUtility;
    
    private String name = SendMailJobWorker.class.getSimpleName();
    
    @Override
    public String getName() {
        return this.name;
    }
    
    @Override
    public JobWorkerResult doRealWork() throws MasterjobsWorkerException {
        log.info("sono il " + getName() + " e sto funzionando...");
        SendMailJobWorkerData data = getWorkerData();
        Boolean mailSent = false;
        try {
            mailSent = simpleMailSenderUtility.sendMail(
                    data.getIdAzienda(),
                    data.getFromName(),
                    data.getSubject(),
                    data.getTo(),
                    data.getBody(),
                    data.getCc(),
                    data.getBcc(),
                    data.getAttachments(),
                    data.getReplyTo());
        } catch (Exception ex) {
            String errorMessage = "errore nell'invio della mail";
            log.error(errorMessage, ex);
            throw new MasterjobsWorkerException(errorMessage, ex);
        }
        if (!mailSent) {
            throw new MasterjobsWorkerException("la sendMail() ha tornato false");
        }
        return null;
    }
}
