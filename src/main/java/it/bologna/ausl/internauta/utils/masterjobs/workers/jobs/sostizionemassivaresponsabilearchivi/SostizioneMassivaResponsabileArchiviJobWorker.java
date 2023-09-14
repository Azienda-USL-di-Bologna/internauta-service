package it.bologna.ausl.internauta.utils.masterjobs.workers.jobs.sostizionemassivaresponsabilearchivi;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.bologna.ausl.internauta.service.krint.KrintScriptaService;
import it.bologna.ausl.internauta.service.repositories.logs.MassiveActionLogRepository;
import it.bologna.ausl.internauta.service.repositories.scripta.AttoreArchivioRepository;
import it.bologna.ausl.internauta.service.repositories.scripta.PersonaVedenteRepository;
import it.bologna.ausl.internauta.utils.masterjobs.annotations.MasterjobsWorker;
import it.bologna.ausl.internauta.utils.masterjobs.exceptions.MasterjobsWorkerException;
import it.bologna.ausl.internauta.utils.masterjobs.workers.jobs.JobWorker;
import it.bologna.ausl.internauta.utils.masterjobs.workers.jobs.JobWorkerResult;
import it.bologna.ausl.model.entities.logs.MassiveActionLog;
import it.bologna.ausl.model.entities.logs.OperazioneKrint;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Questo Job si occupa si sostituire responsabile e struttura di un elenco di archivi.
 * Rispetto ai parametri del job per ogni archivio si può ricadere su 3 casistiche:
 * Caso A: Archivio con vecchio responsabile diverso dal nuovo. Qui avverrà la sostituzione.
 * Caso B: Archivio in cui il vecchio e il nuovo responsabile sono lo stesso, ma la struttura è cambiata. Qui si fa update della struttura.
 * Caso C: Archivio in cui sia responsabile che struttura non cambiano. Qui non deve far nulla.
 * 
 * @author gusgus
 */
@MasterjobsWorker
public class SostizioneMassivaResponsabileArchiviJobWorker extends JobWorker<SostizioneMassivaResponsabileArchiviJobWorkerData, JobWorkerResult> {
    private static final Logger log = LoggerFactory.getLogger(SostizioneMassivaResponsabileArchiviJobWorker.class);
    private final String name = SostizioneMassivaResponsabileArchiviJobWorker.class.getSimpleName();
    
    @Autowired
    private PersonaVedenteRepository personaVedenteRepository;
    
    @Autowired
    private AttoreArchivioRepository attoreArchivioRepository;
    
    @Autowired
    private MassiveActionLogRepository massiveActionLogRepository;
    
    @Autowired
    private KrintScriptaService krintScriptaService;
    
    @PersistenceContext
    private EntityManager em;
    
    @Autowired
    private ObjectMapper objectMapper;

    @Override
    public String getName() {
        return this.name;
    }
    
    @Override
    protected JobWorkerResult doRealWork() throws MasterjobsWorkerException {
        log.info("Inizio job");
        
        SostizioneMassivaResponsabileArchiviJobWorkerData data = getWorkerData();
        Integer[] idsArchivi = data.getIdsArchivi();
        Integer idPersonaNuovoResponsabile = data.getIdPersonaNuovoResponsabile();
        Integer idStrutturaNuovoResponsabile = data.getIdStrutturaNuovoResponsabile();
        Integer idMassiveActionLog = data.getIdMassiveActionLog();
        Integer idPersonaOperazione = data.getIdPersonaOperazione();
        
        log.info(String.format("PARAMETRI. idPersonaNuovoResponsabile: %1$s, idStrutturaNuovoResponsabile: %2$s, idMassiveActionLog: %3$s, idPersonaOperazione: %4$s, totaleArchivi: %5$s", 
                idPersonaNuovoResponsabile, idStrutturaNuovoResponsabile, idMassiveActionLog, idPersonaOperazione, idsArchivi.length));

        // CASO A. Non faccio update ma delete e insert perché voglio sfruttare i trigger per far aggiornare i permessi sulla blackbox.
        List<Map<String, Object>> idsCasoAMap = attoreArchivioRepository.sostituisciResponsabile(idsArchivi, idPersonaNuovoResponsabile, idStrutturaNuovoResponsabile);
        List<SostizioneMassivaResponsabileInfo> idsCasoA = objectMapper.convertValue(idsCasoAMap, new TypeReference<List<SostizioneMassivaResponsabileInfo>>(){});
        // TODO: Controllare perché non si riesce a convertire direttamente in List<SostizioneMassivaResponsabileInfo> (serve converter?)
        log.info(String.format("Num archivi con responsabile sostituito: %1$s", idsCasoA.size()));

        // CASO B.
        Set<Integer> idsCasoB = attoreArchivioRepository.aggiornaStrutturaResponsabile(idsArchivi, idPersonaNuovoResponsabile, idStrutturaNuovoResponsabile);
        // TODO: Questa parte è da finire
        log.info(String.format("Num archivi con struttura responsabile aggiornata: %1$s", idsCasoB.size()));

        // CASO C.
        List<Integer> idsArchiviList = new ArrayList(Arrays.asList(idsArchivi));
        idsArchiviList.removeAll(idsCasoA);
        idsArchiviList.removeAll(idsCasoB);
        log.info(String.format("Num archivi non modificati: %1$s", idsArchiviList.size()));
        
        // Ciclo i CASI A e per ogni archivio faccio il krint
        log.info(String.format("Faccio il krint dei responsabili sostituiti"));
        for (Map<String, Object> info : idsCasoAMap) {
//            krintScriptaService.writeSostituzioneResponsabileDaAmministratoreGedi(
//                    idsCasoA,
//                    info,
//                    OperazioneKrint.CodiceOperazione.SCRIPTA_ARCHIVIO_UPDATE_RESPONSABILE_GESTIONE_MASSIVA
//            );
        }
        
        // Ciclo i CASI B e per ogni archivio faccio il krint
        log.info(String.format("Faccio il krint delle strutture aggiornate"));
        
        // Inserisco la notifica per l'AG
        log.info(String.format("Inserisco la notifica per l'AG"));
        
        // Inserisco la notifica per il nuovo responsabile
        log.info(String.format("Inserisco la notifica il nuovo responsabile"));
        
        // Aggiorno la massiveActionLog
        log.info(String.format("Aggiorno la massiveActionLog"));
        MassiveActionLog m = massiveActionLogRepository.getById(idMassiveActionLog);
        m.setCompletionDate(ZonedDateTime.now());
        Map<String, Object> additionalData = m.getAdditionalData();
        if (additionalData == null) {
            additionalData = new HashMap();
        }
        additionalData.put("responsabiliSostituiti", idsCasoA.size());
        additionalData.put("struttureResponsabileSostituite", idsCasoB.size());
        additionalData.put("fascicoliNonAggiornati", idsArchiviList.size());
        m.setAdditionalData(additionalData);
        massiveActionLogRepository.save(m);
        
        log.info(String.format("Job finito"));
        
        return null;
    }

    @Override
    public boolean isExecutable() {
        ZonedDateTime.now();
        // Controllo se now è tra le 7 e le 18, se si torno false se no torno true
        // Aggiiungere List<SostizioneMassivaResponsabileInfo> al notified job.
        return super.isExecutable(); // TODO
    }
}
