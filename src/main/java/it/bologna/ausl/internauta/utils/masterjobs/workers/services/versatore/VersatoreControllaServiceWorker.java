package it.bologna.ausl.internauta.utils.masterjobs.workers.services.versatore;

import it.bologna.ausl.internauta.service.utils.CachedEntities;
import it.bologna.ausl.internauta.utils.masterjobs.annotations.MasterjobsWorker;
import it.bologna.ausl.internauta.utils.masterjobs.exceptions.MasterjobsWorkerException;
import it.bologna.ausl.internauta.utils.masterjobs.workers.WorkerResult;
import it.bologna.ausl.internauta.utils.masterjobs.workers.services.ServiceWorker;
import it.bologna.ausl.internauta.utils.masterjobs.workers.services.VersatoreServiceCore;
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
 * 
 * Classe che implementa il servizio del controllo dei versamenti del versatore.
 * Si occupa di leggere i parametri da configurazione.parametri_azienda e richiamare il core del servizio 
 * (la parte in cui si accodano effettivamente i mestieri di versamento) che è definita nella classe VersatoreServiceCore
 */
@MasterjobsWorker
public class VersatoreControllaServiceWorker extends ServiceWorker {
    private static Logger log = LoggerFactory.getLogger(VersatoreControllaServiceWorker.class);
    
    private String name = VersatoreControllaServiceWorker.class.getSimpleName();
    
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
        
        // richiama il metodo del core per effettuare l'accodamento del mestiere di verrsamento
        VersatoreServiceCore versatoreServiceCore = new VersatoreServiceCore(masterjobsJobsQueuer, masterjobsObjectsFactory);
        versatoreServiceCore.queueVersatoreJobs(SessioneVersamento.AzioneVersamento.CONTROLLO_VERSAMENTO, 
                aziendeAttiveConParametri, Applicazione.Applicazioni.scripta.toString());
        return null;
    }
}
