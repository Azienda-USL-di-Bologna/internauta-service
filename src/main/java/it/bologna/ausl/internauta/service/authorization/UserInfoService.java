package it.bologna.ausl.internauta.service.authorization;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.PathBuilder;
import it.bologna.ausl.blackbox.PermissionManager;
import it.bologna.ausl.blackbox.exceptions.BlackBoxPermissionException;
import it.bologna.ausl.blackbox.utils.BlackBoxConstants;
import it.bologna.ausl.internauta.utils.bds.types.PermessoEntitaStoredProcedure;
import it.bologna.ausl.internauta.service.authorization.utils.UtenteProcton;
import it.bologna.ausl.internauta.service.configuration.utils.PostgresConnectionManager;
import it.bologna.ausl.internauta.service.exceptions.http.Http404ResponseException;
import it.bologna.ausl.internauta.service.permessi.Permesso;
import it.bologna.ausl.internauta.service.repositories.baborg.AziendaRepository;
import it.bologna.ausl.internauta.service.repositories.baborg.PersonaRepository;
import it.bologna.ausl.internauta.service.repositories.baborg.RuoloRepository;
import it.bologna.ausl.internauta.service.repositories.baborg.StrutturaRepository;
import it.bologna.ausl.internauta.service.repositories.baborg.UtenteRepository;
import it.bologna.ausl.internauta.service.repositories.baborg.UtenteStrutturaRepository;
import it.bologna.ausl.internauta.service.utils.CachedEntities;
import it.bologna.ausl.model.entities.baborg.Azienda;
import it.bologna.ausl.model.entities.baborg.Persona;
import it.bologna.ausl.model.entities.baborg.QUtente;
import it.bologna.ausl.model.entities.baborg.Ruolo;
import it.bologna.ausl.model.entities.baborg.Utente;
import it.bologna.ausl.model.entities.baborg.projections.generated.AziendaWithPlainFields;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.projection.ProjectionFactory;
import org.springframework.stereotype.Component;
import it.bologna.ausl.internauta.service.utils.InternautaConstants;
import it.bologna.ausl.internauta.service.utils.ParametriAziendeReader;
import it.bologna.ausl.internauta.utils.bds.types.CategoriaPermessiStoredProcedure;
import it.bologna.ausl.internauta.utils.bds.types.PermessoStoredProcedure;
import it.bologna.ausl.model.entities.baborg.AfferenzaStruttura;
import it.bologna.ausl.model.entities.baborg.QUtenteStruttura;
import it.bologna.ausl.model.entities.baborg.Ruolo.CodiciRuolo;
import it.bologna.ausl.model.entities.baborg.Struttura;
import it.bologna.ausl.model.entities.baborg.UtenteStruttura;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.cache.annotation.CacheEvict;
import it.bologna.ausl.model.entities.baborg.projections.CustomAziendaLogin;
import it.bologna.ausl.model.entities.baborg.projections.generated.UtenteStrutturaWithIdAfferenzaStrutturaAndIdStruttura;
import it.bologna.ausl.model.entities.configurazione.ParametroAziende;
import java.util.Arrays;
import java.util.Set;
import java.util.TreeSet;
import org.sql2o.Connection;
import org.sql2o.Sql2o;
import it.bologna.ausl.model.entities.logs.projections.KrintBaborgStruttura;
import it.bologna.ausl.model.entities.logs.projections.KrintBaborgAzienda;
import it.bologna.ausl.model.entities.logs.projections.KrintBaborgPersona;
import it.bologna.ausl.model.entities.scrivania.Attivita;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashSet;
import java.util.stream.Stream;

/**
 * Service per la creazione dell'oggetto UserInfoOld TODO: descrivere la
 * gestione con la cache
 */
@Component
public class UserInfoService {

    @Autowired
    AziendaRepository aziendaRepository;

    @Autowired
    PersonaRepository personaRepository;

    @Autowired
    UtenteRepository utenteRepository;

    @Autowired
    StrutturaRepository strutturaRepository;

    @Autowired
    UtenteStrutturaRepository utenteStrutturaRepository;

    @Autowired
    RuoloRepository ruoloRepository;

    @Autowired
    ProjectionFactory factory;

    @Autowired
    PermissionManager permissionManager;

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    CachedEntities cachedEntities;

    @Autowired
    PostgresConnectionManager postgresConnectionManager;

    @Autowired
    ParametriAziendeReader parametriAziende;

    /**
     * E' necessario mettere in autowired la stessa UserInfoService per poter
     * usare le funzioni cacheable. Se una funzione cacheable non viene chiamata
     * così: userInfoService.nomeFunzione() non verrà usata come cacheable
     */
    @Autowired
    UserInfoService userInfoService;

    private static final Logger LOGGER = LoggerFactory.getLogger(UserInfoService.class);

    /**
     * carica l'utente a partire dall'id
     *
     * @param id
     * @return
     */
    @Cacheable(value = "userInfo__ribaltorg__", key = "{#id}")
    public Utente loadUtente(Integer id) {
        Utente res = null;
        Optional<Utente> utenteOp = utenteRepository.findById(id);
        if (utenteOp.isPresent()) {
            res = utenteOp.get();
            
            // Questo pezzo di codice ci permette di essere sicure che utenteList sia caricata.
            // E' una possibile soluzione al problema del menu scrivania che a volte non compare.
            if (res.getIdPersona().getUtenteList() != null) {
                res.getIdPersona().getUtenteList().size();
            }

//            res.getIdPersona().setApplicazione(applicazione);
        }
        return res;
    }

    @CacheEvict(value = "userInfo__ribaltorg__", key = "{#id}")
    public void loadUtenteRemoveCache(Integer id) {
    }

    /**
     * carica l'utente a partire dallo username e dal path dell'azienda dalla
     * quale proviene la richiesta
     *
     * @param username
     * @param aziendaPath
     * @return
     */
    @Cacheable(value = "userInfo__ribaltorg__", key = "{#username, #aziendaPath}")
    public Utente loadUtente(String username, String aziendaPath) {
        Utente res = null;
        Azienda azienda = cachedEntities.getAziendaFromPath(aziendaPath);
        if (azienda != null) {
            BooleanExpression utenteFilter = QUtente.utente.username.eq(username).and(QUtente.utente.idAzienda.id.eq(azienda.getId()));
            Optional<Utente> utenteOp = utenteRepository.findOne(utenteFilter);
            if (utenteOp.isPresent()) {
                res = utenteOp.get();
                
                // Questo pezzo di codice ci permette di essere sicure che utenteList sia caricata.
                // E' una possibile soluzione al problema del menu scrivania che a volte non compare.
                if (res.getIdPersona().getUtenteList() != null) {
                    res.getIdPersona().getUtenteList().size();
                }
//                res.getIdPersona().setApplicazione(applicazione);
            }
        }
        return res;
    }

    /**
     * invalida la cache della funzione loadUtente(String username, String
     * aziendaPath)
     *
     * @param username
     * @param aziendaPath
     */
    @CacheEvict(value = "userInfo__ribaltorg__", key = "{#username, #aziendaPath}")
    public void loadUtenteRemoveCache(String username, String aziendaPath) {
    }

    /**
     * carica l'utente cachable a partire dai campi configurati per il login SSO
     *
     * @param entityClass classe entity da cui cercare l'utente (Utente o
     * Persona)
     * @param field campo del db da usare per cercare l'utente
     * @param ssoFieldValue campo che identifica l'utente iniettato da shibbolet
     * nella richiesta
     * @param azienda campo che identifica l'azienda
     * @param onlyActive indica se caricare solo gli utenti attivi
     * @return
     */
    @Cacheable(value = "userInfo__ribaltorg__", key = "{#entityClass.getName(), #field, #ssoFieldValue, #azienda.getId(), #onlyActive}")
    public Utente loadUtente(Class entityClass, String field, String ssoFieldValue, Azienda azienda, Boolean onlyActive) {

        Utente res = null;
        BooleanExpression filter;
        PathBuilder<Utente> qUtente = new PathBuilder(Utente.class, "utente");
        if (entityClass.isAssignableFrom(Persona.class)) {
            PathBuilder<Persona> qPersona = qUtente.get("idPersona", Persona.class);
            filter = qPersona.get(field).eq(ssoFieldValue.toUpperCase());
        } else {
            filter = qUtente.get(field).eq(ssoFieldValue.toUpperCase());
        }
        filter = filter.and(QUtente.utente.idAzienda.id.eq(azienda.getId()));
        if (onlyActive) {
            filter = filter.and(QUtente.utente.attivo.eq(true));
        }
        Optional<Utente> utenteOp = utenteRepository.findOne(filter);

        if (utenteOp.isPresent()) {
            res = utenteOp.get();
            
            // Questo pezzo di codice ci permette di essere sicure che utenteList sia caricata.
            // E' una possibile soluzione al problema del menu scrivania che a volte non compare.
            if (res.getIdPersona().getUtenteList() != null) {
                res.getIdPersona().getUtenteList().size();
            }
//            res.getIdPersona().setApplicazione(applicazione);
            return res;
        }
        return res;
    }

