package it.bologna.ausl.internauta.service.controllers.tip;

import it.bologna.ausl.internauta.service.controllers.tip.validations.TipDataValidator;
import it.bologna.ausl.internauta.service.exceptions.http.ControllerHandledExceptions;
import it.bologna.ausl.model.entities.tip.ImportazioneOggetto;
import it.bologna.ausl.model.entities.tip.SessioneImportazione;
import it.bologna.ausl.model.entities.tip.data.ColonneImportazioneOggetto;
import it.bologna.ausl.model.entities.tip.data.TipErroriImportazione;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.util.HashMap;
import java.util.Map;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.servlet.http.HttpServletRequest;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.csv.QuoteMode;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.http.ResponseEntity;
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
    
    @RequestMapping(value = "uploadCSVPregressi ", method = RequestMethod.POST)
    public ResponseEntity<?> uploadCSVPregressi (
            HttpServletRequest request,
            @RequestParam("idAzienda") Integer idAzienda,
            @RequestParam("tipologia") SessioneImportazione.TipologiaPregresso tipologia,
            @RequestParam("idStrutturaDefault") Integer idStrutturaDefault,
            @RequestParam("idArchivioDefault") Integer idArchivioDefault,
            @RequestParam("separatore") String separatore,
            @RequestParam("idVicarioDefault") Integer idVicarioDefault,
            @RequestParam("csv") MultipartFile csv) {
        File csvFile = null;
        try {
            try {
                csvFile = File.createTempFile("uploadCSVPregressi_", ".csv");
                csvFile.deleteOnExit();
                try (FileOutputStream fos = new FileOutputStream(csvFile);) {
                    try (InputStream csvIs = csv.getInputStream()) {
                        IOUtils.copy(csvIs, fos);
                    }
                }
            } catch (Throwable ex) {
                String errorMessage = "errore nella creazione del file csv temporaneo";
                log.error(errorMessage, ex);
            }
            validateCsv(tipologia, csvFile, separatore);
        } catch (Throwable ex) {
            log.error("errore nel caricamento del csv");
            
        }
        finally {
            if (csvFile != null) {
                csvFile.delete();
            }
        }
        return null;
    }
    
    private void validateCsv(SessioneImportazione.TipologiaPregresso tipologia, File csvFile, String separatore) throws FileNotFoundException, IOException {
        TipDataValidator tipDataValidator = TipDataValidator.getTipDataValidator(tipologia);
        try (
                Reader csvReader = new FileReader(csvFile);
                CSVParser csvParser = new CSVParser(csvReader,  CSVFormat.DEFAULT.builder()
                    .setDelimiter(separatore)
                    .setQuote('"')
                    .setQuoteMode(QuoteMode.MINIMAL)
                    .setRecordSeparator("\r\n")
                    .setHeader().build())
            ) {

            for (CSVRecord csvRecord : csvParser) {
                Map<String, String> csvRowMap = buildCsvRowMap(csvParser, csvRecord);
                ImportazioneOggetto importazioneOggettoRow = buildImportazioneOggettoRow(tipologia, csvRowMap);
                TipErroriImportazione error = tipDataValidator.validate(importazioneOggettoRow);
                importazioneOggettoRow.setErrori(error);
                entityManager.persist(importazioneOggettoRow);
            }
        }
    }
    
    private Map<String, String> buildCsvRowMap(CSVParser csvParser, CSVRecord csvRecord) {
        Map<String, String> res = new HashMap<>();
        for (Map.Entry<String, Integer> entry : csvParser.getHeaderMap().entrySet()) {
            String header = entry.getKey();
            int columnIndex = entry.getValue();
            String value = csvRecord.get(columnIndex);
            res.put(header, value);
        }
        return res;
    }
    
    private <T extends ImportazioneOggetto> T buildImportazioneOggettoRow(SessioneImportazione.TipologiaPregresso tipologia, Map<String, String> csvRowMap) {
        ImportazioneOggetto importazioneOggetto = ImportazioneOggetto.getImportazioneOggettoImpl(tipologia);
        BeanWrapper wrapper = new BeanWrapperImpl(importazioneOggetto);
        for (String headerName: csvRowMap.keySet()) {
            ColonneImportazioneOggetto colonnaEnum = ColonneImportazioneOggetto.findKey(headerName, tipologia);
            wrapper.setPropertyValue(colonnaEnum.toString(), csvRowMap.get(headerName));
        }
        return (T) wrapper.getWrappedInstance();
    }
}
