package it.bologna.ausl.internauta.utils.masterjobs.workers.jobs.utils;

import it.bologna.ausl.internauta.utils.masterjobs.MasterjobsObjectsFactory;
import it.bologna.ausl.internauta.utils.masterjobs.exceptions.MasterjobsQueuingException;
import it.bologna.ausl.internauta.utils.masterjobs.exceptions.MasterjobsWorkerException;
import it.bologna.ausl.internauta.utils.masterjobs.workers.jobs.MasterjobsJobsQueuer;
import it.bologna.ausl.internauta.utils.masterjobs.workers.jobs.MultiJobQueueDescriptor;
import it.bologna.ausl.internauta.utils.masterjobs.workers.jobs.calcolapersonevedentidaarchivi.CalcolaPersoneVedentiDaArchiviRadiceJobWorker;
import it.bologna.ausl.internauta.utils.masterjobs.workers.jobs.calcolapersonevedentidaarchivi.CalcolaPersoneVedentiDaArchiviRadiceJobWorkerData;
import it.bologna.ausl.internauta.utils.masterjobs.workers.jobs.calcolapersonevedentidoc.CalcolaPersoneVedentiDocJobWorker;
import it.bologna.ausl.internauta.utils.masterjobs.workers.jobs.calcolapersonevedentidoc.CalcolaPersoneVedentiDocJobWorkerData;
import it.bologna.ausl.internauta.utils.masterjobs.workers.jobs.calcolopermessiarchivio.CalcoloPermessiArchivioJobWorker;
import it.bologna.ausl.internauta.utils.masterjobs.workers.jobs.calcolopermessiarchivio.CalcoloPermessiArchivioJobWorkerData;
import it.bologna.ausl.internauta.utils.masterjobs.workers.jobs.calcolopermessigerarchiaarchivio.CalcoloPermessiGerarchiaArchivioJobWorker;
import it.bologna.ausl.internauta.utils.masterjobs.workers.jobs.calcolopermessigerarchiaarchivio.CalcoloPermessiGerarchiaArchivioJobWorkerData;
import it.bologna.ausl.internauta.utils.masterjobs.workers.jobs.versatore.VersatoreJobWorker;
import it.bologna.ausl.internauta.utils.masterjobs.workers.jobs.versatore.VersatoreJobWorkerData;
import it.bologna.ausl.model.entities.configurazione.Applicazione;
import it.bologna.ausl.model.entities.versatore.SessioneVersamento;
import it.bologna.ausl.model.entities.masterjobs.Set.SetPriority;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author gusgus
 */
public class AccodatoreVeloce {
    private static final Logger log = LoggerFactory.getLogger(AccodatoreVeloce.class);
    private final MasterjobsJobsQueuer masterjobsJobsQueuer;
    private final MasterjobsObjectsFactory masterjobsObjectsFactory;

    public AccodatoreVeloce(MasterjobsJobsQueuer masterjobsJobsQueuer, MasterjobsObjectsFactory masterjobsObjectsFactory) {
        this.masterjobsJobsQueuer = masterjobsJobsQueuer;
        this.masterjobsObjectsFactory = masterjobsObjectsFactory;
    }
    
    public void accodaCalcolaPersoneVedentiDoc(List<Integer> idDocs) throws MasterjobsWorkerException {
        List<MultiJobQueueDescriptor> toQueue = new ArrayList<>();
        for (Integer idDoc : idDocs) {
            CalcolaPersoneVedentiDocJobWorkerData workerData = new CalcolaPersoneVedentiDocJobWorkerData(idDoc);
            CalcolaPersoneVedentiDocJobWorker worker = masterjobsObjectsFactory.getJobWorker(
                CalcolaPersoneVedentiDocJobWorker.class, 
                workerData, 
                false
            );
            toQueue.add(
                MultiJobQueueDescriptor
                    .newBuilder()
                    .addWorker(worker)
                    .waitForObject(false)
                    .skipIfAlreadyPresent(true)
                    .priority(SetPriority.NORMAL)
                    .build()
            );
        }
        
        try {
            masterjobsJobsQueuer.queueMultiJobs(toQueue, null);
        } catch (MasterjobsQueuingException ex) {
            String errorMessage = String.format("Errore nell'accodamento di %s", CalcolaPersoneVedentiDocJobWorker.class.getSimpleName());
            log.error(errorMessage, ex);
            throw new MasterjobsWorkerException(errorMessage, ex);
        }
    }
    
    public void accodaCalcolaPersoneVedentiDoc(Integer idDoc) throws MasterjobsWorkerException {
        accodaCalcolaPersoneVedentiDoc(idDoc, null, null, null);
    }
   
