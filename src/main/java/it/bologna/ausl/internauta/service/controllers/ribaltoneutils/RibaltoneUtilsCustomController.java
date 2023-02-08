package it.bologna.ausl.internauta.service.controllers.ribaltoneutils;

import it.bologna.ausl.internauta.utils.masterjobs.MasterjobsObjectsFactory;
import it.bologna.ausl.internauta.utils.masterjobs.exceptions.MasterjobsQueuingException;
import it.bologna.ausl.internauta.utils.masterjobs.exceptions.MasterjobsWorkerInitializationException;
import it.bologna.ausl.internauta.utils.masterjobs.workers.jobs.MasterjobsJobsQueuer;
import it.bologna.ausl.internauta.utils.masterjobs.workers.jobs.lanciatrasformatore.LanciaTrasformatoreJobWorker;
import it.bologna.ausl.internauta.utils.masterjobs.workers.jobs.lanciatrasformatore.LanciaTrasformatoreJobWorkerData;
import it.bologna.ausl.model.entities.configurazione.Applicazione;
import it.bologna.ausl.model.entities.masterjobs.Set;
import it.bologna.ausl.model.entities.ribaltoneutils.RibaltoneDaLanciare;
import java.io.IOException;
import javax.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

/**
 *
 * @author Top
 */
@RestController
@RequestMapping(value = "${ribaltoneutils.mapping.url.root}")
public class RibaltoneUtilsCustomController {   
    
    @Autowired
    MasterjobsObjectsFactory masterjobsObjectsFactory;
    
    @Autowired
    MasterjobsJobsQueuer masterjobsJobsQueuer;

    @RequestMapping(value = "lanciaTrasformatore", method = RequestMethod.POST)
    public ResponseEntity<?> lanciaTrasformatore(
            @RequestBody RibaltoneDaLanciare ribaltoneDaLanciare,
            HttpServletRequest request) throws IOException, MasterjobsWorkerInitializationException, MasterjobsQueuingException {
        
        if (!StringUtils.hasText(ribaltoneDaLanciare.getNote())){
            ribaltoneDaLanciare.setNote("nessuna nota");
        }else
        {
            ribaltoneDaLanciare.setNote(ribaltoneDaLanciare.getNote().replace("____", "----"));
                    
        }
       
        LanciaTrasformatoreJobWorkerData lanciaTrasformatoreJobWorkerData = new LanciaTrasformatoreJobWorkerData(
                ribaltoneDaLanciare.getIdAzienda().getId(), 
                ribaltoneDaLanciare.getRibaltaArgo(),
                ribaltoneDaLanciare.getRibaltaInternauta(), 
                ribaltoneDaLanciare.getEmail(),
                ribaltoneDaLanciare.getFonteRibaltone(), 
                ribaltoneDaLanciare.getTrasforma(),
                ribaltoneDaLanciare.getIdUtente().getId(),
                ribaltoneDaLanciare.getNote()
        );
        LanciaTrasformatoreJobWorker jobWorker = masterjobsObjectsFactory.getJobWorker(LanciaTrasformatoreJobWorker.class, lanciaTrasformatoreJobWorkerData, false);
        masterjobsJobsQueuer.queue(jobWorker, null, null, Applicazione.Applicazioni.trasformatore.toString(), false, Set.SetPriority.NORMAL);
        
        return new ResponseEntity("", HttpStatus.OK);
    }
    
}
