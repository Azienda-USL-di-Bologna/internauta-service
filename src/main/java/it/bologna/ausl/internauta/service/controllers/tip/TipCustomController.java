package it.bologna.ausl.internauta.service.controllers.tip;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.bologna.ausl.internauta.service.configuration.utils.ReporitoryConnectionManager;
import it.bologna.ausl.internauta.service.exceptions.http.ControllerHandledExceptions;
import it.bologna.ausl.internauta.service.exceptions.http.Http500ResponseException;
import it.bologna.ausl.internauta.service.exceptions.http.HttpInternautaResponseException;
import it.bologna.ausl.internauta.service.utils.NonCachedEntities;
import it.bologna.ausl.internauta.utils.masterjobs.MasterjobsObjectsFactory;
import it.bologna.ausl.internauta.utils.masterjobs.workers.jobs.MasterjobsJobsQueuer;
import it.bologna.ausl.model.entities.tip.SessioneImportazione;
import it.bologna.ausl.model.entities.tip.projections.generated.SessioneImportazioneWithPlainFields;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.servlet.http.HttpServletRequest;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.projection.ProjectionFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 *
 * @author gdm
 */
@RestController
@RequestMapping(value = "${tip.mapping.url.root}")
public class TipCustomController implements ControllerHandledExceptions {
    private static final Logger log = LoggerFactory.getLogger(TipCustomController.class);
    
    @PersistenceContext
    private EntityManager entityManager;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @Autowired
    private NonCachedEntities nonCachedEntities;
    
    @Autowired
    private ReporitoryConnectionManager reporitoryConnectionManager;
    
    @Autowired
    private TransactionTemplate transactionTemplate;
    
    @Autowired
    private ProjectionFactory projectionFactory;
    
    @Autowired
    private MasterjobsObjectsFactory masterjobsObjectsFactory;
    
    @Autowired
    private MasterjobsJobsQueuer masterjobsJobsQueuer;
    
    /**
     * importa il csv nella tabella importazioni_docimenti o importazioni_archivi a seconda della tipologia
     * @param request
     * @param idSessione l'id della sessione a cui attribuire le righe importate, se non passata ne verrà creata una nuova
     * @param idAzienda l'azienda da settare sulla sessione (in caso di sessione esistente sarà ignorato)
     * @param tipologia la tipologia dell'importazione della sessione (in caso di sessione esistente deve essere la stessa, altrimento viene tornato errore)
     * @param idStrutturaDefault la struttura di default da settare sulla sessione (in caso di sessione esistente sarà ignorato)
     * @param idArchivioDefault l'archivio di defautl da settare sulla sessione (in caso di sessione esistente sarà ignorato)
     * @param separatore il separatore delle colonne del csv
     * @param idVicarioDefault il vicario di default da settare sulla sessione (in caso di sessione esistente sarà ignorato)
     * @param csv il csv da importare
     * @return la sessione creata/usata
     * @throws HttpInternautaResponseException 
     */
    @RequestMapping(value = "uploadCSVPregressi", method = RequestMethod.POST)
    public ResponseEntity<?> uploadCSVPregressi (
            HttpServletRequest request,
            @RequestParam(name = "idSessione", required = false) Long idSessione,
            @RequestParam("idAzienda") Integer idAzienda,
            @RequestParam("tipologia") SessioneImportazione.TipologiaPregresso tipologia,
            @RequestParam("idStrutturaDefault") Integer idStrutturaDefault,
            @RequestParam("idArchivioDefault") Integer idArchivioDefault,
            @RequestParam("separatore") String separatore,
            @RequestParam("idVicarioDefault") Integer idVicarioDefault,
            @RequestParam("csv") MultipartFile csv) throws HttpInternautaResponseException {
        File csvFile = null;
        try {
            // creo il csv come file temporaneo e lo cancello al termine
            csvFile = File.createTempFile("uploadCSVPregressi_", ".csv");
            csvFile.deleteOnExit();
            try (FileOutputStream fos = new FileOutputStream(csvFile);) {
                try (InputStream csvIs = csv.getInputStream()) {
                    IOUtils.copy(csvIs, fos);
                }
            }
            // lancia l'importazione
            TipImportManager tipImportManager = new TipImportManager(entityManager, objectMapper, nonCachedEntities, reporitoryConnectionManager, transactionTemplate);
            SessioneImportazione sessioneImportazione = tipImportManager.csvImportAndValidate(idSessione, idAzienda, tipologia, idStrutturaDefault, idArchivioDefault, separatore, idVicarioDefault, csvFile);
            
            // torna la sessione creata/usata con la projection SessioneImportazioneWithPlainFields
            return ResponseEntity.ok(projectionFactory.createProjection(SessioneImportazioneWithPlainFields.class, sessioneImportazione));
        } catch (Exception ex) {
            if (HttpInternautaResponseException.class.isAssignableFrom(ex.getClass()))
                throw (HttpInternautaResponseException)ex;
            else {
                String errorMessage = "errore nella creazione del file csv temporaneo";
                log.error(errorMessage, ex);
                throw new Http500ResponseException("01", errorMessage, ex);
            }
        }
        finally {
            if (csvFile != null) {
                csvFile.delete();
            }
        }
    }
    
    @RequestMapping(value = "transferDocumentiPregressi", method = RequestMethod.GET)
    public void transferDocumentiPregressi (
        HttpServletRequest request,
        @RequestParam(name = "idSessione", required = true) Long idSessione) {
        
        TipTransferManager tipTransferManager = new TipTransferManager(
                entityManager, objectMapper, nonCachedEntities, reporitoryConnectionManager, 
                transactionTemplate, masterjobsObjectsFactory, masterjobsJobsQueuer);
        tipTransferManager.transferSessioneDocumento(idSessione);
    }
    
    @RequestMapping(value = "validateSessione", method = RequestMethod.GET)
    public ResponseEntity<?> validateSessione (
        HttpServletRequest request,
        @RequestParam(name = "idSessione", required = true) Long idSessione) throws HttpInternautaResponseException {
        
        TipImportManager tipImportManager = new TipImportManager(entityManager, objectMapper, nonCachedEntities, reporitoryConnectionManager, transactionTemplate);
        SessioneImportazione sessioneImportazione = tipImportManager.validateSessione(idSessione);
        
        // torna la sessione creata/usata con la projection SessioneImportazioneWithPlainFields
        return ResponseEntity.ok(projectionFactory.createProjection(SessioneImportazioneWithPlainFields.class, sessioneImportazione)); 
    }
    @RequestMapping(value = "csvError", method = RequestMethod.GET)
    public ResponseEntity<?> csvError (
        HttpServletRequest request,
        @RequestParam(name = "idSessione", required = true) Long idSessione) throws HttpInternautaResponseException {
        TipImportManager tipImportManager = new TipImportManager(entityManager, objectMapper, nonCachedEntities, reporitoryConnectionManager, transactionTemplate);
        SessioneImportazione sessioneImportazione = tipImportManager.validateSessione(idSessione);
         return ResponseEntity.ok(projectionFactory.createProjection(SessioneImportazioneWithPlainFields.class, sessioneImportazione)); 
    }
}