    /**
     * invalida la cache della funzione loadUtente(Class entityClass, String
     * field, String ssoFieldValue, Azienda azienda)
     *
     * @param entityClass
     * @param field
     * @param ssoFieldValue
     * @param azienda
     * @param onlyActive
     */
    @CacheEvict(value = "userInfo__ribaltorg__", key = "{#entityClass.getName(), #field, #ssoFieldValue, #azienda.getId(), #onlyActive}")
    public void loadUtenteRemoveCache(Class entityClass, String field, String ssoFieldValue, Azienda azienda, Boolean onlyActive) {
    }

    /**
     * restituisce i ruoli aziendali/interaziendali a seconda del parametro
     * interaziendali o entrambi nel caso in cui il parametro sia null
     *
     * @param utente
     * @param interaziendali
     * @return la lista dei ruoli
     */
    @Cacheable(value = "getRuoliPerModuli_utente__ribaltorg__", key = "{#utente.getId()}")
    public Map<String, List<Ruolo>> getRuoliPerModuli(Utente utente, Boolean interaziendali) {
        Map<String, List<Ruolo>> mappaRuoli = new HashMap();
        Persona persona = utente.getIdPersona();
        Integer[] idAziende = getAziendePersona(persona).stream().map(a -> a.getId()).collect(Collectors.toList()).toArray(new Integer[0]);

        // modulo generale
        Set<Ruolo> ruoliModuloGenerale = getRuoliGenerali(utente, interaziendali);

        // modulo Matrint
        Set<Ruolo> ruoliModuloMatrint = getRuoliMatrint(utente, interaziendali, idAziende, true);
        // modulo Pool
        Set<Ruolo> ruoliModuloPool = getRuoliPool(utente, interaziendali, idAziende, true);

        mappaRuoli.put(Ruolo.ModuliRuolo.GENERALE.toString(), new ArrayList<>(ruoliModuloGenerale));
        mappaRuoli.put(Ruolo.ModuliRuolo.MATRINT.toString(), new ArrayList<>(ruoliModuloMatrint));
        mappaRuoli.put(Ruolo.ModuliRuolo.POOLS.toString(), new ArrayList<>(ruoliModuloPool));

        return mappaRuoli;
    }

    @Cacheable(value = "getRuoliMatrint_utente__ribaltorg__", key = "{#utente.getId()}")
    public Set<Ruolo> getRuoliMatrint(Utente utente, Boolean interaziendali, Integer[] idAziende, Boolean calcolaRuoliStandard) {
        Set<Ruolo> res = new HashSet();

        if (calcolaRuoliStandard) {
            res.addAll(getRuoliStandard(utente, interaziendali));
        }

        List<ParametroAziende> filtraResponsabiliMatrintParams = parametriAziende.getParameters("AccessoMatrintFiltratoPerRuolo", idAziende);
        if (filtraResponsabiliMatrintParams != null && !filtraResponsabiliMatrintParams.isEmpty() && filtraResponsabiliMatrintParams.stream().anyMatch(param -> parametriAziende.getValue(param, Boolean.class))) {
            res.addAll(getRuoliStrutture(utente, Arrays.asList(Ruolo.CodiciRuolo.R)));
        }
        try {
            List<Integer> idUtentiDelegati = getPermessiDelega(utente);
            idUtentiDelegati.stream().map(idUtente -> utenteRepository.getOne(idUtente)).forEach(u -> {
                res.addAll(getRuoliMatrint(u, interaziendali, idAziende, false));
            });
        } catch (BlackBoxPermissionException ex) {
            LOGGER.error("errore nel calcolo dei permessi Delegato", ex);
        }
        return res;
    }

    @CacheEvict(value = "getRuoliMatrint_utente__ribaltorg__", key = "{#utente.getId()}")
    public void getRuoliMatrintRemoveCache(Utente utente) {
    }

    @Cacheable(value = "getRuoliPool_utente__ribaltorg__", key = "{#utente.getId()}")
    public Set<Ruolo> getRuoliPool(Utente utente, Boolean interaziendali, Integer[] idAziende, Boolean calcolaRuoliStandard) {
        Set<Ruolo> res = new HashSet();

        if (calcolaRuoliStandard) {
            res.addAll(getRuoliStandard(utente, interaziendali));
        }

        List<ParametroAziende> filtraResponsabiliParams = parametriAziende.getParameters("AccessoPoolFiltratoPerRuolo", idAziende);
        if (filtraResponsabiliParams != null && !filtraResponsabiliParams.isEmpty() && filtraResponsabiliParams.stream().anyMatch(param -> parametriAziende.getValue(param, Boolean.class))) {
            res.addAll(getRuoliStrutture(utente, Arrays.asList(Ruolo.CodiciRuolo.R)));
        }
//        try {
//            List<Integer> idUtentiDelegati = getPermessiDelega(utente);
//            idUtentiDelegati.stream().map(idUtente -> utenteRepository.getOne(idUtente)).forEach(u -> {
//                res.addAll(getRuoliPool(u, interaziendali, idAziende, false));
//            });
//        } catch (BlackBoxPermissionException ex) {
//            LOGGER.error("errore nel calcolo dei permessi Delegato", ex);
//        }
        return res;
    }

    @CacheEvict(value = "getRuoliPool_utente__ribaltorg__", key = "{#utente.getId()}")
    public void getRuoliPoolRemoveCache(Utente utente) {
    }

    @Cacheable(value = "getRuoliGenerali_utente__ribaltorg__", key = "{#utente.getId()}")
    public Set<Ruolo> getRuoliGenerali(Utente utente, Boolean interaziendali) {
        Set<Ruolo> res = getRuoliStandard(utente, interaziendali);
        res.addAll(getRuoliStrutture(utente, Stream.of(Ruolo.CodiciRuolo.values()).collect(Collectors.toList())));
//        try {
//            List<Integer> idUtentiDelegati = getPermessiDelega(utente);
//            idUtentiDelegati.stream().map(idUtente -> utenteRepository.getOne(idUtente)).forEach(u -> {
//                res.addAll(getRuoliGenerali(u, interaziendali));
//            });
//        } catch (BlackBoxPermissionException ex) {
//            LOGGER.error("errore nel calcolo dei permessi delegato", ex);
//        }
        return res;
    }

    @CacheEvict(value = "getRuoliGenerali_utente__ribaltorg__", key = "{#utente.getId()}")
    public void getRuoliGeneraliRemoveCache(Utente utente) {
    }

    @Cacheable(value = "getRuoliStandard_utente__ribaltorg__", key = "{#utente.getId()}")
    public Set<Ruolo> getRuoliStandard(Utente utente, Boolean interaziendali) {
        Set<Ruolo> res = new HashSet<>();
        List<Ruolo> ruoliAll = ruoloRepository.findAll();
        for (Ruolo ruolo : ruoliAll) {
            if (interaziendali == null || interaziendali == true) {
                res.addAll(getRuoliInteraziendali(utente.getIdPersona()));
            }
            if (interaziendali == null || interaziendali == false) {
                if (ruolo.getSuperAziendale() == false) {
                    if ((utente.getBitRuoli() & ruolo.getMascheraBit()) > 0) {
                        res.add(ruolo);
                    }
                }
            }
        }
        return res;
    }

    @CacheEvict(value = "getRuoliStandard_utente__ribaltorg__", key = "{#utente.getId()}")
    public void getRuoliStandardRemoveCache(Utente utente) {
    }

