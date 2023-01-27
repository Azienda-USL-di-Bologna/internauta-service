package it.bologna.ausl.internauta.service.gedi.utils;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.bologna.ausl.internauta.service.argo.utils.gd.FascicoloUtils;
import it.bologna.ausl.internauta.service.exceptions.sai.FascicoloNotFoundException;
import it.bologna.ausl.internauta.utils.parameters.manager.ParametriAziendeReader;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import it.bologna.ausl.internauta.service.exceptions.sai.FascicoloPadreNotDefinedException;
import it.bologna.ausl.internauta.utils.masterjobs.MasterjobsObjectsFactory;
import it.bologna.ausl.internauta.utils.masterjobs.exceptions.MasterjobsWorkerException;
import it.bologna.ausl.internauta.utils.masterjobs.exceptions.MasterjobsWorkerInitializationException;
import it.bologna.ausl.internauta.utils.masterjobs.workers.jobs.MasterjobsJobsQueuer;
import it.bologna.ausl.internauta.utils.masterjobs.workers.jobs.fascicolatoresai.FascicolatoreSAIWorker;
import it.bologna.ausl.internauta.utils.masterjobs.workers.jobs.fascicolatoresai.FascicolatoreSAIWorkerData;
import it.bologna.ausl.model.entities.baborg.Azienda;
import it.bologna.ausl.model.entities.baborg.Persona;
import it.bologna.ausl.model.entities.baborg.Utente;
import it.bologna.ausl.model.entities.configurazione.Applicazione;
import it.bologna.ausl.model.entities.masterjobs.Set;
import it.bologna.ausl.model.entities.shpeck.Outbox;

/**
 *
 * @author Salo
 */
@Component
public class SAIUtils {

    private static final Logger log = LoggerFactory.getLogger(SAIUtils.class);

    @Autowired
    private FascicoloUtils fascicoloUtils;

    @Autowired
    private ParametriAziendeReader parametriAziendeReader;
    
    @Autowired
    private MasterjobsJobsQueuer masterjobsJobsQueuer; 
    
    @Autowired
    private MasterjobsObjectsFactory masterjobsObjectsFactory; 
    
    @Autowired
    private ObjectMapper objectMapper;

    // fascicola pec
    public String fascicolaPec(Integer idOutbox,
            Azienda azienda,
            String cognome,
            String nome,
            String codiceFiscale,
            String mittente,
            String numerazioneGerarchicaDelPadre,
            Utente utente,
            Persona persona) throws Exception {
        String idFascicoloPadre = null;
        log.info("Cerco il fascicolo padre");
        Map<String, Object> fascicoloPadre = null;
        Map<String, Object> datiPerFascicolazione;
        if (numerazioneGerarchicaDelPadre == null) {
            log.info("fascicolazione gerarchida del padre non passata, la cerco in parametri_aziene");            
            datiPerFascicolazione = getDatiPerFascicolazione(mittente, azienda.getId());
        } else {
            datiPerFascicolazione = getDatiPerFascicolazione("default", azienda.getId());
        }
                    
        numerazioneGerarchicaDelPadre = (String) datiPerFascicolazione.get("numerazioneGerarchicaFascicolo");
        String nomeFascicoloTemplate = ((String) datiPerFascicolazione.get("templateNomeSottoFascicolo"));
        nomeFascicoloTemplate = nomeFascicoloTemplate.replace("[CF]", codiceFiscale);
        nomeFascicoloTemplate = nomeFascicoloTemplate.replace("[COGNOME]", cognome != null? cognome: "");
        nomeFascicoloTemplate = nomeFascicoloTemplate.replace("[NOME]", nome != null? nome: "");
        nomeFascicoloTemplate = nomeFascicoloTemplate.replaceAll("\\s+", " ").trim(); // toglie gli spazi in mezzo e all'inizio e alla fine
        if (numerazioneGerarchicaDelPadre != null) {
            fascicoloPadre = fascicoloUtils.getFascicoloByNumerazioneGerarchica(azienda.getId(), numerazioneGerarchicaDelPadre);
            if (fascicoloPadre != null) {
                idFascicoloPadre = (String) fascicoloPadre.get("id_fascicolo");
            } else {
                String errorMessage = String.format("Impossibile trovare il fascicolo %s ", numerazioneGerarchicaDelPadre);
                log.error(errorMessage);
                throw new FascicoloNotFoundException(errorMessage);
            }
        } else {
            String errorMessage = "non è stato possibile reperire la numerazione gerarchica del padre";
            log.error(errorMessage);
            throw new FascicoloPadreNotDefinedException(errorMessage);
        }
        log.info("id fascicolo padre: " + idFascicoloPadre);

        log.info("Cerco il fascicolo destinazione ...");
        Map<String, Object> fascicoloDestinazione = fascicoloUtils.getFascicoloByPatternInNameAndIdFascicoloPadre(azienda.getId(), codiceFiscale, idFascicoloPadre);
        if (fascicoloDestinazione != null) {
            log.info("fascicolo destinazione: " + fascicoloDestinazione.toString());
        } else {
            log.info("Not found fascicolo destinazione: va creato");
            
//            String nomeFascicoloTemplate = "SAI di " + codiceFiscale;
           
            fascicoloDestinazione = createFascicoloDestinazione(azienda.getId(), nomeFascicoloTemplate, fascicoloPadre);

            // QUA SI DOVREBBERO DUPLICARE I PERMESSI, MA ABBIAMO DECISO DI NO
        }

        String numerazioneFascicoloDestinazione = (String) fascicoloDestinazione.get("numerazione_gerarchica");
        log.info("Accodo jobs di fascicolazione outbox");
        
        FascicolatoreSAIWorker fascicolatoreSAIWorker = createFascicolatoreSAIWorker(
                idOutbox, azienda.getId(), null, null, numerazioneFascicoloDestinazione, utente.getId(), persona.getId());
        masterjobsJobsQueuer.queue(
                fascicolatoreSAIWorker, 
                idOutbox.toString(), 
                Outbox.class.getSimpleName(), 
                Applicazione.Applicazioni.sai.toString(), 
                false, 
                Set.SetPriority.NORMAL);

        return numerazioneFascicoloDestinazione;
    }
    
