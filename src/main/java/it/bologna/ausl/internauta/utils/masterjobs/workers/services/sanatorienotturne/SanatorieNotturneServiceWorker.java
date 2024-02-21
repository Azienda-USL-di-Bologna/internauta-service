package it.bologna.ausl.internauta.utils.masterjobs.workers.services.sanatorienotturne;

import it.bologna.ausl.internauta.utils.masterjobs.annotations.MasterjobsWorker;
import it.bologna.ausl.internauta.utils.masterjobs.exceptions.MasterjobsWorkerException;
import it.bologna.ausl.internauta.utils.masterjobs.workers.WorkerResult;
import it.bologna.ausl.internauta.utils.masterjobs.workers.jobs.copiatrasferisciabilitazioniarchivi.CopiaTrasferisciAbilitazioniArchiviJobWorker;
import it.bologna.ausl.internauta.utils.masterjobs.workers.jobs.sanatoriacontatti.SanatoiaContattiJobWorkerData;
import it.bologna.ausl.internauta.utils.masterjobs.workers.jobs.sanatoriacontatti.SanatoriaContattiJobWorker;
import it.bologna.ausl.internauta.utils.masterjobs.workers.jobs.sanatoriapermessiveicolati.SanatoriaPermessiVeicolatiJobWorker;
import it.bologna.ausl.internauta.utils.masterjobs.workers.jobs.sanatoriapermessiveicolati.SanatoriaPermessiVeicolatiJobWorkerData;
import it.bologna.ausl.internauta.utils.masterjobs.workers.services.ServiceWorker;
import it.bologna.ausl.model.entities.configurazione.Applicazione;
import it.bologna.ausl.model.entities.masterjobs.Set;
import java.util.ArrayList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author gusgus
 */
@MasterjobsWorker
public class SanatorieNotturneServiceWorker extends ServiceWorker {
    private static Logger log = LoggerFactory.getLogger(SanatorieNotturneServiceWorker.class);
    
    private String name = SanatorieNotturneServiceWorker.class.getSimpleName();
    
    @Override
    public String getName() {
        return this.name;
    }
    
    //scrivere isexecutable dove controllo che il ribaltone abbia finito
    

    @Override
    public WorkerResult doWork() throws MasterjobsWorkerException {
        log.info("sono il " + getName() + " e sto funzionando...");
        
        SanatoiaContattiJobWorkerData sanatoiaContattiJobWorkerData = new SanatoiaContattiJobWorkerData();
        sanatoiaContattiJobWorkerData.setAspettaRibaltone(true);
        SanatoriaContattiJobWorker jobWorker = masterjobsObjectsFactory.getJobWorker(
                SanatoriaContattiJobWorker.class,
                sanatoiaContattiJobWorkerData,
                false
        );
        ArrayList arrayList = new ArrayList();
        arrayList.add(jobWorker);
        masterjobsJobsQueuer.queueOnCommit(
                arrayList,
                null,
                null,
                Applicazione.Applicazioni.rubrica.toString(),
                Boolean.FALSE,
                Set.SetPriority.NORMAL, 
                null);
        arrayList = new ArrayList();
        
        SanatoriaPermessiVeicolatiJobWorkerData sanatoriaPermessiVeicolatiJobWorkerData = new SanatoriaPermessiVeicolatiJobWorkerData();
        sanatoriaPermessiVeicolatiJobWorkerData.setAspettaRibaltone(true);
        SanatoriaPermessiVeicolatiJobWorker jobWorker1 = masterjobsObjectsFactory.getJobWorker(
                SanatoriaPermessiVeicolatiJobWorker.class,
                sanatoriaPermessiVeicolatiJobWorkerData,
                false
        );
        arrayList.add(jobWorker1);
        masterjobsJobsQueuer.queueOnCommit(
                arrayList,
                null,
                null,
                Applicazione.Applicazioni.rubrica.toString(),
                Boolean.FALSE,
                Set.SetPriority.NORMAL, 
                null);
        return null;
    }
}
