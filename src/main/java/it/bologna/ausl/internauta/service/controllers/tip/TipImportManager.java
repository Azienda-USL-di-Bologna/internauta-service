package it.bologna.ausl.internauta.service.controllers.tip;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.PathBuilder;
import com.querydsl.jpa.impl.JPAQueryFactory;
import it.bologna.ausl.internauta.service.configuration.utils.ReporitoryConnectionManager;
import it.bologna.ausl.internauta.service.controllers.tip.validations.TipDataValidator;
import it.bologna.ausl.internauta.service.exceptions.http.Http400ResponseException;
import it.bologna.ausl.internauta.service.exceptions.http.Http500ResponseException;
import it.bologna.ausl.internauta.service.exceptions.http.HttpInternautaResponseException;
import it.bologna.ausl.internauta.service.utils.NonCachedEntities;
import it.bologna.ausl.minio.manager.MinIOWrapper;
import it.bologna.ausl.minio.manager.MinIOWrapperFileInfo;
import it.bologna.ausl.minio.manager.exceptions.MinIOWrapperException;
import it.bologna.ausl.model.entities.baborg.Azienda;
import it.bologna.ausl.model.entities.tip.ImportazioneDocumento;
import it.bologna.ausl.model.entities.tip.ImportazioneOggetto;
import it.bologna.ausl.model.entities.tip.QSessioneImportazione;
import it.bologna.ausl.model.entities.tip.SessioneImportazione;
import it.bologna.ausl.model.entities.tip.data.ColonneImportazioneOggetto;
import it.bologna.ausl.model.entities.tip.data.TipErroriImportazione;
import it.bologna.ausl.model.entities.versatore.QSessioneVersamento;
import it.bologna.ausl.model.entities.versatore.SessioneVersamento;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.Map;
import javax.persistence.EntityManager;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.csv.QuoteMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.StringUtils;

/**
 *
 * @author gdm
 * 
 * Questa classe gestisce l'importazione e il trasferimento dei pregressi.
 * Si occupa sia di importare le righe del csv nella tabella di mezzo (importazioni_documenti/importazioni_archivi) con le opportune validazioni iniziali,
 * sia di trasferire i dati dalla tabella di mezzo alle tabele effettive di scripta
 */
public class TipImportManager {
    private static final Logger log = LoggerFactory.getLogger(TipCustomController.class);
    
    private final EntityManager entityManager;
    
    private final ObjectMapper objectMapper;
    
    private final NonCachedEntities nonCachedEntities;
    
    private final ReporitoryConnectionManager reporitoryConnectionManager;
    
    private final TransactionTemplate transactionTemplate;

    public TipImportManager(EntityManager entityManager, ObjectMapper objectMapper, NonCachedEntities nonCachedEntities, ReporitoryConnectionManager reporitoryConnectionManager, TransactionTemplate transactionTemplate) {
        this.entityManager = entityManager;
        this.objectMapper = objectMapper;
        this.nonCachedEntities = nonCachedEntities;
        this.reporitoryConnectionManager = reporitoryConnectionManager;
        this.transactionTemplate = transactionTemplate;
    }
    
    /**
     * esegue l'importazione e la validazione nella tabella di mezzo (importazioni_documenti o importazioni_archivi a seconda che si stia eseguendo un'importazione di documenti o archivi).
     * ogni riga del csv viene letta, validata e salvata sulla tabella.
     * Il commit avviene ad ogni riga.
     * La validazione produce un json che viene inserito nella campo errori. Inoltra a seconda della validazione ogni riga avrà uno stato che ne indicherà la possibilità di essere trasferita o meno
     * Crea sempre una nuova sessione
     * 
     * @param idAzienda
     * @param tipologia
     * @param idStrutturaDefault
     * @param idArchivioDefault
     * @param separatore
     * @param idVicarioDefault
     * @param csv
     * @throws HttpInternautaResponseException 
     */
    public void csvImportAndValidate(
            Integer idAzienda, 
            SessioneImportazione.TipologiaPregresso tipologia, 
            Integer idStrutturaDefault, 
            Integer idArchivioDefault, 
            String separatore,
            Integer idVicarioDefault,
            File csv) throws HttpInternautaResponseException {
        
        csvImportAndValidate(null, idAzienda, tipologia, idStrutturaDefault, idArchivioDefault, separatore, idVicarioDefault, csv);
     }
    
