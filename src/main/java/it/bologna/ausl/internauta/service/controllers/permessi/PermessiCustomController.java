package it.bologna.ausl.internauta.service.controllers.permessi;

import com.fasterxml.jackson.core.JsonProcessingException;
import it.bologna.ausl.internauta.service.utils.CacheUtilities;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import it.bologna.ausl.blackbox.PermissionManager;
import it.bologna.ausl.blackbox.PermissionRepositoryAccess;
import it.bologna.ausl.blackbox.exceptions.BlackBoxPermissionException;
import it.bologna.ausl.internauta.service.authorization.AuthenticatedSessionData;
import it.bologna.ausl.internauta.service.authorization.AuthenticatedSessionDataBuilder;
import it.bologna.ausl.internauta.model.bds.types.PermessoEntitaStoredProcedure;
import it.bologna.ausl.internauta.model.bds.types.PermessoStoredProcedure;
import it.bologna.ausl.internauta.service.authorization.UserInfoService;
import it.bologna.ausl.internauta.service.exceptions.AuthorizationException;
import it.bologna.ausl.internauta.service.exceptions.http.ControllerHandledExceptions;
import it.bologna.ausl.internauta.service.exceptions.http.Http400ResponseException;
import it.bologna.ausl.internauta.service.exceptions.http.Http403ResponseException;
import it.bologna.ausl.internauta.service.exceptions.http.Http409ResponseException;
import it.bologna.ausl.internauta.service.exceptions.http.HttpInternautaResponseException;
import it.bologna.ausl.internauta.service.krint.KrintUtils;
import it.bologna.ausl.internauta.service.permessi.PermessoError;
import it.bologna.ausl.internauta.service.repositories.baborg.AziendaRepository;
import it.bologna.ausl.internauta.service.repositories.baborg.PersonaRepository;
import it.bologna.ausl.internauta.service.repositories.baborg.UtenteRepository;
import it.bologna.ausl.internauta.service.repositories.scripta.ArchivioRepository;
import it.bologna.ausl.internauta.service.utils.CachedEntities;
import static it.bologna.ausl.internauta.service.utils.InternautaConstants.Permessi.Ambiti.PECG;
import static it.bologna.ausl.internauta.service.utils.InternautaConstants.Permessi.Tipi.PEC;
import static it.bologna.ausl.internauta.service.utils.InternautaConstants.Permessi.Ambiti.MATRINT;
import static it.bologna.ausl.internauta.service.utils.InternautaConstants.Permessi.Tipi.DELEGA;
import static it.bologna.ausl.internauta.service.utils.InternautaConstants.Permessi.Predicati;
import it.bologna.ausl.internauta.model.bds.types.CategoriaPermessiStoredProcedure;
import it.bologna.ausl.internauta.model.bds.types.EntitaStoredProcedure;
import it.bologna.ausl.model.entities.baborg.Azienda;
import it.bologna.ausl.model.entities.baborg.Pec;
import it.bologna.ausl.model.entities.baborg.PecAzienda;
import it.bologna.ausl.model.entities.baborg.Persona;
import it.bologna.ausl.model.entities.baborg.Ruolo;
import it.bologna.ausl.model.entities.baborg.Struttura;
import it.bologna.ausl.model.entities.baborg.Utente;
import it.bologna.ausl.model.entities.scripta.Archivio;
import it.bologna.ausl.model.entities.scripta.projections.archivio.ArchivioProjectionUtils;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 *
 * @author gusgus
 */
@RestController
@RequestMapping(value = "${permessi.mapping.url.root}")
public class PermessiCustomController implements ControllerHandledExceptions {

    private static final Logger LOGGER = LoggerFactory.getLogger(PermessiCustomController.class);

    @Autowired
    CachedEntities cachedEntities;

    @PersistenceContext
    EntityManager eM;

    @Autowired
    PermissionRepositoryAccess permissionRepositoryAccess;

