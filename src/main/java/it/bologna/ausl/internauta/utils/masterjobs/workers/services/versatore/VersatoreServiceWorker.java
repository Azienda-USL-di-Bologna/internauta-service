package it.bologna.ausl.internauta.utils.masterjobs.workers.services.versatore;

import it.bologna.ausl.internauta.service.utils.CachedEntities;
import it.bologna.ausl.internauta.utils.masterjobs.annotations.MasterjobsWorker;
import it.bologna.ausl.internauta.utils.masterjobs.exceptions.MasterjobsWorkerException;
import it.bologna.ausl.internauta.utils.masterjobs.workers.WorkerResult;
import it.bologna.ausl.internauta.utils.masterjobs.workers.services.ServiceWorker;
import it.bologna.ausl.internauta.utils.masterjobs.workers.services.VersamentoServiceCore;
import it.bologna.ausl.internauta.utils.parameters.manager.ParametriAziendeReader;
import it.bologna.ausl.model.entities.configurazione.Applicazione;
import it.bologna.ausl.model.entities.versatore.SessioneVersamento;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

/**
 *
 * @author gdm
 
 Classe che implementa il servizio del versatore.
 Si occupa di leggere i parametri da configurazione.parametri_azienda e richiamare il core del servizio 
 (la parte in cui si accodano effettivamente i mestieri di versamento) che è definita nella classe VersamentoServiceCore
 */
@MasterjobsWorker
public class VersatoreServiceWorker extends ServiceWorker {
    private static Logger log = LoggerFactory.getLogger(VersatoreServiceWorker.class);
    
    private String name = VersatoreServiceWorker.class.getSimpleName();
    
    @Autowired
    private CachedEntities cachedEntities;
    
    @Autowired
    private ParametriAziendeReader parametriAziendaReader;
    
    @Override
    public String getName() {
        return this.name;
    }
    
    @Override
    public WorkerResult doWork() throws MasterjobsWorkerException {
        
        Map<Integer, Map<String, Object>> aziendeAttiveConParametri = VersatoreServiceUtils.getAziendeAttiveConParametri(parametriAziendaReader, cachedEntities);
        
        // richiama il metodo del core per effettuare l'accodamento del mestiere di versamento
        VersamentoServiceCore versatoreServiceCore = new VersamentoServiceCore(masterjobsJobsQueuer, masterjobsObjectsFactory);
        versatoreServiceCore.queueVersatoreJobs(aziendeAttiveConParametri, Applicazione.Applicazioni.scripta.toString());
        return null;
    }
}