    /**
     * esegue l'importazione e la validazione nella tabella di mezzo (importazioni_documenti o importazioni_archivi a seconda che si stia eseguendo un'importazione di documenti o archivi).ogni riga del csv viene letta, validata e salvata sulla tabella.
     * Il commit avviene ad ogni riga.
 La validazione produce un json che viene inserito nella campo errori. Inoltra a seconda della validazione ogni riga avrà uno stato che ne indicherà la possibilità di essere trasferita o meno
 E' possibile creare una nuova sessione di importazione, oppure aggiungere righe a una sessione già esistente.
  Se viene passato un idSessione, le righe vengono aggiunte alla sessione indicata, altrimenti, se viene passato null, verrà creata una nuova sessione.
     * 
     * @param idSessione la sessione a cui aggiungere le righe da importare, se null ne viene creata una nuova
     * @param idAzienda l'azienda per cui si sta eseguendo l'importazione, la sessione apparterrà all'azienda passata
     * @param tipologia la tipologia dell'importazione che si sta eseguendo
     * @param idStrutturaDefault la struttura di default da assegnare alla sessione (solo per nuove sessioni)
     * @param idArchivioDefault l'archivio di default da assegnare alla sessione (solo per nuove sessioni)
     * @param separatore il separatore delle colonne del csv
     * @param idVicarioDefault il vicario di default da assegnare alla sessione (solo per nuove sessioni)
     * @param csv il csv da importare
     * @return la sessione creata/usata
     * @throws HttpInternautaResponseException 
     */
    public SessioneImportazione csvImportAndValidate(
            Long idSessione,
            Integer idAzienda, 
            SessioneImportazione.TipologiaPregresso tipologia, 
            Integer idStrutturaDefault, 
            Integer idArchivioDefault, 
            String separatore,
            Integer idVicarioDefault, 
            File csv) throws HttpInternautaResponseException {
        ZonedDateTime now = ZonedDateTime.now();
        SessioneImportazione sessioneImportazione;
        try {
            // non si può usare l'azienda cached, perché deve essere attacata all'entityManager per poter essere inserita come foreignKey della sessione
            Azienda azienda = nonCachedEntities.getAzienda(idAzienda);
            
            if (idSessione != null) {
                // se mi è stato passato l'idSessione la carico dal DB e ne aggiorno il version, se non la trovo torno errore 400
                sessioneImportazione =  entityManager.find(SessioneImportazione.class, idSessione);
                if (sessioneImportazione != null) { // se trovo la sessione devo usare per forza la stessa tipologia, altrimenti torno errore
                    if (tipologia != sessioneImportazione.getTipologia()) {
                        String errorMessage =String.format(
                                "non è possibili importare tipologie diverse all'interno della stessa sessione. Tipologia sessione %s, tipologia passata %s", 
                                sessioneImportazione.getTipologia(), tipologia);
                        log.error(errorMessage);
                        throw new Http400ResponseException("04", errorMessage);
                    } else {
                        sessioneImportazione.setVersion(now);
                    }
                }
                else {
                    String errorMessage = String.format("sessione con %s non trovata", idSessione);
                    log.error(errorMessage);
                    throw new Http400ResponseException("03", errorMessage);
                }
            } else { // se non mi viene passato l'idSessione, ne creo una nuova
                sessioneImportazione = new SessioneImportazione(
                    tipologia,
                    "Importazione_" + now.toString(),
                    azienda,
                    nonCachedEntities.getStruttura(idStrutturaDefault), 
                    nonCachedEntities.getArchivio(idArchivioDefault), 
                    nonCachedEntities.getPersona(idVicarioDefault));
            }
            // valida tutte le righe e le importa
            validateAndImport(sessioneImportazione, tipologia, csv, csv.getName(), separatore, azienda.getCodice());
        } catch (MinIOWrapperException | IOException | HttpInternautaResponseException ex) {
            // se becco un errore http allora lo rilancio così com'è per poterlo tornare al chiamante
            if (HttpInternautaResponseException.class.isAssignableFrom(ex.getClass()))
                throw (HttpInternautaResponseException)ex;
            else { // altrimenti lancio un generico internal server error
                String errorMessage = "errore nel caricamento del csv";
                log.error(errorMessage, ex);
                throw new Http500ResponseException("01", errorMessage, ex);
            }
        }
        return sessioneImportazione;
    }
    
