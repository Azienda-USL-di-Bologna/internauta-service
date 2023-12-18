package it.bologna.ausl.internauta.utils.masterjobs.workers.services.automatismifineanno;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.bologna.ausl.internauta.service.repositories.scripta.ArchivioRepository;
import it.bologna.ausl.internauta.service.utils.CachedEntities;
import it.bologna.ausl.internauta.utils.masterjobs.annotations.MasterjobsWorker;
import it.bologna.ausl.internauta.utils.masterjobs.exceptions.MasterjobsQueuingException;
import it.bologna.ausl.internauta.utils.masterjobs.exceptions.MasterjobsWorkerException;
import it.bologna.ausl.internauta.utils.masterjobs.workers.WorkerResult;
import it.bologna.ausl.internauta.utils.masterjobs.workers.jobs.automatismifineanno.AutomatismiFineAnnoJobWorker;
import it.bologna.ausl.internauta.utils.masterjobs.workers.jobs.automatismifineanno.AutomatismiFineAnnoJobWorkerData;
import it.bologna.ausl.internauta.utils.masterjobs.workers.services.ServiceWorker;
import it.bologna.ausl.internauta.utils.parameters.manager.ParametriAziendeReader;
import it.bologna.ausl.model.entities.baborg.Azienda;
import it.bologna.ausl.model.entities.configurazione.Applicazione;
import it.bologna.ausl.model.entities.configurazione.ParametroAziende;
import it.bologna.ausl.model.entities.masterjobs.Set;
import it.bologna.ausl.model.entities.scripta.Archivio;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

/**
 *
 * @author conte
 */
@MasterjobsWorker
public class AutomatismiFineAnnoServiceWorker extends ServiceWorker {
    private static final Logger log = LoggerFactory.getLogger(AutomatismiFineAnnoServiceWorker.class);
    
    public static final String AUTOMATISMI_FINE_ANNO = "automatismi_fine_anno";
    
    @Autowired
    private CachedEntities cachedEntities;
    
    @Autowired
    private ParametriAziendeReader parametriAziende;  
    
    @Autowired
    private ArchivioRepository archivioRepository;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @Override
    public String getName() {
        return getClass().getSimpleName();
    }
    
    @Override
    public WorkerResult doWork() throws MasterjobsWorkerException {
        log.info("starting {}...", getName());
        List<Azienda> allAziende = cachedEntities.getAllAziende();
        //TODO: deve avere usa gedi internauta a true
        // ciclio tutte le aziende per ottenerne i dati dei fascicoli speciali e, se non esistono già, crearli
        for (Azienda azienda : allAziende) {
            
            List<ParametroAziende> parametersDatiFascicoloSpeciale = parametriAziende.getParameters("datiFascicoloSpeciale", new Integer[]{azienda.getId()});
            if (parametersDatiFascicoloSpeciale != null && !parametersDatiFascicoloSpeciale.isEmpty()) {
                // ottengo il json contenente i dati per creare i fascicoli speciali
                Map<String, Object> parametriCreazioneFascicoliSpeciali = parametriAziende.getValue(parametersDatiFascicoloSpeciale.get(0), new TypeReference<Map<String, Object>>(){});
               // JSONObject datiFascicoloSpeciale = new JSONObject(parametersDatiFascicoloSpeciale.get(0).getValore());
                // mi assicuro che non esista già il fascicolo speciale per l'azienda che sto ciclando (con numerazione gerarchica 1/anno)
                //Integer anno = ZonedDateTime.now().getYear();
                Integer anno = 2024;
                Archivio archivioSpeciale = archivioRepository.findByNumerazioneGerarchicaAndIdAzienda( "1/" + anno.toString(), azienda.getId());
                if (archivioSpeciale == null ){
                    // ottengo la lista di nomi dei fascicoli e la converto in una Map<String, String> da passare poi al JobWorkerData
                    Map<String, String> nomeFascicoliSpecialiMap = objectMapper.convertValue(parametriCreazioneFascicoliSpeciali.get("nomeFascicoliSpeciali"),new TypeReference<Map<String,String>>(){});
                    
//                    JSONObject nomeFascicoliSpeciali = datiFascicoloSpeciale.getJSONObject("nomeFascicoliSpeciali");
//                    Map<String, String> nomeFascicoliSpecialiMap = new HashMap<>();
//                    Map<String, Object> toMap = nomeFascicoliSpeciali.toMap();
//                    toMap.forEach((key, item) -> {
//                        nomeFascicoliSpecialiMap.put(key, item.toString());
//                    });
                    // qui sono sicuro che il faqscicolo speciale non esiste quindi procedo ad accodare il job per crearlo
                    AutomatismiFineAnnoJobWorkerData automatismiFineAnnoJobWorkerData = new AutomatismiFineAnnoJobWorkerData(
                            azienda.getId(),
                            Integer.valueOf(parametriCreazioneFascicoliSpeciali.get("idUtenteResponsabileFascicoloSpeciale").toString()),
                            Integer.valueOf(parametriCreazioneFascicoliSpeciali.get("idVicarioFascicoloSpeciale").toString()),
                            Integer.valueOf(parametriCreazioneFascicoliSpeciali.get("idClassificazioneFascSpeciale").toString()),
                            nomeFascicoliSpecialiMap
                    );
//                    AutomatismiFineAnnoJobWorkerData automatismiFineAnnoJobWorkerData = new AutomatismiFineAnnoJobWorkerData(
//                            azienda.getId(),
//                            datiFascicoloSpeciale.getInt("idUtenteResponsabileFascicoloSpeciale"),
//                            datiFascicoloSpeciale.getInt("idVicarioFascicoloSpeciale"),
//                            datiFascicoloSpeciale.getInt("idClassificazioneFascSpeciale"),
//                            nomeFascicoliSpecialiMap
//                    );
                    
                    AutomatismiFineAnnoJobWorker jobWorker = super.masterjobsObjectsFactory.getJobWorker(
                            AutomatismiFineAnnoJobWorker.class,
                            automatismiFineAnnoJobWorkerData,
                            false
                    );
                    try {
                        super.masterjobsJobsQueuer.queue(jobWorker, null, null, Applicazione.Applicazioni.scripta.toString(), false, Set.SetPriority.NORMAL, true,null);
                        log.info("ho accodato un job per l'azienda {}", azienda.getId());
                    } catch (MasterjobsQueuingException ex) {
                        String errorMessage = "errore nell'accodamento del job delle automazioni di fine anno";
                        log.error(errorMessage);
                        throw new MasterjobsWorkerException(errorMessage, ex);
                    }
                } else {
                    log.info("non accodo nessun job per l'azienda {} perché esiste già il fascicolo 1/{} con id {}", azienda.getId(), archivioSpeciale.getAnno(), archivioSpeciale.getId());
                }
            }
        }
        log.info("end {}...", getName());
        return null;
    }
}
