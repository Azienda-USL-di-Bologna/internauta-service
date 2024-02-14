package it.bologna.ausl.internauta.utils.masterjobs.workers.jobs.copiatrasferisciabilitazioniarchivi;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.querydsl.core.Tuple;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import it.bologna.ausl.blackbox.PermissionManager;
import it.bologna.ausl.blackbox.exceptions.BlackBoxPermissionException;
import it.bologna.ausl.blackbox.utils.UtilityFunctions;
import it.bologna.ausl.internauta.model.bds.types.CategoriaPermessiStoredProcedure;
import it.bologna.ausl.internauta.model.bds.types.EntitaStoredProcedure;
import it.bologna.ausl.internauta.model.bds.types.PermessoEntitaStoredProcedure;
import it.bologna.ausl.internauta.model.bds.types.PermessoStoredProcedure;
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
import it.bologna.ausl.internauta.utils.parameters.manager.ParametriAziendeReader;
import it.bologna.ausl.model.entities.baborg.Azienda;
import it.bologna.ausl.model.entities.baborg.Persona;
import it.bologna.ausl.model.entities.baborg.Struttura;
import it.bologna.ausl.model.entities.baborg.Utente;
import it.bologna.ausl.model.entities.configurazione.Applicazione;
import it.bologna.ausl.model.entities.configurazione.ParametroAziende;
import it.bologna.ausl.model.entities.logs.MassiveActionLog;
import it.bologna.ausl.model.entities.logs.OperazioneKrint;
import it.bologna.ausl.model.entities.masterjobs.JobNotified;
import it.bologna.ausl.model.entities.masterjobs.Set;
import it.bologna.ausl.model.entities.scripta.Archivio;
import it.bologna.ausl.model.entities.scripta.AttoreArchivio;
import it.bologna.ausl.model.entities.scripta.PermessoArchivio;
import it.bologna.ausl.model.entities.scripta.QArchivio;
import it.bologna.ausl.model.entities.scripta.QAttoreArchivio;
import it.bologna.ausl.model.entities.scrivania.Attivita;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.persistence.Table;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * C'è un bug che ho deciso di lasciare. Se l'utente sorgente ha un un MODIFICA
 * propagato, e la destinazione ha un ELIMINA, Allora dovrei lasciare l'elimina
 * e dare il modifica in tutti i sottofascicoli, purché non abbiano anche li un
 * permesso più alto.
 *
 * @author gusgus
 */
@MasterjobsWorker
public class CopiaTrasferisciAbilitazioniArchiviJobWorker extends JobWorker<CopiaTrasferisciAbilitazioniArchiviJobWorkerData, JobWorkerResult> {

    private static final Logger log = LoggerFactory.getLogger(CopiaTrasferisciAbilitazioniArchiviJobWorker.class);
    private final String name = CopiaTrasferisciAbilitazioniArchiviJobWorker.class.getSimpleName();

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
    private PermissionManager permissionManager;

    @Autowired
    private KrintUtils krintUtils;

    @Autowired
    private HttpSessionData httpSessionData;

