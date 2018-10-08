package it.bologna.ausl.internauta.service.utils;

import it.bologna.ausl.internauta.service.repositories.baborg.AziendaRepository;
import it.bologna.ausl.internauta.service.repositories.baborg.PersonaRepository;
import it.bologna.ausl.internauta.service.repositories.configurazione.ApplicazioneRepository;
import it.bologna.ausl.model.entities.baborg.Azienda;
import it.bologna.ausl.model.entities.baborg.Persona;
import it.bologna.ausl.model.entities.configuration.Applicazione;
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
    
    @Cacheable(value = "azienda", key = "{#id}")
    public Azienda getAzienda(Integer id) {
        return aziendaRepository.getOne(id);
    }

    @Cacheable(value = "applicazione", key = "{#id}")
    public Applicazione getApplicazione(String id) {
        return applicazioneRepository.getOne(id);
    }

    @Cacheable(value = "persona", key = "{#id}")
    public Persona getPersona(Integer id) {
        return personaRepository.getOne(id);
    }
}
