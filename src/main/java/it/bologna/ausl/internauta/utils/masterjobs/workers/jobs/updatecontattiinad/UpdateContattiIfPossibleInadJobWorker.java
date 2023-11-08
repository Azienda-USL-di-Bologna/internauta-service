package it.bologna.ausl.internauta.utils.masterjobs.workers.jobs.updatecontattiinad;

import it.bologna.ausl.internauta.service.controllers.rubrica.inad.InadExtractResponse;
import it.bologna.ausl.internauta.service.controllers.rubrica.inad.InadListDigitalAddressResponse;
import it.bologna.ausl.internauta.service.controllers.rubrica.inad.InadManager;
import it.bologna.ausl.internauta.utils.masterjobs.annotations.MasterjobsWorker;
import it.bologna.ausl.internauta.utils.masterjobs.exceptions.MasterjobsWorkerException;
import it.bologna.ausl.internauta.utils.masterjobs.workers.jobs.JobWorker;
import it.bologna.ausl.internauta.utils.masterjobs.workers.jobs.JobWorkerResult;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

/**
 *
 * @author utente
 */
@MasterjobsWorker
public class UpdateContattiIfPossibleInadJobWorker extends JobWorker<UpdateContattiIfPossibleInadJobWorkerData, JobWorkerResult> {
    private static final Logger log = LoggerFactory.getLogger(UpdateContattiIfPossibleInadJobWorker.class);
    
    @Autowired
    private InadManager inadManager;
   
    @Override
    public String getName() {
        return getClass().getSimpleName();
    }

    @Override
    public boolean isExecutable() {
        UpdateContattiIfPossibleInadJobWorkerData workerData = getWorkerData();
        String idRequest = workerData.getIdRequest();
        InadListDigitalAddressResponse statusRequestToExtractDomiciliDigitali = inadManager.statusRequestToExtractDomiciliDigitali(idRequest);
        return statusRequestToExtractDomiciliDigitali.getStato().equals(InadListDigitalAddressResponse.StatoRichiestaListaDomiciliDigitali.DISPONIBILE);
    }
    
    
    
    @Override
    public JobWorkerResult doRealWork() throws MasterjobsWorkerException {
        log.info(String.format("job %s started", getName()));       
        UpdateContattiIfPossibleInadJobWorkerData workerData = getWorkerData();
        String idRequest = workerData.getIdRequest();
        List<InadExtractResponse> extractMultiDomiciliDigitaliFromCodiciFiscali = inadManager.extractMultiDomiciliDigitaliFromCodiciFiscali(idRequest);
        for (InadExtractResponse inadExtractResponse : extractMultiDomiciliDigitaliFromCodiciFiscali) {
            inadManager.updateOrCreateDettaglioContattoFromInadExtractResponse(inadExtractResponse);
        }
        log.info(String.format("job %s ended", getName()));

        return null;
    }
    
}
