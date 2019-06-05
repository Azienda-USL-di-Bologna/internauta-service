package it.bologna.ausl.internauta.service.authorization;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.core.types.dsl.PathBuilder;
import edu.emory.mathcs.backport.java.util.Arrays;
import it.bologna.ausl.blackbox.PermissionManager;
import it.bologna.ausl.blackbox.exceptions.BlackBoxPermissionException;
import it.bologna.ausl.internauta.utils.bds.types.PermessoEntitaStoredProcedure;
import it.bologna.ausl.internauta.service.authorization.jwt.LoginController;
import it.bologna.ausl.internauta.service.repositories.baborg.AziendaRepository;
import it.bologna.ausl.internauta.service.repositories.baborg.PersonaRepository;
import it.bologna.ausl.internauta.service.repositories.baborg.RuoloRepository;
import it.bologna.ausl.internauta.service.repositories.baborg.UtenteRepository;
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
import it.bologna.ausl.model.entities.baborg.projections.CustomAziendaLogin;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.stream.Collectors;
import org.springframework.cache.annotation.CacheEvict;

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
    
    private static final Logger log = LoggerFactory.getLogger(LoginController.class);

    /**
     * carica l'azienda a partire dal path che ha effettuato la richiesta
     *
     * @param path
     * @return
     */
    @Cacheable(value = "aziendaInfo__ribaltorg__", key = "{#path}")
    public Azienda loadAziendaByPath(String path) {
        BooleanExpression filter;
        
        if ((path.equals(pathAziendaDefault)  || path.equals("localhost")) && internautaMode.equalsIgnoreCase("test")){
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
    public void loadAziendaByPathRemoveCache(String path) {}

    /**
     * carica l'utente a partire dall'id
     *
     * @param id
     * @param applicazione
     * @return
     */
    @Cacheable(value = "userInfo__ribaltorg__", key = "{#id, #applicazione}")
    public Utente loadUtente(Integer id, String applicazione) {
        Utente res = null;
        Optional<Utente> utenteOp = utenteRepository.findById(id);
        if (utenteOp.isPresent()) {
            res = utenteOp.get();
            res.getIdPersona().setApplicazione(applicazione);
        }
        return res;
    }
    
    @CacheEvict(value = "userInfo__ribaltorg__", key = "{#id, #applicazione}")
    public void loadUtenteRemoveCache(Integer id, String applicazione) {}

    /**
     * carica l'utente a partire dallo username e dal path dell'azienda dalla
     * quale proviene la richiesta
     *
     * @param username
     * @param aziendaPath
     * @param applicazione
     * @return
     */
    @Cacheable(value = "userInfo__ribaltorg__", key = "{#username, #aziendaPath, #applicazione}")
    public Utente loadUtente(String username, String aziendaPath, String applicazione) {
        Utente res = null;
        Azienda azienda = loadAziendaByPath(aziendaPath);
        if (azienda != null) {
            BooleanExpression utenteFilter = QUtente.utente.username.eq(username).and(QUtente.utente.idAzienda.id.eq(azienda.getId()));
            Optional<Utente> utenteOp = utenteRepository.findOne(utenteFilter);
            if (utenteOp.isPresent()) {
                res = utenteOp.get();
                res.getIdPersona().setApplicazione(applicazione);
            }
        }
        return res;
    }
    
    /**
     * invalida la cache della funzione loadUtente(String username, String aziendaPath)
     *
     * @param username
     * @param aziendaPath
     */
    @CacheEvict(value = "userInfo__ribaltorg__", key = "{#username, #aziendaPath, #applicazione}")
    public void loadUtenteRemoveCache(String username, String aziendaPath, String applicazione) {}

    /**
     * carica l'utente cachable a partire dai campi configurati per il login SSO
     *
     * @param entityClass classe entity da cui cercare l'utente (Utente o
     * Persona)
     * @param field campo del db da usare per cercare l'utente
     * @param ssoFieldValue campo che identifica l'utente iniettato da shibbolet
     * nella richiesta
     * @param azienda campo che identifica l'azienda
     * @param applicazione
     * @return
     */
    @Cacheable(value = "userInfo__ribaltorg__", key = "{#entityClass.getName(), #field, #ssoFieldValue, #azienda.getId(), #applicazione}")
    public Utente loadUtente(Class entityClass, String field, String ssoFieldValue, Azienda azienda, String applicazione) {

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
            res.getIdPersona().setApplicazione(applicazione);
            return utenteOp.get();
        }
        return res;
    }
    
    /**
     * invalida la cache della funzione loadUtente(Class entityClass, String field, String ssoFieldValue, Azienda azienda)
     * 
     * @param entityClass
     * @param field
     * @param ssoFieldValue
     * @param azienda 
     * @param applicazione 
     */
    @CacheEvict(value = "userInfo__ribaltorg__", key = "{#entityClass.getName(), #field, #ssoFieldValue, #azienda.getId(), #applicazione}")
    public void loadUtenteRemoveCache(Class entityClass, String field, String ssoFieldValue, Azienda azienda, String applicazione) {}

     /**
    * restituisce i ruoli aziendali/interaziendali a seconda del parametro interaziendali o entrambi nel caso in cui il parametro sia null
    * @param utente
    * @param interaziendali
    * @return la lista dei ruoli
    */
    @Cacheable(value = "getRuoli__ribaltorg__", key = "{#utente.getId()}")
    public List<Ruolo> getRuoli(Utente utente, Boolean interaziendali) {
        List<Ruolo> res = new ArrayList<>();
        List<Ruolo> ruoliAll = ruoloRepository.findAll();
        for (Ruolo ruolo : ruoliAll) {
            if(interaziendali == null ||interaziendali == true){
                if (ruolo.getSuperAziendale()) {
                    if ((utente.getIdPersona().getBitRuoli() & ruolo.getMascheraBit()) > 0) {
                       res.add(ruolo);
                    }
                }
            }
            if(interaziendali == null || interaziendali == false) {
                if (ruolo.getSuperAziendale() == false) {
                    if ((utente.getBitRuoli() & ruolo.getMascheraBit()) > 0) {
                        res.add(ruolo);
                    }
                }
            }
        }
        return res;
    }
    
    /**
    * restituisce tutti i ruoli di tutte le aziende della persona dell'utente, divisi per interaziendali e aziendali.
    * I ruoli aziendali sono raggruppati per azienda
    * Vale anche il viceversa: le aziende sono raggruppate per ruolo
    * @param utente
    * @return una mappa in cui la chiave è il codice azienda e il valore la lista dei codici ruolo per quell'azienda
    * nel caso dei ruoli interaziendali la chiave è 'interaziendali' unita a una mappa in cui la chiave è il ruolo e il valore è una lista di codici azienda
    */
    @Cacheable(value = "getRuoliUtentiPersona__ribaltorg__", key = "{#utente.getId()}")
    public Map<String, List<String>> getRuoliUtentiPersona(Utente utente) {
                               
        Map<String, List<String>> mapAziendeRuoli = new HashMap<>();       

        // popolo mappa azienda->listaRuoli
        mapAziendeRuoli = utente.getIdPersona().getUtenteList().stream().collect(
                Collectors.toMap(u -> 
                        u.getIdAzienda().getCodice(), u -> 
                                getRuoli(u, false).stream().map(r -> 
                                        r.getNomeBreve().toString()).collect(Collectors.toList())));
        mapAziendeRuoli.put("interaziendali", getRuoli(utente, true).stream().map(r -> r.getNomeBreve().toString()).collect(Collectors.toList())); 
        
        // popolo mappa ruolo->listaAziene
        Map<String, List<String>> mapRuoloAziende = new HashMap<>();
        for(Map.Entry<String, List<String>> entry : mapAziendeRuoli.entrySet()){
            for(String codiceRuolo: entry.getValue()){                                
                List<String> listAziende = mapRuoloAziende.get(codiceRuolo);
                if(listAziende == null){
                    listAziende = new ArrayList<>();
                }
                listAziende.add(entry.getKey());                              
                mapRuoloAziende.put(codiceRuolo, listAziende);
            }
        }
        
        // mergio le due mappe
        Map<String, List<String>> finalMap = new HashMap<>(mapAziendeRuoli);
        finalMap.putAll(mapRuoloAziende);
        
        return finalMap;
    }
    
       
    
    
    @CacheEvict(value = "getRuoli__ribaltorg__", key = "{#utente.getId()}")
    public void getRuoliRemoveCache(Utente utente) {}

    public List<AziendaWithPlainFields> getAziendePersonaWithPlainField(Utente utente) {
        List<AziendaWithPlainFields> res = new ArrayList();

        Utente refreshedUtente = utenteRepository.getOne(utente.getId());
        List<Utente> utenti = refreshedUtente.getIdPersona().getUtenteList();

        if (utenti != null && !utenti.isEmpty()) {
            utenti.stream().forEach(u -> {
                if (u.getAttivo())
                    res.add(factory.createProjection(AziendaWithPlainFields.class, u.getIdAzienda()));
            });
        }
        return res;
    }
//    
//    @Cacheable(value = "getAziendePersona__ribaltorg__", key = "{#utente.getId()}")
//    public List<Azienda> getAziendePersona(Utente utente) {
//        List<Azienda> res = new ArrayList();
//
//        Utente refreshedUtente = utenteRepository.getOne(utente.getId());
//        List<Utente> utenti = refreshedUtente.getIdPersona().getUtenteList();
//
//        if (utenti != null && !utenti.isEmpty()) {
//            utenti.stream().forEach(u -> {
//                if(u.getAttivo())
//                    res.add(u.getIdAzienda());
//            });
//        }
//        return res;
//    }
    
    @Cacheable(value = "getUtentiPersonaByUtente__ribaltorg__", key = "{#utente.getId()}")
    public List<Utente> getUtentiPersonaByUtente(Utente utente) {
        List<Utente> res = new ArrayList();

        Utente refreshedUtente = utenteRepository.getOne(utente.getId());
        List<Utente> utenti = refreshedUtente.getIdPersona().getUtenteList();

        if (utenti != null && !utenti.isEmpty()) {
            utenti.stream().forEach( u -> {
                if(u.getAttivo()) {
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
    public void getUtentiPersonaByUtenteRemoveCache(Utente utente) {}
    
    /**
     * restituisce tutte le aziende degli utenti della persona passata
     * @param persona
     * @return 
     */
    @Cacheable(value = "getAziendePersona__ribaltorg__", key = "{#persona.getId()}")
    public List<Azienda> getAziendePersona(Persona persona) {
        List<Azienda> res = new ArrayList();
        List<Utente> utentiPersona = getUtentiPersona(persona);
        
        if (utentiPersona != null && !utentiPersona.isEmpty()) {
            utentiPersona.stream().forEach(u -> {
                if(u.getAttivo())
                    res.add(getAziendaUtente(u));
            });
        }
        
        return res;
    }
    
    
    
    @CacheEvict(value = "getPermessiDiFlusso__ribaltorg__", key = "{#utente.getId()}")
    public void getPermessiDiFlussoRemoveCache(Utente utente) {}
    
    @Cacheable(value = "getPermessiDiFlusso__ribaltorg__", key = "{#utente.getId()}")
    public List<PermessoEntitaStoredProcedure> getPermessiDiFlusso(Utente utente) throws BlackBoxPermissionException {
        return permissionManager.getPermissionsOfSubject(utente, null,
                Arrays.asList(new String[]{InternautaConstants.Permessi.Ambiti.PICO.toString(),
                    InternautaConstants.Permessi.Ambiti.DETE.toString(),
                    InternautaConstants.Permessi.Ambiti.DELI.toString()}),
                Arrays.asList(new String[]{InternautaConstants.Permessi.Tipi.FLUSSO.toString()}),
                false);
    }
    
    
    @Cacheable(value = "getPermessiDiFlussoByCodiceAzienda__ribaltorg__", key = "{#utente.getId()}")
    public Map<String, List<PermessoEntitaStoredProcedure>> getPermessiDiFlussoByCodiceAzienda(Utente utente) throws BlackBoxPermissionException {
        Map<String, List<PermessoEntitaStoredProcedure>> map = new HashMap<>();
        
        List<Utente> utentiPersona = utente.getIdPersona().getUtenteList().stream().filter(u -> u.getAttivo() == true).collect(Collectors.toList());
        
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
    
    @Cacheable(value = "getAziendaLogin_ribaltorg__", key = "{#user.getId()}")
    public CustomAziendaLogin getAziendaLogin(Utente user) {
        return factory.createProjection(CustomAziendaLogin.class, user.getIdAzienda());
    }
    
    @Cacheable(value = "getAltreAziendeCustomLogin_ribaltorg__", key = "{#user.getId()}")
    public List<CustomAziendaLogin> getAltreAziendeCustomLogin(Utente user) {
        
        return user.getIdPersona().getUtenteList().stream()
                .map(u -> factory.createProjection(CustomAziendaLogin.class, u.getIdAzienda()))
                .collect(Collectors.toList());
    }
    
    
}
