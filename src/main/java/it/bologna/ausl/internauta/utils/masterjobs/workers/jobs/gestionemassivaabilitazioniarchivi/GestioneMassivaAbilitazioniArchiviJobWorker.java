package it.bologna.ausl.internauta.utils.masterjobs.workers.jobs.gestionemassivaabilitazioniarchivi;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.querydsl.core.Tuple;
import com.querydsl.jpa.impl.JPAQueryFactory;
import it.bologna.ausl.internauta.service.authorization.jwt.AuthorizationUtils;
import it.bologna.ausl.internauta.service.controllers.scripta.InfoAbilitazioniMassiveArchivi;
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
import it.bologna.ausl.model.entities.baborg.QPersona;
import it.bologna.ausl.model.entities.baborg.Struttura;
import it.bologna.ausl.model.entities.baborg.Utente;
import it.bologna.ausl.model.entities.configurazione.Applicazione;
import it.bologna.ausl.model.entities.logs.MassiveActionLog;
import it.bologna.ausl.model.entities.logs.OperazioneKrint;
import it.bologna.ausl.model.entities.masterjobs.JobNotified;
import it.bologna.ausl.model.entities.masterjobs.Set;
import it.bologna.ausl.model.entities.scripta.PermessoArchivio;
import it.bologna.ausl.model.entities.scripta.QArchivio;
import it.bologna.ausl.model.entities.scrivania.Attivita;
import it.bologna.ausl.model.entities.scrivania.Attivita.TipoAttivita;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * 
 * @author gusgus
 */
@MasterjobsWorker
public class GestioneMassivaAbilitazioniArchiviJobWorker extends JobWorker<GestioneMassivaAbilitazioniArchiviJobWorkerData, JobWorkerResult> {
    private static final Logger log = LoggerFactory.getLogger(GestioneMassivaAbilitazioniArchiviJobWorker.class);
    private final String name = GestioneMassivaAbilitazioniArchiviJobWorker.class.getSimpleName();
    
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
        
        GestioneMassivaAbilitazioniArchiviJobWorkerData data = getWorkerData();
        Integer[] idsArchivi = data.getIdsArchivi();
        InfoAbilitazioniMassiveArchivi abilitazioniRichieste = data.getAbilitazioniRichieste();
        Integer idMassiveActionLog = data.getIdMassiveActionLog();
        Integer idPersonaOperazione = data.getIdPersonaOperazione();
        Integer idUtenteOperazione = data.getIdUtenteOperazione();
        Integer idAzienda = data.getIdAzienda();
        Applicazione app = applicazioneRepository.findById(Applicazione.Applicazioni.scripta.name()).get();
        
        Utente utenteOperazione = utenteRepository.getById(idUtenteOperazione);
        Azienda azienda = aziendaRepository.getById(idAzienda);
        Persona personaOperazione = personaRepository.getById(idPersonaOperazione);
        MassiveActionLog m = massiveActionLogRepository.getById(idMassiveActionLog);
        
        String idsArchiviString = Arrays.toString(idsArchivi);
        idsArchiviString = idsArchiviString.substring(1, idsArchiviString.length() - 1);
        
        
        log.info(String.format("Creo la mappa archivi e la mappa persone"));
        // Mi preparo due mappe che conterranno tutte le info utili per poi creare notifiche e krint
        Map<Integer, InfoArchivio> mappaArchivi = buildInitalArchiviMap(idsArchivi); // es: 1818118: {vicariAggiunti:[], vicariEliminati:[], permessiPersonaAggiunti: {VISUALIZZA: [], MODIFICA: [], ELIMINA: []}, permessiPersonaRimossi}
        Map<Integer, InfoPersona> mappaPersone = buildInitialPersoneMap(abilitazioniRichieste); // es: 9383721; {vicariatiOttenuti:[], vicariatiPerduti:[], permessiOttenuti: ??, permessiPerduti: []}                
        
        // Setto come utente loggato l'utente amministratore gedi che effettua l'operazione
        authorizationUtils.insertInContext(utenteOperazione, 0, null, Applicazione.Applicazioni.scripta.toString(), false);
        
        log.info(String.format("PARAMETRI. idMassiveActionLog: %1$s, idPersonaOperazione: %2$s, totaleArchivi: %3$s, totalePersone: %4$s", 
                 idMassiveActionLog, idPersonaOperazione, idsArchivi.length, mappaArchivi.size()));

