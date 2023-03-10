package it.bologna.ausl.internauta.utils.masterjobs.workers.services.lanciatrasformatore;

import com.fasterxml.jackson.core.type.TypeReference;
import it.bologna.ausl.internauta.service.utils.CachedEntities;
import it.bologna.ausl.internauta.utils.masterjobs.annotations.MasterjobsWorker;
import it.bologna.ausl.internauta.utils.masterjobs.exceptions.MasterjobsQueuingException;
import it.bologna.ausl.internauta.utils.masterjobs.exceptions.MasterjobsWorkerException;
import it.bologna.ausl.internauta.utils.masterjobs.workers.WorkerResult;
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
 * @author mido
 */
@MasterjobsWorker
public class LanciaTrasformatoreServiceWorker extends ServiceWorker {
    private static Logger log = LoggerFactory.getLogger(LanciaTrasformatoreServiceWorker.class);

    public static final String LANCIA_TRASFORMATORE_PRO_RIBALTONE = "lancia_trasformatore_pro_ribaltone";
    
    
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
        Boolean trasformaPrimaDiRibaltare = null;
        String email = null;
        String fonteRibaltone = null;
        Persona persona = cachedEntities.getPersonaFromCodiceFiscale("RIBALTONE");
        for (Azienda azienda : allAziende) {
            Utente user = persona.getUtenteList().stream()
                    .filter(utente -> utente.getIdAzienda().getId().equals(azienda.getId())).findFirst().get();
            List<ParametroAziende> parameters = parametriAziende.getParameters("mailRibaltone", new Integer[]{azienda.getId()}, new String[]{Applicazione.Applicazioni.trasformatore.toString()});
            if (parameters != null && !parameters.isEmpty()) {
                email = parametriAziende.getValue(parameters.get(0), new TypeReference<List<String>>() {}).get(0);
                log.info("la mail è: " + email);
            }
            parameters = parametriAziende.getParameters("trasformaPrimaDiRibaltare", new Integer[]{azienda.getId()}, new String[]{Applicazione.Applicazioni.trasformatore.toString()});
            if (parameters != null && !parameters.isEmpty()) {
                trasformaPrimaDiRibaltare = parametriAziende.getValue(parameters.get(0), Boolean.class);
                log.info("il trasformaPrimaDiRibaltare è: " + trasformaPrimaDiRibaltare);
            }
            parameters = parametriAziende.getParameters("fonte_ribaltone", new Integer[]{azienda.getId()}, new String[]{Applicazione.Applicazioni.ribaltorg.toString()});
            if (parameters != null && !parameters.isEmpty()) {
                fonteRibaltone = parametriAziende.getValue(parameters.get(0), String.class);
                log.info("la fonte del ribaltone è: " + fonteRibaltone);
            }
//
            LanciaTrasformatoreJobWorkerData lanciaTrasformatoreJobWorkerData = new LanciaTrasformatoreJobWorkerData(azienda.getId(),azienda.getRibaltaArgo(),azienda.getRibaltaInternauta(),email,fonteRibaltone,trasformaPrimaDiRibaltare,user.getId(), "servizio Notturno");
            LanciaTrasformatoreJobWorker jobWorker = super.masterjobsObjectsFactory.getJobWorker(LanciaTrasformatoreJobWorker.class, lanciaTrasformatoreJobWorkerData, false);
            try {
                super.masterjobsJobsQueuer.queue(jobWorker, null, null, Applicazione.Applicazioni.trasformatore.toString(), false, Set.SetPriority.NORMAL);
                
            } catch (MasterjobsQueuingException ex) {
                String errorMessage = "errore nell'accodamento del job di trasformazione";
                log.error(errorMessage);
                throw new MasterjobsWorkerException(errorMessage, ex);
            }
        }
        log.info(String.format("end %s...", getName()));
        return null;
    }
    
}
