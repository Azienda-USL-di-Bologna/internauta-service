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
import it.bologna.ausl.internauta.service.repositories.scripta.ArchivioRepository;
import it.bologna.ausl.internauta.service.repositories.scripta.MezzoRepository;
import it.bologna.ausl.internauta.service.repositories.scripta.RegistroRepository;
import it.bologna.ausl.model.entities.baborg.Azienda;
import it.bologna.ausl.model.entities.baborg.Persona;
import it.bologna.ausl.model.entities.baborg.QAzienda;
import it.bologna.ausl.model.entities.baborg.QPersona;
import it.bologna.ausl.model.entities.baborg.QRuolo;
import it.bologna.ausl.model.entities.baborg.Ruolo;
import it.bologna.ausl.model.entities.baborg.Struttura;
import it.bologna.ausl.model.entities.baborg.Utente;
import it.bologna.ausl.model.entities.configurazione.Applicazione;
import it.bologna.ausl.model.entities.logs.OperazioneKrint;
import it.bologna.ausl.model.entities.scripta.Archivio;
import it.bologna.ausl.model.entities.scripta.Mezzo;
import it.bologna.ausl.model.entities.scripta.QMezzo;
import it.bologna.ausl.model.entities.scripta.QRegistro;
import it.bologna.ausl.model.entities.scripta.Registro;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.projection.ProjectionFactory;
import org.springframework.stereotype.Component;

/**
 *
 * @author gdm
 */
@Component
public class NonCachedEntities {

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
    private ArchivioRepository archivioRepository;

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
    private MezzoRepository mezzoRepository;

    @Autowired
    private UserInfoService userInfoService;

    @Autowired
    private PredicatoAmbitoRepository predicatoAmbitoRepository;

    public Azienda getAzienda(Integer id) {
        Optional<Azienda> azienda = aziendaRepository.findById(id);
        if (azienda.isPresent()) {
            return azienda.get();
        } else {
            return null;
        }
    }

    public Azienda getAziendaFromCodice(String codice) {
        BooleanExpression filter = QAzienda.azienda.codice.eq(codice);
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

    public Applicazione getApplicazione(String id) {
        Optional<Applicazione> applicazione = applicazioneRepository.findById(id);
        if (applicazione.isPresent()) {
            return applicazione.get();
        } else {
            return null;
        }
    }

    public Ruolo getRuoloByNomeBreve(Ruolo.CodiciRuolo nomeBreve) {
        Optional<Ruolo> findOne = ruoloRepository.findOne(QRuolo.ruolo.nomeBreve.eq(nomeBreve.toString()));
        if (findOne.isPresent()) {
            return findOne.get();
        } else {
            return null;
        }
    }

    public Struttura getStruttura(Integer id) {
        Optional<Struttura> struttura = strutturaRepository.findById(id);
        if (struttura.isPresent()) {
            return struttura.get();
        } else {
            return null;
        }
    }

    public Persona getPersonaFromUtente(Utente utente) throws BlackBoxPermissionException {
        Utente refreshedUtente = utenteRepository.getOne(utente.getId());
        Persona persona = getPersona(refreshedUtente.getIdPersona().getId());
//        Optional<Persona> personaOp = personaRepository.findById(utente.getIdPersona().getId());
        if (persona != null) {
//            persona.setApplicazione(utente.getIdPersona().getApplicazione());
            persona.setPermessiPec(userInfoService.getPermessiPec(utente));
            return persona;
        } else {
            return null;
        }
    }

    public Persona getPersona(Integer id) {
        Optional<Persona> persona = personaRepository.findById(id);
        if (persona.isPresent()) {
//            persona.get().setApplicazione(applicazione);
            return persona.get();
        } else {
            return null;
        }
    }

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

    public Persona getPersonaFromIdUtente(Integer idUtente) throws BlackBoxPermissionException {
        return getPersonaFromUtente(getUtente(idUtente));
    }

    public Utente getUtente(Integer id) {
        Optional<Utente> utente = utenteRepository.findById(id);
        if (utente.isPresent()) {
            return utente.get();
        } else {
            return null;
        }
    }

    public OperazioneKrint getOperazioneKrint(OperazioneKrint.CodiceOperazione codiceOperazione) {
        return operazioneKrinRepository.findByCodice(codiceOperazione.toString()).orElse(null);
    }

    public OperazioneKrint getLastOperazioneVersionataKrint(OperazioneKrint.CodiceOperazione codiceOperazione) {
        return operazioneKrinRepository.findByCodice(codiceOperazione.toString()).orElse(null);
    }
    
    public Registro getRegistro(Integer idAzienda, Registro.CodiceRegistro codice) {
        QRegistro qRegistro = QRegistro.registro;
        BooleanExpression filtro = qRegistro.codice.eq(codice)
                .and(qRegistro.idAzienda.id.eq(idAzienda));
        Optional<Registro> registro = registroRepository.findOne(filtro);
        if (registro.isPresent()) {
            return registro.get();
        } else {
            return null;
        }
    }
    
    public Archivio getArchivio(Integer id) {
        Optional<Archivio> archivio = archivioRepository.findById(id);
        if (archivio.isPresent()) {
            return archivio.get();
        } else {
            return null;
        }
    }
    
    public Archivio getArchivioFromNumerazioneGerarchicaAndIdAzienda(String numerazioneGerarchica, Integer idAzienda) {
        return archivioRepository.findByNumerazioneGerarchicaAndIdAzienda(numerazioneGerarchica, idAzienda);
    }
    
    public Mezzo getMezzoFromCodice(Mezzo.CodiciMezzo codice) {
        Optional<Mezzo> mezzo = mezzoRepository.findOne(QMezzo.mezzo.codice.eq(codice.toString()));
        if (mezzo.isPresent()) {
            return mezzo.get();
        } else {
            return null;
        }
    }
}