    /**
     * restituisce i ruoli aziendali/interaziendali a seconda del parametro
     * interaziendali o entrambi nel caso in cui il parametro sia null
     *
     * @param utente
     * @param interaziendali
     * @return la lista dei ruoli
     */
//    @Cacheable(value = "getRuoli_utente__ribaltorg__", key = "{#utente.getId()}")
//    public List<Ruolo> getRuoli(Utente utente, Boolean interaziendali) {
//        Set<Ruolo> res = new HashSet<>();
//        List<Ruolo> ruoliAll = ruoloRepository.findAll();
//        for (Ruolo ruolo : ruoliAll) {
//            if (interaziendali == null || interaziendali == true) {
//                res.addAll(getRuoliInteraziendali(utente.getIdPersona()));
//            }
//            if (interaziendali == null || interaziendali == false) {
//                if (ruolo.getSuperAziendale() == false) {
//                    if ((utente.getBitRuoli() & ruolo.getMascheraBit()) > 0) {
//                        res.add(ruolo);
//                    }
//                }
//            }
//        }
//        Persona persona = utente.getIdPersona();
//        Integer[] idAziende = getAziendePersona(persona).stream().map(a -> a.getId()).collect(Collectors.toList()).toArray(new Integer[0]);
//        List<ParametroAziende> filtraResponsabiliMatrintParams = parametriAziende.getParameters("AccessoMatrintFiltratoPerRuolo", idAziende);
//        if (filtraResponsabiliMatrintParams != null && !filtraResponsabiliMatrintParams.isEmpty() && filtraResponsabiliMatrintParams.stream().anyMatch(param -> parametriAziende.getValue(param, Boolean.class))) {
//            res.addAll(getRuoliStrutture(utente, Arrays.asList(Ruolo.CodiciRuolo.R)));
//        }
//        try {
//            List<Integer> idUtentiAvatar = getPermessiDelega(utente);
//            idUtentiAvatar.stream().map(idUtente -> utenteRepository.getOne(idUtente)).forEach(u -> {
//                res.addAll(getRuoli(u, interaziendali));
//            });
//        } catch (BlackBoxPermissionException ex) {
//            LOGGER.error("errore nel calcolo dei permessi DELEGATI", ex);
//        }
//
//        return new ArrayList(res);
//    }
    /**
     * Torna la lista dei ruoli intersecati con i ruoli passati in input
     * dell'utente sulle sue strutture
     *
     * @param utente
     * @param codiciRuoloUtenteStruttura torna una lista che li contiente se
     * questi sono presenti in utenti_strutture per l'utente passato
     * @return la lista dei ruoli intersacati con i ruoli passati in input
     * dell'utente sulle sue strutture
     */
    @Cacheable(value = "getRuoliStrutture_utente__ribaltorg__", key = "{#utente.getId(), #codiciRuoloUtenteStruttura != null? #codiciRuoloUtenteStruttura.toString(): 'null'}")
    public Set<Ruolo> getRuoliStrutture(Utente utente, List<Ruolo.CodiciRuolo> codiciRuoloUtenteStruttura) {
        Set<Ruolo> res = new HashSet();
        Iterable<UtenteStruttura> struttureUtente = utenteStrutturaRepository.findAll(
                QUtenteStruttura.utenteStruttura.attivo.eq(true).and(
                        QUtenteStruttura.utenteStruttura.idUtente.id.eq(utente.getId()))
        );
        for (Ruolo.CodiciRuolo codiceRuolo : codiciRuoloUtenteStruttura) {
            Ruolo ruolo = cachedEntities.getRuoloByNomeBreve(codiceRuolo);
            if (struttureUtente != null) {
                for (UtenteStruttura utenteStruttura : struttureUtente) {
                    if ((utenteStruttura.getBitRuoli() & ruolo.getMascheraBit()) > 0) {
                        res.add(ruolo);
                    }
                }
            }
        }
        return res;
    }

    @CacheEvict(value = "getRuoliStrutture_utente__ribaltorg__", key = "{#utente.getId(), #codiciRuoloUtenteStruttura != null? #codiciRuoloUtenteStruttura.toString(): 'null'}")
    public void getRuoliStruttureRemoveCache(Utente utente, List<Ruolo.CodiciRuolo> codiciRuoloUtenteStruttura) {
    }

    /**
     * restituisce i ruoli di una persona
     *
     * @param persona
     * @return List<Ruolo>
     */
    @Cacheable(value = "getRuoliInteraziendali_persona__ribaltorg__", key = "{#persona.getId()}")
    public List<Ruolo> getRuoliInteraziendali(Persona persona) {
        List<Ruolo> res = new ArrayList<>();
        List<Ruolo> ruoliAll = ruoloRepository.findAll();
        for (Ruolo ruolo : ruoliAll) {
            if (ruolo.getSuperAziendale()) {
                if ((persona.getBitRuoli() & ruolo.getMascheraBit()) > 0) {
                    res.add(ruolo);
                }
            }
        }
        return res;
    }

    /**
     * restituisce tutti i ruoli di tutte le aziende degli utenti della persona
     * divisi per interaziendali e aziendali.I ruoli aziendali sono raggruppati
     * per azienda Se ancheByRuolo è true o null vale anche il viceversa: le
     * aziende sono raggruppate per ruolo
     *
     * @param persona
     * @param ancheByRuolo
     * @return una mappa in cui la chiave è il codice azienda e il valore la
     * lista dei codici ruolo per quell'azienda nel caso dei ruoli
     * interaziendali la chiave è 'interaziendali' Se viene passato ancheByRuolo
     * true o null alla mappa viene aggiunt una mappa in cui la chiave è il
     * codice ruolo e il valore è una lista di codici azienda
     */
    @Cacheable(value = "getRuoliUtentiPersona_persona__ribaltorg__", key = "{#persona.getId(), #ancheByRuolo.booleanValue()}")
    public Map<String, Map<String, List<String>>> getRuoliUtentiPersona(Persona persona, Boolean ancheByRuolo) {

        if (ancheByRuolo == null) {
            ancheByRuolo = true;
        }
//        if(persona.getUtenteList() == null){
        persona.setUtenteList(getUtentiPersona(persona));
//        }

        // popolo mappa azienda->listaRuoli
        Map<String, Map<String, List<String>>> mapAziendeRuoli
                = persona.getUtenteList().stream().collect(
                        Collectors.toMap(
                                u -> u.getIdAzienda().getCodice(),
                                u -> getRuoliPerModuli(u, false).entrySet().stream().collect(
                                        Collectors.toMap(Map.Entry::getKey, e -> e.getValue().stream().map(r -> r.getNomeBreve().toString()).collect(Collectors.toList())))
                        )
                );
        List<Ruolo> ruoliInteraziendali = getRuoliInteraziendali(persona);
        if (ruoliInteraziendali != null && !ruoliInteraziendali.isEmpty()) {
            Map<String, List<String>> map = new HashMap();
            map.put(Ruolo.ModuliRuolo.GENERALE.toString(), ruoliInteraziendali.stream().map(r -> r.getNomeBreve().toString()).collect(Collectors.toList()));
            mapAziendeRuoli.put("interaziendali", map);
        }

        // popolo mappa ruolo->listaAziene
        Map<String, Map<String, List<String>>> mapRuoloAziende = new HashMap();

        if (ancheByRuolo) {
            for (Map.Entry<String, Map<String, List<String>>> aziendaEntry : mapAziendeRuoli.entrySet()) {
                Map<String, List<String>> mappaRuoli = aziendaEntry.getValue();

                for (Map.Entry<String, List<String>> ruoloModuliEntry : mappaRuoli.entrySet()) {
                    for (String ruolo : ruoloModuliEntry.getValue()) {
                        if (mapRuoloAziende.containsKey(ruolo)) {
                            Map<String, List<String>> mappaRuoloModuliAziendeInEsame = mapRuoloAziende.get(ruolo);
                            if (mappaRuoloModuliAziendeInEsame.containsKey(ruoloModuliEntry.getKey())) {
                                List<String> listaRuoliModuloInEsame = mappaRuoloModuliAziendeInEsame.get(ruoloModuliEntry.getKey());
                                if (!listaRuoliModuloInEsame.contains(aziendaEntry.getKey())) {
                                    listaRuoliModuloInEsame.add(aziendaEntry.getKey());
                                }
                            } else {
                                mappaRuoloModuliAziendeInEsame.put(ruoloModuliEntry.getKey(), Lists.newArrayList(aziendaEntry.getKey()));
                            }
                        } else {
                            Map<String, List<String>> mappaRuoloModuliAziende = new HashMap();
                            mappaRuoloModuliAziende.put(ruoloModuliEntry.getKey(), Lists.newArrayList(aziendaEntry.getKey()));
                            mapRuoloAziende.put(ruolo, mappaRuoloModuliAziende);
                        }
                    }
                }
            }
        }

        // mergio le due mappe
        Map<String, Map<String, List<String>>> finalMap = new HashMap(mapAziendeRuoli);
        finalMap.putAll(mapRuoloAziende);

        return finalMap;
    }

