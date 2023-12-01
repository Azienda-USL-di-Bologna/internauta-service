package it.bologna.ausl.internauta.utils.masterjobs.workers.services.rubricainad;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.querydsl.jpa.impl.JPAQueryFactory;
import it.bologna.ausl.internauta.service.controllers.rubrica.inad.InadParameters;
import it.bologna.ausl.internauta.utils.masterjobs.annotations.MasterjobsWorker;
import it.bologna.ausl.internauta.utils.masterjobs.exceptions.MasterjobsQueuingException;
import it.bologna.ausl.internauta.utils.masterjobs.exceptions.MasterjobsWorkerException;
import it.bologna.ausl.internauta.utils.masterjobs.workers.WorkerResult;
import it.bologna.ausl.internauta.utils.masterjobs.workers.jobs.updatecontattiinad.UpdateContattiInadJobWorker;
import it.bologna.ausl.internauta.utils.masterjobs.workers.jobs.updatecontattiinad.UpdateContattiInadJobWorkerData;
import it.bologna.ausl.internauta.utils.masterjobs.workers.services.ServiceWorker;
import it.bologna.ausl.internauta.utils.parameters.manager.ParametriAziendeReader;
import it.bologna.ausl.model.entities.baborg.QAzienda;
import it.bologna.ausl.model.entities.configurazione.Applicazione;
import it.bologna.ausl.model.entities.masterjobs.Set;
import java.util.List;
import javax.persistence.EntityManager;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

/**
 *
 * @author gdm
 */
@MasterjobsWorker
public class UpdateContattiInadServiceWorker extends ServiceWorker {
    private static Logger log = LoggerFactory.getLogger(UpdateContattiInadServiceWorker.class);
    
    @Autowired
    private ParametriAziendeReader parametriAziendaReader;
    
    @Autowired
    private EntityManager entityManager;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @Autowired
    private InadParameters inadParameters;
    
    private String name = UpdateContattiInadServiceWorker.class.getSimpleName();
    
    @Override
    public String getName() {
        return this.name;
    }
    
    @Override
    public WorkerResult doWork() throws MasterjobsWorkerException{
        log.info("sono il " + getName() + " e sto funzionando...");
        
        JPAQueryFactory jPAQueryFactory = new JPAQueryFactory(entityManager);
        QAzienda qAzienda = QAzienda.azienda;
        List<Integer> idAziende = jPAQueryFactory
            .select(qAzienda.id)
            .from(qAzienda)
            .fetch();
        
        for (Integer idAzienda : idAziende) {
            //controllo enabled su parametri recupero anche numeroContattiInadAggiornabili
            InadParameters buildedParameters = null;
            try {
                
                buildedParameters = inadParameters.buildParameters(idAzienda, parametriAziendaReader, objectMapper);
            } catch (JsonProcessingException ex) {
                throw new MasterjobsWorkerException("fallito il build dei parametri inad", ex);
            }
            if(buildedParameters != null && buildedParameters.getEnabled()){
                Integer numeroContattiInadAggiornabili = buildedParameters.getNumeroContattiAggiornabili();
                        
                //aggiungere il workerData col parametro 
                UpdateContattiInadJobWorkerData updateContattiInadJobWorkerData = new UpdateContattiInadJobWorkerData(numeroContattiInadAggiornabili, idAzienda);
                UpdateContattiInadJobWorker jobWorker = super.masterjobsObjectsFactory.getJobWorker(
                            UpdateContattiInadJobWorker.class, 
                            updateContattiInadJobWorkerData, 
                            false);
                try {
                    super.masterjobsJobsQueuer.queue(jobWorker, null, null, Applicazione.Applicazioni.gedi.toString(), false, Set.SetPriority.NORMAL, null);
                } catch (MasterjobsQueuingException ex) {
                    String errorMessage = "errore nell'accodamento del job di" + getName();
                    log.error(errorMessage, ex);
                    throw new MasterjobsWorkerException(errorMessage, ex);
                }
            }
        }
        return null;
    }
}