    @Autowired
    PermissionManager permissionManager;

    @Autowired
    CacheUtilities permessiUtilities;

    @Autowired
    UserInfoService userInfoService;

    @Autowired
    PersonaRepository personaRepository;

    @Autowired
    AziendaRepository aziendaRepository;

    @Autowired
    UtenteRepository utenteRepository;

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    private AuthenticatedSessionDataBuilder authenticatedSessionDataBuilder;
    
    @Autowired
    private KrintUtils krintUtils;
    
    @Autowired
    private ArchivioProjectionUtils archivioProjectionUtils;
    
    @Autowired
    private ArchivioRepository archivioRepository;

    /**
     * E' il controller base.Riceve una lista di PermessoEntitaStoredProcedure e
     * chiama direttamente la managePermissions la quale di fatto passaera la
     * lista di PermessoEntitaStoredProcedure alla store procedute. Attenzione:
     * usando questo controller non verrà eseguito nessun controllo di
     * sicurezza.
     *
     * @param permessiEntita
     * @param request
     * @throws BlackBoxPermissionException
     */
    @RequestMapping(value = "managePermissions", method = RequestMethod.POST)
    public void updatePermesso(@RequestBody List<PermessoEntitaStoredProcedure> permessiEntita, HttpServletRequest request) throws BlackBoxPermissionException {
        permissionRepositoryAccess.managePermissions(permessiEntita, null);
    }