    public List<String> getCodiciAziendaListDovePersonaHaRuolo(Persona persona, CodiciRuolo nomeBreveRuolo) {
        Map<String, Map<String, List<String>>> ruoliUtentiPersona = userInfoService.getRuoliUtentiPersona(persona, true);
        if (ruoliUtentiPersona.containsKey(nomeBreveRuolo.toString())) {
            Map<String, List<String>> listaModuliDelRuolo = ruoliUtentiPersona.get(nomeBreveRuolo.toString());
            if (listaModuliDelRuolo.containsKey(Ruolo.ModuliRuolo.GENERALE.toString())) {
                return listaModuliDelRuolo.get(Ruolo.ModuliRuolo.GENERALE.toString());
            }
        }
        return new ArrayList();
    }
    
    
    @Cacheable(value = "getRuoliUtentiPersona_utente__ribaltorg__", key = "{#utente.getId(), #ancheByRuolo.booleanValue()}")
    public Map<String, Map<String, List<String>>> getRuoliUtentiPersona(Utente utente, Boolean ancheByRuolo) {
        return getRuoliUtentiPersona(utente.getIdPersona(), ancheByRuolo);
    }

    @CacheEvict(value = "getRuoli_utente__ribaltorg__", key = "{#utente.getId()}")
    public void getRuoliRemoveCache(Utente utente) {
    }

    /**
     * Il value comincia per getRuoli perché così la cache viene svuotata al
     * login vedere CacheUtilities per crederci.
     *
     * @param persona
     * @return
     */
    @Cacheable(value = "getRuoliListaCodiciAziendaOsservatore_persona__ribaltorg__", key = "{#persona.getId()}")
    public List<String> getListaCodiciAziendaOsservatore(Persona persona) {
        Map<String, Map<String, List<String>>> ruoliUtentiPersona = getRuoliUtentiPersona(persona, true);
        Map<String, List<String>> listeOsservatore = ruoliUtentiPersona.get(Ruolo.CodiciRuolo.OS.toString());
        List<String> listaCodiciAziendaOsservatore;
        if (listeOsservatore == null) {
            listaCodiciAziendaOsservatore = new ArrayList();
        } else {
            listaCodiciAziendaOsservatore = listeOsservatore.get(Ruolo.ModuliRuolo.GENERALE.toString());
        }
        return listaCodiciAziendaOsservatore;
    }

    public List<AziendaWithPlainFields> getAziendePersonaWithPlainField(Utente utente) {
        List<AziendaWithPlainFields> res = new ArrayList();

        Utente refreshedUtente = utenteRepository.getOne(utente.getId());
        List<Utente> utenti = refreshedUtente.getIdPersona().getUtenteList();

        if (utenti != null && !utenti.isEmpty()) {
            utenti.stream().forEach(u -> {
                if (u.getAttivo()) {
                    res.add(factory.createProjection(AziendaWithPlainFields.class, u.getIdAzienda()));
                }
            });
        }
        return res;
    }

    @Cacheable(value = "getUtentiPersonaByUtente__ribaltorg__", key = "{#utente.getId(), #onlyActive.booleanValue()}")
    public List<Utente> getUtentiPersonaByUtente(Utente utente, Boolean onlyActive) {
        List<Utente> res = new ArrayList();

        Utente refreshedUtente = utenteRepository.getOne(utente.getId());
        List<Utente> utenti = refreshedUtente.getIdPersona().getUtenteList();

        if (utenti != null && !utenti.isEmpty()) {
            utenti.stream().forEach(u -> {
                if (!onlyActive || u.getAttivo()) {
                    res.add(u);
                }
            });
        }
        return res;
    }

    @Cacheable(value = "getUtentiPersona__ribaltorg__", key = "{#persona.getId()}")
    public List<Utente> getUtentiPersona(Persona persona) {
        Persona refreshedPersona = personaRepository.getOne(persona.getId());
        // Aggiungo questo stream.collect per ricreare la lista uguale a se stessa alrimenti mi da errore.
        return refreshedPersona.getUtenteList().stream().collect(Collectors.toList());
    }

    @Cacheable(value = "getAziendaUtente__ribaltorg__", key = "{#utente.getId()}")
    public Azienda getAziendaUtente(Utente utente) {
        Utente refreshUtente = utenteRepository.getOne(utente.getId());
        return refreshUtente.getIdAzienda();
    }

    @CacheEvict(value = "getUtentiPersonaByUtente__ribaltorg__", key = "{#utente.getId()}")
    public void getUtentiPersonaByUtenteRemoveCache(Utente utente) {
    }

    @CacheEvict(value = "getUtentiPersona__ribaltorg__", key = "{#persona.getId()}")
    public void getUtentiPersonaRemoveCache(Persona persona) {
    }

    @Cacheable(value = "getUtenteStrutturaList__ribaltorg__", key = "{#utente.getId(), #soloAttive}")
    public List<UtenteStruttura> getUtenteStrutturaList(Utente utente, boolean soloAttive) {
        Utente refreshedUtente = utenteRepository.getOne(utente.getId());
        if (soloAttive) {
            return refreshedUtente.getUtenteStrutturaList().stream().filter(us -> us.getAttivo()).collect(Collectors.toList());
        } else {
            return refreshedUtente.getUtenteStrutturaList();
        }
    }

    /**
     * Torna per l'utente dell'utenteStruttura passato, la struttura sulla quale
     * ha un afferenza Diretta, se non ne ha torna la Unificata, se non ne ha ne
     * torna una a caso
     *
     * @param utenteStruttura
     * @return
     */
//    @Cacheable(value = "getUtenteStrutturaAfferenzaPrincipaleAttiva__ribaltorg__", key = "{#utenteStruttura.getId()}")
    public UtenteStrutturaWithIdAfferenzaStrutturaAndIdStruttura getUtenteStrutturaAfferenzaPrincipaleAttiva(UtenteStruttura utenteStruttura) {
        Iterable<UtenteStruttura> afferenze = utenteStrutturaRepository.findAll(
                QUtenteStruttura.utenteStruttura.idUtente.id.eq(utenteStruttura.getIdUtente().getId())
                        .and(QUtenteStruttura.utenteStruttura.attivo.eq(Boolean.TRUE)));
        UtenteStruttura afferenzaPrincipale = null;
        for (UtenteStruttura afferenza : afferenze) {
            if (afferenzaPrincipale == null) {
                afferenzaPrincipale = afferenza;
            } else if (afferenzaPrincipale.getIdAfferenzaStruttura().getCodice() != AfferenzaStruttura.CodiciAfferenzaStruttura.DIRETTA) {
                if (afferenza.getIdAfferenzaStruttura().getCodice() == AfferenzaStruttura.CodiciAfferenzaStruttura.DIRETTA) {
                    afferenzaPrincipale = afferenza;
                } else if (afferenzaPrincipale.getIdAfferenzaStruttura().getCodice() != AfferenzaStruttura.CodiciAfferenzaStruttura.UNIFICATA) {
                    if (afferenza.getIdAfferenzaStruttura().getCodice() == AfferenzaStruttura.CodiciAfferenzaStruttura.UNIFICATA) {
                        afferenzaPrincipale = afferenza;
                    }
                }
            }
        }
        return factory.createProjection(UtenteStrutturaWithIdAfferenzaStrutturaAndIdStruttura.class, afferenzaPrincipale);
    }

    @CacheEvict(value = "getUtenteStrutturaList__ribaltorg__", key = "{#utente.getId(), #soloAttive}")
    public void getUtenteStrutturaListRemoveCache(Utente utente, boolean soloAttive) {
    }

    /**
     * restituisce tutte le aziende degli utenti attivi della persona passata
     *
     * @param persona
     * @return
     */
    @Cacheable(value = "getAziendePersona__ribaltorg__", key = "{#persona.getId()}")
    public List<Azienda> getAziendePersona(Persona persona) {
        List<Azienda> res = new ArrayList();
        List<Utente> utentiPersona = getUtentiPersona(persona);

        if (utentiPersona != null && !utentiPersona.isEmpty()) {
            utentiPersona.stream().forEach(u -> {
                if (u.getAttivo()) {
                    res.add(getAziendaUtente(u));
                }
            });
        }

        return res;
    }

