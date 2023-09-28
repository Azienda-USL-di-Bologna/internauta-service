package it.bologna.ausl.internauta.utils.masterjobs.workers.jobs.sostizionemassivaresponsabilearchivi;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.bologna.ausl.internauta.service.authorization.jwt.AuthorizationUtils;
import it.bologna.ausl.internauta.service.krint.KrintScriptaService;
import it.bologna.ausl.internauta.service.krint.KrintUtils;
import it.bologna.ausl.internauta.service.repositories.baborg.AziendaRepository;
import it.bologna.ausl.internauta.service.repositories.baborg.PersonaRepository;
import it.bologna.ausl.internauta.service.repositories.baborg.StrutturaRepository;
import it.bologna.ausl.internauta.service.repositories.baborg.UtenteRepository;
import it.bologna.ausl.internauta.service.repositories.configurazione.ApplicazioneRepository;
import it.bologna.ausl.internauta.service.repositories.logs.MassiveActionLogRepository;
import it.bologna.ausl.internauta.service.repositories.scripta.AttoreArchivioRepository;
import it.bologna.ausl.internauta.service.repositories.scrivania.AttivitaRepository;
import it.bologna.ausl.internauta.service.utils.HttpSessionData;
import it.bologna.ausl.internauta.utils.masterjobs.annotations.MasterjobsWorker;
import it.bologna.ausl.internauta.utils.masterjobs.exceptions.MasterjobsWorkerException;
import it.bologna.ausl.internauta.utils.masterjobs.repository.JobNotifiedRepository;
import it.bologna.ausl.internauta.utils.masterjobs.workers.jobs.JobWorker;
import it.bologna.ausl.internauta.utils.masterjobs.workers.jobs.JobWorkerResult;
import it.bologna.ausl.internauta.utils.masterjobs.workers.jobs.calcolopermessigerarchiaarchivio.CalcoloPermessiGerarchiaArchivioJobWorkerData;
import it.bologna.ausl.model.entities.baborg.Azienda;
import it.bologna.ausl.model.entities.baborg.Persona;
import it.bologna.ausl.model.entities.baborg.Struttura;
import it.bologna.ausl.model.entities.baborg.Utente;
import it.bologna.ausl.model.entities.configurazione.Applicazione;
import it.bologna.ausl.model.entities.logs.MassiveActionLog;
import it.bologna.ausl.model.entities.logs.OperazioneKrint;
import it.bologna.ausl.model.entities.masterjobs.JobNotified;
import it.bologna.ausl.model.entities.masterjobs.Set;
import it.bologna.ausl.model.entities.scrivania.Attivita;
import it.bologna.ausl.model.entities.scrivania.Attivita.TipoAttivita;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
    private AziendaRepository aziendaRepository;
    
    @Autowired
    private UtenteRepository utenteRepository;
    
    @Autowired
    private PersonaRepository personaRepository;
    
    @Autowired
    private StrutturaRepository strutturaRepository;
    
    @Autowired
    private AttoreArchivioRepository attoreArchivioRepository;
    
    @Autowired
    private MassiveActionLogRepository massiveActionLogRepository;
    
    @Autowired
    private KrintScriptaService krintScriptaService;
    
    @Autowired
    private AttivitaRepository attivitaRepository;
    
    @Autowired
    private ApplicazioneRepository applicazioneRepository;
    
    @Autowired
    private AuthorizationUtils authorizationUtils;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @Autowired
    private JobNotifiedRepository jobNotifiedRepository;
    
    @Autowired
    private KrintUtils krintUtils;
    
    @Autowired
    private HttpSessionData httpSessionData;

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
        Integer idUtenteOperazione = data.getIdUtenteOperazione();
        Integer idAzienda = data.getIdAzienda();
        Applicazione app = applicazioneRepository.findById(Applicazione.Applicazioni.scripta.name()).get();
        
        Utente utenteOperazione = utenteRepository.getById(idUtenteOperazione);
        Persona personaNuovoResponsabile = personaRepository.getById(idPersonaNuovoResponsabile);
        Struttura strutturaNuovoResponsabile = strutturaRepository.getById(idStrutturaNuovoResponsabile);
        Azienda azienda = aziendaRepository.getById(idAzienda);
        Persona personaOperazione = personaRepository.getById(idPersonaOperazione);
        MassiveActionLog m = massiveActionLogRepository.getById(idMassiveActionLog);
        
        // Setto come utente loggato l'utente amministratore gedi che effettua l'operazione
        authorizationUtils.insertInContext(utenteOperazione, 0, null, Applicazione.Applicazioni.scripta.toString(), false);
        
        log.info(String.format("PARAMETRI. idPersonaNuovoResponsabile: %1$s, idStrutturaNuovoResponsabile: %2$s, idMassiveActionLog: %3$s, idPersonaOperazione: %4$s, totaleArchivi: %5$s", 
                idPersonaNuovoResponsabile, idStrutturaNuovoResponsabile, idMassiveActionLog, idPersonaOperazione, idsArchivi.length));

        String idsArchiviString = Arrays.toString(idsArchivi);
        idsArchiviString = idsArchiviString.substring(1, idsArchiviString.length() - 1);
        
        // CASO A. Non faccio update ma delete e insert perché voglio sfruttare i trigger per far aggiornare i permessi sulla blackbox.
        List<Map<String, Object>> idsCasoAMap = attoreArchivioRepository.sostituisciResponsabile(
                idsArchivi, 
                idPersonaNuovoResponsabile, 
                idStrutturaNuovoResponsabile,
                personaNuovoResponsabile.getDescrizione(),
                strutturaNuovoResponsabile.getNome(),
                idsArchiviString);
        List<HashMap<String, Object>> idsCasoAHashMap = objectMapper.convertValue(idsCasoAMap, new TypeReference<List<HashMap<String, Object>>>(){});
        log.info(String.format("Num archivi con responsabile sostituito: %1$s", idsCasoAMap.size()));

        // CASO B.
        List<Map<String, Object>> idsCasoBMap = attoreArchivioRepository.aggiornaStrutturaResponsabile(
                idsArchivi, 
                idPersonaNuovoResponsabile, 
                idStrutturaNuovoResponsabile,
                personaNuovoResponsabile.getDescrizione(),
                strutturaNuovoResponsabile.getNome());
        List<HashMap<String, Object>> idsCasoBHashMap = objectMapper.convertValue(idsCasoBMap, new TypeReference<List<HashMap<String, Object>>>(){});
        log.info(String.format("Num archivi con struttura responsabile aggiornata: %1$s", idsCasoBMap.size()));

        List<Integer> idsArchiviList = new ArrayList(Arrays.asList(idsArchivi));

        // Ciclo i CASI A e per ogni archivio faccio il krint
        log.info(String.format("Faccio il krint dei responsabili sostituiti"));
        for (HashMap<String, Object> info : idsCasoAHashMap) {
            krintScriptaService.writeSostituzioneResponsabileDaAmministratoreGedi(
                    info,
                    idMassiveActionLog,
                    OperazioneKrint.CodiceOperazione.SCRIPTA_ARCHIVIO_UPDATE_RESPONSABILE_GESTIONE_MASSIVA
            );
            idsArchiviList.remove((Integer) info.get("idArchivio")); // per il CASO C
            
            // Visto il cambiamento effettuato vogliamo rialcolare i permessi sull'archivio etc
            JobNotified jn = new JobNotified();
            jn.setJobName("CalcoloPermessiGerarchiaArchivioJobWorker");
            jn.setJobData(objectMapper.convertValue(new CalcoloPermessiGerarchiaArchivioJobWorkerData(
                (Integer) info.get("idArchivio")
            ), Map.class));
            jn.setWaitObject(false);
            jn.setApp(app.getId());
            jn.setPriority(Set.SetPriority.NORMAL);
            jn.setSkipIfAlreadyPresent(Boolean.TRUE);
            jobNotifiedRepository.save(jn);
        }
        
        // Ciclo i CASI B e per ogni archivio faccio il krint
        log.info(String.format("Faccio il krint delle strutture aggiornate"));
        for (HashMap<String, Object> info : idsCasoBHashMap) {
            krintScriptaService.writeSostituzioneResponsabileDaAmministratoreGedi(
                    info,
                    idMassiveActionLog,
                    OperazioneKrint.CodiceOperazione.SCRIPTA_ARCHIVIO_UPDATE_STRUTTURA_GESTIONE_MASSIVA
            );
            idsArchiviList.remove((Integer) info.get("idArchivio")); // per il CASO C
        }
        
        // CASO C. Man mano che ciclavo le mappe del caso A e B ho creato la lista archivi del caso C
        log.info(String.format("Num archivi non modificati: %1$s", idsArchiviList.size()));
        
        // Inserisco la notifica per l'AG
        log.info(String.format("Inserisco la notifica per l'AG"));
        String oggettoAttivita = "";
        ZonedDateTime dataOraOperazione = m.getInsertionDate();
        DateTimeFormatter formatterData = DateTimeFormatter.ofPattern("dd-MM-yyyy");
        String dataFormattata = dataOraOperazione.format(formatterData);
        DateTimeFormatter formatterOrario = DateTimeFormatter.ofPattern("HH:mm");
        String orarioFormattato = dataOraOperazione.format(formatterOrario);
        
        if (idsArchivi.length == 1) 
            oggettoAttivita = String.format( "La modifica massiva che hai richiesto il %1$s alle %2$s della responsabilità di un fascicolo e relativi sottofascicoli è avvenuta con successo.", dataFormattata, orarioFormattato);
        else
            oggettoAttivita = String.format( "La modifica massiva che hai richiesto il %1$s alle %2$s della responsabilità di %3$s fascicoli e relativi sottofascicoli è avvenuta con successo.", dataFormattata, orarioFormattato, idsArchivi.length);
        if (!idsCasoAMap.isEmpty()) {
            if (idsCasoAMap.size() == 1)
                oggettoAttivita = oggettoAttivita + String.format( " Un fascicolo ha cambiato responsabile.");
            else
                oggettoAttivita = oggettoAttivita + String.format( " %1$s fascicoli hanno cambiato responsabile.", idsCasoAMap.size());
        }
        if (!idsCasoBMap.isEmpty()) {
            if (idsCasoBMap.size() == 1) 
                oggettoAttivita = oggettoAttivita + String.format( " Un fascicolo ha cambiato struttura.");
            else
                oggettoAttivita = oggettoAttivita + String.format( " %1$s fascicoli hanno cambiato struttura.", idsCasoBMap.size());
        }
        if (!idsArchiviList.isEmpty()) {
            if (idsArchiviList.size() == 1)
                oggettoAttivita = oggettoAttivita + String.format( " Un fascicolo non ha subito modifiche.");
            else
                oggettoAttivita = oggettoAttivita + String.format( " %1$s fascicoli non hanno subito modifiche.", idsArchiviList.size());
        }
        insertAttivita(azienda, personaOperazione, oggettoAttivita, app);
        
        if (idsCasoAMap.size() > 0) {
            // Inserisco la notifica per il nuovo responsabile
            log.info(String.format("Inserisco la notifica il nuovo responsabile"));
            if (idsCasoAMap.size() == 1)
                oggettoAttivita = String.format( "Hai ricevuto la responsabilità su un fascicolo dall'amministratore %2$s.", idsCasoAMap.size(), personaOperazione.getDescrizione());
            else
                oggettoAttivita = String.format( "Hai ricevuto la responsabilità su %1$s fascicoli dall'amministratore %2$s.", idsCasoAMap.size(), personaOperazione.getDescrizione());
            insertAttivita(azienda, personaNuovoResponsabile, oggettoAttivita, app);
        } else {
            log.info(String.format("Nessun fascicolo ha ricevuto il nuovo responsabile."));
        }
        // Aggiorno la massiveActionLog
        log.info(String.format("Aggiorno la massiveActionLog"));
        m.setCompletionDate(ZonedDateTime.now());
        Map<String, Object> additionalData = m.getAdditionalData();
        if (additionalData == null) {
            additionalData = new HashMap();
        }
        additionalData.put("responsabiliSostituiti", idsCasoAMap.size());
        additionalData.put("struttureResponsabileSostituite", idsCasoBMap.size());
        additionalData.put("fascicoliNonAggiornati", idsArchiviList.size());
        m.setAdditionalData(additionalData);
        massiveActionLogRepository.save(m);
        
        log.info(String.format("Effettuo il salvataggio dei krint creati"));
        krintUtils.saveAllKrintsInSessionData();
        httpSessionData.resetDataMap();
        
        log.info(String.format("Job finito"));
        
        return null;
    }

    @Override
    public boolean isExecutable() {
        return true; // Per il momento lo eseguiamo subito perché dobbiamo fare la presentazione.
        // Controllo se now è tra le 7 e le 18, se si torno false se no torno true
//        ZonedDateTime now = ZonedDateTime.now();
//        LocalTime oraCorrente = now.toLocalTime();
//        LocalTime inizioOrario = LocalTime.of(7, 0);   // 7:00
//        LocalTime fineOrario = LocalTime.of(18, 0);    // 18:00
//        return !(oraCorrente.isAfter(inizioOrario) && oraCorrente.isBefore(fineOrario));
    }
    
    private void insertAttivita(Azienda azienda, Persona persona, String oggetto, Applicazione app) {
        Attivita a = new Attivita();
        a.setIdAzienda(azienda);
        a.setIdPersona(persona);
        a.setIdApplicazione(app);
        a.setTipo(TipoAttivita.NOTIFICA.toString().toLowerCase());
        a.setOggetto(oggetto);
        a.setDescrizione("Sostituzione massiva responsabile");
        a.setProvenienza("Amministrazione Gedi");
        attivitaRepository.saveAndFlush(a);
    }
}
