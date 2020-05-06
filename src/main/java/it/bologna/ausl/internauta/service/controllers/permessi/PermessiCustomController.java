package it.bologna.ausl.internauta.service.controllers.permessi;

import com.fasterxml.jackson.core.JsonProcessingException;
import it.bologna.ausl.internauta.service.permessi.PermessiUtilities;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import it.bologna.ausl.blackbox.PermissionManager;
import it.bologna.ausl.blackbox.PermissionRepositoryAccess;
import it.bologna.ausl.blackbox.exceptions.BlackBoxPermissionException;
import it.bologna.ausl.internauta.utils.bds.types.PermessoEntitaStoredProcedure;
import it.bologna.ausl.internauta.utils.bds.types.PermessoStoredProcedure;
import it.bologna.ausl.internauta.service.authorization.UserInfoService;
import it.bologna.ausl.internauta.service.exceptions.http.ControllerHandledExceptions;
import it.bologna.ausl.internauta.service.exceptions.http.Http400ResponseException;
import it.bologna.ausl.internauta.service.exceptions.http.Http403ResponseException;
import it.bologna.ausl.internauta.service.exceptions.http.Http409ResponseException;
import it.bologna.ausl.internauta.service.exceptions.http.HttpInternautaResponseException;
import it.bologna.ausl.internauta.service.permessi.PermessoError;
import it.bologna.ausl.internauta.service.repositories.baborg.PersonaRepository;
import it.bologna.ausl.internauta.service.repositories.baborg.UtenteRepository;
import static it.bologna.ausl.internauta.service.utils.InternautaConstants.Permessi.Ambiti.PECG;
import static it.bologna.ausl.internauta.service.utils.InternautaConstants.Permessi.Tipi.PEC;
import it.bologna.ausl.internauta.utils.bds.types.CategoriaPermessiStoredProcedure;
import it.bologna.ausl.model.entities.baborg.Pec;
import it.bologna.ausl.model.entities.baborg.PecAzienda;
import it.bologna.ausl.model.entities.baborg.Persona;
import it.bologna.ausl.model.entities.baborg.Struttura;
import it.bologna.ausl.model.entities.baborg.Utente;
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
import javax.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
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
    PermissionRepositoryAccess permissionRepositoryAccess;

    @Autowired
    PermissionManager permissionManager;

    @Autowired
    PermessiUtilities permessiUtilities;

    @Autowired
    UserInfoService userInfoService;

    @Autowired
    PersonaRepository personaRepository;

    @Autowired
    UtenteRepository utenteRepository;

    @Autowired
    ObjectMapper objectMapper;

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

    // VECCHIA VERSIONE CHE NON GESTIVA LE LISTE
