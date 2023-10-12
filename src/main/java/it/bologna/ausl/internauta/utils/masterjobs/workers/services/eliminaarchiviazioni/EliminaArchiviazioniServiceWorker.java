package it.bologna.ausl.internauta.utils.masterjobs.workers.services.eliminaarchiviazioni;

import it.bologna.ausl.internauta.utils.masterjobs.workers.services.lanciatrasformatore.*;
import com.fasterxml.jackson.core.type.TypeReference;
import it.bologna.ausl.internauta.service.utils.CachedEntities;
import it.bologna.ausl.internauta.utils.masterjobs.annotations.MasterjobsWorker;
import it.bologna.ausl.internauta.utils.masterjobs.exceptions.MasterjobsQueuingException;
import it.bologna.ausl.internauta.utils.masterjobs.exceptions.MasterjobsWorkerException;
import it.bologna.ausl.internauta.utils.masterjobs.workers.WorkerResult;
import it.bologna.ausl.internauta.utils.masterjobs.workers.jobs.eliminaarchiviazioni.EliminaArchiviazioniJobWorker;
import it.bologna.ausl.internauta.utils.masterjobs.workers.jobs.eliminaarchiviazioni.EliminaArchiviazioniJobWorkerData;
import it.bologna.ausl.internauta.utils.masterjobs.workers.jobs.lanciatrasformatore.LanciaTrasformatoreJobWorker;
import it.bologna.ausl.internauta.utils.masterjobs.workers.jobs.lanciatrasformatore.LanciaTrasformatoreJobWorkerData;
import it.bologna.ausl.internauta.utils.masterjobs.workers.services.ServiceWorker;
import it.bologna.ausl.internauta.utils.parameters.manager.ParametriAziendeReader;
import it.bologna.ausl.model.entities.baborg.Azienda;
import it.bologna.ausl.model.entities.baborg.Persona;
import it.bologna.ausl.model.entities.baborg.Utente;
import it.bologna.ausl.model.entities.configurazione.Applicazione;
import it.bologna.ausl.model.entities.configurazione.ParametroAziende;
import it.bologna.ausl.model.entities.masterjobs.Set;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

/**
 *
 * @author conte
 */
@MasterjobsWorker
public class EliminaArchiviazioniServiceWorker extends ServiceWorker {
    private static Logger log = LoggerFactory.getLogger(EliminaArchiviazioniServiceWorker.class);

    public static final String ELIMINA_ARCHIVIAZIONI = "elimina_archiviazioni";
    
    
    @Autowired
    private CachedEntities cachedEntities;
    
    @Autowired
    private ParametriAziendeReader parametriAziende;    
    
    @Override
    public String getName() {
        return getClass().getSimpleName();
    }
    
    @Override
    public WorkerResult doWork() throws MasterjobsWorkerException {
        log.info(String.format("starting %s...", getName()));
        List<Azienda> allAziende = cachedEntities.getAllAziende();
        Integer tempoEliminaArchiviazioni = null;
        for (Azienda azienda : allAziende) {
            List<ParametroAziende> parameters = parametriAziende.getParameters("tempoEliminaArchiviazioni", new Integer[]{azienda.getId()});
            if (parameters != null && !parameters.isEmpty()) {
                tempoEliminaArchiviazioni = parametriAziende.getValue(parameters.get(0), Integer.class);
                log.info("il tempo di permanenza delle archiviazioni logicamente eliminate è di {} giorni", tempoEliminaArchiviazioni);
                if (tempoEliminaArchiviazioni >= 0) {
                    EliminaArchiviazioniJobWorkerData eliminaArchiviazioniJobWorkerData = new EliminaArchiviazioniJobWorkerData(azienda.getId(), tempoEliminaArchiviazioni, "servizio Notturno");
                    EliminaArchiviazioniJobWorker jobWorker = super.masterjobsObjectsFactory.getJobWorker(EliminaArchiviazioniJobWorker.class, eliminaArchiviazioniJobWorkerData, false);
                    try {
                        super.masterjobsJobsQueuer.queue(jobWorker, null, null, Applicazione.Applicazioni.gedi.toString(), false, Set.SetPriority.NORMAL);

                    } catch (MasterjobsQueuingException ex) {
                        String errorMessage = "errore nell'accodamento del job di trasformazione";
                        log.error(errorMessage);
                        throw new MasterjobsWorkerException(errorMessage, ex);
                    }
                }
            }
        }
        log.info(String.format("end %s...", getName()));
        return null;
    }
    
}