    /**
     * Questa funzione si occupa di recuperare i delegati visibili al CI/CA
     *
     * @param aziendaSelezionata
     * @return
     * @throws BlackBoxPermissionException
     * @throws
     * it.bologna.ausl.internauta.service.exceptions.AuthorizationException
     */
    @RequestMapping(value = "getDelegatiMatrint", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List> getDelegatiMatrint(
            @RequestParam("idAziendaSelezionata") Integer idAziendaSelezionata
    ) throws BlackBoxPermissionException, AuthorizationException {

        AuthenticatedSessionData authenticatedUserProperties = authenticatedSessionDataBuilder.getAuthenticatedUserProperties();
        Utente utente = authenticatedUserProperties.getUser();
        Persona persona = utente.getIdPersona();

        if (!userInfoService.isCI(utente) && !userInfoService.isCA(utente) && !userInfoService.isR(utente, Ruolo.ModuliRuolo.GENERALE)) {
            throw new AuthorizationException("Utente non CA/CI/R non può caricare delegati");
        }

        if (!userInfoService.isCI(utente) && userInfoService.isCA(utente)) {
            // Dunque è un CA, ma lo è dell'azienda giusta?
            List<Azienda> aziendeWherePersonaIsCa = userInfoService.getAziendeWherePersonaIsCa(persona);
            if (!aziendeWherePersonaIsCa.stream().anyMatch(o -> o.getId().equals(idAziendaSelezionata))) {
                throw new AuthorizationException("Utente CA ma non dell'azienda richiesta");
            }
        } else if (!userInfoService.isCI(utente) && !userInfoService.isCA(utente)) {
            // Dunque è un R, ma lo è dell'azienda giusta?
            List<Azienda> aziendeWherePersonaIsR = userInfoService.getAziendeWherePersonaIsR(persona);
            if (!aziendeWherePersonaIsR.stream().anyMatch(o -> o.getId().equals(idAziendaSelezionata))) {
                throw new AuthorizationException("Utente R ma non dell'azienda richiesta");
            }
        }

        Azienda azienda = aziendaRepository.getOne(idAziendaSelezionata);

        List<PermessoEntitaStoredProcedure> res = permissionManager.getPermissionsByPredicate(Predicati.DELEGA.toString(), MATRINT.toString(), DELEGA.toString(), azienda, azienda);

        if (res != null) {
            for (PermessoEntitaStoredProcedure permesso : res) {
                EntitaStoredProcedure soggetto = permesso.getSoggetto();
                EntitaStoredProcedure oggetto = permesso.getOggetto();
                String descrizioneSoggetto = utenteRepository.getOne(soggetto.getIdProvenienza()).getIdPersona().getDescrizione();
                String descrizioneOggetto = utenteRepository.getOne(oggetto.getIdProvenienza()).getIdPersona().getDescrizione();
                soggetto.setDescrizione(descrizioneSoggetto);
                oggetto.setDescrizione(descrizioneOggetto);

            }
        }

        return new ResponseEntity(res, HttpStatus.OK);
    }

    /**
     * Questo codice sembra poter tornare utile in futuro! non cancellare!
     * Set<Class<?>> entityClasses = new
     * Reflections("it.bologna.ausl.model.entities").getTypesAnnotatedWith(Entity.class);
     *
     * Class<?> utenteClass = Utente.class; HashMap<String, Class>
     * hashMapSchemaTable = new HashMap<String, Class>();
     *
     * for (Class entityClass : entityClasses) { LOGGER.info("classe trovata: "
     * + entityClass.getName()); Table annotation = (Table)
     * entityClass.getAnnotation(Table.class); String schema =
     * annotation.schema(); String name = annotation.name();
     * hashMapSchemaTable.put(schema + "--" + name, entityClass); }
     *
     * for (PermessoEntitaStoredProcedure permesso : res) {
     * EntitaStoredProcedure soggetto = permesso.getSoggetto(); String
     * schemaSoggetto = soggetto.getSchema(); String tableSoggetto =
     * soggetto.getTable(); EntitaStoredProcedure oggetto =
     * permesso.getOggetto(); String schemaOggetto = oggetto.getSchema(); String
     * tableOggetto = oggetto.getTable();
     *
     * EntityInterface findSoggetto = (EntityInterface)
     * eM.find(hashMapSchemaTable.get(schemaSoggetto + "--" + tableSoggetto),
     * soggetto.getIdProvenienza()); EntityInterface findOggetto =
     * (EntityInterface) eM.find(hashMapSchemaTable.get(schemaOggetto + "--" +
     * tableOggetto), oggetto.getIdProvenienza());
     *
     * soggetto.setDescrizione(findSoggetto.getEntityDescription());
     * oggetto.setDescrizione(findOggetto.getEntityDescription()); }
     */
    
    
    /**
     * E' il controller che gestisce i permessi per i Gestori PEC. Prima della
     * chiamata alla Black Box viene controllato che l'utente loggato sia
     * autorizzato a gestire la PEC e la persona. In particolare: - Il CI può
     * gestire qualunque PEC e persona. - Il CA può: - Gestire PEC collegate
     * all'azienda di cui è CA. - Gestire persone che abbiano un utente attivo
     * nell'azienda di cui è CA.
     *
     * @param json - Contiene LISTE DI tre oggetti: 1 - persona - E' la persona
     * che sta ricevendo il permesso 2 - pec - E' la pec su cui il soggetto avrà
     * il permesso. Avrà la pecAziendaList già espansa, con l'idAzienda già
     * espanso. 3 - permesso - E' di tipo PermessoStoredProcedure, conterrà
     * predicato, originePermesso.
     * @param request
     * @throws BlackBoxPermissionException
     * @throws
     * it.bologna.ausl.internauta.service.exceptions.http.HttpInternautaResponseException
     */
    @Transactional
    @RequestMapping(value = "managePermissionsGestoriPec", method = RequestMethod.POST)
    public void managePermissionsGestoriPec(@RequestBody List<Map<String, Object>> json, HttpServletRequest request) throws BlackBoxPermissionException, HttpInternautaResponseException {

        List<Map<String, Object>> data;
        // Controllo che i dati nella richiesta rispettino gli standard richiesti
        try {
            data = objectMapper.convertValue(json, new TypeReference<List<Map<String, Object>>>() {
            });
        } catch (IllegalArgumentException ex) {
            LOGGER.error("Errore nel casting della persona.", ex);
            throw new Http400ResponseException("1", "Errore nel casting della persona.");
        }

        System.out.println(data);

        for (Map<String, Object> element : data) {
            Persona persona;
            Pec pec;
            PermessoStoredProcedure permesso;

            // Controllo che ogni elemento abbia i dati che mi aspetto
            try {
                persona = objectMapper.convertValue(element.get("persona"), Persona.class);
            } catch (IllegalArgumentException ex) {
                LOGGER.error("Errore nel casting della persona.", ex);
                throw new Http400ResponseException("1", "Errore nel casting della persona.");
            }

            try {
                pec = objectMapper.convertValue(element.get("pec"), Pec.class);
            } catch (IllegalArgumentException ex) {
                LOGGER.error("Errore nel casting della pec.", ex);
                throw new Http400ResponseException("2", "Errore nel casting della pec.");
            }

            try {
                permesso = objectMapper.convertValue(element.get("permesso"), PermessoStoredProcedure.class);
            } catch (IllegalArgumentException ex) {
                LOGGER.error("Errore nel casting del permesso.", ex);
                throw new Http400ResponseException("3", "Errore nel casting del permesso.");
            }

            if (pec.getPecAziendaList() == null) {
                throw new Http400ResponseException("6", "La pec passata non ha il campo pecAziendaList espanso.");
            }

            if (!pec.getPecAziendaList().isEmpty()) {
                for (PecAzienda pa : pec.getPecAziendaList()) {
                    if (pa.getIdAzienda() == null) {
                        throw new Http400ResponseException("7", "Le entità della pecAziendaList non hanno l'idAzienda espanso.");
                    }
                }
            } else {
                throw new Http403ResponseException("1", "Non è possibile associare un permesso su una pec non collegata ad alcuna azienda.");
            }

            List<Integer> idAziendePec = pec.getPecAziendaList().stream().map(pecAzienda -> pecAzienda.getIdAzienda().getId()).collect(Collectors.toList());
            List<Integer> idAziendePersona;
            if (persona.getAttiva()) {
                idAziendePersona = userInfoService.getAziendePersona(persona).stream().map(azienda -> (azienda.getId())).collect(Collectors.toList());
            } else {
                idAziendePersona = userInfoService.getAziendePersonaSpenta(persona).stream().map(azienda -> (azienda.getId())).collect(Collectors.toList());
            }
//            List<Integer> idAziendePersona = userInfoService.getAziendePersona(persona).stream().map(azienda -> (azienda.getId())).collect(Collectors.toList());

            if (permesso != null) {
                LOGGER.info("Sto aggiungendo un pemesso, verifico che Pec e Persona abbiano aziende comuni...");
                LOGGER.info("idAziendePec " + idAziendePec.toString() + ", idAziendePersona " + idAziendePersona.toString());
                if (Collections.disjoint(idAziendePec, idAziendePersona)) {
                    throw new Http403ResponseException("2", "Pec e Persona passati non hanno aziende in comune.");
                }
                LOGGER.info("Verificato, regolare.");

            }

            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            Utente loggedUser = (Utente) authentication.getPrincipal();

            if (!userInfoService.isCI(loggedUser)) {
                Persona personaLogged = personaRepository.getOne(loggedUser.getIdPersona().getId());
                List<Integer> idAziendeCA = userInfoService.getAziendeWherePersonaIsCa(personaLogged).stream().map(azienda -> azienda.getId()).collect(Collectors.toList());

                if (idAziendeCA == null || idAziendeCA.isEmpty()) {
                    // Non sono ne CA ne CI fermo tutto.
                    throw new Http403ResponseException("3", "L'utente loggato non è ne CI ne CA.");
                } else {

                    if (Collections.disjoint(idAziendeCA, idAziendePec)) {
                        // Nessuna azienda associata alla pec è un azienda del CA, fermo tutto.
                        throw new Http403ResponseException("4", "L'utente loggato non è CA di almeno un'azienda della pec.");
                    }

                    if (Collections.disjoint(idAziendeCA, idAziendePersona)) {
                        // Nessuna utente della persona appartiene ad un azienda del CA, fermo tutto.
                        throw new Http403ResponseException("5", "L'utente loggato non è CA di almeno un'azienda degli utenti della persona.");
                    }
                }
            }

            List<PermessoStoredProcedure> permessi;
            if (permesso != null) {
                permessi = Arrays.asList(new PermessoStoredProcedure[]{permesso});
            } else {
                permessi = new ArrayList<>();
            }

            permissionManager.managePermissions(persona, pec, PECG.toString(), PEC.toString(), permessi, null);

        }
    }

    /**
     * E' il controller che gestisce i permessi per le associazioni
     * PEC-Struttura. Prima della chiamata alla Black Box viene controllato che
     * l'utente loggato sia autorizzato a gestire la PEC e la struttura. In
     * particolare: - Il CI può gestire qualunque PEC e struttura. - Il CA può:
     * - Gestire PEC collegate all'azienda di cui è CA. - Gestire strutture che
     * appartengano all'azienda di cui è CA.
     *
     * @param json - Contiene tre oggetti: 1 - struttura - E' la struttura che
     * sta ricevendo il permesso 2 - pec - E' la pec su cui il soggetto avrà il
     * permesso. Avrà la pecAziendaList già espansa, con l'idAzienda già
     * espanso. 3 - permesso - E' di tipo PermessoStoredProcedure, conterrà
     * predicato, originePermesso e propagaSoggetto.
     * @param request
     * @throws
     * it.bologna.ausl.internauta.service.exceptions.http.HttpInternautaResponseException
     * @throws it.bologna.ausl.blackbox.exceptions.BlackBoxPermissionException
     */
    @RequestMapping(value = "managePermissionsAssociazioniPecStruttura", method = RequestMethod.POST)
    public void managePermissionsAssociazioniPecStruttura(@RequestBody Map<String, Object> json, HttpServletRequest request) throws HttpInternautaResponseException, BlackBoxPermissionException {
        Struttura struttura;
        Pec pec;
        PermessoStoredProcedure permesso;

        // Controllo che i dati nella richiesta rispettino gli standard richiesti
        try {
            struttura = objectMapper.convertValue(json.get("struttura"), Struttura.class);
        } catch (IllegalArgumentException ex) {
            throw new Http400ResponseException("1", "Errore nel casting della struttura.");
        }

        try {
            pec = objectMapper.convertValue(json.get("pec"), Pec.class);
        } catch (IllegalArgumentException ex) {
            throw new Http400ResponseException("2", "Errore nel casting della pec.");
        }

        try {
            permesso = objectMapper.convertValue(json.get("permesso"), PermessoStoredProcedure.class);
        } catch (IllegalArgumentException ex) {
            throw new Http400ResponseException("3", "Errore nel casting del permesso.");
        }
        if (pec.getPecAziendaList() == null) {
            throw new Http400ResponseException("7", "La pec passata non ha il campo pecAziendaList espanso.");
        }

        if (!pec.getPecAziendaList().isEmpty()) {
            for (PecAzienda pa : pec.getPecAziendaList()) {
                if (pa.getIdAzienda() == null) {
                    throw new Http400ResponseException("8", "Le entità della pecAziendaList non hanno l'idAzienda espanso.");
                }
            }
        } else {
            throw new Http403ResponseException("1", "Non è possibile associare un permesso su una pec non collegata ad alcuna azienda.");
        }

        List<Integer> idAziendePec = pec.getPecAziendaList().stream().map(pecAzienda -> pecAzienda.getIdAzienda().getId()).collect(Collectors.toList());

        if (!idAziendePec.contains(struttura.getIdAzienda().getId())) {
            throw new Http403ResponseException("2", "Pec e Struttura passati non hanno aziende in comune.");
        }

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Utente loggedUser = (Utente) authentication.getPrincipal();

        if (!userInfoService.isCI(loggedUser)) {
            Persona personaLogged = personaRepository.getOne(loggedUser.getIdPersona().getId());
            List<Integer> idAziendeCA = userInfoService.getAziendeWherePersonaIsCa(personaLogged).stream().map(azienda -> azienda.getId()).collect(Collectors.toList());

            if (idAziendeCA == null || idAziendeCA.isEmpty()) {
                // Non sono ne CA ne CI fermo tutto.
                throw new Http403ResponseException("3", "L'utente loggato non è ne CI ne CA.");
            } else {

                if (Collections.disjoint(idAziendeCA, idAziendePec)) {
                    // Nessuna azienda associata alla pec è un azienda del CA, fermo tutto.
                    throw new Http403ResponseException("4", "L'utente loggato non è CA di almeno un'azienda della pec.");
                }

                if (!idAziendeCA.contains(struttura.getIdAzienda().getId())) {
                    throw new Http403ResponseException("5", "L'utente loggato non è CA della azienda della struttura.");
                }
            }
        }

        permissionManager.managePermissions(struttura, pec, PECG.toString(), PEC.toString(), Arrays.asList(new PermessoStoredProcedure[]{permesso}), null);
    }

    /**
     *
     * @param params mappa che contiene permessiEntita Date Lista
     * PermessoAggiunto
     * @param request
     * @throws BlackBoxPermissionException
     * @throws
     * it.bologna.ausl.internauta.service.exceptions.http.Http409ResponseException
     * @throws com.fasterxml.jackson.core.JsonProcessingException
     */
    @Transactional(rollbackFor = Throwable.class)
    @RequestMapping(value = "managePermissionsAdvanced", method = RequestMethod.POST)
    public void managePermissionsAdvanced(@RequestBody Map<String, Object> params, HttpServletRequest request) throws BlackBoxPermissionException, Http409ResponseException, JsonProcessingException {
        List<PermessoEntitaStoredProcedure> permessiEntita = objectMapper.convertValue(params.get("permessiEntita"), new TypeReference<List<PermessoEntitaStoredProcedure>>() {});
        List<PermessoEntitaStoredProcedure> permessiAggiunti = null;

        LocalDate dataDiLavoro = LocalDate.now();
        if (params.containsKey("dataDiLavoro") && params.get("dataDiLavoro") != null) {
            Long dataMillis = (Long) params.get("dataDiLavoro");
            dataDiLavoro = Instant.ofEpochMilli(dataMillis).atZone(ZoneId.systemDefault()).toLocalDate();
        }
        List<PermessoError> risultanze = new ArrayList<>();
        List<String> ambiti = null;
        if (params.get("ambitiInteressati") != null) {
            ambiti = (List<String>) params.get("ambitiInteressati");
        }

        List<String> tipi = null;
        if (params.get("ambitiInteressati") != null) {
            tipi = (List<String>) params.get("tipiInteressati");
        }

        /*
            Se sono stati aggiunti permessi allora controllo se c'è un permesso nel futuro. Perché in quel caso devo bloccare l'aggiunta del permesso
            nel caso che il nuovo permesso ha una data fine nulla o che supera la data inizio del permesso futuro
         */
        if (params.get("permessiAggiunti") != null) {
            permessiAggiunti = objectMapper.convertValue(params.get("permessiAggiunti"), new TypeReference<List<PermessoEntitaStoredProcedure>>() {
            });

            for (PermessoEntitaStoredProcedure permessiAggiunto : permessiAggiunti) {

                for (CategoriaPermessiStoredProcedure categoriaPermessiStoredProcedure : permessiAggiunto.getCategorie()) {

                    for (PermessoStoredProcedure permessoStoredProcedure : categoriaPermessiStoredProcedure.getPermessi()) {

                        List<PermessoEntitaStoredProcedure> risposte;
                        risposte = permissionRepositoryAccess.getPermissionsOfSubjectFutureFromDate(
                                permessiAggiunto.getSoggetto(),
                                Lists.newArrayList(permessiAggiunto.getOggetto()),
                                Lists.newArrayList(permessoStoredProcedure.getPredicato()),
                                Lists.newArrayList(categoriaPermessiStoredProcedure.getAmbito()),
                                Lists.newArrayList(categoriaPermessiStoredProcedure.getTipo()),
                                true,
                                permessoStoredProcedure.getAttivoDal() != null ? permessoStoredProcedure.getAttivoDal().atZone(ZoneId.systemDefault()) : null,
                                permessoStoredProcedure.getAttivoAl() != null ? permessoStoredProcedure.getAttivoAl().atZone(ZoneId.systemDefault()) : null);

                        if (risposte != null && !risposte.isEmpty()) {
                            for (PermessoEntitaStoredProcedure risposta : risposte) {
                                for (CategoriaPermessiStoredProcedure resCategoria : risposta.getCategorie()) {

                                    for (PermessoStoredProcedure resPermesso : resCategoria.getPermessi()) {
                                        if (permessoStoredProcedure.getAttivoAl() == null || permessoStoredProcedure.getAttivoAl().isAfter(resPermesso.getAttivoDal())) {
                                            String soggetto = permessiAggiunto.getSoggetto().getIdProvenienza().toString();
                                            String oggetto = permessiAggiunto.getOggetto().getIdProvenienza().toString();
                                            String predicato = permessoStoredProcedure.getPredicato();
                                            String ambito = categoriaPermessiStoredProcedure.getAmbito();
                                            String tipo = categoriaPermessiStoredProcedure.getTipo();
                                            PermessoError permessoError = new PermessoError(soggetto, oggetto, predicato, ambito, tipo,
                                                    resPermesso.getAttivoDal().toLocalDate(), resPermesso.getAttivoAl() != null ? resPermesso.getAttivoAl().toLocalDate() : null);
                                            risultanze.add(permessoError);
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        if (!risultanze.isEmpty()) {
            throw new Http409ResponseException("permesso_futuro", objectMapper.writeValueAsString(risultanze));
        }
        
                
        // Clono
        List<PermessoEntitaStoredProcedure> permessiListClone = objectMapper.convertValue(permessiEntita, new TypeReference<List<PermessoEntitaStoredProcedure>>(){});
        
        krintUtils.manageKrintPermissions(permessiListClone);
        permissionRepositoryAccess.managePermissions(permessiEntita, dataDiLavoro);
        Set<Integer> idUtentiSet = new HashSet();

        permessiEntita.stream().forEach(p -> {
            if (p.getSoggetto().getTable().equals("utenti")) {
                idUtentiSet.add(p.getSoggetto().getIdProvenienza());
            }

        });

        idUtentiSet.forEach(idUtente -> {
            Utente u = utenteRepository.getOne(idUtente);
            Persona idPersona = u.getIdPersona();
            List<Utente> utentiPersona = userInfoService.getUtentiPersona(idPersona);
            utentiPersona.forEach(utente -> {
                permessiUtilities.cleanCachePermessiUtente(utente.getId());
            });
        });
    }
    
    @RequestMapping(value = "getPermessiArchivio", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List> getPermessiArchivio(
            @RequestParam("idArchivio") Integer idArchivio
    ) throws BlackBoxPermissionException, AuthorizationException {
        Archivio archivio = archivioRepository.getById(idArchivio);
        List<PermessoEntitaStoredProcedure> permessi = archivioProjectionUtils.getPermessi(archivio);
        return new ResponseEntity(permessi, HttpStatus.OK);
    }
    
}