    /**
     * restituisce tutte le aziende degli utenti attivi della persona passata
     *
     * @param persona
     * @return
     */
    @Cacheable(value = "getAziendePersonaSpenta__ribaltorg__", key = "{#persona.getId()}")
    public List<Azienda> getAziendePersonaSpenta(Persona persona) {
        List<Azienda> res = new ArrayList();
        List<Utente> utentiPersona = getUtentiPersona(persona);

        if (utentiPersona != null && !utentiPersona.isEmpty()) {
            utentiPersona.stream().forEach(u -> {
                if (!u.getAttivo()) {
                    res.add(getAziendaUtente(u));
                }
            });
        }

        return res;
    }

    @CacheEvict(value = "getPermessiDiFlusso__ribaltorg__", key = "{#utente.getId()}")
    public void getPermessiDiFlussoRemoveCache(Utente utente) {
    }

    @Cacheable(value = "getPermessiDiFlusso__ribaltorg__", key = "{#utente.getId()}")
    public List<PermessoEntitaStoredProcedure> getPermessiDiFlusso(Utente utente) throws BlackBoxPermissionException {
        return getPermessiDiFlusso(utente, null, null, null);
    }

//    @Cacheable(value = "getPermessiDiFlusso__ribaltorg__", key = "{#utente.getId(), #dataPermesso != null? #dataPermesso.toEpochDay(): 'null', #estraiStorico}")
//    public void getPermessiDiFlussoRemoveCache(Utente utente, LocalDate dataPermesso, Boolean estraiStorico) {
//    }
    @Cacheable(value = "getPermessiDiFlusso__ribaltorg__", key = "{#utente.getId(), #dataPermesso != null? #dataPermesso.toLocalDate().toEpochDay(): 'null', #estraiStorico, #idProvenienzaOggetto != null? #idProvenienzaOggetto: 'null'}")
    public List<PermessoEntitaStoredProcedure> getPermessiDiFlusso(Utente utente, LocalDateTime dataPermesso,
            Boolean estraiStorico, Integer idProvenienzaOggetto) throws BlackBoxPermissionException {
        BlackBoxConstants.Direzione direzione;
        if (estraiStorico != null && estraiStorico) {
            direzione = BlackBoxConstants.Direzione.PASSATO;
        } else {
            direzione = BlackBoxConstants.Direzione.PRESENTE;
        }
        return permissionManager.getPermissionsOfSubjectAdvanced(utente, idProvenienzaOggetto != null ? Lists.newArrayList(new Struttura(idProvenienzaOggetto)) : null,
                null, Arrays.asList(new String[]{InternautaConstants.Permessi.Ambiti.PICO.toString(),
            InternautaConstants.Permessi.Ambiti.DETE.toString(),
            InternautaConstants.Permessi.Ambiti.DELI.toString()}),
                Arrays.asList(new String[]{InternautaConstants.Permessi.Tipi.FLUSSO.toString()}),
                false, dataPermesso != null ? dataPermesso.toLocalDate() : null, null, direzione);
    }

    @Cacheable(value = "getPermessiFilteredByAdditionalData__ribaltorg__", key = "{#utente.getId(), #dataPermesso != null? #dataPermesso.toLocalDate().toEpochDay(): 'null', #modalita, "
            + "#idProvenienzaOggetto != null? #idProvenienzaOggetto: 'null', #ambitiPermesso != null? #ambitiPermesso.toString(): 'null', "
            + "#tipiPermesso != null? #tipiPermesso.toString(): 'null'}")
    public List<PermessoEntitaStoredProcedure> getPermessiFilteredByAdditionalData(Utente utente, LocalDateTime dataPermesso,
            String modalita, Integer idProvenienzaOggetto, List<InternautaConstants.Permessi.Ambiti> ambitiPermesso, List<InternautaConstants.Permessi.Tipi> tipiPermesso) throws BlackBoxPermissionException {
        BlackBoxConstants.Direzione direzione;
        if (modalita != null) {
            switch (modalita) {
                case "storico":
                    direzione = BlackBoxConstants.Direzione.PASSATO;
                    break;
                case "non_scaduti":
                    direzione = BlackBoxConstants.Direzione.NON_SCADUTI;
                    break;
                case "futuro":
                    direzione = BlackBoxConstants.Direzione.FUTURO;
                    break;
                default:
                    direzione = BlackBoxConstants.Direzione.PRESENTE;
            }
        } else {
            direzione = BlackBoxConstants.Direzione.PRESENTE;
        }
        return permissionManager.getPermissionsOfSubjectAdvanced(utente,
                idProvenienzaOggetto != null ? Lists.newArrayList(new Struttura(idProvenienzaOggetto)) : null,
                null,
                ambitiPermesso != null ? ambitiPermesso.stream().map(ambito -> ambito.toString()).collect(Collectors.toList()) : null,
                tipiPermesso != null ? tipiPermesso.stream().map(tipo -> tipo.toString()).collect(Collectors.toList()) : null,
                false, dataPermesso != null ? dataPermesso.toLocalDate() : null, null, direzione);
    }

    /**
     * Restituisce tutti i permessi di un utente di tipo flusso e per gli ambiti
     * PICO, DETE e DELI con un ordinamento di default (eg.
     * Struttura/Ambito/Permesso/AttivoDal)
     *
     * @param utente L'utente di cui si stanno chiedendo i permessi
     * @return La lista dei permessi dell'utente
     * @throws BlackBoxPermissionException
     */
//    @Cacheable(value = "getPermessiDiFlussoByIdUtente__ribaltorg__", key = "{#utente.getId()}")
    public List<Permesso> getPermessiDiFlussoByIdUtente(Utente utente) throws BlackBoxPermissionException {
        return userInfoService.getPermessiDiFlussoByIdUtente(utente, null, null, null);
    }

    @Cacheable(value = "getPermessiDiFlussoByIdUtente__ribaltorg__", key = "{#utente.getId(), #dataPermesso != null? #dataPermesso.toLocalDate().toEpochDay(): 'null', #estraiStorico, #idProvenienzaOggetto != null? #idProvenienzaOggetto: 'null'}")
    public List<Permesso> getPermessiDiFlussoByIdUtente(Utente utente, LocalDateTime dataPermesso,
            Boolean estraiStorico, Integer idProvenienzaOggetto) throws BlackBoxPermissionException {
        List<PermessoEntitaStoredProcedure> permessiDiFlusso = getPermessiDiFlusso(utente, dataPermesso, estraiStorico, idProvenienzaOggetto);
        // Riorganizziamo i dati in un oggetto facilmente leggibile dal frontend
        List<Permesso> permessiUtente = new ArrayList<>();

        permessiDiFlusso.forEach((permessoEntita) -> {
            permessoEntita.getCategorie().forEach((categoria) -> {
                categoria.getPermessi().forEach((permessoCategoria) -> {
                    Permesso permesso = new Permesso();
                    permesso.setAmbito(categoria.getAmbito());
                    permesso.setPermesso(permessoCategoria.getPredicato());
                    permesso.setAttivoDal(permessoCategoria.getAttivoDal());
                    permesso.setAttivoAl(permessoCategoria.getAttivoAl() != null ? permessoCategoria.getAttivoAl() : null);
                    permesso.setNomeStruttura(strutturaRepository.getOne(permessoEntita.getOggetto().getIdProvenienza()).getNome());
                    permessiUtente.add(permesso);
                });
            });
        });
        Collections.sort(permessiUtente);

        return permessiUtente;
    }

    @Cacheable(value = "getPermessiFilteredByAdditionalDataByIdUtente__ribaltorg__", key = "{#utente.getId(), #dataPermesso != null? #dataPermesso.toLocalDate().toEpochDay(): 'null', #modalita, "
            + "#idProvenienzaOggetto != null? #idProvenienzaOggetto: 'null', #ambitiPermesso != null? #ambitiPermesso.toString(): 'null', "
            + "#tipiPermesso != null? #tipiPermesso.toString(): 'null'}")
    public List<Permesso> getPermessiFilteredByAdditionalDataByIdUtente(
            Utente utente,
            LocalDateTime dataPermesso,
            String modalita,
            Integer idProvenienzaOggetto,
            List<InternautaConstants.Permessi.Ambiti> ambitiPermesso,
            List<InternautaConstants.Permessi.Tipi> tipiPermesso) throws BlackBoxPermissionException {

        List<PermessoEntitaStoredProcedure> permessiFilteredByAdditionalData = getPermessiFilteredByAdditionalData(utente, dataPermesso, modalita, idProvenienzaOggetto, ambitiPermesso, tipiPermesso);
        // Riorganizziamo i dati in un oggetto facilmente leggibile dal frontend
        List<Permesso> permessiUtente = new ArrayList<>();

        permessiFilteredByAdditionalData.forEach((permessoEntita) -> {
            permessoEntita.getCategorie().forEach((categoria) -> {
                categoria.getPermessi().forEach((permessoCategoria) -> {
                    Permesso permesso = new Permesso();
                    permesso.setAmbito(categoria.getAmbito());
                    permesso.setPermesso(permessoCategoria.getPredicato());
                    permesso.setAttivoDal(permessoCategoria.getAttivoDal());
                    permesso.setAttivoAl(permessoCategoria.getAttivoAl() != null ? permessoCategoria.getAttivoAl() : null);
                    permesso.setNomeStruttura(strutturaRepository.getOne(permessoEntita.getOggetto().getIdProvenienza()).getNome());
                    permessiUtente.add(permesso);
                });
            });
        });
        Collections.sort(permessiUtente);

        return permessiUtente;
    }

