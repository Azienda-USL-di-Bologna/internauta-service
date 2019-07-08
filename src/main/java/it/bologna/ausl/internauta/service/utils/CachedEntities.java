package it.bologna.ausl.internauta.service.utils;

import it.bologna.ausl.blackbox.exceptions.BlackBoxPermissionException;
import it.bologna.ausl.internauta.service.authorization.UserInfoService;
import it.bologna.ausl.internauta.service.repositories.baborg.AziendaRepository;
import it.bologna.ausl.internauta.service.repositories.baborg.PersonaRepository;
import it.bologna.ausl.internauta.service.repositories.baborg.UtenteRepository;
import it.bologna.ausl.internauta.service.repositories.configurazione.ApplicazioneRepository;
import it.bologna.ausl.model.entities.baborg.Azienda;
import it.bologna.ausl.model.entities.baborg.Persona;
import it.bologna.ausl.model.entities.baborg.Utente;
import it.bologna.ausl.model.entities.configuration.Applicazione;
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
    private PersonaRepository personaRepository;

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

    @Cacheable(value = "applicazione", key = "{#id}")
    public Applicazione getApplicazione(String id) {
        Optional<Applicazione> applicazione = applicazioneRepository.findById(id);
        if (applicazione.isPresent())
            return applicazione.get();
        else 
            return null;
    }

    @Cacheable(value = "persona__ribaltorg__", key = "{#utente.getId()}")
    public Persona getPersona(Utente utente) throws BlackBoxPermissionException {
//        Utente refreshedUtente = utenteRepository.getOne(utente.getId());
        Optional<Persona> personaOp = personaRepository.findById(utente.getIdPersona().getId());
        if (personaOp.isPresent()) {
            Persona persona = personaOp.get();
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
}
