package it.bologna.ausl.internauta.utils.masterjobs.workers.services.versatore;

import com.fasterxml.jackson.core.type.TypeReference;
import it.bologna.ausl.internauta.utils.masterjobs.workers.services.foo.*;
import it.bologna.ausl.internauta.utils.masterjobs.annotations.MasterjobsWorker;
import it.bologna.ausl.internauta.utils.masterjobs.exceptions.MasterjobsWorkerException;
import it.bologna.ausl.internauta.utils.masterjobs.workers.WorkerResult;
import it.bologna.ausl.internauta.utils.masterjobs.workers.services.ServiceWorker;
import it.bologna.ausl.internauta.utils.masterjobs.workers.services.VersatoreServiceCore;
import it.bologna.ausl.internauta.utils.parameters.manager.ParametriAziendeReader;
import it.bologna.ausl.internauta.utils.parameters.manager.ParametriAziendeReader.ParametriAzienda;
import it.bologna.ausl.model.entities.configurazione.Applicazione;
import it.bologna.ausl.model.entities.configurazione.ParametroAziende;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

/**
 *
 * @author gdm
 */
@MasterjobsWorker
public class VersatoreServiceWorker extends ServiceWorker {
    private static Logger log = LoggerFactory.getLogger(VersatoreServiceWorker.class);
    
    private String name = VersatoreServiceWorker.class.getSimpleName();
    
    @Autowired
    private ParametriAziendeReader parametriAziendaReader;
    
    @Override
    public String getName() {
        return this.name;
    }
    
    @Override
    public WorkerResult doWork() throws MasterjobsWorkerException {
        
        List<ParametroAziende> parameters = parametriAziendaReader.getParameters(ParametriAzienda.versatoreConfiguration);
        Set<Integer> idAziendeAttive = new HashSet<>();
        parameters.stream().filter(p -> 
            ((Boolean)parametriAziendaReader.getValue(p, new TypeReference<Map<String, Object>>(){}).get("active")) == true
        ).forEach(p -> {
            idAziendeAttive.addAll(Arrays.asList(p.getIdAziende()));
        });
        
        for (Integer idAzienda : idAziendeAttive) {            
            ParametroAziende versatoreConfigAziendaParam = parametriAziendaReader.getParameters(ParametriAzienda.versatoreConfiguration, new Integer[]{idAzienda}).get(0);
            VersatoreServiceCore versatoreServiceCore = new VersatoreServiceCore(masterjobsJobsQueuer, masterjobsObjectsFactory);
            Map<String, Object> versatoreConfigAziendaValue = parametriAziendaReader.getValue(versatoreConfigAziendaParam, new TypeReference<Map<String, Object>>(){});
            String hostId = (String) versatoreConfigAziendaValue.get("hostId");
            Integer threadPoolSize = (Integer) versatoreConfigAziendaValue.get("threadPoolSize");
            
            versatoreServiceCore.queueVersatoreJob(idAzienda, hostId, false, null, threadPoolSize, Applicazione.Applicazioni.scripta.toString());
        }
        return null;
    }
}
