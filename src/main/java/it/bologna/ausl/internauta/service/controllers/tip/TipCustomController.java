package it.bologna.ausl.internauta.service.controllers.tip;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import it.bologna.ausl.internauta.service.configuration.utils.ReporitoryConnectionManager;
import it.bologna.ausl.internauta.service.exceptions.http.ControllerHandledExceptions;
import it.bologna.ausl.internauta.service.exceptions.http.Http500ResponseException;
import it.bologna.ausl.internauta.service.exceptions.http.HttpInternautaResponseException;
import it.bologna.ausl.internauta.service.repositories.tip.SessioneImportazioneRepository;
import it.bologna.ausl.internauta.service.utils.NonCachedEntities;
import it.bologna.ausl.internauta.utils.masterjobs.MasterjobsObjectsFactory;
import it.bologna.ausl.internauta.utils.masterjobs.workers.jobs.MasterjobsJobsQueuer;
import it.bologna.ausl.model.entities.tip.ImportazioneDocumento;
import it.bologna.ausl.model.entities.tip.QImportazioneDocumento;
import it.bologna.ausl.model.entities.tip.SessioneImportazione;
import it.bologna.ausl.model.entities.tip.data.ColonneImportazioneOggetto;
import it.bologna.ausl.model.entities.tip.projections.generated.SessioneImportazioneWithPlainFields;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.tomcat.util.http.fileupload.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.projection.ProjectionFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.StreamUtils;
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

    @Autowired
    private SessioneImportazioneRepository sessioneImportazioneRepository;

    /**
     * importa il csv nella tabella importazioni_docimenti o
     * importazioni_archivi a seconda della tipologia
     *
     * @param request
     * @param idSessione l'id della sessione a cui attribuire le righe
     * importate, se non passata ne verrà creata una nuova
     * @param idAzienda l'azienda da settare sulla sessione (in caso di sessione
     * esistente sarà ignorato)
     * @param tipologia la tipologia dell'importazione della sessione (in caso
     * di sessione esistente deve essere la stessa, altrimento viene tornato
     * errore)
     * @param idStrutturaDefault la struttura di default da settare sulla
     * sessione (in caso di sessione esistente sarà ignorato)
     * @param idArchivioDefault l'archivio di defautl da settare sulla sessione
     * (in caso di sessione esistente sarà ignorato)
     * @param separatore il separatore delle colonne del csv
     * @param idVicarioDefault il vicario di default da settare sulla sessione
     * (in caso di sessione esistente sarà ignorato)
     * @param csv il csv da importare
     * @return la sessione creata/usata
     * @throws HttpInternautaResponseException
     */
    @RequestMapping(value = "uploadCSVPregressi", method = RequestMethod.POST)
    public ResponseEntity<?> uploadCSVPregressi(
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
            if (HttpInternautaResponseException.class.isAssignableFrom(ex.getClass())) {
                throw (HttpInternautaResponseException) ex;
            } else {
                String errorMessage = "errore nella creazione del file csv temporaneo";
                log.error(errorMessage, ex);
                throw new Http500ResponseException("01", errorMessage, ex);
            }
        } finally {
            if (csvFile != null) {
                csvFile.delete();
            }
        }
    }

    @RequestMapping(value = "transferDocumentiPregressi", method = RequestMethod.GET)
    public void transferDocumentiPregressi(
            HttpServletRequest request,
            @RequestParam(name = "idSessione", required = true) Long idSessione) {

        TipTransferManager tipTransferManager = new TipTransferManager(
                entityManager, objectMapper, nonCachedEntities, reporitoryConnectionManager,
                transactionTemplate, masterjobsObjectsFactory, masterjobsJobsQueuer);
        tipTransferManager.transferSessioneDocumento(idSessione);
    }

    @RequestMapping(value = "validateSessione", method = RequestMethod.GET)
    public ResponseEntity<?> validateSessione(
            HttpServletRequest request,
            @RequestParam(name = "idSessione", required = true) Long idSessione) throws HttpInternautaResponseException {

        TipImportManager tipImportManager = new TipImportManager(entityManager, objectMapper, nonCachedEntities, reporitoryConnectionManager, transactionTemplate);
        SessioneImportazione sessioneImportazione = tipImportManager.validateSessione(idSessione);

        // torna la sessione creata/usata con la projection SessioneImportazioneWithPlainFields
        return ResponseEntity.ok(projectionFactory.createProjection(SessioneImportazioneWithPlainFields.class, sessioneImportazione));
    }

    @RequestMapping(value = "downloadCSVError", method = RequestMethod.GET)
    public void downloadCSVError(
            @RequestParam(name = "idSessione", required = true) Long idSessione,
            HttpServletResponse response,
            HttpServletRequest request) throws FileNotFoundException {

        File createTempFile = null;
        //gli do un nome a caso
        try {
            SessioneImportazione sessione = sessioneImportazioneRepository.findById(idSessione).get();
            SessioneImportazione.TipologiaPregresso tipologia = sessione.getTipologia();

            QImportazioneDocumento qImportazioneDocumento = QImportazioneDocumento.importazioneDocumento;
            JPAQueryFactory jPAQueryFactory = new JPAQueryFactory(entityManager);
            JPAQuery<ImportazioneDocumento> importazioneDocumenti = jPAQueryFactory
                    .select(qImportazioneDocumento)
                    .from(qImportazioneDocumento)
                    .where(qImportazioneDocumento.idSessioneImportazione.eq(sessione))
                    .fetchAll();

            createTempFile = File.createTempFile("TIP_error_", ".csv");
            createTempFile.deleteOnExit();
            try (FileWriter fileWriter = new FileWriter(createTempFile); CSVPrinter csvPrinter = new CSVPrinter(fileWriter, CSVFormat.DEFAULT.withDelimiter(';'))) {

                Class aEnum = ColonneImportazioneOggetto.getColumnsEnum(tipologia);
                Object[] enumConstants = aEnum.getEnumConstants();
                ColonneImportazioneOggetto[] colonne = (ColonneImportazioneOggetto[]) enumConstants;
                // Scrivo l'intestazione del CSV
                for (ColonneImportazioneOggetto key : colonne) {
                    String columnName = key.getValue().get(0).toString();
                    csvPrinter.print(columnName);
                }
                
                csvPrinter.println();
                // Scrivo i dati dei risultati
                for (Iterator<ImportazioneDocumento> importDoc = importazioneDocumenti.iterate(); importDoc.hasNext();) {
                    ImportazioneDocumento importazioneDocumento = importDoc.next();
                    csvPrinter.printRecord(
                            TipUtils.buildCsvRowFromImportazioneOggetto(
                                    colonne, 
                                    importazioneDocumento, 
                                    objectMapper));
//                    csvPrinter.println();

                }
            } catch (IOException ex) {
                // errore
                log.error("errore durante la creazione del csv", ex);
            }

            //devo aprirlo dalla temp come inputstream
            try (InputStream is = new FileInputStream(createTempFile)) {
                response.setHeader("Content-Type", "text/csv");
                StreamUtils.copy(is, response.getOutputStream());
            }
        } catch (IOException ex) {
            log.error("errore il ritorno del csv", ex);
        } finally {
            //devo eliminare il csv
            if (createTempFile != null && createTempFile.exists()) {
                createTempFile.delete();
            }

        }
    }
}