        log.info(String.format("Eliminazione vicari"));
        // Eliminazione vicari
        List<Integer> idPersonaVicariDaRimuovere = abilitazioniRichieste.getIdPersonaVicariDaRimuovere();
        if (idPersonaVicariDaRimuovere != null) {
            List<Map<String, Object>> vicariRimossi = attoreArchivioRepository.deleteVicari(idsArchivi, idPersonaVicariDaRimuovere.toArray(new Integer[0]));
            for (Map<String, Object> vicarioRimosso : vicariRimossi) {
                InfoArchivio infoArchivio = mappaArchivi.get((Integer)vicarioRimosso.get("idArchivio"));
                infoArchivio.getVicariEliminati().add((Integer)vicarioRimosso.get("idPersona"));

                InfoPersona infoPersona = mappaPersone.get((Integer)vicarioRimosso.get("idPersona"));
                infoPersona.getVicariatiPerduti().add((Integer)vicarioRimosso.get("idArchivio"));
            }
        }
        
        // Eliminazione permessi
        // TODO
        
        log.info(String.format("Inserimento vicari"));
        // Inserimento vicari
        List<Integer> idPersonaVicariDaAggiungere = abilitazioniRichieste.getIdPersonaVicariDaAggiungere();
        if (idPersonaVicariDaAggiungere != null) {
            String idsPersoneVicariDaAggiungereString = idPersonaVicariDaAggiungere.stream().map(String::valueOf).collect(Collectors.joining(","));
            List<Map<String, Object>> vicariInseriti = attoreArchivioRepository.insertVicari(idsArchiviString, idsPersoneVicariDaAggiungereString);
            for (Map<String, Object> vicarioInserito : vicariInseriti) {
                InfoArchivio infoArchivio = mappaArchivi.get((Integer)vicarioInserito.get("idArchivio"));
                infoArchivio.getVicariAggiunti().add((Integer)vicarioInserito.get("idPersona"));

                InfoPersona infoPersona = mappaPersone.get((Integer)vicarioInserito.get("idPersona"));
                infoPersona.getVicariatiOttenuti().add((Integer)vicarioInserito.get("idArchivio"));
            }
        }
        
        // Inserimento permessi
        // TODO
        // NB: Nella mappa mappaPersone la chiave predicatoPermessoOttenuto è vuota. Durante questo processo di inserimento deve venir riempita questa proprietà
        
        log.info(String.format("krinto"));
        // KRINT
        for (Map.Entry<Integer, InfoArchivio> entry : mappaArchivi.entrySet()) {
            Integer idArchivio = entry.getKey();
            InfoArchivio info = entry.getValue();
            krintScriptaService.writeGestioneMassivaAbilitazioniArchiviDaAmministratoreGedi(
                    idArchivio, 
                    idMassiveActionLog,
                    info, 
                    mappaPersone, 
                    OperazioneKrint.CodiceOperazione.SCRIPTA_ARCHIVIO_GESTIONE_MASSIVA_ABILITAZIONI
            );
        }
        
        // NOTIFICHE IN SCRIVANIA
        log.info(String.format("Notifico l'AG"));
        // Notifica all'AG
        String oggettoAttivita = "";
        if (idsArchivi.length == 1) 
            oggettoAttivita = "La modifica massiva di vicari e permessi di un fascicolo e relativi sottofascicoli è avvenuta con successo.";
        else
            oggettoAttivita = String.format("La modifica massiva di vicari e permessi di %1$s fascicoli e relativi sottofascicoli è avvenuta con successo.", idsArchivi.length);
        insertAttivita(azienda, personaOperazione, oggettoAttivita, app);
        
        log.info(String.format("Notifico i vari utenti"));
        // Notifiche agli utenti coinvolti
        for (Map.Entry<Integer, InfoPersona> entry : mappaPersone.entrySet()) {
            Integer idPersona = entry.getKey();
            InfoPersona info = entry.getValue();
            HashSet<Integer> idArchiviCoinvolti = new HashSet();
            idArchiviCoinvolti.addAll(info.getPermessiOttenuti());
            idArchiviCoinvolti.addAll(info.getPermessiPerduti());
            idArchiviCoinvolti.addAll(info.getVicariatiOttenuti());
            idArchiviCoinvolti.addAll(info.getVicariatiPerduti());
            if (!idArchiviCoinvolti.isEmpty()) {
                if (idArchiviCoinvolti.size() == 1) 
                    oggettoAttivita = String.format("L'amministratore %1$s ha modificato le abilitazioni di un fascicolo che ti coinvolgono.", personaOperazione.getDescrizione());
                else
                    oggettoAttivita = String.format("L'amministratore %1$s ha modificato le abilitazioni di %2$s fascicoli che ti coinvolgono.", personaOperazione.getDescrizione(), idArchiviCoinvolti.size());
                insertAttivita(azienda, personaOperazione, oggettoAttivita, app);
            }
        }
        