//
//    @Transactional
//    @RequestMapping(value = "managePermissionsGestoriPec", method = RequestMethod.POST)
//    public void managePermissionsGestoriPec(@RequestBody Map<String, Object> json, HttpServletRequest request) throws BlackBoxPermissionException, HttpInternautaResponseException {
//        Persona persona;
//        Pec pec;
//        PermessoStoredProcedure permesso;
//
//        // Controllo che i dati nella richiesta rispettino gli standard richiesti
//        try {
//            persona = mapper.convertValue(json.get("persona"), Persona.class);
//        } catch (IllegalArgumentException ex) {
//            LOGGER.error("Errore nel casting della persona.", ex);
//            throw new Http400ResponseException("1", "Errore nel casting della persona.");
//        }
//
//        try {
//            pec = mapper.convertValue(json.get("pec"), Pec.class);
//        } catch (IllegalArgumentException ex) {
//            LOGGER.error("Errore nel casting della pec.", ex);
//            throw new Http400ResponseException("2", "Errore nel casting della pec.");
//        }
//
//        try {
//            permesso = mapper.convertValue(json.get("permesso"), PermessoStoredProcedure.class);
//        } catch (IllegalArgumentException ex) {
//            LOGGER.error("Errore nel casting del permesso.", ex);
//            throw new Http400ResponseException("3", "Errore nel casting del permesso.");
//        }
//
////        if (permesso.getPredicato() == null) {
////            throw new Http400ResponseException("4", "Il permesso passato è sprovvisto del predicato.");
////        }
////        
////        if (permesso.getOriginePermesso()== null) {
////            throw new Http400ResponseException("5", "Il permesso passato è sprovvisto dell'origine_permesso.");
////        }
//        if (pec.getPecAziendaList() == null) {
//            throw new Http400ResponseException("6", "La pec passata non ha il campo pecAziendaList espanso.");
//        }
//
//        if (!pec.getPecAziendaList().isEmpty()) {
//            for (PecAzienda pa : pec.getPecAziendaList()) {
//                if (pa.getIdAzienda() == null) {
//                    throw new Http400ResponseException("7", "Le entità della pecAziendaList non hanno l'idAzienda espanso.");
//                }
//            }
//        } else {
//            throw new Http403ResponseException("1", "Non è possibile associare un permesso su una pec non collegata ad alcuna azienda.");
//        }
//
//        List<Integer> idAziendePec = pec.getPecAziendaList().stream().map(pecAzienda -> pecAzienda.getIdAzienda().getId()).collect(Collectors.toList());
//        List<Integer> idAziendePersona = userInfoService.getAziendePersona(persona).stream().map(azienda -> (azienda.getId())).collect(Collectors.toList());
//
//        if (Collections.disjoint(idAziendePec, idAziendePersona)) {
//            throw new Http403ResponseException("2", "Pec e Persona passati non hanno aziende in comune.");
//        }
//
//        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
//        Utente loggedUser = (Utente) authentication.getPrincipal();
//
//        if (!userInfoService.isCI(loggedUser)) {
//            Persona personaLogged = personaRepository.getOne(loggedUser.getIdPersona().getId());
//            List<Integer> idAziendeCA = userInfoService.getAziendeWherePersonaIsCa(personaLogged).stream().map(azienda -> azienda.getId()).collect(Collectors.toList());
//
//            if (idAziendeCA == null || idAziendeCA.isEmpty()) {
//                // Non sono ne CA ne CI fermo tutto.
//                throw new Http403ResponseException("3", "L'utente loggato non è ne CI ne CA.");
//            } else {
//
//                if (Collections.disjoint(idAziendeCA, idAziendePec)) {
//                    // Nessuna azienda associata alla pec è un azienda del CA, fermo tutto.
//                    throw new Http403ResponseException("4", "L'utente loggato non è CA di almeno un'azienda della pec.");
//                }
//
//                if (Collections.disjoint(idAziendeCA, idAziendePersona)) {
//                    // Nessuna utente della persona appartiene ad un azienda del CA, fermo tutto.
//                    throw new Http403ResponseException("5", "L'utente loggato non è CA di almeno un'azienda degli utenti della persona.");
//                }
//            }
//        }
//
//        List<PermessoStoredProcedure> permessi;
//        if (permesso != null) {
//            permessi = Arrays.asList(new PermessoStoredProcedure[]{permesso});
//        } else {
//            permessi = new ArrayList<>();
//        }
//
//        permissionManager.managePermissions(persona, pec, PECG.toString(), PEC.toString(), permessi);
//    }
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
            List<Integer> idAziendePersona = userInfoService.getAziendePersona(persona).stream().map(azienda -> (azienda.getId())).collect(Collectors.toList());

            if (Collections.disjoint(idAziendePec, idAziendePersona)) {
                throw new Http403ResponseException("2", "Pec e Persona passati non hanno aziende in comune.");
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
        ObjectMapper mapper = new ObjectMapper();

        // Controllo che i dati nella richiesta rispettino gli standard richiesti
        try {
            struttura = mapper.convertValue(json.get("struttura"), Struttura.class);
        } catch (IllegalArgumentException ex) {
            throw new Http400ResponseException("1", "Errore nel casting della struttura.");
        }

        try {
            pec = mapper.convertValue(json.get("pec"), Pec.class);
        } catch (IllegalArgumentException ex) {
            throw new Http400ResponseException("2", "Errore nel casting della pec.");
        }

        try {
            permesso = mapper.convertValue(json.get("permesso"), PermessoStoredProcedure.class);
        } catch (IllegalArgumentException ex) {
            throw new Http400ResponseException("3", "Errore nel casting del permesso.");
        }

//        if (permesso.getPredicato() == null) {
//            throw new Http400ResponseException("4", "Il permesso passato è sprovvisto del predicato.");
//        }
//        
//        if (permesso.getOriginePermesso()== null) {
//            throw new Http400ResponseException("5", "Il permesso passato è sprovvisto dell'origine_permesso.");
//        }
//        
//        if (permesso.getPropagaSoggetto()== null) {
//            throw new Http400ResponseException("6", "Il permesso passato è sprovvisto del propaga soggetto.");
//        }
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
     * @throws it.bologna.ausl.internauta.service.exceptions.http.Http409ResponseException
     * @throws com.fasterxml.jackson.core.JsonProcessingException
     */
    @RequestMapping(value = "managePermissionsAdvanced", method = RequestMethod.POST)
    public void managePermissionsAdvanced(@RequestBody Map<String, Object> params, HttpServletRequest request) throws BlackBoxPermissionException, Http409ResponseException, JsonProcessingException {
        List<PermessoEntitaStoredProcedure> permessiEntita = objectMapper.convertValue(params.get("permessiEntita"), new TypeReference<List<PermessoEntitaStoredProcedure>>() {});
        List<PermessoEntitaStoredProcedure> permessiAggiunti;

        LocalDate dataDiLavoro = null;
        if (params.get("dataDiLavoro") != null) {
            Long dataMillis = (Long) params.get("dataDiLavoro");
            dataDiLavoro = Instant.ofEpochMilli(dataMillis).atZone(ZoneId.systemDefault()).toLocalDate();
        }
        List<PermessoError> risultanze = new ArrayList<>();
        if (params.get("permessiAggiunti") != null) {
            permessiAggiunti = objectMapper.convertValue(params.get("permessiAggiunti"), new TypeReference<List<PermessoEntitaStoredProcedure>>(){});

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
                                permessoStoredProcedure.getAttivoDal().toLocalDate(),
                                permessoStoredProcedure.getAttivoAl() != null ? permessoStoredProcedure.getAttivoAl().toLocalDate(): null);
                        
                        if (risposte != null && !risposte.isEmpty()) {
//                            if (permessoStoredProcedure.getAttivoAl() != null) {
                                for (PermessoEntitaStoredProcedure risposta : risposte) {
                                    for (CategoriaPermessiStoredProcedure resCategoria : risposta.getCategorie()) {
                                        
                                        for (PermessoStoredProcedure resPermesso : resCategoria.getPermessi()) {
                                            if (permessoStoredProcedure.getAttivoAl() == null || permessoStoredProcedure.getAttivoAl().isAfter(resPermesso.getAttivoDal())) {
                                                String soggetto = permessiAggiunto.getSoggetto().getIdProvenienza().toString();
                                                String oggetto = permessiAggiunto.getOggetto().getIdProvenienza().toString();
                                                String predicato = permessoStoredProcedure.getPredicato();
                                                String ambito = categoriaPermessiStoredProcedure.getAmbito();
                                                String tipo = categoriaPermessiStoredProcedure.getTipo();
                                                PermessoError permessoError=new PermessoError(soggetto, oggetto, predicato, ambito, tipo, resPermesso.getAttivoDal().toLocalDate());
                                                //throw new Http409ResponseException("permesso_furuto", "trovato un permesso nel futuro che si sovrappone");
                                                risultanze.add(permessoError);
                                            }
                                        }
                                    }
                                }
//                            } 
//                            else {
//                                String soggetto = permessiAggiunto.getSoggetto().getIdProvenienza().toString();
//                                String oggetto = permessiAggiunto.getOggetto().getIdProvenienza().toString();
//                                String predicato = permessoStoredProcedure.getPredicato();
//                                String ambito = categoriaPermessiStoredProcedure.getAmbito();
//                                String tipo = categoriaPermessiStoredProcedure.getTipo();
//                                PermessoError permessoError=new PermessoError(soggetto, oggetto, predicato, ambito, tipo, permessoStoredProcedure.getAttivoDal().toLocalDate());
//                                //throw new Http409ResponseException("permesso_furuto", "trovato un permesso nel futuro che si sovrappone");
//                                risultanze.add(permessoError);
//                            }
                        }
                    }
                }
            }
        }
        
        if (!risultanze.isEmpty()){
            throw new Http409ResponseException("permesso_futuro", objectMapper.writeValueAsString(risultanze));
        }
        
        permissionRepositoryAccess.managePermissions(permessiEntita, dataDiLavoro);

//        for (PermessoEntitaStoredProcedure p : permessiEntita) {
//            Utente u = utenteRepository.getOne(p.getSoggetto().getIdProvenienza());
//            userInfoService.getPermessiDiFlussoRemoveCache(u);
//        }
//        permessiEntita.stream().forEach(p -> {
//            Utente u = utenteRepository.getOne(p.getSoggetto().getIdProvenienza());
//            userInfoService.getPermessiDiFlussoRemoveCache(u);
//            userInfoService.getPermessiDiFlussoRemoveCache(u, null, true);
//            userInfoService.getPermessiDiFlussoRemoveCache(u, null, false);
//        });
            Set<Integer> idUtentiSet = new HashSet();

            permessiEntita.stream().forEach(p -> {
                idUtentiSet.add(p.getSoggetto().getIdProvenienza());
            });

            idUtentiSet.forEach(idUtente -> {
                permessiUtilities.cleanCachePermessiUtente(idUtente);
            });
        }
    }
