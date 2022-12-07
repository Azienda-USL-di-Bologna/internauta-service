package it.bologna.ausl.internauta.service.utils;

import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.Expressions;
import it.bologna.ausl.blackbox.exceptions.BlackBoxPermissionException;
import it.bologna.ausl.internauta.service.authorization.UserInfoService;
import it.bologna.ausl.internauta.service.repositories.baborg.AziendaRepository;
import it.bologna.ausl.internauta.service.repositories.baborg.PersonaRepository;
import it.bologna.ausl.internauta.service.repositories.baborg.RuoloRepository;
import it.bologna.ausl.internauta.service.repositories.baborg.StrutturaRepository;
import it.bologna.ausl.internauta.service.repositories.baborg.UtenteRepository;
import it.bologna.ausl.internauta.service.repositories.configurazione.ApplicazioneRepository;
import it.bologna.ausl.internauta.service.repositories.logs.OperazioneKrinRepository;
import it.bologna.ausl.internauta.service.repositories.permessi.PredicatoAmbitoRepository;
import it.bologna.ausl.internauta.service.repositories.scripta.RegistroRepository;
import it.bologna.ausl.internauta.service.repositories.tools.SupportedFileRepository;
import it.bologna.ausl.internauta.utils.parameters.manager.ParametriAziendeReader;
import it.bologna.ausl.model.entities.baborg.Azienda;
import it.bologna.ausl.model.entities.baborg.Persona;
import it.bologna.ausl.model.entities.baborg.QAzienda;
import it.bologna.ausl.model.entities.baborg.QPersona;
import it.bologna.ausl.model.entities.baborg.QRuolo;
import it.bologna.ausl.model.entities.baborg.Ruolo;
import it.bologna.ausl.model.entities.baborg.Struttura;
import it.bologna.ausl.model.entities.baborg.Utente;
import it.bologna.ausl.model.entities.configurazione.Applicazione;
import it.bologna.ausl.model.entities.configurazione.ParametroAziende;
import it.bologna.ausl.model.entities.logs.OperazioneKrint;
import it.bologna.ausl.model.entities.scripta.QRegistro;
import it.bologna.ausl.model.entities.scripta.Registro;
import it.bologna.ausl.model.entities.tools.SupportedFile;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.projection.ProjectionFactory;
import org.springframework.stereotype.Component;

/**
 *
 * @author gdm
 */
@Component
public class CachedEntities {

    @Value("${nextsdr.request.default.azienda-path}")
    String pathAziendaDefault;

    @Value("${nextsdr.request.default.azienda-codice}")
    String codiceAziendaDefault;

    @Value("${internauta.mode}")
    String internautaMode;

    @Autowired
    protected ProjectionFactory factory;

    @Autowired
    private AziendaRepository aziendaRepository;

    @Autowired
    private ApplicazioneRepository applicazioneRepository;

    @Autowired
    private StrutturaRepository strutturaRepository;

    @Autowired
    private PersonaRepository personaRepository;

    @Autowired
    private RegistroRepository registroRepository;

    @Autowired
    private OperazioneKrinRepository operazioneKrinRepository;

    @Autowired
    private RuoloRepository ruoloRepository;

    @Autowired
    private UtenteRepository utenteRepository;
    
    @Autowired
    private ParametriAziendeReader parametriAziende;
    
    @Autowired
    private SupportedFileRepository supportedFileRepository;

    /**
     * torna tutte le aziende
     * @return 
     */
    @Cacheable(value = "aziende")
    public List<Azienda> getAllAziende() {
        return aziendaRepository.findAll();
    }
    
    @Cacheable(value = "azienda", key = "{#id}")
    public Azienda getAzienda(Integer id) {
        Optional<Azienda> azienda = aziendaRepository.findById(id);
        if (azienda.isPresent()) {
            return azienda.get();
        } else {
            return null;
        }
    }

    @Cacheable(value = "aziendaFromCodice__ribaltorg__", key = "{#codice}")
    public Azienda getAziendaFromCodice(String codice) {
        BooleanExpression filter = QAzienda.azienda.codice.eq(codice);
        Optional<Azienda> azienda = aziendaRepository.findOne(filter);
        if (azienda.isPresent()) {
            return azienda.get();
        } else {
            return null;
        }
    }
    
    @Cacheable(value = "aziendaFromCodiceRegioneAndCodice__ribaltorg__", key = "{#codiceRegioneAzienda}")
    public Azienda getAziendaFromCodiceRegioneAndCodice(String codiceRegioneAzienda) {
        BooleanExpression filter = QAzienda.azienda.codiceRegione.concat(QAzienda.azienda.codice).eq(codiceRegioneAzienda);
        Optional<Azienda> azienda = aziendaRepository.findOne(filter);
        if (azienda.isPresent()) {
            return azienda.get();
        } else {
            return null;
        }
    }
    