        // Aggiorno la massiveActionLog
        log.info(String.format("Aggiorno la massiveActionLog"));
        m.setCompletionDate(ZonedDateTime.now());
//        Map<String, Object> additionalData = m.getAdditionalData();
//        if (additionalData == null) {
//            additionalData = new HashMap();
//        }
//        additionalData.put("responsabiliSostituiti", idsCasoAMap.size());
//        additionalData.put("struttureResponsabileSostituite", idsCasoBMap.size());
//        additionalData.put("fascicoliNonAggiornati", idsArchiviList.size());
//        m.setAdditionalData(additionalData);
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
        a.setDescrizione("Abilitazioni massive");
        a.setProvenienza("Amministrazione Gedi");
        attivitaRepository.saveAndFlush(a);
    }
    
    /**
     * 
     * @param idsArchivi
     * @return 
     */
    private Map<Integer, InfoArchivio> buildInitalArchiviMap(Integer[] idsArchivi) {
        Map<Integer, InfoArchivio> mappaArchivi = new HashMap();
        QArchivio qArchivio = QArchivio.archivio;
        JPAQueryFactory jpaQueryFactory = new JPAQueryFactory(entityManager);
        List<Tuple> infoArchivi = jpaQueryFactory
                .select(qArchivio.id, qArchivio.numerazioneGerarchica)
                .from(qArchivio)
                .where(qArchivio.id.in(idsArchivi))
                .fetch();
        for (Tuple infoArchvio : infoArchivi) {
            Map<String, List<Integer>> permessiPersonaAggiunti = new HashMap();
            permessiPersonaAggiunti.put(PermessoArchivio.Predicato.VISUALIZZA.toString(), new ArrayList());
            permessiPersonaAggiunti.put(PermessoArchivio.Predicato.MODIFICA.toString(), new ArrayList());
            permessiPersonaAggiunti.put(PermessoArchivio.Predicato.ELIMINA.toString(), new ArrayList());
            InfoArchivio info = new InfoArchivio(infoArchvio.get(qArchivio.numerazioneGerarchica), new ArrayList(), new ArrayList(), permessiPersonaAggiunti, new ArrayList());
            mappaArchivi.put(infoArchvio.get(qArchivio.id), info);
        }
        return mappaArchivi;
    }
    
    /**
     * 
     */
    private Map<Integer, InfoPersona> buildInitialPersoneMap(InfoAbilitazioniMassiveArchivi abilitazioniRichieste) {
        Map<Integer, InfoPersona> mappaPersone = new HashMap();
        HashSet<Integer> idsPersone = new HashSet();
        if (abilitazioniRichieste.getIdPersonaPermessiDaRimuovere() != null) 
            idsPersone.addAll(abilitazioniRichieste.getIdPersonaPermessiDaRimuovere());
        if (abilitazioniRichieste.getIdPersonaVicariDaAggiungere() != null) 
            idsPersone.addAll(abilitazioniRichieste.getIdPersonaVicariDaAggiungere());
        if (abilitazioniRichieste.getIdPersonaVicariDaRimuovere() != null) 
            idsPersone.addAll(abilitazioniRichieste.getIdPersonaVicariDaRimuovere());
        if (abilitazioniRichieste.getPermessiPersonaDaAggiungere() != null) 
            idsPersone.addAll(abilitazioniRichieste.getPermessiPersonaDaAggiungere().stream().map(p -> p.getIdPersona()).collect(Collectors.toList()));
        
        QPersona qPersona = QPersona.persona;
        JPAQueryFactory jpaQueryFactory = new JPAQueryFactory(entityManager);
        List<Tuple> infoPersone = jpaQueryFactory
                .select(qPersona.id, qPersona.descrizione)
                .from(qPersona)
                .where(qPersona.id.in(idsPersone))
                .fetch();
        for (Tuple infoPersona : infoPersone) {
            InfoPersona info = new InfoPersona(infoPersona.get(qPersona.descrizione), new ArrayList(), new ArrayList(), null, new ArrayList(), new ArrayList());
            mappaPersone.put(infoPersona.get(qPersona.id), info);
        }
        
        return mappaPersone;
    }
}