    // NB: non è by CODICEAZIENDA, ma è ByUtente da cui prendo Persona da cui prendo codice azienda
    @Cacheable(value = "getPermessiDiFlussoByCodiceAzienda_utente__ribaltorg__", key = "{#utente.getId()}")
    public Map<String, List<PermessoEntitaStoredProcedure>> getPermessiDiFlussoByCodiceAzienda(Utente utente) throws BlackBoxPermissionException {
        return getPermessiDiFlussoByCodiceAzienda(utente.getIdPersona());
    }
    
    @CacheEvict(value = "getPermessiDiFlussoByCodiceAzienda_utente__ribaltorg__", key = "{#utente.getId()}")
    public void getPermessiDiFlussoByCodiceAziendaRemoveCache(Utente utente) {
    }
  
    // NB: non è by CODICEAZIENDA, ma è ByPersona da cui prendo codice azienda
    @Cacheable(value = "getPermessiDiFlussoByCodiceAzienda_persona__ribaltorg__", key = "{#persona.getId()}")
    public Map<String, List<PermessoEntitaStoredProcedure>> getPermessiDiFlussoByCodiceAzienda(Persona persona) throws BlackBoxPermissionException {
        Map<String, List<PermessoEntitaStoredProcedure>> map = new HashMap<>();

        List<Utente> utentiPersona = persona.getUtenteList().stream().filter(u -> u.getAttivo() == true).collect(Collectors.toList());

        for (int i = 0; i < utentiPersona.size(); i++) {
            map.put(utentiPersona.get(i).getIdAzienda().getCodice(),
                    permissionManager.getPermissionsOfSubjectActualFromDate(utentiPersona.get(i), null, null,
                            Arrays.asList(new String[]{InternautaConstants.Permessi.Ambiti.PICO.toString(),
                        InternautaConstants.Permessi.Ambiti.DETE.toString(),
                        InternautaConstants.Permessi.Ambiti.DELI.toString()}),
                            Arrays.asList(new String[]{InternautaConstants.Permessi.Tipi.FLUSSO.toString()}),
                            false, null)
            );
        }
        return map;
    }

    @Cacheable(value = "getPermessiDiFlussoByPersona__ribaltorg__", key = "{#persona.getId()}")
    public Map<String, List<PermessoEntitaStoredProcedure>> getPermessiDiFlussoByPersona(Persona persona) throws BlackBoxPermissionException {
        Map<String, List<PermessoEntitaStoredProcedure>> map = new HashMap<>();

        List<Utente> utentiPersona = utenteRepository.findByIdPersonaAndAttivo(persona, true);

        for (int i = 0; i < utentiPersona.size(); i++) {
            map.put(utentiPersona.get(i).getIdAzienda().getCodice(),
                    permissionManager.getPermissionsOfSubjectActualFromDate(utentiPersona.get(i), null, null,
                            Arrays.asList(new String[]{InternautaConstants.Permessi.Ambiti.PICO.toString(),
                        InternautaConstants.Permessi.Ambiti.DETE.toString(),
                        InternautaConstants.Permessi.Ambiti.DELI.toString()}),
                            Arrays.asList(new String[]{InternautaConstants.Permessi.Tipi.FLUSSO.toString()}),
                            false, null)
            );
        }
        return map;
    }

    // NB: non è by CODICEAZIENDA, ma è ByPersona da cui prendo codice azienda
    @Cacheable(value = "getPredicatiDiFlussoByCodiceAzienda__ribaltorg__", key = "{#persona.getId()}")
    public Map<String, List<String>> getPredicatiDiFlussoByCodiceAzienda(Persona persona) throws BlackBoxPermissionException {

        Map<String, List<PermessoEntitaStoredProcedure>> map = getPermessiDiFlussoByCodiceAzienda(persona);

        Map<String, List<String>> resMap = new HashMap<>();
        for (Map.Entry<String, List<PermessoEntitaStoredProcedure>> entry : map.entrySet()) {

            Set<String> setPredicati = new TreeSet<>();
            for (PermessoEntitaStoredProcedure pesp : entry.getValue()) {
                for (CategoriaPermessiStoredProcedure c : pesp.getCategorie()) {
                    for (PermessoStoredProcedure p : c.getPermessi()) {
                        setPredicati.add(p.getPredicato());
                    }
                }
            }
            if (!setPredicati.isEmpty()) {
                resMap.put(entry.getKey(), Lists.newArrayList(setPredicati));
            }
        }

        return resMap;

    }

    public Map<Integer, List<String>> getPermessiPec(Utente utente) throws BlackBoxPermissionException {
        return getPermessiPec(utente.getIdPersona());
    }

    @Cacheable(value = "getPermessiPec__ribaltorg__", key = "{#persona.getId()}")
    public Map<Integer, List<String>> getPermessiPec(Persona persona) throws BlackBoxPermissionException {
//        LOGGER.info("reperimento permessi PEC");
        List<PermessoEntitaStoredProcedure> pecWithStandardPermissions = null;
        try {
            pecWithStandardPermissions = permissionManager.getPermissionsOfSubjectActualFromDate(
                    persona,
                    null,
                    null,
                    Arrays.asList(new String[]{InternautaConstants.Permessi.Ambiti.PECG.toString()}),
                    Arrays.asList(new String[]{InternautaConstants.Permessi.Tipi.PEC.toString()}), false, null);
        } catch (BlackBoxPermissionException ex) {
            LOGGER.error("Errore nel caricamento dei permessi PEC dalla BlackBox", ex);

        }
        if (pecWithStandardPermissions != null) {
            try {
                Map<Integer, List<String>> res = pecWithStandardPermissions.stream().collect(Collectors.toMap(
                        p -> p.getOggetto().getIdProvenienza(),
                        p -> p.getCategorie().get(0).getPermessi().stream().map(c -> c.getPredicato()).collect(Collectors.toList())));
                return res;
            } catch (Exception ex) {
                LOGGER.error("Errore nella gestione dei permessi pec recuperati", ex);
                return null;
            }
        } else {
            LOGGER.warn("Nessu npermesso PEC");
            return null;
        }
    }

    @CacheEvict(value = "getPermessiPec__ribaltorg__", key = "{#persona.getId()}")
    public void getPermessiPecRemoveCache(Persona persona) {
    }

    @Cacheable(value = "getAziendeWherePersonaIsCa__ribaltorg__", key = "{#persona.getId()}")
    public List<Azienda> getAziendeWherePersonaIsCa(Persona persona) {
        List<Azienda> aziende = null;

        aziende = persona.getUtenteList().stream().filter(
                utente -> getRuoliPerModuli(utente, false).get(Ruolo.ModuliRuolo.GENERALE.toString()).stream().anyMatch(ruolo -> ruolo.getNomeBreve() == Ruolo.CodiciRuolo.CA)
        ).map(utente -> utente.getIdAzienda()).collect(Collectors.toList());

        return aziende;
    }

    @Cacheable(value = "getAziendeWherePersonaIsR__ribaltorg__", key = "{#persona.getId()}")
    public List<Azienda> getAziendeWherePersonaIsR(Persona persona) {
        List<Azienda> aziende = null;

        aziende = persona.getUtenteList().stream().filter(
                utente -> getRuoliPerModuli(utente, false).get(Ruolo.ModuliRuolo.GENERALE.toString()).stream().anyMatch(ruolo -> ruolo.getNomeBreve() == Ruolo.CodiciRuolo.R)
        ).map(utente -> utente.getIdAzienda()).collect(Collectors.toList());

        return aziende;
    }