    @Cacheable(value = "aziendaFromNome__ribaltorg__", key = "{#nome}")
    public Azienda getAziendaFromNome(String nome) {
        BooleanExpression filter = QAzienda.azienda.nome.eq(nome);
        Optional<Azienda> azienda = aziendaRepository.findOne(filter);
        if (azienda.isPresent()) {
            return azienda.get();
        } else {
            return null;
        }
    }

    /**
     * carica l'azienda a partire dal path che ha effettuato la richiesta
     *
     * @param path
     * @return
     */
    @Cacheable(value = "aziendaFromPath__ribaltorg__", key = "{#path}")
    public Azienda getAziendaFromPath(String path) {
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

    @CacheEvict(value = "aziendaFromPath__ribaltorg__", key = "{#path}")
    public void getAziendaFromPathRemoveCache(String path) {
    }

    @Cacheable(value = "applicazione", key = "{#id}")
    public Applicazione getApplicazione(String id) {
        Optional<Applicazione> applicazione = applicazioneRepository.findById(id);
        if (applicazione.isPresent()) {
            return applicazione.get();
        } else {
            return null;
        }
    }

    @Cacheable(value = "ruolo", key = "{#nomeBreve.toString()}")
    public Ruolo getRuoloByNomeBreve(Ruolo.CodiciRuolo nomeBreve) {
        Optional<Ruolo> findOne = ruoloRepository.findOne(QRuolo.ruolo.nomeBreve.eq(nomeBreve.toString()));
        if (findOne.isPresent()) {
            return findOne.get();
        } else {
            return null;
        }
    }

    @Cacheable(value = "struttura__ribaltorg__", key = "{#id}")
    public Struttura getStruttura(Integer id) {
        Optional<Struttura> struttura = strutturaRepository.findById(id);
        if (struttura.isPresent()) {
            return struttura.get();
        } else {
            return null;
        }
    }

    @Cacheable(value = "persona__ribaltorg__", key = "{#id}")
    public Persona getPersona(Integer id) {
        Optional<Persona> persona = personaRepository.findById(id);
        if (persona.isPresent()) {
//            persona.get().setApplicazione(applicazione);
            return persona.get();
        } else {
            return null;
        }
    }

    @Cacheable(value = "personaFromCodiceFiscale__ribaltorg__", key = "{#codiceFiscale}")
    public Persona getPersonaFromCodiceFiscale(String codiceFiscale) {
        BooleanExpression filter = QPersona.persona.codiceFiscale.eq(codiceFiscale.toUpperCase());
        Optional<Persona> persona = personaRepository.findOne(filter);
        if (persona.isPresent()) {
//            persona.get().setApplicazione(applicazione);
            return persona.get();
        } else {
            return null;
        }
    }

    @Cacheable(value = "utente__ribaltorg__", key = "{#id}")
    public Utente getUtente(Integer id) {
        Optional<Utente> utente = utenteRepository.findById(id);
        if (utente.isPresent()) {
            return utente.get();
        } else {
            return null;
        }
    }

    @Cacheable(value = "operazioneKrint__ribaltorg__", key = "{#codiceOperazione}")
    public OperazioneKrint getOperazioneKrint(OperazioneKrint.CodiceOperazione codiceOperazione) {
        return operazioneKrinRepository.findByCodice(codiceOperazione.toString()).orElse(null);
    }

    public OperazioneKrint getLastOperazioneVersionataKrint(OperazioneKrint.CodiceOperazione codiceOperazione) {
        return operazioneKrinRepository.findByCodice(codiceOperazione.toString()).orElse(null);
    }

//    @Cacheable(value = "predicatoAmbito__ribaltorg__", key = "{#id}")
//    public PredicatoAmbito getPredicatoAmbito(Integer id) {
//        PredicatoAmbito predicatoAmbito = this.predicatoAmbitoRepository.getOne(id);
//        return predicatoAmbito;
//    }
    public void getRuoloByNomeBreve(String ruoloNomeBreve) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Cacheable(value = "registroAzienda", key = "{#idAzienda, #codice.toString()}")
    public Registro getRegistro(Integer idAzienda, Registro.CodiceRegistro codice) {
        QRegistro qRegistro = QRegistro.registro;
        BooleanExpression filtro = qRegistro.codice.eq(codice.toString())
                .and(qRegistro.idAzienda.id.eq(idAzienda));
        Optional<Registro> registro = registroRepository.findOne(filtro);
        if (registro.isPresent()) {
            return registro.get();
        } else {
            return null;
        }
    }
    
    @Cacheable(value = "getParameters", key = "{#nome, #idAzienda}")
    public List<ParametroAziende> getParameters(String nome, Integer idAzienda) {
        return parametriAziende.getParameters(nome, new Integer[]{idAzienda});
    }
    
    
    @Cacheable(value = "supportedFiles")
    public Map<String, SupportedFile> getSupportedFiles() {
        List<SupportedFile> all = supportedFileRepository.findAll();
        Map<String, SupportedFile>  m = new HashMap();
        for (SupportedFile f : all) {
            m.put(f.getMimeType(), f);
        }
        return m;
    }
}
