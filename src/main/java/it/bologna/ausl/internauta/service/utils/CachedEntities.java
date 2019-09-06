package it.bologna.ausl.internauta.service.utils;

import it.bologna.ausl.blackbox.exceptions.BlackBoxPermissionException;
import it.bologna.ausl.internauta.service.authorization.UserInfoService;
import it.bologna.ausl.internauta.service.repositories.baborg.AziendaRepository;
import it.bologna.ausl.internauta.service.repositories.baborg.PersonaRepository;
import it.bologna.ausl.internauta.service.repositories.baborg.StrutturaRepository;
import it.bologna.ausl.internauta.service.repositories.baborg.UtenteRepository;
import it.bologna.ausl.internauta.service.repositories.configurazione.ApplicazioneRepository;
import it.bologna.ausl.internauta.service.repositories.logs.OperazioneKrinRepository;
import it.bologna.ausl.model.entities.baborg.Azienda;
import it.bologna.ausl.model.entities.baborg.Persona;
import it.bologna.ausl.model.entities.baborg.Struttura;
import it.bologna.ausl.model.entities.baborg.Utente;
import it.bologna.ausl.model.entities.configuration.Applicazione;
import it.bologna.ausl.model.entities.logs.OperazioneKrint;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

/**
 *
 * @author gdm
 */
@Component
public class CachedEntities {

    @Autowired
    private AziendaRepository aziendaRepository;
    
    @Autowired
    private ApplicazioneRepository applicazioneRepository;
    
    @Autowired
    private StrutturaRepository strutturaRepository;
   
    @Autowired
    private PersonaRepository personaRepository;

    @Autowired
    private OperazioneKrinRepository operazioneKrinRepository;
    
    @Autowired
    private UtenteRepository utenteRepository;
    
    @Autowired
    private UserInfoService userInfoService;
    
    
    @Cacheable(value = "azienda", key = "{#id}")
    public Azienda getAzienda(Integer id) {
        Optional<Azienda> azienda = aziendaRepository.findById(id);
        if (azienda.isPresent())
            return azienda.get();
        else 
            return null;
    }
    
    @Cacheable(value = "aziendaFromIdUtente__ribaltorg__", key = "{#idUtente}")
    public Azienda getAziendaFromIdUtente(Integer idUtente) {
        Optional<Utente> utente = utenteRepository.findById(idUtente);
        if (utente.isPresent())
            return getAzienda(utente.get().getIdAzienda().getId());
        else 
            return null;
    }

    @Cacheable(value = "applicazione", key = "{#id}")
    public Applicazione getApplicazione(String id) {
        Optional<Applicazione> applicazione = applicazioneRepository.findById(id);
        if (applicazione.isPresent())
            return applicazione.get();
        else 
            return null;
    }
    
    @Cacheable(value = "struttura", key = "{#id}")
    public Struttura getStruttura(Integer id) {
        Optional<Struttura> struttura = strutturaRepository.findById(id);
        if (struttura.isPresent())
            return struttura.get();
        else 
            return null;
    }

    @Cacheable(value = "personaFromUtente__ribaltorg__", key = "{#utente.getId()}")
    public Persona getPersonaFromUtente(Utente utente) throws BlackBoxPermissionException {
        Utente refreshedUtente = utenteRepository.getOne(utente.getId());
        Persona persona = getPersona(refreshedUtente.getIdPersona().getId());
//        Optional<Persona> personaOp = personaRepository.findById(utente.getIdPersona().getId());
        if (persona != null) {
//            persona.setApplicazione(utente.getIdPersona().getApplicazione());
            persona.setPermessiPec(userInfoService.getPermessiPec(utente));
            return persona;
        } else
            return null;
    }

    @Cacheable(value = "persona__ribaltorg__", key = "{#id}")
    public Persona getPersona(Integer id) {
        Optional<Persona> persona = personaRepository.findById(id);
        if (persona.isPresent()) {
//            persona.get().setApplicazione(applicazione);
            return persona.get();
        } else
            return null;
    }
    
    @Cacheable(value = "personaFromIdUtente__ribaltorg__", key = "{#idUtente}")
    public Persona getPersonaFromIdUtente(Integer idUtente) throws BlackBoxPermissionException {
        return getPersonaFromUtente(getUtente(idUtente));
    }
    
    @Cacheable(value = "utente__ribaltorg__", key = "{#id}")
    public Utente getUtente(Integer id) {
        Optional<Utente> utente = utenteRepository.findById(id);
        if (utente.isPresent()) {
            return utente.get();
        } else
            return null;
    }
    
    @Cacheable(value = "operazioneKrint__ribaltorg__", key = "{#codiceOperazione}")
    public OperazioneKrint getOperazioneKrint(OperazioneKrint.CodiceOperazione codiceOperazione){
        return operazioneKrinRepository.findByCodice(codiceOperazione.toString()).orElse(null);
    }
    
    public OperazioneKrint getLastOperazioneVersionataKrint(OperazioneKrint.CodiceOperazione codiceOperazione){
        return operazioneKrinRepository.findByCodice(codiceOperazione.toString()).orElse(null);
    }
}