    @Cacheable(value = "aziendaFromIdUtente__ribaltorg__", key = "{#idUtente}")
    public Azienda getAziendaFromIdUtente(Integer idUtente) {
        Optional<Utente> utente = utenteRepository.findById(idUtente);
        if (utente.isPresent()) {
            return cachedEntities.getAzienda(utente.get().getIdAzienda().getId());
        } else {
            return null;
        }
    }

    @Cacheable(value = "getRuoliIsCI__ribaltorg__", key = "{#user.getId()}")
    public boolean isCI(Utente user) {
        if (user.getRuoliUtentiPersona() == null) {
            user.setRuoliUtentiPersona(getRuoliUtentiPersona(user, true));
        }
        Map<String, Map<String, List<String>>> ruoliUtentiPersona = user.getRuoliUtentiPersona();
        return ruoliUtentiPersona.containsKey(Ruolo.CodiciRuolo.CI.toString());
    }

//    @Cacheable(value = "isR__ribaltorg__", key = "{#user.getId()}")
//    public boolean isR(Utente user) {
//       return isR(user, null);
//    }
    @Cacheable(value = "getRuoliIIsR__ribaltorg__", key = "{#user.getId(), #modulo.toString()}")
    public boolean isR(Utente user, Ruolo.ModuliRuolo modulo) {
        if (user.getRuoliUtentiPersona() == null) {
            user.setRuoliUtentiPersona(getRuoliUtentiPersona(user, true));
        }
        Map<String, Map<String, List<String>>> ruoliUtentiPersona = user.getRuoliUtentiPersona();
        boolean containsKey = ruoliUtentiPersona.containsKey(Ruolo.CodiciRuolo.R.toString());
        if (containsKey) {
            List<String> get = ruoliUtentiPersona.get(Ruolo.CodiciRuolo.R.toString()).get(modulo.toString());
            if (get != null) {
                return get.contains(user.getIdAzienda().getCodice());
            }
        }
        return false;
    }

    /**
     * Torna "true" se l'utente è CA di almeno un'azienda
     *
     * @param user
     * @return
     */
    @Cacheable(value = "getRuoliIsCA__ribaltorg__", key = "{#user.getId()}")
    public boolean isCA(Utente user) {
        if (user.getRuoliUtentiPersona() == null) {
            user.setRuoliUtentiPersona(getRuoliUtentiPersona(user, true));
        }
        Map<String, Map<String, List<String>>> ruoliUtentiPersona = user.getRuoliUtentiPersona();
        boolean containsKey = ruoliUtentiPersona.containsKey(Ruolo.CodiciRuolo.CA.toString());
        if (containsKey) {
            List<String> get = ruoliUtentiPersona.get(Ruolo.CodiciRuolo.CA.toString()).get(Ruolo.ModuliRuolo.GENERALE.toString());
            if (get != null) {
                return get.contains(user.getIdAzienda().getCodice());
            }
        }
        return false;
    }

    @Cacheable(value = "getRuoliIsSD__ribaltorg__", key = "{#user.getId()}")
    public boolean isSD(Utente user) {
        if (user.getRuoliUtentiPersona() == null) {
            user.setRuoliUtentiPersona(getRuoliUtentiPersona(user, true));
        }
        Map<String, Map<String, List<String>>> ruoliUtentiPersona = user.getRuoliUtentiPersona();
        return ruoliUtentiPersona.containsKey(Ruolo.CodiciRuolo.SD.toString());
    }

    /**
     * dato un user torna la lista di utenti (id) di cui quell'user è delegato
     *
     * @param user
     * @return
     * @throws BlackBoxPermissionException
     */
    @Cacheable(value = "getPermessiDelega__ribaltorg__", key = "{#user.getId()}")
    public List<Integer> getPermessiDelega(Utente user) throws BlackBoxPermissionException {
        List<PermessoEntitaStoredProcedure> permissionsOfSubject = permissionManager.getPermissionsOfSubjectActualFromDate(user, null,
                Arrays.asList(new String[]{InternautaConstants.Permessi.Predicati.DELEGA.toString()}),
                Arrays.asList(new String[]{InternautaConstants.Permessi.Ambiti.MATRINT.toString()}),
                Arrays.asList(new String[]{InternautaConstants.Permessi.Tipi.DELEGA.toString()}),
                false, null);

        List<Integer> utentiDeleganti = permissionsOfSubject.stream().map(p -> p.getOggetto().getIdProvenienza()).collect(Collectors.toList());
        return utentiDeleganti;
    }

    @CacheEvict(value = "getPermessiDelega__ribaltorg__", key = "{#user.getId()}")
    public void getPermessiDelegaRemoveCache(Utente user) {
    }

    @Cacheable(value = "getPermessiAvatar__ribaltorg__", key = "{#user.getId()}")
    public List<Integer> getPermessiAvatar(Utente user) throws BlackBoxPermissionException {
        List<PermessoEntitaStoredProcedure> permissionsOfSubject = permissionManager.getPermissionsOfSubjectActualFromDate(user, null,
                Arrays.asList(new String[]{InternautaConstants.Permessi.Predicati.DELEGA.toString()}),
                Arrays.asList(new String[]{InternautaConstants.Permessi.Ambiti.AVATAR.toString()}),
                Arrays.asList(new String[]{InternautaConstants.Permessi.Tipi.DELEGA.toString()}),
                false, null);

        List<Integer> utentiAvataranti = permissionsOfSubject.stream().map(p -> p.getOggetto().getIdProvenienza()).collect(Collectors.toList());
        return utentiAvataranti;
    }

    @CacheEvict(value = "getPermessiAvatar__ribaltorg__", key = "{#user.getId()}")
    public void getPermessiAvatarRemoveCache(Utente user) {
    }

    public CustomAziendaLogin getAziendaCustomLogin(Utente user) {
        return factory.createProjection(CustomAziendaLogin.class, user.getIdAzienda());
    }

    public List<CustomAziendaLogin> getAllAziendeCustomLogin(Utente user, Boolean soloUtenzaAttiva) {

        if (soloUtenzaAttiva) {
            return user.getIdPersona().getUtenteList().stream().filter(u -> u.getAttivo())
                    .map(u -> factory.createProjection(CustomAziendaLogin.class, u.getIdAzienda()))
                    .collect(Collectors.toList());
        } else {
            return user.getIdPersona().getUtenteList().stream()
                    .map(u -> factory.createProjection(CustomAziendaLogin.class, u.getIdAzienda()))
                    .collect(Collectors.toList());
        }
    }

    public Map<String, Object> getPermessiKrint(Persona persona) throws BlackBoxPermissionException {

        // TODO: da implementare
        Map<String, Object> result = new HashMap<>();

        // permessi PEC. Chiave: l'id della casella, valore: la lista di permessi per quella casella
        result.put(InternautaConstants.Krint.PermessiKey.permessiPec.toString(), userInfoService.getPermessiPec(persona));
        result.put(InternautaConstants.Krint.PermessiKey.permessiFlusso.toString(), userInfoService.getPredicatiDiFlussoByCodiceAzienda(persona));

        return result;
    }

    public List<KrintBaborgAzienda> getAziendeKrint(Persona persona) {
        return persona.getUtenteList().stream()
                .map(u -> factory.createProjection(KrintBaborgAzienda.class, u.getIdAzienda()))
                .collect(Collectors.toList());
    }

    public KrintBaborgAzienda getAziendaKrint(Utente utente) {
        return factory.createProjection(KrintBaborgAzienda.class, utente.getIdAzienda());
    }

    //@Cacheable(value = "getStruttureKrint__ribaltorg__", key = "{#utente.getId()}")
    public List<KrintBaborgStruttura> getStruttureKrint(Utente utente) {
        utente.setUtenteStrutturaList(getUtenteStrutturaList(utente, true));
        return utente.getUtenteStrutturaList().stream()
                .map(us -> {
                    //us.setIdStruttura(getIdStruttura(us));
                    return factory.createProjection(KrintBaborgStruttura.class, us);
                })
                .collect(Collectors.toList());
    }

    @Cacheable(value = "getIdStrutturaFromUtenteStruttura__ribaltorg__", key = "{#utenteStruttura.getId()}")
    public Struttura getIdStrutturaFromUtenteStruttura(UtenteStruttura utenteStruttura) {
        return utenteStrutturaRepository.getOne(utenteStruttura.getId()).getIdStruttura();
    }

