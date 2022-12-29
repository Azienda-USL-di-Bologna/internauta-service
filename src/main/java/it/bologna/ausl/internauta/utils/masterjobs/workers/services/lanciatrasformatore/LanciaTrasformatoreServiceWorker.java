package it.bologna.ausl.internauta.utils.masterjobs.workers.services.lanciatrasformatore;
import it.bologna.ausl.internauta.service.repositories.ribaltoneutils.RibaltoneDaLanciareRepository;
import it.bologna.ausl.internauta.service.utils.CachedEntities;
import it.bologna.ausl.internauta.utils.masterjobs.annotations.MasterjobsWorker;
import it.bologna.ausl.internauta.utils.masterjobs.exceptions.MasterjobsWorkerException;
import it.bologna.ausl.internauta.utils.masterjobs.workers.WorkerResult;
import it.bologna.ausl.internauta.utils.masterjobs.workers.services.ServiceWorker;
import it.bologna.ausl.model.entities.baborg.Azienda;
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
    private RibaltoneDaLanciareRepository ribaltoneDaLanciareRepository;
    
    @Autowired
    private CachedEntities cachedEntities;
    
    
    @Override
    public String getName() {
        return getClass().getSimpleName();
    }
    
    @Override
    public WorkerResult doWork() throws MasterjobsWorkerException {
        log.info(String.format("starting %s...", getName()));
        List<Azienda> allAziende = cachedEntities.getAllAziende();
        for (Azienda azienda : allAziende) {
            ribaltoneDaLanciareRepository.sendNotifyInternauta(azienda.getCodice(), Boolean.TRUE, Boolean.TRUE, Boolean.TRUE);
            
        }
        return null;
    }
    
}