    public void transferInScripta(Long idSessione) {
        JPAQueryFactory queryFactory = new JPAQueryFactory(entityManager);
        QSessioneImportazione qSessioneImportazione = QSessioneImportazione.sessioneImportazione;
        SessioneImportazione sessioneImportazione = transactionTemplate.execute( a -> {
            return queryFactory
                .select(qSessioneImportazione)
                .from(qSessioneImportazione)
                .where(qSessioneImportazione.id.eq(idSessione))
                .fetchOne();
        });
        switch (sessioneImportazione.getTipologia()) {
            case DELIBERA:
            case DETERMINA:
            case PROTOCOLLO_IN_ENTRATA:
            case PROTOCOLLO_IN_USCITA:
                
                break;
            default:
                throw new AssertionError();
        }
        
    }
    
    /**
     * Per ogni riga del csv esegue la validazione e la scrittura sul DB.
     * il csv viene anche salvato sul repository (minIO)
     * NB: salvo errori di parsing, tutte le righe, tranne quelle già importate nella sessione, vengono salvate. 
     * In ogni riga poi saranno indicati gli eventuiali errori di importazione nella colonna "errori" e il suo stato nella colonna "stato"
     * @param sessioneImportazione la sessione da usare
     * @param tipologia la tipologia di importazione
     * @param csvFile il file csv da cui leggere le righe da importare
     * @param csvOriginalFileName il nome con cui nominare il file sul repository (minIO)
     * @param separatore il separatore delle colonne del csv
     * @param codiceAzienda il codice anzieda dell'azienda della sessione (serve per il caricamento del csv sul repository)
     * @throws FileNotFoundException
     * @throws IOException
     * @throws MinIOWrapperException 
     */
    private void validateAndImport(SessioneImportazione sessioneImportazione, SessioneImportazione.TipologiaPregresso tipologia, File csvFile, String csvOriginalFileName, String separatore, String codiceAzienda) throws FileNotFoundException, IOException, MinIOWrapperException {
        TipDataValidator tipDataValidator = TipDataValidator.getTipDataValidator(tipologia);
        try (
                Reader csvReader = new FileReader(csvFile);
                CSVParser csvParser = new CSVParser(csvReader,  CSVFormat.DEFAULT.builder()
                    .setDelimiter(separatore)
                    .setQuote('"')
                    .setQuoteMode(QuoteMode.MINIMAL)
                    .setRecordSeparator("\r\n")
                    .setAllowMissingColumnNames(true)
                    .setHeader().build())
            ) {

            // come prima cosa salva la sessione
            transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRED);
            transactionTemplate.executeWithoutResult(a -> {
                entityManager.persist(sessioneImportazione);
            });
            
            // carica il csv sul repository (minIO), il suo id sarà poi scritto su ogni riga della tabella di importazione
            MinIOWrapper minIOWrapper = reporitoryConnectionManager.getMinIOWrapper();
            String minIOTipPath = "/tip/csv";
            MinIOWrapperFileInfo csvFileInfo = minIOWrapper.put(csvFile, codiceAzienda, minIOTipPath, sessioneImportazione.getId() + "_" + csvOriginalFileName, null, true);
            
            // clicla su tutte le righe del csv
            for (CSVRecord csvRecord : csvParser) {
                // crea una mappa che ha come chiavi i vari header la riga del csv e come valori i rispettivi valori
                Map<String, String> csvRowMap = buildCsvRowMap(csvParser, csvRecord);
                
                // la riga della tabella di importazione (importazioni_documenti/importazione_archivi a seconda della tipologia)
                ImportazioneOggetto importazioneOggettoRow = buildImportazioneOggettoRow(tipologia, csvRowMap);
                
                log.info(String.format("importo riga con registro %s, numero %s, anno %s per la validazione...", 
                            importazioneOggettoRow.getRegistro(),
                            importazioneOggettoRow.getNumero(),
                            importazioneOggettoRow.getAnno()));
                
                // valida la riga e setta nel campo errori il risultato della validazione
                TipErroriImportazione error = tipDataValidator.validate(importazioneOggettoRow);
                // calcola lo stato di validazione
                ImportazioneDocumento.StatiImportazioneDocumento statoValidazione = error.getStatoValidazione();
                importazioneOggettoRow.setStato(statoValidazione);
                // scrive anche l'id del file sul repository
                importazioneOggettoRow.setIdRepoCsv(csvFileInfo.getFileId());
                
                // se la riga non è già stata importata in questa sessione, la scrive sul DB
                if (!isAlreadyInSession(sessioneImportazione, importazioneOggettoRow)) {
                    transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRED);
                    transactionTemplate.executeWithoutResult(a -> {
                        importazioneOggettoRow.setIdSessioneImportazione(sessioneImportazione);
                        entityManager.persist(importazioneOggettoRow);
                    });
                    log.info("riga importata");
                } else {
                    log.warn(String.format("riga già importata per la validazione", 
                            importazioneOggettoRow.getRegistro(),
                            importazioneOggettoRow.getNumero(),
                            importazioneOggettoRow.getAnno())
                    );
                }
            }
        }
    }
    
    /**
     * crea una mappa che ha come chiavi gli header del csv e come valori i rispettivi valori
     * @param csvParser
     * @param csvRecord
     * @return 
     */
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
    
    /**
     * Crea la riga della tabella di importazione (importazioni_documenti/importazione_archivi a seconda della tipologia)
     * @param <T>
     * @param tipologia la tipolia dell'importazione
     * @param csvRowMap la mappa rappresentante la riga del csv
     * @return un oggetto  di tipo ImportazioniDocumento o ImportazioniArchivio a seconda della tipologia (entrambi implementando l'interfaccia ImportazioneOggetto)
     */
    private <T extends ImportazioneOggetto> T buildImportazioneOggettoRow(SessioneImportazione.TipologiaPregresso tipologia, Map<String, String> csvRowMap) {
        // istanzia la corretta classe a seconda della tipologia
        ImportazioneOggetto importazioneOggetto = ImportazioneOggetto.getImportazioneOggettoImpl(tipologia);
        
        // questo oggetto permette di settare il valore di un campo, conoscendone il nome e il valore (senza dover chiamare direttaemente la funzione setter)
        BeanWrapper wrapper = new BeanWrapperImpl(importazioneOggetto);
        
        /* 
        per ogni header bisogna capire in che campo della classe ImportazioneOggetto scriverlo. Per farlo viene usato un enum che ha come chiave il nome del campo
        della classe e come valori i possibili nomi degli header associati
        */
        for (String headerName: csvRowMap.keySet()) {
            // reperisce il valore enum corretto a seconda dell'header
            ColonneImportazioneOggetto colonnaEnum = ColonneImportazioneOggetto.findKey(headerName, tipologia);
            if (colonnaEnum != null) {
                // se trovo il campo, ne setto il valore tramite il wrapper istanziato prima
                wrapper.setPropertyValue(colonnaEnum.toString(), csvRowMap.get(headerName));
            } else { // se non lo trovo, stampo un errore e lo ignoro
                log.error(String.format("header csv %s non previsto dal tracciato, il campo sarà ignorato", headerName));
            }
        }
        return (T) wrapper.getWrappedInstance();
    }
    
    /**
     * controlla se la riga da importare esiste già nella sessione
     * la chiave della riga è registro(se è presente nel csv)/numero/anno/sessione
     * @param sessioneImportazione
     * @param importazioneOggetto
     * @return true se la riga esiste già, false altrimenti
     */
    private boolean isAlreadyInSession(SessioneImportazione sessioneImportazione, ImportazioneOggetto importazioneOggetto) {
        return transactionTemplate.execute(a -> {
            JPAQueryFactory queryFactory = new JPAQueryFactory(entityManager);
            PathBuilder qImportazioneOggetto= new PathBuilder(importazioneOggetto.getClass(), importazioneOggetto.getClass().getSimpleName());
            BooleanExpression filter = 
                    qImportazioneOggetto.getString("numero").eq(importazioneOggetto.getNumero()).and(
                    qImportazioneOggetto.getString("anno").eq(importazioneOggetto.getAnno()));
            if (StringUtils.hasText(importazioneOggetto.getRegistro())) {
                filter = filter.and(qImportazioneOggetto.getString("registro").eq(importazioneOggetto.getRegistro()));
            }
            if (sessioneImportazione.getId() != null) {
                filter = filter.and(
                     qImportazioneOggetto.get("idSessioneImportazione").getNumber("id", Long.class).eq(sessioneImportazione.getId())
                );
            }
            
            boolean found = 
                    queryFactory
                        .selectOne()
                        .from(qImportazioneOggetto)
                        .where(filter)
                        .fetchFirst() != null;
            return found;
        });   
    }
}
