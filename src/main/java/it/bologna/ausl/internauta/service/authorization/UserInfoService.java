package it.bologna.ausl.internauta.service.authorization;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.core.types.dsl.PathBuilder;
import it.bologna.ausl.blackbox.PermissionManager;
import it.bologna.ausl.blackbox.exceptions.BlackBoxPermissionException;
import it.bologna.ausl.internauta.utils.bds.types.PermessoEntitaStoredProcedure;
import it.bologna.ausl.internauta.service.authorization.jwt.LoginController;
import it.bologna.ausl.internauta.service.authorization.utils.UtenteProcton;
import it.bologna.ausl.internauta.service.configuration.utils.PostgresConnectionManager;
import it.bologna.ausl.internauta.service.exceptions.http.Http404ResponseException;
import it.bologna.ausl.internauta.service.repositories.baborg.AziendaRepository;
import it.bologna.ausl.internauta.service.repositories.baborg.PersonaRepository;
import it.bologna.ausl.internauta.service.repositories.baborg.RuoloRepository;
import it.bologna.ausl.internauta.service.repositories.baborg.UtenteRepository;
import it.bologna.ausl.internauta.service.repositories.baborg.UtenteStrutturaRepository;
import it.bologna.ausl.model.entities.baborg.Azienda;
import it.bologna.ausl.model.entities.baborg.Persona;
import it.bologna.ausl.model.entities.baborg.QAzienda;
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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.projection.ProjectionFactory;
import org.springframework.stereotype.Component;
import it.bologna.ausl.internauta.service.utils.InternautaConstants;
import it.bologna.ausl.internauta.utils.bds.types.CategoriaPermessiStoredProcedure;
import it.bologna.ausl.internauta.utils.bds.types.PermessoStoredProcedure;
import it.bologna.ausl.model.entities.baborg.Struttura;
import it.bologna.ausl.model.entities.baborg.UtenteStruttura;
import it.nextsw.common.interceptors.exceptions.AbortLoadInterceptorException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.stream.Collectors;
import org.springframework.cache.annotation.CacheEvict;
import it.bologna.ausl.model.entities.baborg.projections.CustomAziendaLogin;
import java.util.Arrays;
import java.util.Set;
import java.util.TreeSet;
import org.sql2o.Connection;
import org.sql2o.Sql2o;
import it.bologna.ausl.model.entities.logs.projections.KrintBaborgStruttura;
import it.bologna.ausl.model.entities.logs.projections.KrintBaborgAzienda;
import it.bologna.ausl.model.entities.logs.projections.KrintBaborgPersona;


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
    UtenteStrutturaRepository utenteStrutturaRepository;

    @Autowired
    RuoloRepository ruoloRepository;

    @Autowired
    ProjectionFactory factory;

    @Autowired
    PermissionManager permissionManager;

    @Autowired
    ObjectMapper objectMapper;

    @Value("${nextsdr.request.default.azienda-path}")
    String pathAziendaDefault;

    @Value("${nextsdr.request.default.azienda-codice}")
    String codiceAziendaDefault;

    @Value("${internauta.mode}")
    String internautaMode;
    
    @Autowired
    PostgresConnectionManager postgresConnectionManager;

    private static final Logger LOGGER = LoggerFactory.getLogger(LoginController.class);

    /**
     * carica l'azienda a partire dal path che ha effettuato la richiesta
     *
     * @param path
     * @return
     */
    @Cacheable(value = "aziendaInfo__ribaltorg__", key = "{#path}")
    public Azienda loadAziendaByPath(String path) {
        BooleanExpression filter;

        if ((path.equals(pathAziendaDefault) || path.equals("localhost")) && internautaMode.equalsIgnoreCase("test")) {
            filter = QAzienda.azienda.codice.eq(codiceAziendaDefault);
        } else {
            filter = Expressions.booleanTemplate("arraycontains({0}, string_to_array({1}, ','))=true", QAzienda.azienda.path, path);
        }

        Optional<Azienda> aziendaOp = aziendaRepository.findOne(filter);
        if (aziendaOp.isPresent()) {
            return aziendaOp.get();
        } else {
            return null;
        }
    }

    @CacheEvict(value = "aziendaInfo__ribaltorg__", key = "{#path}")
    public void loadAziendaByPathRemoveCache(String path) {
    }

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
        Azienda azienda = loadAziendaByPath(aziendaPath);
        if (azienda != null) {
            BooleanExpression utenteFilter = QUtente.utente.username.eq(username).and(QUtente.utente.idAzienda.id.eq(azienda.getId()));
            Optional<Utente> utenteOp = utenteRepository.findOne(utenteFilter);
            if (utenteOp.isPresent()) {
                res = utenteOp.get();
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
     * @return
     */
    @Cacheable(value = "userInfo__ribaltorg__", key = "{#entityClass.getName(), #field, #ssoFieldValue, #azienda.getId()}")
    public Utente loadUtente(Class entityClass, String field, String ssoFieldValue, Azienda azienda) {

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

        Optional<Utente> utenteOp = utenteRepository.findOne(filter);

        if (utenteOp.isPresent()) {
            res = utenteOp.get();
//            res.getIdPersona().setApplicazione(applicazione);
            return utenteOp.get();
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
     */
    @CacheEvict(value = "userInfo__ribaltorg__", key = "{#entityClass.getName(), #field, #ssoFieldValue, #azienda.getId()}")
    public void loadUtenteRemoveCache(Class entityClass, String field, String ssoFieldValue, Azienda azienda) {
    }

    /**
     * restituisce i ruoli aziendali/interaziendali a seconda del parametro
     * interaziendali o entrambi nel caso in cui il parametro sia null
     *
     * @param utente
     * @param interaziendali
     * @return la lista dei ruoli
     */
    @Cacheable(value = "getRuoli__ribaltorg__", key = "{#utente.getId()}")
    public List<Ruolo> getRuoli(Utente utente, Boolean interaziendali) {
        List<Ruolo> res = new ArrayList<>();
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
    
    @Cacheable(value = "getRuoliInteraziendali__ribaltorg__", key = "{#persona.getId()}")
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
     * per azienda 
     * Se ancheByRuolo è true o null vale anche il viceversa: le aziende sono raggruppate per ruolo 
     * @param persona
     * @param ancheByRuolo    
     * @return una mappa in cui la chiave è il codice azienda e il valore la
     * lista dei codici ruolo per quell'azienda nel caso dei ruoli
     * interaziendali la chiave è 'interaziendali' 
     * Se viene passato ancheByRuolo true o null alla mappa viene aggiunt una mappa in cui la
     * chiave è il codice ruolo e il valore è una lista di codici azienda
     */
    public Map<String, List<String>> getRuoli(Persona persona, Boolean ancheByRuolo){
        
        if(ancheByRuolo == null)
            ancheByRuolo = true;
        Map<String, List<String>> mapAziendeRuoli = new HashMap<>();
        
        if(persona.getUtenteList() == null){
            persona.setUtenteList(getUtentiPersona(persona));
        }

        // popolo mappa azienda->listaRuoli
        mapAziendeRuoli = persona.getUtenteList().stream().collect(
                Collectors.toMap(u
                        -> u.getIdAzienda().getCodice(), u
                        -> getRuoli(u, false).stream().map(r
                        -> r.getNomeBreve().toString()).collect(Collectors.toList())));
        mapAziendeRuoli.put("interaziendali", getRuoliInteraziendali(persona).stream().map(r -> r.getNomeBreve().toString()).collect(Collectors.toList()));

        // popolo mappa ruolo->listaAziene
        Map<String, List<String>> mapRuoloAziende = new HashMap<>();
        
        if(ancheByRuolo){            
            for (Map.Entry<String, List<String>> entry : mapAziendeRuoli.entrySet()) {
                for (String codiceRuolo : entry.getValue()) {
                    List<String> listAziende = mapRuoloAziende.get(codiceRuolo);
                    if (listAziende == null) {
                        listAziende = new ArrayList<>();
                    }
                    listAziende.add(entry.getKey());
                    mapRuoloAziende.put(codiceRuolo, listAziende);
                }
            }
        }

        // mergio le due mappe
        Map<String, List<String>> finalMap = new HashMap<>(mapAziendeRuoli);
        finalMap.putAll(mapRuoloAziende);

        return finalMap;
        
    }
    
    
    
    public Map<String, List<String>> getRuoliUtentiPersona(Utente utente, Boolean ancheByRuolo) {       
        return getRuoli(utente.getIdPersona(), ancheByRuolo);        
    }

    @CacheEvict(value = "getRuoli__ribaltorg__", key = "{#utente.getId()}")
    public void getRuoliRemoveCache(Utente utente) {
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


    @Cacheable(value = "getUtentiPersonaByUtente__ribaltorg__", key = "{#utente.getId()}")
    public List<Utente> getUtentiPersonaByUtente(Utente utente) {
        List<Utente> res = new ArrayList();

        Utente refreshedUtente = utenteRepository.getOne(utente.getId());
        List<Utente> utenti = refreshedUtente.getIdPersona().getUtenteList();

        if (utenti != null && !utenti.isEmpty()) {
            utenti.stream().forEach(u -> {
                if (u.getAttivo()) {
                    res.add(u);
                }
            });
        }
        return res;
    }

    @Cacheable(value = "getUtentiPersona__ribaltorg__", key = "{#persona.getId()}")
    public List<Utente> getUtentiPersona(Persona persona) {
        Persona refreshedPersona = personaRepository.getOne(persona.getId());
        return refreshedPersona.getUtenteList();
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
    
    @Cacheable(value = "getUtenteStrutturaList__ribaltorg__", key = "{#persona.getId()}")
    public List<UtenteStruttura> getUtenteStrutturaList(Utente utente) {
        Utente refreshedUtente = utenteRepository.getOne(utente.getId());
        return refreshedUtente.getUtenteStrutturaList();
    }

    /**
     * restituisce tutte le aziende degli utenti della persona passata
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

    @CacheEvict(value = "getPermessiDiFlusso__ribaltorg__", key = "{#utente.getId()}")
    public void getPermessiDiFlussoRemoveCache(Utente utente) {
    }

    @Cacheable(value = "getPermessiDiFlusso__ribaltorg__", key = "{#utente.getId()}")
    public List<PermessoEntitaStoredProcedure> getPermessiDiFlusso(Utente utente) throws BlackBoxPermissionException {
        return permissionManager.getPermissionsOfSubject(utente, null,
                Arrays.asList(new String[]{InternautaConstants.Permessi.Ambiti.PICO.toString(),
            InternautaConstants.Permessi.Ambiti.DETE.toString(),
            InternautaConstants.Permessi.Ambiti.DELI.toString()}),
                Arrays.asList(new String[]{InternautaConstants.Permessi.Tipi.FLUSSO.toString()}),
                false);
    }

   
    public Map<String, List<PermessoEntitaStoredProcedure>> getPermessiDiFlussoByCodiceAzienda(Utente utente) throws BlackBoxPermissionException {
        return getPermessiDiFlussoByCodiceAzienda(utente.getIdPersona());
    }
      
    
    @Cacheable(value = "getPermessiDiFlussoByCodiceAzienda__ribaltorg__", key = "{#persona.getId()}")
    public Map<String, List<PermessoEntitaStoredProcedure>> getPermessiDiFlussoByCodiceAzienda(Persona persona) throws BlackBoxPermissionException {
        Map<String, List<PermessoEntitaStoredProcedure>> map = new HashMap<>();

        List<Utente> utentiPersona = persona.getUtenteList().stream().filter(u -> u.getAttivo() == true).collect(Collectors.toList());

        for (int i = 0; i < utentiPersona.size(); i++) {
            map.put(utentiPersona.get(i).getIdAzienda().getCodice(),
                    permissionManager.getPermissionsOfSubject(utentiPersona.get(i), null,
                            Arrays.asList(new String[]{InternautaConstants.Permessi.Ambiti.PICO.toString(),
                        InternautaConstants.Permessi.Ambiti.DETE.toString(),
                        InternautaConstants.Permessi.Ambiti.DELI.toString()}),
                            Arrays.asList(new String[]{InternautaConstants.Permessi.Tipi.FLUSSO.toString()}),
                            false)
            );
        }
        return map;
    }
    
    @Cacheable(value = "getPredicatiDiFlussoByCodiceAzienda__ribaltorg__", key = "{#persona.getId()}")
    public Map<String, List<String>> getPredicatiDiFlussoByCodiceAzienda(Persona persona) throws BlackBoxPermissionException {
        
        Map<String, List<PermessoEntitaStoredProcedure>> map = getPermessiDiFlussoByCodiceAzienda(persona);
        

        Map<String, List<String>> resMap = new HashMap<>();
        for (Map.Entry<String, List<PermessoEntitaStoredProcedure>> entry : map.entrySet()) {
            
            Set<String> setPredicati = new TreeSet<>();
            for(PermessoEntitaStoredProcedure pesp: entry.getValue()){
                for(CategoriaPermessiStoredProcedure c:pesp.getCategorie()){
                    for(PermessoStoredProcedure p: c.getPermessi()){
                        setPredicati.add(p.getPredicato());
                    }
                }
            }
            if(!setPredicati.isEmpty()){                
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
        
        List<PermessoEntitaStoredProcedure> pecWithStandardPermissions = null;
        try {
            pecWithStandardPermissions = permissionManager.getPermissionsOfSubject(
                    persona,
                    null,
                    Arrays.asList(new String[]{InternautaConstants.Permessi.Ambiti.PECG.toString()}),
                    Arrays.asList(new String[]{InternautaConstants.Permessi.Tipi.PEC.toString()}), false);
        } catch (BlackBoxPermissionException ex) {
            LOGGER.error("Errore nel caricamento dei permessi PEC dalla BlackBox", ex);

        }

        Map<Integer, List<String>> res = pecWithStandardPermissions.stream().collect(Collectors.toMap(
                p -> p.getOggetto().getIdProvenienza(),
                p -> p.getCategorie().get(0).getPermessi().stream().map(c -> c.getPredicato()).collect(Collectors.toList())));
        return res;
    }
    
    @CacheEvict(value = "getPermessiPec__ribaltorg__", key = "{#persona.getId()}")
    public void getPermessiPecRemoveCache(Persona persona) {
    }

    @Cacheable(value = "getAziendeWherePersonaIsCa__ribaltorg__", key = "{#persona.getId()}")
    public List<Azienda> getAziendeWherePersonaIsCa(Persona persona) {
        List<Azienda> aziende = null;

        aziende = persona.getUtenteList().stream().filter(
                utente -> getRuoli(utente, false).stream().anyMatch(ruolo -> ruolo.getNomeBreve() == Ruolo.CodiciRuolo.CA)
        ).map(utente -> utente.getIdAzienda()).collect(Collectors.toList());

        return aziende;
    }

    @Cacheable(value = "isCI__ribaltorg__", key = "{#user.getId()}")
    public boolean isCI(Utente user) {
        List<Ruolo> ruoli = user.getRuoli();
        Boolean isCI = ruoli.stream().anyMatch(p -> p.getNomeBreve() == Ruolo.CodiciRuolo.CI);
        return isCI;
    }

    @Cacheable(value = "isCA__ribaltorg__", key = "{#user.getId()}")
    public boolean isCA(Utente user) {
        List<Ruolo> ruoli = user.getRuoli();
        Boolean isCA = ruoli.stream().anyMatch(p -> p.getNomeBreve() == Ruolo.CodiciRuolo.CA);
        return isCA;
    }

    @Cacheable(value = "getPermessiDelega__ribaltorg__", key = "{#user.getId()}")
    public List<Integer> getPermessiDelega(Utente user) throws BlackBoxPermissionException {
        List<PermessoEntitaStoredProcedure> permissionsOfSubject = permissionManager.getPermissionsOfSubject(user,
                Arrays.asList(new String[]{InternautaConstants.Permessi.Predicati.DELEGA.toString()}),
                Arrays.asList(new String[]{InternautaConstants.Permessi.Ambiti.AVATAR.toString()}),
                Arrays.asList(new String[]{InternautaConstants.Permessi.Tipi.DELEGA.toString()}), false);

        List<Integer> utentiDeleganti = permissionsOfSubject.stream().map(p -> p.getOggetto().getIdProvenienza()).collect(Collectors.toList());
        return utentiDeleganti;
    }

    @CacheEvict(value = "getPermessiDelega__ribaltorg__", key = "{#user.getId()}")
    public void getPermessiDelegaRemoveCache(Utente user) {
    }
    
    public CustomAziendaLogin getAziendaCustomLogin(Utente user) {
        return factory.createProjection(CustomAziendaLogin.class, user.getIdAzienda());
    }
    
    public List<CustomAziendaLogin> getAllAziendeCustomLogin(Utente user) {
        
        return user.getIdPersona().getUtenteList().stream()
                .map(u -> factory.createProjection(CustomAziendaLogin.class, u.getIdAzienda()))
                .collect(Collectors.toList());
    }
    
    
    

    
    public Map<String, Object> getPermessiKrint(Persona persona) throws BlackBoxPermissionException{
        
        // TODO: da implementare        
        Map<String, Object> result = new HashMap<>();
        
        // permessi PEC. Chiave: l'id della casella, valore: la lista di permessi per quella casella
        result.put(InternautaConstants.Krint.PermessiKey.permessiPec.toString(), getPermessiPec(persona));
        result.put(InternautaConstants.Krint.PermessiKey.permessiFlusso.toString(), getPredicatiDiFlussoByCodiceAzienda(persona));
        
        
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
    
    
    public List<KrintBaborgStruttura> getStruttureKrint(Utente utente) {
        utente.setUtenteStrutturaList(getUtenteStrutturaList(utente));
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
        return  factory.createProjection(KrintBaborgPersona.class, utente.getIdPersona());          
    }
    
    
    
    @Cacheable(value = "getUtenteProcton", key = "{#idPersona, #codiceAzienda}")
    public UtenteProcton getUtenteProcton(Integer idPersona, String codiceAzienda) throws Http404ResponseException {
        Persona persona = personaRepository.getOne(idPersona);
        String qUtenteProcton = "SELECT DISTINCT ON (u.id_utente) "
                + "u.id_utente as idUtente, "
                + "CASE WHEN u.id_struttura IS NOT NULL THEN u.id_struttura ELSE af.id_struttura END as idStruttura " +
            "FROM procton.utenti u \n" +
            "LEFT JOIN procton.appartenenze_funzionali af ON u.id_utente = af.id_utente AND af.unificata != 0\n" +
            "WHERE cf = :codiceFiscale \n" +
            "LIMIT 1";
        
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
}
