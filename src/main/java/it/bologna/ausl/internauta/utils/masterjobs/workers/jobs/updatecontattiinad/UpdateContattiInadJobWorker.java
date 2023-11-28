package it.bologna.ausl.internauta.utils.masterjobs.workers.jobs.updatecontattiinad;

import com.querydsl.jpa.impl.JPAQueryFactory;
import it.bologna.ausl.internauta.service.controllers.rubrica.inad.InadListDigitalAddressResponse;
import it.bologna.ausl.internauta.service.controllers.rubrica.inad.InadManager;
import it.bologna.ausl.internauta.utils.masterjobs.annotations.MasterjobsWorker;
import it.bologna.ausl.internauta.utils.masterjobs.exceptions.MasterjobsQueuingException;
import it.bologna.ausl.internauta.utils.masterjobs.exceptions.MasterjobsWorkerException;
import it.bologna.ausl.internauta.utils.masterjobs.workers.jobs.JobWorker;
import it.bologna.ausl.internauta.utils.masterjobs.workers.jobs.JobWorkerResult;
import it.bologna.ausl.model.entities.configurazione.Applicazione;
import it.bologna.ausl.model.entities.masterjobs.Set;
import it.bologna.ausl.model.entities.rubrica.Contatto;
import it.bologna.ausl.model.entities.rubrica.QContatto;
import it.bologna.ausl.model.entities.rubrica.QDettaglioContatto;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

/**
 *
 * @author utente
 */
@MasterjobsWorker
public class UpdateContattiInadJobWorker extends JobWorker<UpdateContattiInadJobWorkerData, JobWorkerResult> {
    private static final Logger log = LoggerFactory.getLogger(UpdateContattiInadJobWorker.class);
    
    @Autowired
    private InadManager inadManager;
    
   
    @Override
    public String getName() {
        return getClass().getSimpleName();
    }    
    
    @Override
    public JobWorkerResult doRealWork() throws MasterjobsWorkerException {
        log.info(String.format("job %s started", getName()));
        
        UpdateContattiInadJobWorkerData workerData = getWorkerData();
        Integer idAzienda = workerData.getIdAzienda();
        JPAQueryFactory jPAQueryFactory = new JPAQueryFactory(entityManager);
        QDettaglioContatto qDettaglioContatto = QDettaglioContatto.dettaglioContatto;
        QContatto qContatto = QContatto.contatto;
        List<String> cfDaAggiornare = jPAQueryFactory
            .select(qContatto.codiceFiscale)
            .from(qContatto)
//            .from(qDettaglioContatto)
//            .join(qContatto)
//            .where(qDettaglioContatto.domicilioDigitale)
             .where(qContatto.categoria.eq(Contatto.CategoriaContatto.ESTERNO.toString())
                     .and(qContatto.codiceFiscale.isNotNull()))
            .orderBy(qContatto.dataUltimoAggiornamentoDomicilioDigitale.asc())
            .limit(workerData.getNumeroContattiDaAggiornare())
            .fetch();
        
        try {
            InadListDigitalAddressResponse response = inadManager.requestToExtractDomiciliDigitaliFromCodiciFiscali(cfDaAggiornare, idAzienda);
            UpdateContattiIfPossibleInadJobWorkerData updateContattiIfPossibleInadJobWorkerData = new UpdateContattiIfPossibleInadJobWorkerData(response.getId(), idAzienda);
            UpdateContattiIfPossibleInadJobWorker jobWorker = super.masterjobsObjectsFactory.getJobWorker(
                UpdateContattiIfPossibleInadJobWorker.class,
                updateContattiIfPossibleInadJobWorkerData,
                false,
                120000);
            try {
                super.masterjobsJobsQueuer.queue(jobWorker, null, null, Applicazione.Applicazioni.gedi.toString(), false, Set.SetPriority.NORMAL);
            } catch (MasterjobsQueuingException ex) {
                String errorMessage = "errore nell'accodamento del job di" + getName();
                log.error(errorMessage);
                throw new MasterjobsWorkerException(errorMessage, ex);
            }
        } catch (Exception ex) {
            log.error("errore nella requestToExtractDomiciliDigitaliFromCodiciFiscali", ex);
        }
        
        log.info(String.format("job %s ended", getName()));

        return null;
    }
    
}