    @Autowired
    private ParametriAziendeReader parametriAziende;

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    protected JobWorkerResult doRealWork() throws MasterjobsWorkerException {
        log.info("Inizio job");

        log.info("Setto come utente loggato l'utente amministratore gedi che effettua l'operazione");
        CopiaTrasferisciAbilitazioniArchiviJobWorkerData data = getWorkerData();
        Integer idUtenteOperazione = data.getIdUtenteOperazione();
        Utente utenteOperazione = utenteRepository.getById(idUtenteOperazione);
        authorizationUtils.insertInContext(utenteOperazione, 0, null, Applicazione.Applicazioni.scripta.toString(), false);

        log.info("Prendo i parametri");
        MassiveActionLog.OperationType operationType = data.getOperationType();
        Integer idPersonaSorgente = data.getIdPersonaSorgente();
        Integer idPersonaDestinazione = data.getIdPersonaDestinazione();
        Integer idMassiveActionLog = data.getIdMassiveActionLog();
        Integer idPersonaOperazione = data.getIdPersonaOperazione();
        Integer idAzienda = data.getIdAzienda();
        Integer idUtenteDestinazione = data.getIdUtenteDestinazione();
        Integer idStrutturaDestinazione = data.getIdStrutturaDestinazione();

        log.info("Carico dal db un po' di entità");
        Applicazione app = applicazioneRepository.findById(Applicazione.Applicazioni.scripta.name()).get();
        Persona personaSorgente = personaRepository.getById(idPersonaSorgente);
        Persona personaDestinazione = personaRepository.getById(idPersonaDestinazione);
        Utente utenteDestinazione = utenteRepository.getById(idUtenteDestinazione);
        Azienda azienda = aziendaRepository.getById(idAzienda);
        List<Utente> utentiAzienda = personaSorgente.getUtenteList().stream().filter(u -> u.getIdAzienda().equals(azienda)).collect(Collectors.toList());
        ZonedDateTime dataSpegnimentoUtente = null;
        if (utentiAzienda != null && !utentiAzienda.isEmpty()) {
            dataSpegnimentoUtente = utentiAzienda.get(0).getDataSpegnimento();
        }
        Persona personaOperazione = personaRepository.getById(idPersonaOperazione);
        MassiveActionLog m = massiveActionLogRepository.getById(idMassiveActionLog);
        Struttura strutturaVeicolante = strutturaRepository.getById(idStrutturaDestinazione);

//        log.info("Prendo pure la struttura di afferenza diretta/unificata dell'utente destinazione");
//        StrutturaWithPlainFields strutturaVeicolanteWithPlainFields = (StrutturaWithPlainFields)userInfoService.getUtenteStrutturaAfferenzaPrincipaleAttiva(utenteDestinazione.getUtenteStrutturaList().get(0)).getIdStruttura();
//        Struttura strutturaVeicolante = strutturaRepository.getById(strutturaVeicolanteWithPlainFields.getId());
        log.info("Istanzio qualche utilità");
        JPAQueryFactory jpaQueryFactory = new JPAQueryFactory(entityManager);
        QAttoreArchivio qAttoreArchivio = QAttoreArchivio.attoreArchivio;
        QArchivio qArchivio = QArchivio.archivio;

        log.info("controllo se per questa azienda devo escludere i fascicoli chiusi/prechiusi");
        Boolean escludiArchiviChiusiFromAbilitazioniMassiveGedi = false;

//        Boolean escludiArchiviChiusiFromAbilitazioniMassiveGedi;
        List<ParametroAziende> escludiArchiviChiusiFromAbilitazioniMassiveGediParams = parametriAziende.getParameters(ParametriAziendeReader.ParametriAzienda.escludiArchiviChiusiFromAbilitazioniMassiveGedi);
        if (escludiArchiviChiusiFromAbilitazioniMassiveGediParams != null && !escludiArchiviChiusiFromAbilitazioniMassiveGediParams.isEmpty()) {
            escludiArchiviChiusiFromAbilitazioniMassiveGedi = escludiArchiviChiusiFromAbilitazioniMassiveGediParams.stream()
                    .anyMatch(param -> Arrays.stream(param.getIdAziende()).anyMatch(idAzienda::equals) && parametriAziende.getValue(param, Boolean.class));
        }

//        List<ParametroAziende> escludiArchiviChiusiFromAbilitazioniMassiveGediParams = parametriAziende.getParameters(ParametriAziendeReader.ParametriAzienda.escludiArchiviChiusiFromAbilitazioniMassiveGedi);
//        if (escludiArchiviChiusiFromAbilitazioniMassiveGediParams != null && !escludiArchiviChiusiFromAbilitazioniMassiveGediParams.isEmpty()) {
//            escludiArchiviChiusiFromAbilitazioniMassiveGedi = parametriAziende.getValue(escludiArchiviChiusiFromAbilitazioniMassiveGediParams.get(0), Boolean.class);
//        }
        log.info(String.format("PARAMETRI. idPersonaSorgente: %1$s, idPersonaDestinazione: %2$s, idMassiveActionLog: %3$s, idPersonaOperazione: %4$s",
                idPersonaSorgente, idPersonaDestinazione, idMassiveActionLog, idPersonaOperazione));

        Map<Integer, InfoArchivio> archiviInfo = new HashMap();
        // List<String>
        // es di mappa {44: {abilitazioniAggiunte: ['RESPONSABILE'], }}

        // RESPONSABILE
        // In caso di TRASFERIMENTO va effettuato il cambio di responsabile.
        if (operationType.equals(MassiveActionLog.OperationType.TRASFERISCI_ABILITAZIONI)) {
            log.info("Mi occupo del trasferimento del RESPONSABILE");

            List<Tuple> archivi = null;
            if (escludiArchiviChiusiFromAbilitazioniMassiveGedi) {
                archivi = jpaQueryFactory
                        .select(qAttoreArchivio.idArchivio.id, qArchivio.numerazioneGerarchica)
                        .from(qAttoreArchivio)
                        .join(qArchivio).on(qArchivio.id.eq(qAttoreArchivio.idArchivio.id))
                        .where(
                                qAttoreArchivio.idPersona.id.eq(idPersonaSorgente)
                                        .and(qAttoreArchivio.ruolo.eq(AttoreArchivio.RuoloAttoreArchivio.RESPONSABILE.toString()))
                                        .and(qArchivio.idAzienda.id.eq(idAzienda))
                                        .and(qArchivio.livello.eq(1))
                                        .and(qArchivio.stato.eq(Archivio.StatoArchivio.APERTO.toString()))
                        )
                        .fetch();
            } else {
                archivi = jpaQueryFactory
                        .select(qAttoreArchivio.idArchivio.id, qArchivio.numerazioneGerarchica)
                        .from(qAttoreArchivio)
                        .join(qArchivio).on(qArchivio.id.eq(qAttoreArchivio.idArchivio.id))
                        .where(
                                qAttoreArchivio.idPersona.id.eq(idPersonaSorgente)
                                        .and(qAttoreArchivio.ruolo.eq(AttoreArchivio.RuoloAttoreArchivio.RESPONSABILE.toString()))
                                        .and(qArchivio.idAzienda.id.eq(idAzienda))
                                        .and(qArchivio.livello.eq(1))
                        )
                        .fetch();
            }

            List<Integer> idsArchivi = new ArrayList();

            for (Tuple archivio : archivi) {
                Integer idArchivio = archivio.get(qAttoreArchivio.idArchivio.id);
                idsArchivi.add(idArchivio);
                List<String> abilitazioniAggiunte = new ArrayList();
                abilitazioniAggiunte.add(PermessoArchivio.Predicato.RESPONSABILE.toString());
                archiviInfo.put(idArchivio, new InfoArchivio(
                        archivio.get(qArchivio.numerazioneGerarchica),
                        abilitazioniAggiunte,
                        idArchivio
                ));
            }

            Integer[] idsArchiviArray = idsArchivi.toArray(new Integer[0]);
            String idsArchiviString = Arrays.toString(idsArchiviArray);
            idsArchiviString = idsArchiviString.substring(1, idsArchiviString.length() - 1);

            attoreArchivioRepository.sostituisciResponsabile(
                    idsArchiviArray,
                    idPersonaDestinazione,
                    idStrutturaDestinazione,
                    personaDestinazione.getDescrizione(),
                    strutturaVeicolante.getNome(),
                    idsArchiviString);

            log.info(String.format("Numero archivi coinvolti dal cambio responsabile: %1$s", idsArchivi.size()));
        }

        // VICARIO
        log.info("Mi occupo dei VICARIATI");
        // Ora mi occupo dei vicari, in caso di trasferimento devo togliere il vicariato al vecchio utente
        //List<Integer> idArchiviSuCuiInserireVicario = null;
        // Faccio la select degli archivi
        List<Tuple> archivi = null;
        if (escludiArchiviChiusiFromAbilitazioniMassiveGedi) {
            archivi = jpaQueryFactory
                    .select(qAttoreArchivio.idArchivio.id, qArchivio.numerazioneGerarchica)
                    .from(qAttoreArchivio)
                    .join(qArchivio).on(qArchivio.id.eq(qAttoreArchivio.idArchivio.id))
                    .where(qAttoreArchivio.idPersona.id.eq(idPersonaSorgente)
                            .and(qAttoreArchivio.ruolo.eq(AttoreArchivio.RuoloAttoreArchivio.VICARIO.toString()))
                            .and(qArchivio.idAzienda.id.eq(idAzienda))
                            .and(qArchivio.livello.eq(1))
                            .and(qArchivio.stato.eq(Archivio.StatoArchivio.APERTO.toString()))
                    )
                    .fetch();
        } else {
            archivi = jpaQueryFactory
                    .select(qAttoreArchivio.idArchivio.id, qArchivio.numerazioneGerarchica)
                    .from(qAttoreArchivio)
                    .join(qArchivio).on(qArchivio.id.eq(qAttoreArchivio.idArchivio.id))
                    .where(qAttoreArchivio.idPersona.id.eq(idPersonaSorgente)
                            .and(qAttoreArchivio.ruolo.eq(AttoreArchivio.RuoloAttoreArchivio.VICARIO.toString()))
                            .and(qArchivio.idAzienda.id.eq(idAzienda))
                            .and(qArchivio.livello.eq(1))
                    )
                    .fetch();
        }
        if (operationType.equals(MassiveActionLog.OperationType.TRASFERISCI_ABILITAZIONI)) {
            // Faccio una delete che mi torni gli id archivi
            attoreArchivioRepository.deleteVicariByIdPersonaAndIdAzienda(idPersonaSorgente, idAzienda);
        }

        // Riempo la mappa con l'elenco di id, ancora non so se davvero ci sarà il nuovo vicario o meno (perché magari c'è già)
        for (Tuple archivio : archivi) {
            Integer idArchivio = archivio.get(qAttoreArchivio.idArchivio.id);
            if (!archiviInfo.containsKey(idArchivio)) {
                archiviInfo.put(idArchivio, new InfoArchivio(
                        archivio.get(qArchivio.numerazioneGerarchica),
                        new ArrayList(),
                        idArchivio
                ));
            }
        }

        // Ora che ho gli archivi inserisco il nuovo vicario (a meno che non ci sia già)
        String idArchiviSuCuiInserireVicarioString = archivi.stream()
                .map(a -> a.get(qAttoreArchivio.idArchivio.id).toString())
                .collect(Collectors.joining(", "));
        List<Map<String, Object>> vicariInseriti = attoreArchivioRepository.insertVicari(idArchiviSuCuiInserireVicarioString, idPersonaDestinazione.toString());
        for (Map<String, Object> vicarioinserito : vicariInseriti) {
            archiviInfo.get((Integer) vicarioinserito.get("idArchivio")).getAbilitazioniAggiunte().add(PermessoArchivio.Predicato.VICARIO.toString());
        }
        log.info(String.format("Numero archivi in cui è stato aggiunto vicario: %1$s", vicariInseriti.size()));

        // PERMESSI
        log.info("Mi occupo dei permessi");
        log.info("Prendo i permessi della persona sorgente");
        // Ora mi occupo dei permessi. Chiedo alla blackbox i permessi non propagati ELIMINA, MODIFICA, VISUALIZZA della sorgente
        // Così poi capisco quale devo inserire, devo inserire il più alto, se non ce n'è già un più alto.
        // gli passo dataSpegnimentoUtente cosi recupero i mermessi attivi a quella data 
        // se dovesse essere null la blackbox mettera now() come data inizio e queindi verranno presi i permessi attivi oggi
        List<PermessoEntitaStoredProcedure> permessiBlackBoxPersonaSorgente = getPermessiBlackBox(personaSorgente, false, null, dataSpegnimentoUtente);

        log.info("Li filtro per tenere solo quelli della azienda di riferimento");
        // Di questi permessi devo tenere solo quelli che appartengono alla azienda su cui sto lavorando
        // Quindi ora, recupero questa info e poi faccio un filtro.
        List<Integer> idArchivi = permessiBlackBoxPersonaSorgente.stream().map(p -> p.getOggetto().getIdProvenienza()).collect(Collectors.toList());
        
        JPAQuery<Tuple> queryArchiviDiAziendaX = jpaQueryFactory
                .select(qArchivio.id, qArchivio.livello, qArchivio.idArchivioRadice.id, qArchivio.numerazioneGerarchica)
                .from(qArchivio);
        if (escludiArchiviChiusiFromAbilitazioniMassiveGedi) {
            queryArchiviDiAziendaX = queryArchiviDiAziendaX.where(qArchivio.id.in(idArchivi)
                    .and(qArchivio.idAzienda.id.eq(idAzienda))
                    .and(qArchivio.stato.eq(Archivio.StatoArchivio.APERTO.toString()))
            );
        } else {
            queryArchiviDiAziendaX = queryArchiviDiAziendaX.where(qArchivio.id.in(idArchivi)
                    .and(qArchivio.idAzienda.id.eq(idAzienda))
            );
        }
        archivi = queryArchiviDiAziendaX.fetch();
        // Questa lista conterrà gli id degli archivi che davvero mi interessano
        List<Integer> idArchiviFiltrati = new ArrayList();
        // Questa lista conterrà gli Archivi che mi interessano con popolato il campo id, mi servirà poi per usare la blackbox
        List<Archivio> archiviList = new ArrayList();

        for (Tuple archivio : archivi) {
            Integer idArchivio = archivio.get(qArchivio.id);
            idArchiviFiltrati.add(idArchivio);
            archiviList.add(new Archivio(idArchivio));

            if (!archiviInfo.containsKey(idArchivio)) {
                archiviInfo.put(idArchivio, new InfoArchivio(
                        archivio.get(qArchivio.numerazioneGerarchica),
                        new ArrayList(),
                        archivio.get(qArchivio.idArchivioRadice.id)
                ));
            }
        }

        permessiBlackBoxPersonaSorgente = permessiBlackBoxPersonaSorgente.stream().filter(p -> idArchiviFiltrati.contains(p.getOggetto().getIdProvenienza())).collect(Collectors.toList());

        // Mi creo delle mappe molto utili
        Map<Integer, String> mappaArchiviConPermessoPiuAltoPersonaSorgente = getMappaArchiviConPermessoPiuAltoDaPermessiBlackBox(permessiBlackBoxPersonaSorgente);
        Map<Integer, PermessoEntitaStoredProcedure> mappaArchiviConOggettoneBlackBoxCorrispondenteSorgente = getMappaArchiviConOggettoneBlackBoxCorrispondente(permessiBlackBoxPersonaSorgente);

        log.info("Prendo i permessi dell'utente destinazione sugli archivi coinvolti");
        // Ora mi interessa prendere tutti i permessi che ha la destinazione su questi archivi in mappaArchiviConPermessoPiuAltoPersonaSorgente,
        // Così da assicurarmi di non andare a cancellare dei permessi
        List<PermessoEntitaStoredProcedure> permessiBlackBoxPersonaDestinazione = getPermessiBlackBox(personaDestinazione, true, archiviList);
        Map<Integer, String> mappaArchiviConPermessoPiuAltoPersonaDestinazione = getMappaArchiviConPermessoPiuAltoDaPermessiBlackBox(permessiBlackBoxPersonaDestinazione);
        Map<Integer, PermessoEntitaStoredProcedure> mappaArchiviConOggettoneBlackBoxCorrispondenteDestinazione = getMappaArchiviConOggettoneBlackBoxCorrispondente(permessiBlackBoxPersonaDestinazione);

        /* A questo punto è il momento di preparare l'oggettone con i permessi da salvare
           per ogni arcchivio ci sono queste casistiche:
           CASO A - La destinazione non ha alcun permesso sull'archivio, il pemresso va quindi costruito e inserito
           CASO B - La destinazione ha dei permessi sull'archivio ma non di tipo ELIMINA; VISUALIZZA; MODIFICA, il permesso va quindi aggiunto all'oggettone
           CASO C - La destinazione ha un permesso più alto rispetto alla sorgente. Non va fatto nulla
           CASO D - La destinazione ha un permesso uguale alla sorgente, il pemresso va aggiornato nel caso la sorgente lo propagi e la destinaizone no. (Qui ci sono dei sottocasi quindi)
           CASO E - La destinazione ha un permesso più basso della sorgente. Qui va eliminato il vecchio permesso e aggiunto il nuovo.
         */
        List<PermessoEntitaStoredProcedure> permessiDaSalvare = new ArrayList();
        Table tableAnnotation = null;
        try {
            tableAnnotation = UtilityFunctions.getFirstAnnotationOverEntity(personaDestinazione.getClass(), Table.class);
        } catch (ClassNotFoundException ex) {
            throw new MasterjobsWorkerException(ex);
        }
        EntitaStoredProcedure entitaSoggetto = new EntitaStoredProcedure(personaDestinazione.getId(), tableAnnotation.schema(), tableAnnotation.name());

        log.info("Ciclo gli archivi per preparare l'oggettone da salvare");
        // Ciclo tutti gli archivi su cui la sorgente ha un permesso tra ELIMINA, MODIFICA, VISUALIZZA
        for (Integer idArchivio : mappaArchiviConPermessoPiuAltoPersonaSorgente.keySet()) {

//            if (!archiviInfo.containsKey(idArchivio)) {
//                archiviInfo.put(idArchivio, new ArrayList());
//            }
            String predicatoPiuAltoSorgente = mappaArchiviConPermessoPiuAltoPersonaSorgente.get(idArchivio);
            PermessoEntitaStoredProcedure oggettoneSorgente = mappaArchiviConOggettoneBlackBoxCorrispondenteSorgente.get(idArchivio);
            Boolean predicatoSorgentePropagato = isPredicatoPropagatoPerOggettoInQuestoOggettone(oggettoneSorgente, predicatoPiuAltoSorgente);

            String predicatoPiuAltoDestinazione = null;
            PermessoEntitaStoredProcedure oggettoneDestinazione = null;
            Boolean predicatoDestinazionePropagato = null;

            if (mappaArchiviConPermessoPiuAltoPersonaDestinazione.containsKey(idArchivio)) {
                // predicatoPiuAltoDestinazione potrebbe essere null perché magari la destinazione è vicario dell'archivio
                predicatoPiuAltoDestinazione = mappaArchiviConPermessoPiuAltoPersonaDestinazione.get(idArchivio);
                oggettoneDestinazione = mappaArchiviConOggettoneBlackBoxCorrispondenteDestinazione.get(idArchivio);
                if (predicatoPiuAltoDestinazione != null) {
                    predicatoDestinazionePropagato = isPredicatoPropagatoPerOggettoInQuestoOggettone(oggettoneDestinazione, predicatoPiuAltoDestinazione);
                }
            }

            // Ora devo capire se la sorgente ha un predicato più alto rispetto alla destinazione
            // NB: Se il predicato è lo stesso, allora devo capire se la sorgente ce l'ha propagato e la destinazione no
            // perchè in questo caso vogliamo andare ad aggiornare il permesso della destinazione propagandolo.
            // NB2: se il permesso in trasferimento è più basso del permesso già avuto, NON passa. indipendentemente rispetto alla propagazione.
            // NB3: se il permesso in trasferimento è più alto del permeso già avuto, passa. indipendentemente rispetto alla propagazione.
            Boolean devoInserireIlNuovoPermesso = true;

            if (predicatoPiuAltoDestinazione != null) {
                if (predicatoPiuAltoSorgente.equals(predicatoPiuAltoDestinazione)) {
                    // CASO D, devo vedere se la sorgente è porpagata e la destinaizone no
                    if (!(predicatoSorgentePropagato && !predicatoDestinazionePropagato)) {
                        // Non c'è bisongno di copiare il permesso
                        devoInserireIlNuovoPermesso = false;
                    }
                } else if (predicatoPiuAltoDestinazione.equals(PermessoArchivio.Predicato.ELIMINA.toString())
                        || (predicatoPiuAltoDestinazione.equals(PermessoArchivio.Predicato.MODIFICA.toString())
                        && predicatoPiuAltoSorgente.equals(PermessoArchivio.Predicato.VISUALIZZA.toString()))) {
                    // CASO C
                    devoInserireIlNuovoPermesso = false;
                }
            }

            if (devoInserireIlNuovoPermesso) {
                archiviInfo.get(idArchivio).getAbilitazioniAggiunte().add(predicatoPiuAltoSorgente);

                PermessoEntitaStoredProcedure nuovoOggettone;

                if (oggettoneDestinazione != null) {
                    /**
                     * La destinazione ha già un permessone su questo archivio,
                     * allora lo prendo, tolgo gli eventuali permessi di
                     * elimina, modifica, visualizza, poi inserisco il nuovo
                     * permesso, capendo se propagato o meno vedendo la
                     * sorgente.
                     */
                    List<PermessoStoredProcedure> p = oggettoneDestinazione.getCategorie().get(0).getPermessi();
                    List<PermessoStoredProcedure> permessiFiltrati = p.stream().filter(s
                            -> !s.getPredicato().equals(PermessoArchivio.Predicato.ELIMINA.toString())
                            && !s.getPredicato().equals(PermessoArchivio.Predicato.MODIFICA.toString())
                            && !s.getPredicato().equals(PermessoArchivio.Predicato.VISUALIZZA.toString()))
                            .collect(Collectors.toList());
                    permessiFiltrati.add(createPermessoStoredProcedure(
                            predicatoPiuAltoSorgente,
                            predicatoSorgentePropagato,
                            strutturaVeicolante
                    ));
                    oggettoneDestinazione.getCategorie().get(0).setPermessi(permessiFiltrati);
                    nuovoOggettone = oggettoneDestinazione;
                } else {
                    /* La destinazione non ha già qualche permesso su questo archivio, 
                       allora costusisco io l'oggettone
                     */
                    EntitaStoredProcedure entitaOggetto = new EntitaStoredProcedure(
                            oggettoneSorgente.getOggetto().getIdProvenienza(),
                            oggettoneSorgente.getOggetto().getSchema(),
                            oggettoneSorgente.getOggetto().getTable()
                    );
                    nuovoOggettone = new PermessoEntitaStoredProcedure(
                            entitaSoggetto,
                            entitaOggetto,
                            Arrays.asList(new CategoriaPermessiStoredProcedure[]{
                        new CategoriaPermessiStoredProcedure(
                        "SCRIPTA",
                        "ARCHIVIO",
                        Arrays.asList(new PermessoStoredProcedure[]{
                            createPermessoStoredProcedure(
                            predicatoPiuAltoSorgente,
                            predicatoSorgentePropagato,
                            strutturaVeicolante
                            )
                        })
                        )}
                            )
                    );
//                    oggettoneSorgente.setSoggetto(entitaSoggetto);
//                    oggettoneSorgente.getCategorie().get(0).setPermessi(Arrays.asList(new PermessoStoredProcedure[]{
//                        createPermessoStoredProcedure(
//                                predicatoPiuAltoSorgente,
//                                predicatoSorgentePropagato,\\
//                                strutturaVeicolante
//                        )
//                    }));
//                    nuovoOggettone = oggettoneSorgente;
                }

                permessiDaSalvare.add(nuovoOggettone);
            }
            // Altra cosa che devo fare in caso di TRASFERIMENTO, è spegnere i permessi della sorgente
            if (operationType.equals(MassiveActionLog.OperationType.TRASFERISCI_ABILITAZIONI)) {
                oggettoneSorgente.getCategorie().get(0).setPermessi(new ArrayList());
                permessiDaSalvare.add(oggettoneSorgente);
            }
        }

        if (!permessiDaSalvare.isEmpty()) {
            log.info("L'oggettone da salvare è pieno");
            try {
                permissionManager.managePermissions(permessiDaSalvare, null);
            } catch (BlackBoxPermissionException ex) {
                log.error("Errore nel salvataggio permessi blackbox", ex);
                throw new MasterjobsWorkerException("Errore nel salvataggio permessi blackbox");
            }
        }

        HashSet<Integer> idArchiviRadiceSuCuiAggiungereJob = new HashSet();

        // Mo loggo, mi serve una mappa dove per ogni archivio ho una lista della abilitazioni che la destinazione ha ricevuto.
        // La lista di abilitazioni può essere vuota nel caso in cui la destinazione già aveva l'abilitazione, ma la sorgente l'ha persa. Info utile in caso di trasferimento.
        log.info(String.format("Eseguo il krint e accodo il calcolo permessi su ogni archivio"));
        for (Map.Entry<Integer, InfoArchivio> entry : archiviInfo.entrySet()) {
            Integer idArchivio = entry.getKey();
            InfoArchivio info = entry.getValue();
//            for (String a : listaAbilitazioniOttenute) {
//                log.info("abilitazione ottenuta");
//            }
            OperazioneKrint.CodiceOperazione operazioneKrint = null;
            if (operationType.equals(MassiveActionLog.OperationType.COPIA_ABILITAZIONI)) {
                operazioneKrint = OperazioneKrint.CodiceOperazione.SCRIPTA_ARCHIVIO_COPIA_MASSIVA_ABILITAZIONI;
            } else {
                operazioneKrint = OperazioneKrint.CodiceOperazione.SCRIPTA_ARCHIVIO_TRASFERIMENTO_MASSIVO_ABILITAZIONI;
            }

            if (operationType.equals(MassiveActionLog.OperationType.TRASFERISCI_ABILITAZIONI) || !info.getAbilitazioniAggiunte().isEmpty()) {
                krintScriptaService.writeCopiaTrasferimentoAbilitazioniArchivi(
                        idArchivio,
                        personaSorgente,
                        personaDestinazione,
                        info.getAbilitazioniAggiunte(),
                        info.getNumerazioneGerarchica(),
                        info.getIdArchivioRadice(),
                        idMassiveActionLog,
                        operazioneKrint
                );

                idArchiviRadiceSuCuiAggiungereJob.add(info.getIdArchivioRadice());
            }
        }

        if (!idArchiviRadiceSuCuiAggiungereJob.isEmpty()) {
            for (Integer idArchivioRadice : idArchiviRadiceSuCuiAggiungereJob) {
                JobNotified jn = new JobNotified();
                jn.setJobName("CalcoloPermessiGerarchiaArchivioJobWorker");
                jn.setJobData(objectMapper.convertValue(new CalcoloPermessiGerarchiaArchivioJobWorkerData(idArchivioRadice), Map.class));
                jn.setWaitObject(false);
                jn.setApp(app.getId());
                jn.setPriority(Set.SetPriority.NORMAL);
                jn.setSkipIfAlreadyPresent(Boolean.TRUE);
                jobNotifiedRepository.save(jn);
            }
        }

        log.info(String.format("Inserisco la notifica sulla scrivania dell'AG"));
        ZonedDateTime dataOraOperazione = m.getInsertionDate();
        DateTimeFormatter formatterData = DateTimeFormatter.ofPattern("dd-MM-yyyy");
        String dataFormattata = dataOraOperazione.format(formatterData);
        DateTimeFormatter formatterOrario = DateTimeFormatter.ofPattern("HH:mm");
        String orarioFormattato = dataOraOperazione.format(formatterOrario);

        String oggettoAttivita = null;
        if (operationType.equals(MassiveActionLog.OperationType.TRASFERISCI_ABILITAZIONI)) {
            oggettoAttivita = String.format("Il trasferimento delle abilitazioni sui fascicoli di %1$s a %2$s, che hai richiesto il %3$s alle %4$s, è avvenuta con successo.",
                    personaSorgente.getDescrizione(),
                    personaDestinazione.getDescrizione(),
                    dataFormattata,
                    orarioFormattato);
            insertAttivita(azienda, personaOperazione, "Trasferimento abilitazioni", oggettoAttivita, app);
        } else {
            oggettoAttivita = String.format("La copia delle abilitazioni sui fascicoli di %1$s a %2$s, che hai richiesto il %3$s alle %4$s, è avvenuta con successo.",
                    personaSorgente.getDescrizione(),
                    personaDestinazione.getDescrizione(),
                    dataFormattata,
                    orarioFormattato);
            insertAttivita(azienda, personaOperazione, "Copia abilitazioni", oggettoAttivita, app);
        }

        log.info(String.format("Aggiorno la massiveActionLog"));
        m.setCompletionDate(ZonedDateTime.now());
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

    /**
     * Per la persona passata vengono recuperati tutti i permessi su archivi non
     * virtuali in cui la persona ha permesso con predicato tra ELIMINA,
     * MODIFICA, VISUALIZZA. Se viene passato a true tuttiPredicati, allora
     * vengono aggiunti ulteiori predicati di filtro e se viene passato un
     * elenco archivi allora questi vengono aggiunti come filtro sugli oggetti
     *
     * @param persona
     * @return
     * @throws BlackBoxPermissionException
     */
    private List<PermessoEntitaStoredProcedure> getPermessiBlackBox(
            Persona persona,
            Boolean tuttiPredicati,
            List<Archivio> archivi,
            ZonedDateTime dataInizio
    ) throws MasterjobsWorkerException {
        List<String> predicati = new ArrayList<>();
        predicati.add(PermessoArchivio.Predicato.VISUALIZZA.toString()); // 2
        predicati.add(PermessoArchivio.Predicato.MODIFICA.toString()); // 4
        predicati.add(PermessoArchivio.Predicato.ELIMINA.toString()); // 8
        if (tuttiPredicati) {
            predicati.add(PermessoArchivio.Predicato.PASSAGGIO.toString());
            predicati.add("BLOCCO");
            predicati.add("NON_PROPAGATO");
            predicati.add(PermessoArchivio.Predicato.VICARIO.toString());
            predicati.add(PermessoArchivio.Predicato.RESPONSABILE_PROPOSTO.toString());
            predicati.add(PermessoArchivio.Predicato.RESPONSABILE.toString());
        }
        List<String> ambiti = new ArrayList<>();
        ambiti.add("SCRIPTA");
        List<String> tipi = new ArrayList<>();
        tipi.add("ARCHIVIO");
        List<PermessoEntitaStoredProcedure> permissionsOfSubjectActualFromDate;      
        try {
            permissionsOfSubjectActualFromDate = permissionManager.getPermissionsOfSubjectActualFromDate(
                    persona,
                    archivi,
                    predicati,
                    ambiti,
                    tipi,
                    false,
                    dataInizio != null ? dataInizio.toLocalDate() : null
            );
        } catch (BlackBoxPermissionException ex) {
            log.error("Errore nel recupero permessi da blackbox", ex);
            throw new MasterjobsWorkerException("Errore nel recupero permessi da blackbox");
        }
        return permissionsOfSubjectActualFromDate;
    }

    private List<PermessoEntitaStoredProcedure> getPermessiBlackBox(
            Persona persona,
            Boolean tuttiPredicati,
            List<Archivio> archivi
    ) throws MasterjobsWorkerException {
        return getPermessiBlackBox(persona, tuttiPredicati, archivi, null);
    }

    /**
     * Data una List<PermessoEntitaStoredProcedure> di permessi su archivi torna
     * una mappa in cui ogni chiave è un idArchivio e ogni valore è il predicato
     * più alto per quell'archivio tra ELIMINA, MODIFICA, VISUALIZZA. Si presume
     * quindi che il soggetto non sia importante (sia sempre lo stesso)
     *
     * @param oggettoneList
     * @return
     */
    private Map<Integer, String> getMappaArchiviConPermessoPiuAltoDaPermessiBlackBox(List<PermessoEntitaStoredProcedure> oggettoneList) {
        Map<Integer, String> m = new HashMap();
        if (oggettoneList != null) {
            for (PermessoEntitaStoredProcedure oggettone : oggettoneList) {
                List<PermessoStoredProcedure> permessi = oggettone.getCategorie().get(0).getPermessi();
                m.put(oggettone.getOggetto().getIdProvenienza(), getPermessoPiuAlto(permessi));
            }
        }
        return m;
    }

    /**
     * Torna una mappa in cui la chiave è l'id provenienza dell'oggetto e il
     * valore è il PermessoEntitaStoredProcedure corrispondente
     *
     * @param oggettoneList
     * @return
     */
    private Map<Integer, PermessoEntitaStoredProcedure> getMappaArchiviConOggettoneBlackBoxCorrispondente(List<PermessoEntitaStoredProcedure> oggettoneList) {
        Map<Integer, PermessoEntitaStoredProcedure> m = new HashMap();
        if (oggettoneList != null) {
            for (PermessoEntitaStoredProcedure oggettone : oggettoneList) {
                m.put(oggettone.getOggetto().getIdProvenienza(), oggettone);
            }
        }
        return m;
    }

    /**
     * Data una lista di PermessoStoredProcedure, torna il permesso più alto
     * presente nella lista tra ELIMINA, MODIFICA, VISUALIZZA.
     *
     * @param permessi
     * @return
     */
    private String getPermessoPiuAlto(List<PermessoStoredProcedure> permessi) {
        String permessoPiuAlto = null;
        for (PermessoStoredProcedure permesso : permessi) {
            String predicato = permesso.getPredicato();
            if (predicato.equals(PermessoArchivio.Predicato.ELIMINA.toString())) {
                permessoPiuAlto = PermessoArchivio.Predicato.ELIMINA.toString();
                break;
            } else if (predicato.equals(PermessoArchivio.Predicato.MODIFICA.toString())) {
                permessoPiuAlto = PermessoArchivio.Predicato.MODIFICA.toString();
            } else if (permessoPiuAlto == null && predicato.equals(PermessoArchivio.Predicato.VISUALIZZA.toString())) {
                permessoPiuAlto = PermessoArchivio.Predicato.VISUALIZZA.toString();
            }
        }
        return permessoPiuAlto;
    }

    /**
     * Creo un permesso per la blackbox.
     *
     * @param predicato
     * @param propagaOggetto
     * @param strutturaVeicolante
     * @return
     * @throws MasterjobsWorkerException
     */
    private PermessoStoredProcedure createPermessoStoredProcedure(String predicato, Boolean propagaOggetto, Struttura strutturaVeicolante) throws MasterjobsWorkerException {
        Table tableAnnotation = null;
        try {
            tableAnnotation = UtilityFunctions.getFirstAnnotationOverEntity(strutturaVeicolante.getClass(), Table.class);
        } catch (ClassNotFoundException ex) {
            throw new MasterjobsWorkerException(ex);
        }

        PermessoStoredProcedure permessoStoredProcedure = new PermessoStoredProcedure();
        permessoStoredProcedure.setPredicato(predicato);
        permessoStoredProcedure.setPropagaSoggetto(false);
        permessoStoredProcedure.setPropagaOggetto(propagaOggetto);
        permessoStoredProcedure.setOriginePermesso("CopiaTrasferisciAbilitazioniArchiviJobWorker");
        EntitaStoredProcedure entitaVeicolante = new EntitaStoredProcedure(strutturaVeicolante.getId(), tableAnnotation.schema(), tableAnnotation.name());
        permessoStoredProcedure.setEntitaVeicolante(entitaVeicolante);

        return permessoStoredProcedure;
    }

    /**
     * Ci si aspetta un unica categoria.
     */
    private Boolean isPredicatoPropagatoPerOggettoInQuestoOggettone(PermessoEntitaStoredProcedure oggettone, String predicato) {
        List<PermessoStoredProcedure> list = oggettone.getCategorie().get(0).getPermessi().stream().filter(p -> p.getPredicato().equals(predicato)).collect(Collectors.toList());
        PermessoStoredProcedure permesso = list.get(0);
        return permesso.getPropagaOggetto();
    }

    private void insertAttivita(Azienda azienda, Persona persona, String descrizioneAzione, String oggetto, Applicazione app) {
        Attivita a = new Attivita();
        a.setIdAzienda(azienda);
        a.setIdPersona(persona);
        a.setIdApplicazione(app);
        a.setTipo(Attivita.TipoAttivita.NOTIFICA.toString().toLowerCase());
        a.setOggetto(oggetto);
        a.setDescrizione(descrizioneAzione);
        a.setProvenienza("Amministrazione Gedi");
        attivitaRepository.saveAndFlush(a);
    }
}
