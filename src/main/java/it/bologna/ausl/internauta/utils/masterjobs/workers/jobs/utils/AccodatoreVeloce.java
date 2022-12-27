package it.bologna.ausl.internauta.utils.masterjobs.workers.jobs.utils;

import it.bologna.ausl.internauta.utils.masterjobs.MasterjobsObjectsFactory;
import it.bologna.ausl.internauta.utils.masterjobs.exceptions.MasterjobsQueuingException;
import it.bologna.ausl.internauta.utils.masterjobs.exceptions.MasterjobsWorkerException;
import it.bologna.ausl.internauta.utils.masterjobs.workers.jobs.MasterjobsJobsQueuer;
import it.bologna.ausl.internauta.utils.masterjobs.workers.jobs.calcolapersonevedentidaarchivi.CalcolaPersoneVedentiDaArchiviRadiceJobWorker;
import it.bologna.ausl.internauta.utils.masterjobs.workers.jobs.calcolapersonevedentidaarchivi.CalcolaPersoneVedentiDaArchiviRadiceJobWorkerData;
import it.bologna.ausl.internauta.utils.masterjobs.workers.jobs.calcolapersonevedentidoc.CalcolaPersoneVedentiDocJobWorker;
import it.bologna.ausl.internauta.utils.masterjobs.workers.jobs.calcolapersonevedentidoc.CalcolaPersoneVedentiDocJobWorkerData;
import it.bologna.ausl.internauta.utils.masterjobs.workers.jobs.calcolopermessiarchivio.CalcoloPermessiArchivioJobWorker;
import it.bologna.ausl.internauta.utils.masterjobs.workers.jobs.calcolopermessiarchivio.CalcoloPermessiArchivioJobWorkerData;
import it.bologna.ausl.model.entities.configurazione.Applicazione;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 *
 * @author gusgus
 */
@Component
public class AccodatoreVeloce {
    private static final Logger log = LoggerFactory.getLogger(AccodatoreVeloce.class);
    
    @Autowired
    private MasterjobsJobsQueuer masterjobsJobsQueuer;
    
    @Autowired
    private MasterjobsObjectsFactory masterjobsObjectsFactory;
    
    public void accodaCalcolaPersoneVedentiDoc(Integer idDoc) throws MasterjobsWorkerException {
        CalcolaPersoneVedentiDocJobWorkerData calcolaPersoneVedentiDocJobWorkerData = new CalcolaPersoneVedentiDocJobWorkerData(idDoc);
        CalcolaPersoneVedentiDocJobWorker jobWorker = masterjobsObjectsFactory.getJobWorker(
                CalcolaPersoneVedentiDocJobWorker.class, 
                calcolaPersoneVedentiDocJobWorkerData, 
                false
        );
        try {
            masterjobsJobsQueuer.queue(
                    jobWorker,
                    null, // ObjectID 
                    null, 
                    null, 
                    false, // waitForObject
                    it.bologna.ausl.model.entities.masterjobs.Set.SetPriority.HIGHEST
            );
        } catch (MasterjobsQueuingException ex) {
            String errorMessage = String.format("Errore nell'accodamento di %s", CalcolaPersoneVedentiDocJobWorker.class.getSimpleName());
            log.error(errorMessage, ex);
            throw new MasterjobsWorkerException(errorMessage, ex);
        }
    }
    
    public void accodaCalcolaPermessiArchivio(Integer idArchivioRadice, String objectId, String objectType, Applicazione applicazione) throws MasterjobsWorkerException {
        CalcoloPermessiArchivioJobWorker worker = masterjobsObjectsFactory.getJobWorker(
                    CalcoloPermessiArchivioJobWorker.class,
                    new CalcoloPermessiArchivioJobWorkerData(idArchivioRadice),
                    false
        );
        try {
            masterjobsJobsQueuer.queue(
                    worker, 
                    idArchivioRadice.toString(), 
                    objectType, 
                    applicazione.getId(), 
                    false, 
                    it.bologna.ausl.model.entities.masterjobs.Set.SetPriority.HIGHEST
            );
        } catch (MasterjobsQueuingException ex) {
            String errorMessage = "Errore nell'accodamento del job CalcoloPermessiArchivio";
            log.error(errorMessage);
            throw new MasterjobsWorkerException(errorMessage, ex);
        }
    }
    
    public void accodaCalcolaPersoneVedentiDaArchiviRadice(Set<Integer> idArchiviRadiceDaPermessizzare, String objectId, String objectType, Applicazione applicazione) throws MasterjobsWorkerException {
        CalcolaPersoneVedentiDaArchiviRadiceJobWorkerData calcolaPersoneVedentiDaArchiviRadiceJobWorkerData = new CalcolaPersoneVedentiDaArchiviRadiceJobWorkerData(idArchiviRadiceDaPermessizzare);
        CalcolaPersoneVedentiDaArchiviRadiceJobWorker jobWorker = masterjobsObjectsFactory.getJobWorker(
                CalcolaPersoneVedentiDaArchiviRadiceJobWorker.class, 
                calcolaPersoneVedentiDaArchiviRadiceJobWorkerData, 
                false
        );
        try {
            masterjobsJobsQueuer.queue(
                    jobWorker,
                    objectId, 
                    objectType, 
                    applicazione.getId(), 
                    true, // waitForObject
                    it.bologna.ausl.model.entities.masterjobs.Set.SetPriority.HIGHEST
            );
        } catch (MasterjobsQueuingException ex) {
            String errorMessage = String.format("Errore nell'accodamento di %s", CalcolaPersoneVedentiDaArchiviRadiceJobWorker.class.getSimpleName());
            log.error(errorMessage, ex);
            throw new MasterjobsWorkerException(errorMessage, ex);
        }
    }
}
