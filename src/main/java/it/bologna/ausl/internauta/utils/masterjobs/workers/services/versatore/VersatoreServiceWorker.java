package it.bologna.ausl.internauta.utils.masterjobs.workers.services.versatore;

import com.fasterxml.jackson.core.type.TypeReference;
import it.bologna.ausl.internauta.service.utils.CachedEntities;
import it.bologna.ausl.internauta.utils.masterjobs.annotations.MasterjobsWorker;
import it.bologna.ausl.internauta.utils.masterjobs.exceptions.MasterjobsWorkerException;
import it.bologna.ausl.internauta.utils.masterjobs.workers.WorkerResult;
import it.bologna.ausl.internauta.utils.masterjobs.workers.services.ServiceWorker;
import it.bologna.ausl.internauta.utils.masterjobs.workers.services.VersatoreServiceCore;
import it.bologna.ausl.internauta.utils.parameters.manager.ParametriAziendeReader;
import it.bologna.ausl.internauta.utils.parameters.manager.ParametriAziendeReader.ParametriAzienda;
import it.bologna.ausl.model.entities.configurazione.Applicazione;
import it.bologna.ausl.model.entities.configurazione.ParametroAziende;
import it.bologna.ausl.model.entities.versatore.SessioneVersamento;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

/**
 *
 * @author gdm
 * 
 * Classe che implementa il servizio del versatore.
 * Si occupa di leggere i parametri da configurazione.parametri_azienda e richiamare il core del servizio 
 * (che è la parte in cui si accodano effettivamente i mestieri di versamento) che è definita nella classe VersatoreServiceCore
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
        /* cicla su tutti i parametri "versatoreConfiguration" e crea una mappa dove per ogni idAzienda ne inserisce i parametri
         * NB: se un'azienda è presente più volte (caso che non dovrebbe capitare) i suoi paraemtri sarannò gli ultimi letti
        */
        Map<Integer, Map<String, Object>> aziendeAttiveConParametri = new HashMap<>();
        List<ParametroAziende> parameters = parametriAziendaReader.getParameters(ParametriAzienda.versatoreConfiguration);
        parameters.stream().filter(p -> 
            ((Boolean)parametriAziendaReader.getValue(p, new TypeReference<Map<String, Object>>(){}).get("active")) == true
        ).forEach(p -> {
            Integer[] aziende;
            // se nel parametro non ci sono aziende, allora il parametro è inteso per tutte
            if (p.getIdAziende() == null || p.getIdAziende().length == 0) {
                aziende = cachedEntities.getAllAziende().stream().map(a -> a.getId()).toArray(Integer[]::new);
            } else { // se ci sono le aziende allora metto nella mappa il parametro per le aziende indicate
                aziende = p.getIdAziende();
            }
            Stream.of(p.getIdAziende()).forEach(
                    idAzienda -> aziendeAttiveConParametri.put(
                            idAzienda, parametriAziendaReader.getValue(p, new TypeReference<Map<String, Object>>(){})
                    )
            );
        });
        
        // richiama il metodo del core per effettuare l'accodamento del mestiere di verrsamento
        VersatoreServiceCore versatoreServiceCore = new VersatoreServiceCore(masterjobsJobsQueuer, masterjobsObjectsFactory);
        versatoreServiceCore.queueVersatoreJobs(SessioneVersamento.AzioneVersamento.VERSAMENTO, 
                aziendeAttiveConParametri, Applicazione.Applicazioni.scripta.toString());
        return null;
    }
}
