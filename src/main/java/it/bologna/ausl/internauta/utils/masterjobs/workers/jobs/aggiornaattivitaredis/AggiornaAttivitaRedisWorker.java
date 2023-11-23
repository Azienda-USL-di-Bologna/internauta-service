package it.bologna.ausl.internauta.utils.masterjobs.workers.jobs.aggiornaattivitaredis;

import it.bologna.ausl.internauta.service.exceptions.intimus.IntimusSendCommandException;
import it.bologna.ausl.internauta.service.utils.IntimusUtils;
import it.bologna.ausl.internauta.utils.masterjobs.annotations.MasterjobsWorker;
import it.bologna.ausl.internauta.utils.masterjobs.exceptions.MasterjobsWorkerException;
import it.bologna.ausl.internauta.utils.masterjobs.workers.jobs.JobWorker;
import it.bologna.ausl.internauta.utils.masterjobs.workers.jobs.JobWorkerResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

/** 
 * @author conte
 */
@MasterjobsWorker
public class AggiornaAttivitaRedisWorker extends JobWorker<AggiornaAttivitaRedisWorkerData, JobWorkerResult> {
    private static Logger log = LoggerFactory.getLogger(AggiornaAttivitaRedisWorker.class);
    
    @Autowired
    private IntimusUtils intimusUtils;
    
    private String name = AggiornaAttivitaRedisWorker.class.getSimpleName();
    
    @Override
    public String getName() {
        return this.name;
    }
    
    @Override
    public JobWorkerResult doRealWork() throws MasterjobsWorkerException {
        log.info(String.format("avvio del job %s", getName()));
        AggiornaAttivitaRedisWorkerData data = getWorkerData();
        try {
            intimusUtils.sendCommand(
                    intimusUtils.buildRefreshAttivitaCommand(
                            data.getIdPersona(),
                            data.getIdAttivita(),
                            data.getOperation())
            );
        } catch (IntimusSendCommandException ex) {
            log.info(ex.getMessage());
        }
//        
//        
//        
//        
//        
//        
//        
//        
//        
//        
//        
//        
//        
//        
//        
//        
//        
//        
//        
//        
//        AggiornaAttivitaRedisWorkerData data = getWorkerData();
//        Boolean mailSent = false;
//        
//        Map<String, String> activityMailTemplate = getActivityTemplate();
//        
//        String subject = activityMailTemplate.get("subject");
//        String body;
//        try {
//            body = buildBody(activityMailTemplate);
//        } catch (Exception ex) {
//            String errorMessage = "errore nella creazione del body della mail";
//            log.error(errorMessage, ex);
//            throw new MasterjobsWorkerException(errorMessage, ex);
//        }
//        String fromAlias = activityMailTemplate.get("fromAlias");
//        
//        try {
//            mailSent = simpleMailSenderUtility.sendMail(
//                    data.getIdAzienda(),
//                    fromAlias,
//                    subject,
//                    data.getTo(),
//                    body,
//                    null,
//                    null,
//                    null,
//                    null,
//                    true);
//        } catch (Exception ex) {
//            String errorMessage = "errore nell'invio della mail";
//            log.error(errorMessage, ex);
//            throw new MasterjobsWorkerException(errorMessage, ex);
//        }
//        if (!mailSent) {
//            throw new MasterjobsWorkerException("la sendMail() ha tornato false");
//        }
        return null;
    }
    
}