    public void accodaCalcolaPersoneVedentiDoc(Integer idDoc, String objectId, String objectType, Applicazione applicazione) throws MasterjobsWorkerException {
        CalcolaPersoneVedentiDocJobWorkerData calcolaPersoneVedentiDocJobWorkerData = new CalcolaPersoneVedentiDocJobWorkerData(idDoc);
        CalcolaPersoneVedentiDocJobWorker jobWorker = masterjobsObjectsFactory.getJobWorker(
                CalcolaPersoneVedentiDocJobWorker.class, 
                calcolaPersoneVedentiDocJobWorkerData, 
                false
        );
        try {
            String app = null;
            if (applicazione != null) app = applicazione.getId();
            masterjobsJobsQueuer.queueInJobsNotified(
                    jobWorker,
                    objectId, // ObjectID 
                    objectType, 
                    app, 
                    false, // waitForObject
                    SetPriority.HIGHEST,
                    true
            );
        } catch (MasterjobsQueuingException ex) {
            String errorMessage = String.format("Errore nell'accodamento di %s", CalcolaPersoneVedentiDocJobWorker.class.getSimpleName());
            log.error(errorMessage, ex);
            throw new MasterjobsWorkerException(errorMessage, ex);
        }
    }
    
    public void accodaCalcolaPermessiGerarchiaArchivio(Integer idArchivioRadice, String objectId, String objectType, Applicazione applicazione) throws MasterjobsWorkerException {
        CalcoloPermessiGerarchiaArchivioJobWorker worker = masterjobsObjectsFactory.getJobWorker(
                    CalcoloPermessiGerarchiaArchivioJobWorker.class,
                    new CalcoloPermessiGerarchiaArchivioJobWorkerData(idArchivioRadice),
                    false
        );
        try {
            String app = null;
            if (applicazione != null) app = applicazione.getId();
            masterjobsJobsQueuer.queueInJobsNotified(
                    worker, 
                    objectId, 
                    objectType, 
                    app, 
                    true, 
                    SetPriority.HIGHEST,
                    true
            );
        } catch (MasterjobsQueuingException ex) {
            String errorMessage = "Errore nell'accodamento del job CalcoloPermessiGerarchiaArchivio";
            log.error(errorMessage);
            throw new MasterjobsWorkerException(errorMessage, ex);
        }
    }
    
    public void accodaCalcolaPermessiArchivio(Integer idArchivio, String objectId, String objectType) throws MasterjobsWorkerException {
        accodaCalcolaPermessiArchivio(idArchivio, objectId, objectType, null);
    }
    
    public void accodaCalcolaPermessiArchivio(Integer idArchivio, String objectId, String objectType, String app) throws MasterjobsWorkerException {
         accodaCalcolaPermessiArchivio(idArchivio, objectId, objectType, app, false);
    }
    
    public void accodaCalcolaPermessiArchivio(Integer idArchivio, String objectId, String objectType, String app, Boolean queueJobCalcolaPersoneVedentiDoc) throws MasterjobsWorkerException {
        CalcoloPermessiArchivioJobWorker worker = masterjobsObjectsFactory.getJobWorker(CalcoloPermessiArchivioJobWorker.class,
                    new CalcoloPermessiArchivioJobWorkerData(idArchivio, queueJobCalcolaPersoneVedentiDoc),
                    false
        );
        try {
            masterjobsJobsQueuer.queueInJobsNotified(
                worker, 
                objectId, 
                objectType, 
                app, 
                false, 
                SetPriority.NORMAL,
                true
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
            String app = null;
            if (applicazione != null) app = applicazione.getId();
            masterjobsJobsQueuer.queueInJobsNotified(
                    jobWorker,
                    objectId, 
                    objectType, 
                    app, 
                    true, // waitForObject
                    SetPriority.NORMAL,
                    true
            );
        } catch (MasterjobsQueuingException ex) {
            String errorMessage = String.format("Errore nell'accodamento di %s", CalcolaPersoneVedentiDaArchiviRadiceJobWorker.class.getSimpleName());
            log.error(errorMessage, ex);
            throw new MasterjobsWorkerException(errorMessage, ex);
        }
    }
    public void accodaVersatore(
            List<Integer> idDocsDaVersare,
            Integer idAzienda,
            String hostId,
            SessioneVersamento.TipologiaVersamento tipologiaVersamento,
            Integer idPersonaForzatura,
            Integer poolSize, 
            Map<String, Object> params
    ) throws MasterjobsWorkerException {
        VersatoreJobWorkerData versatoreJobWorkerData = new VersatoreJobWorkerData(idAzienda, hostId, tipologiaVersamento, poolSize, idPersonaForzatura, params, idDocsDaVersare);
        VersatoreJobWorker jobWorker = null;
        try { // istanzia il woker
            jobWorker = masterjobsObjectsFactory.getJobWorker(VersatoreJobWorker.class, versatoreJobWorkerData, false);
        } catch (Exception ex) {
            String errorMessage = "errore nella creazione del job Versatore";
            log.error(errorMessage, ex);
        }
        try {
            masterjobsJobsQueuer.queueInJobsNotified(jobWorker, "versatore_" + idAzienda, "Versatore", null, true, SetPriority.NORMAL, false);
        } catch (Exception ex) {
            String errorMessage = "errore nell'accodamento del job Versatore";
            log.error(errorMessage, ex);
            throw new MasterjobsWorkerException(errorMessage, ex);
        }
    }
}