    private FascicolatoreSAIWorker createFascicolatoreSAIWorker(Integer idOutbox, Integer idAzienda, String cf, String mittente, String numerazioneGerarchica, Integer idUtente, Integer idPersona) throws MasterjobsWorkerException, MasterjobsWorkerInitializationException {
         FascicolatoreSAIWorkerData fascicolatoreSAIWorkerData = new FascicolatoreSAIWorkerData(
                idOutbox, idAzienda, cf, mittente, numerazioneGerarchica, idUtente, idPersona);
        return masterjobsObjectsFactory.getJobWorker(FascicolatoreSAIWorker.class, fascicolatoreSAIWorkerData, false, 60000);
    }

    private Map<String, Object> createFascicoloDestinazione(Integer idAzienda, String codiceFiscale, Map<String, Object> fascicoloPadre) throws Exception {
        log.info("Creo fascicolo destinazione");
        return fascicoloUtils.createFascicolo(idAzienda, codiceFiscale, fascicoloPadre);
    }

    private void duplicaPermessiFascicolo(Map<String, Object> fascicoloOrigine, Map<String, Object> fascicoloDestinazione) {

    }

    /**
     * Torna la configurazione per l'azienda e l'indirizzo pec passato, se non lo trova, torna la configurazione di default
     * @param indirizzoPec
     * @param idAzienda
     * @return
     * @throws FascicoloPadreNotDefinedException se il parametro fascicoliSAI non esiste o è vuoto, 
     * oppure se la configurazione per l'indirizzo pec passato non esiste e se non esiste neanche la configurazione di default
     */
    public Map<String, Object> getDatiPerFascicolazione(String indirizzoPec, Integer idAzienda) throws FascicoloPadreNotDefinedException {
        Map<String, Object> res;
        Map<String,  Map<String, Object>> mappaPecFascicoli;
        try {
            mappaPecFascicoli = parametriAziendeReader.getValue(
                    parametriAziendeReader.getParameters(ParametriAziendeReader.ParametriAzienda.fascicoliSAI.toString(), new Integer[]{idAzienda}).get(0),
                    new TypeReference<Map<String,  Map<String, Object>>>() {
            });
        } catch (Exception ex) {
            String errorMessage = "errore nella lettura del parametro \"fascicoliSAI\" dal database";
            log.error(indirizzoPec);
            throw new FascicoloPadreNotDefinedException(errorMessage, ex);
        }
        if (mappaPecFascicoli == null || mappaPecFascicoli.isEmpty()) {
            String errorMessage = String.format("non è stato definito nessun fascicolo padre nei parametri_azienda per l'azienda passata idAzienda %d", idAzienda);
            throw new FascicoloPadreNotDefinedException(errorMessage);
        }

        if (mappaPecFascicoli.containsKey(indirizzoPec)) {
            res = mappaPecFascicoli.get(indirizzoPec);
        } else {
            log.warn(String.format("non è stato definito nessun fascicolo padre nei parametri_azienda per la pec %s e l'azienda %d, leggo quello di default", indirizzoPec, idAzienda));
            if (mappaPecFascicoli.containsKey("default")) {
                res = mappaPecFascicoli.get("default");
            } else {
                String errorMessage = String.format("non è stato definito nessun fascicolo padre nei parametri_azienda per la pec %s e l'azienda passata", indirizzoPec);
                throw new FascicoloPadreNotDefinedException(errorMessage);
            }
        }
        return res;
    }
}
