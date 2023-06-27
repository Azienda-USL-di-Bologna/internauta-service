package it.bologna.ausl.internauta.utils.masterjobs.workers.jobs.cambioprofilo;

import it.bologna.ausl.internauta.service.repositories.baborg.AziendaRepository;
import it.bologna.ausl.internauta.service.repositories.baborg.PersonaRepository;
import it.bologna.ausl.internauta.service.repositories.baborg.ProfiliPredicatiRuoliRepository;
import it.bologna.ausl.internauta.service.repositories.baborg.ProfiliRepository;
import it.bologna.ausl.internauta.service.repositories.baborg.UtenteRepository;
import it.bologna.ausl.internauta.utils.masterjobs.exceptions.MasterjobsWorkerException;
import it.bologna.ausl.internauta.utils.masterjobs.workers.jobs.JobWorker;
import it.bologna.ausl.internauta.utils.masterjobs.workers.jobs.JobWorkerResult;
import it.bologna.ausl.internauta.utils.masterjobs.workers.jobs.calcolagerarchiaarchivio.CalcolaGerarchiaArchivioJobWorker;
import it.bologna.ausl.model.entities.baborg.Azienda;
import it.bologna.ausl.model.entities.baborg.Persona;
import it.bologna.ausl.model.entities.baborg.Profili;
import it.bologna.ausl.model.entities.baborg.ProfiliPredicatiRuoli;
import it.bologna.ausl.model.entities.baborg.QProfili;
import it.bologna.ausl.model.entities.baborg.Utente;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

/**
 *
 * @author mido
 */
public class CambioProfiloJobWorker extends JobWorker<CambioProfiloJobWorkerData, JobWorkerResult>{
    
    private static final Logger log = LoggerFactory.getLogger(CambioProfiloJobWorker.class);
    private final String name = CalcolaGerarchiaArchivioJobWorker.class.getSimpleName();
    
    @Autowired
    private AziendaRepository aziendaRepository;
    
    @Autowired
    private UtenteRepository utenteRepository;
    
    @Autowired
    private PersonaRepository personaRepository;
    
    @Autowired
    private ProfiliRepository profiliRepository;
    
    @Autowired 
    private ProfiliPredicatiRuoliRepository profiliPredicatiRuoliRepository;
    
    
    @Override
    protected JobWorkerResult doRealWork() throws MasterjobsWorkerException {
        log.info("Inizio", getName());
        //carico l'azienda
        String codiceAzienda = getWorkerData().getCodiceAzienda();
        Azienda azienda = aziendaRepository.findByCodice(codiceAzienda);
        
        //carico la persona a cui devo spegnere/accendere i permessi/ruoli
        Persona persona = personaRepository.findByCodiceFiscale(getWorkerData().getCodiceFiscale());
        
        //carico l'utente a cui devo spegnere/accendere i permessi/ruoli
        Utente utente = utenteRepository.findByIdAziendaAndIdPersona(azienda, persona);
        
        String profilOld = getWorkerData().getProfiloOld();
        
        String profilNew = getWorkerData().getProfiloNew();
        
        //TODO
//        tabella di congiunzione tra profili e (predicati o ruoli)
//          astra.profili_predicati_ruoli
        Profili profiloOld = profiliRepository.findByIdProfilo(profilOld);
        Profili profiloNew = profiliRepository.findByIdProfilo(profilNew);
        
        profiliPredicatiRuoliRepository.findAll(QProfili.profili)
        profiliPredicatiRuoliRepository.findByIdProfilo(profiloNew);
        //carico tutti i predicati dei permessi che servono al profilo nuovo
        //carico tutti i predicati dei permessi che servivano al profilo vecchio
        //spengo i permessi coi preficati che non stanno nell'intersezione
        //accendo i nuovi permessi che hanno predicati in profilo new e non in old
        //inizio a gestire la parte di ruoli
        //qui non devo tenere conto delle date
        //setto il ruolo a 1 (utente generico)
        //aggiungo al valore del ruolo quello che serve per il profilo
        //setto il ruolo ottenuto sulla persona / sull'utente
        
        
        
        
        return null;
    }

   @Override
    public String getName() {
        return this.name;
    }
    
}
