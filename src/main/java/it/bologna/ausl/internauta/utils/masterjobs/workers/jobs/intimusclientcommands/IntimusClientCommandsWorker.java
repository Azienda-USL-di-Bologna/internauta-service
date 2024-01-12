package it.bologna.ausl.internauta.utils.masterjobs.workers.jobs.intimusclientcommands;

import it.bologna.ausl.internauta.service.exceptions.intimus.IntimusSendCommandException;
import it.bologna.ausl.internauta.service.utils.IntimusUtils;
import it.bologna.ausl.internauta.service.utils.IntimusUtils.IntimusCommand;
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
public class IntimusClientCommandsWorker extends JobWorker<IntimusClientCommandsWorkerData, JobWorkerResult> {
    private static Logger log = LoggerFactory.getLogger(IntimusClientCommandsWorker.class);
    
    @Autowired
    private IntimusUtils intimusUtils;
    
    private String name = IntimusClientCommandsWorker.class.getSimpleName();
    
    @Override
    public String getName() {
        return this.name;
    }
    
    @Override
    public JobWorkerResult doRealWork() throws MasterjobsWorkerException {
        log.info(String.format("avvio del job %s", getName()));
        IntimusClientCommandsWorkerData data = getWorkerData();
        IntimusCommand buildedCommand = null;
        switch (data.getCommand()) {
            case RefreshAttivita:
                buildedCommand = intimusUtils.buildRefreshAttivitaCommand(
                        data.getIdPersona(),
                        (Integer) data.getParams().get("id_attivita"),
                        data.getParams().get("operation").toString());
                break;
            case RefreshMails:
                buildedCommand = intimusUtils.buildRefreshMailsCommand(
                        data.getParams());
                break;
        }
        if (buildedCommand != null) {
            try {
                intimusUtils.sendCommand(buildedCommand);
            } catch (IntimusSendCommandException ex) {
                log.info(ex.getMessage());
            }
        }
        return null;
    }
    
}
