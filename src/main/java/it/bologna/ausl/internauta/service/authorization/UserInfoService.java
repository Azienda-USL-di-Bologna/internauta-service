package it.bologna.ausl.internauta.service.authorization;

import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.core.types.dsl.PathBuilder;
import edu.emory.mathcs.backport.java.util.Arrays;
import it.bologna.ausl.blackbox.PermissionManager;
import it.bologna.ausl.blackbox.exceptions.BlackBoxPermissionException;
import it.bologna.ausl.blackbox.types.PermessoEntitaStoredProcedure;
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
import it.bologna.ausl.internauta.service.repositories.baborg.PermessoRepositoryOld;
import it.bologna.ausl.internauta.service.utils.InternautaConstants;
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
            filter = qPersona.get(field).eq(ssoFieldValue);
        } else {
            filter = qUtente.get(field).eq(ssoFieldValue);
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

    @Cacheable(value = "getRuoli__ribaltorg__", key = "{#utente.getId()}")
    public List<Ruolo> getRuoli(Utente utente) {
        List<Ruolo> res = new ArrayList<>();
        List<Ruolo> ruoliAll = ruoloRepository.findAll();
        for (Ruolo ruolo : ruoliAll) {
            if (ruolo.getSuperAziendale()) {
                if ((utente.getIdPersona().getBitRuoli() & ruolo.getMascheraBit()) > 0) {
                    res.add(ruolo);
                }
            } else {
                if ((utente.getBitRuoli() & ruolo.getMascheraBit()) > 0) {
                    res.add(ruolo);
                }
            }
        }
        return res;
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
    
    @Cacheable(value = "getUtentiPersona__ribaltorg__", key = "{#utente.getId()}")
    public List<Utente> getUtentiPersona(Utente utente) {
        List<Utente> res = new ArrayList();

        Utente refreshedUtente = utenteRepository.getOne(utente.getId());
        List<Utente> utenti = refreshedUtente.getIdPersona().getUtenteList();

        if (utenti != null && !utenti.isEmpty()) {
            utenti.stream().forEach(u -> {
                if(u.getAttivo())
                    res.add(u);
            });
        }
        return res;
    }
    
    @CacheEvict(value = "getUtentiPersona__ribaltorg__", key = "{#utente.getId()}")
    public void getUtentiPersonaRemoveCache(Utente utente) {}
    
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
}
