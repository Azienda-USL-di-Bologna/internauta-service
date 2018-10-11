package it.bologna.ausl.internauta.service.authorization;

import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.core.types.dsl.PathBuilder;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.projection.ProjectionFactory;
import org.springframework.stereotype.Component;

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

    /**
     * carica l'azienda a partire dal path che ha effettuato la richiesta
     *
     * @param path
     * @return
     */
    @Cacheable(value = "aziendaInfo__ribaltorg__", key = "{#path}")
    public Azienda loadAziendaByPath(String path) {
        BooleanExpression filter = Expressions.booleanTemplate("arraycontains({0}, string_to_array({1}, ','))=true", QAzienda.azienda.path, path);
        Optional<Azienda> aziendaOp = aziendaRepository.findOne(filter);
        if (aziendaOp.isPresent()) {
            return aziendaOp.get();
        } else {
            return null;
        }
    }

    /**
     * carica l'utente a partire dall'id
     *
     * @param id
     * @return
     */
    @Cacheable(value = "userInfo__ribaltorg__", key = "{#id}")
    public Utente loadUtente(Integer id) {
        Optional<Utente> utenteOp = utenteRepository.findById(id);
        if (utenteOp.isPresent()) {
            return utenteOp.get();
        } else {
            return null;
        }
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
            }
        }
        return res;
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
            return utenteOp.get();
        } else {
            return null;
        }
    }

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

    @Cacheable(value = "getAziendePersona__ribaltorg__", key = "{#utente.getId()}")
    public List<AziendaWithPlainFields> getAziendePersona(Utente utente) {
        List<AziendaWithPlainFields> res = new ArrayList();

        List<Utente> utenti = utente.getIdPersona().getUtenteList();

        if (utenti != null && !utenti.isEmpty()) {
            utenti.stream().forEach(u -> {
                res.add(factory.createProjection(AziendaWithPlainFields.class, u.getIdAzienda()));
            });
        }
        return res;
    }
}