    public KrintBaborgPersona getPersonaKrint(Utente utente) {
        return factory.createProjection(KrintBaborgPersona.class, utente.getIdPersona());
    }
//
//    public KrintBaborgPersona getPersonaKrint(Utente utente) {
//        return  factory.createProjection(KrintBaborgPersona.class, utente.getIdPersona());
//    }

    @Cacheable(value = "getUtenteProcton", key = "{#idPersona, #codiceAzienda}")
    public UtenteProcton getUtenteProcton(Integer idPersona, String codiceAzienda) throws Http404ResponseException {
        Persona persona = personaRepository.getOne(idPersona);
        String qUtenteProcton = "SELECT DISTINCT ON (u.id_utente) "
                + "u.id_utente as idUtente, "
                + "CASE WHEN u.id_struttura IS NOT NULL THEN u.id_struttura ELSE af.id_struttura END as idStruttura "
                + "FROM procton.utenti u \n"
                + "LEFT JOIN procton.appartenenze_funzionali af ON u.id_utente = af.id_utente AND af.unificata != 0\n"
                + "WHERE cf = :codiceFiscale \n"
                + "LIMIT 1";

        UtenteProcton utenteProcton;
        // Prendo la connessione dal connection manager
        Sql2o dbConnection = postgresConnectionManager.getDbConnection(codiceAzienda);

        try (Connection conn = (Connection) dbConnection.open()) {
            utenteProcton = conn.createQuery(qUtenteProcton)
                    .addParameter("codiceFiscale", persona.getCodiceFiscale())
                    .executeAndFetchFirst(UtenteProcton.class);
        }

        if (utenteProcton == null) {
            throw new Http404ResponseException("1", "Problemi con il recupero dell'id_utente di procton");
        }
        return utenteProcton;
    }

    public List<Integer> getIdStruttureConPermessoSegreteriaByPersona(Persona persona) throws BlackBoxPermissionException {
        List<Integer> idStruttureConPermessoSegreteria = new ArrayList<>();
        Map<String, List<PermessoEntitaStoredProcedure>> permessiDiFlussoByPersona = userInfoService.getPermessiDiFlussoByPersona(persona);
        for (Map.Entry<String, List<PermessoEntitaStoredProcedure>> entry : permessiDiFlussoByPersona.entrySet()) {
            String key = entry.getKey();
            System.out.println(key);
            List<PermessoEntitaStoredProcedure> permessoEntitaStoredProcedureList = entry.getValue();
            for (PermessoEntitaStoredProcedure permessoEntitaStoredProcedure : permessoEntitaStoredProcedureList) {
                List<CategoriaPermessiStoredProcedure> categorie = permessoEntitaStoredProcedure.getCategorie();
                for (CategoriaPermessiStoredProcedure categoria : categorie) {
                    List<PermessoStoredProcedure> permessi = categoria.getPermessi();
                    for (PermessoStoredProcedure permessoStoredProcedure : permessi) {
                        if (permessoStoredProcedure.getPredicato()
                                .equals(InternautaConstants.Permessi.Predicati.SEGR.toString())) {
                            Integer idStruttura = permessoEntitaStoredProcedure.getOggetto().getIdProvenienza();
                            if (!idStruttureConPermessoSegreteria.contains(idStruttura)) {
                                idStruttureConPermessoSegreteria.add(idStruttura);
                                break;
                            }
                        }
                    }
                }
            }
        }
        return idStruttureConPermessoSegreteria;
    }

    public List<Integer> getIdStruttureDiResponsabilitaOfPersona(Persona persona) throws BlackBoxPermissionException {
        List<Integer> idStruttureDiCuiPersonaIsResponsabile = new ArrayList<>();
        List<Utente> utenteList = utenteRepository.findByIdPersonaAndAttivo(persona, true);
        for (Utente utente : utenteList) {
            List<Integer> idStruttureDiUtenteByCodiceRuolo = strutturaRepository.getIdStruttureDiUtenteByCodiceRuolo(utente, Ruolo.CodiciRuolo.R.toString());
            if (idStruttureDiUtenteByCodiceRuolo != null) {
                idStruttureDiUtenteByCodiceRuolo.stream().filter((idStruttura) -> (!idStruttureDiCuiPersonaIsResponsabile.contains(idStruttura))).forEachOrdered((idStruttura) -> {
                    idStruttureDiCuiPersonaIsResponsabile.add(idStruttura);
                });
            }
        }
        return idStruttureDiCuiPersonaIsResponsabile;
    }

    public List<Persona> getPersoneDiStruttureDiCuiPersonaIsSegretario(Persona persona) throws BlackBoxPermissionException {
        List<Integer> idStruttureConPermessoSegreteriaByPersona = getIdStruttureConPermessoSegreteriaByPersona(persona);
        return personaRepository.getPersoneAttiveListInStruttureAttiveList(idStruttureConPermessoSegreteriaByPersona);
    }

    public List<Persona> getPersoneDiStruttureDiCuiPersonaIsResponsabile(Persona persona) throws BlackBoxPermissionException {
        List<Integer> idStruttureDiResponsabilitaOfPersona = getIdStruttureDiResponsabilitaOfPersona(persona);
        return personaRepository.getPersoneAttiveListInStruttureAttiveList(idStruttureDiResponsabilitaOfPersona);
    }

    public List<Persona> addPersoneIfNotPresenti(List<Persona> listaOriginale, List<Persona> elementiDaAggiungereList) {
        if (elementiDaAggiungereList != null) {
            elementiDaAggiungereList.stream().filter((persona) -> (!listaOriginale.contains(persona))).forEachOrdered((object) -> {
                listaOriginale.add(object);
            });
        }
        return listaOriginale;
    }

    public List<Persona> getPersoneDiStruttureDiCuiPersonaIsResponsabileOrSegretario(Persona persona) throws BlackBoxPermissionException {
        List<Persona> listToReturn = new ArrayList<>();
        List<Persona> personeDiStruttureDiCuiPersonaIsSegretario = getPersoneDiStruttureDiCuiPersonaIsSegretario(persona);
        List<Persona> personeDiStruttureDiCuiSonoResponsabile = getPersoneDiStruttureDiCuiPersonaIsResponsabile(persona);
        listToReturn = addPersoneIfNotPresenti(listToReturn, personeDiStruttureDiCuiPersonaIsSegretario);
        listToReturn = addPersoneIfNotPresenti(listToReturn, personeDiStruttureDiCuiSonoResponsabile);
        return listToReturn;
    }

    /**
     * Torna true se l'utente loggato passato come parametro ha il permeso
     * passato sull'azienda passata
     *
     * @param predicato
     * @param user al posto della Persona passare l'Utente loggato
     * @param azienda
     * @param ambito
     * @param tipo
     * @return
     */
    public Boolean userHasPermissionOnAzienda(
            InternautaConstants.Permessi.Predicati predicato,
            Utente user,
            Azienda azienda,
            InternautaConstants.Permessi.Ambiti ambito,
            InternautaConstants.Permessi.Tipi tipo) {
        Map<String, List<PermessoEntitaStoredProcedure>> permessiDiFlussoByCodiceAzienda = user.getPermessiDiFlussoByCodiceAzienda();
        if (!permessiDiFlussoByCodiceAzienda.containsKey(azienda.getCodice())) {
            LOGGER.info("L'utente non ha permessi di flusso per l'azienda selezionata");
            return false;
        } else {
            for (PermessoEntitaStoredProcedure permessoStoredProcedure : permessiDiFlussoByCodiceAzienda.get(azienda.getCodice())) {
                for (CategoriaPermessiStoredProcedure categoria : permessoStoredProcedure.getCategorie()) {
                    if (categoria.getAmbito().equals(ambito.toString()) && categoria.getTipo().equals(tipo.toString())) {
                        for (PermessoStoredProcedure permesso : categoria.getPermessi()) {
                            if (permesso.getPredicato().equals(predicato.toString())) {
                                return true;
                            }
                        }
                    }
                }
            }
        }
        return false;
    }
    
    @Cacheable(value = "getStruttureDelSegretario__ribaltorg__", key = "{#persona.getId()}")
    public Integer[] getStruttureDelSegretario(Persona persona) {
        return personaRepository.getStruttureDelSegretario(persona.getId());
    }
    
    @CacheEvict(value = "getStruttureDelSegretario__ribaltorg__", key = "{#persona.getId()}")
    public void getStruttureDelSegretarioRemoveCache(Persona persona) {
    }
}
