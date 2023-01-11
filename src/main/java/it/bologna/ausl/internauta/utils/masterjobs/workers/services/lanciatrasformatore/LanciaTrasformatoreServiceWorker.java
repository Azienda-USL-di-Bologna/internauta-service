package it.bologna.ausl.internauta.utils.masterjobs.workers.services.lanciatrasformatore;
import com.fasterxml.jackson.core.type.TypeReference;
import it.bologna.ausl.internauta.service.repositories.ribaltoneutils.RibaltoneDaLanciareRepository;
import it.bologna.ausl.internauta.service.utils.CachedEntities;
import it.bologna.ausl.internauta.utils.masterjobs.annotations.MasterjobsWorker;
import it.bologna.ausl.internauta.utils.masterjobs.exceptions.MasterjobsWorkerException;
import it.bologna.ausl.internauta.utils.masterjobs.workers.WorkerResult;
import it.bologna.ausl.internauta.utils.masterjobs.workers.services.ServiceWorker;
import it.bologna.ausl.internauta.utils.parameters.manager.ParametriAziendeReader;
import it.bologna.ausl.model.entities.baborg.Azienda;
import it.bologna.ausl.model.entities.baborg.Persona;
import it.bologna.ausl.model.entities.baborg.Utente;
import it.bologna.ausl.model.entities.configurazione.Applicazione;
import it.bologna.ausl.model.entities.configurazione.ParametroAziende;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.projection.ProjectionFactory;

/**
 *
 * @author mido
 */
@MasterjobsWorker
public class LanciaTrasformatoreServiceWorker extends ServiceWorker {
    private static Logger log = LoggerFactory.getLogger(LanciaTrasformatoreServiceWorker.class);

    public static final String LANCIA_TRASFORMATORE_PRO_RIBALTONE = "lancia_trasformatore_pro_ribaltone";
    
    @Autowired
    private RibaltoneDaLanciareRepository ribaltoneDaLanciareRepository;
    
    @Autowired
    private CachedEntities cachedEntities;
    
    @Autowired
    private ParametriAziendeReader parametriAziende;
    
    @Autowired
    private ProjectionFactory factory;
    
    @Override
    public String getName() {
        return getClass().getSimpleName();
    }
    
    @Override
    public WorkerResult doWork() throws MasterjobsWorkerException {
        log.info(String.format("starting %s...", getName()));
        List<Azienda> allAziende = cachedEntities.getAllAziende();
        String email = null;
        Persona persona = cachedEntities.getPersonaFromCodiceFiscale("RIBALTONE");
        for (Azienda azienda : allAziende) {
            Utente user = persona.getUtenteList().stream()
                    .filter(utente -> utente.getIdAzienda().getId().equals(azienda.getId())).findFirst().get();
            List<ParametroAziende> parameters = parametriAziende.getParameters("mailRibaltone", new Integer[]{azienda.getId()}, new String[]{Applicazione.Applicazioni.trasformatore.toString()});
            if (parameters != null && !parameters.isEmpty()) {
                email = parametriAziende.getValue(parameters.get(0), new TypeReference<List<String>>() {}).get(0);
        }
            ribaltoneDaLanciareRepository.sendNotifyInternauta(azienda.getCodice(), Boolean.TRUE, Boolean.TRUE, "Servizio notturno", email, user.getId());
        }
        log.info(String.format("end %s...", getName()));
        return null;
    }
    
}
