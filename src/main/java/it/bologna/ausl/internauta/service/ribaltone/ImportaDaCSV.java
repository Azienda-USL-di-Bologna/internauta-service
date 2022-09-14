package it.bologna.ausl.internauta.service.ribaltone;

import com.querydsl.core.types.dsl.BooleanExpression;
import it.bologna.ausl.blackbox.utils.UtilityFunctions;
import it.bologna.ausl.internauta.service.configuration.utils.ReporitoryConnectionManager;
import it.bologna.ausl.internauta.service.exceptions.ribaltonecsv.BaborgCSVAnomaliaException;
import it.bologna.ausl.internauta.service.exceptions.ribaltonecsv.BaborgCSVBloccanteException;
import it.bologna.ausl.internauta.service.exceptions.ribaltonecsv.BaborgCSVBloccanteRigheException;
import it.bologna.ausl.internauta.service.exceptions.ribaltonecsv.RibaltoneCSVCheckException;
import it.bologna.ausl.internauta.service.repositories.baborg.AziendaRepository;
import it.bologna.ausl.internauta.service.repositories.baborg.ImportazioniOrganigrammaRepository;
import it.bologna.ausl.internauta.service.repositories.baborg.PersonaRepository;
import it.bologna.ausl.internauta.service.repositories.baborg.UtenteRepository;
import it.bologna.ausl.internauta.service.repositories.gru.MdrAnagraficaRepository;
import it.bologna.ausl.internauta.service.repositories.gru.MdrAppartenentiRepository;
import it.bologna.ausl.internauta.service.repositories.gru.MdrResponsabiliRepository;
import it.bologna.ausl.internauta.service.repositories.gru.MdrStrutturaRepository;
import it.bologna.ausl.internauta.service.repositories.gru.MdrStrutturaRepositoryCustomImpl;
import it.bologna.ausl.internauta.service.repositories.gru.MdrTrasformazioniRepository;
import it.bologna.ausl.internauta.utils.parameters.manager.ParametriAziendeReader;
import it.bologna.ausl.model.entities.baborg.Azienda;
import it.bologna.ausl.model.entities.configurazione.Applicazione;
import it.bologna.ausl.model.entities.configurazione.ParametroAziende;
import it.bologna.ausl.model.entities.gru.MdrAnagrafica;
import it.bologna.ausl.model.entities.gru.MdrAppartenenti;
import it.bologna.ausl.model.entities.gru.MdrResponsabili;
import it.bologna.ausl.model.entities.gru.MdrStruttura;
import it.bologna.ausl.model.entities.gru.MdrTrasformazioni;
import it.bologna.ausl.model.entities.gru.QMdrAnagrafica;
import it.bologna.ausl.model.entities.gru.QMdrAppartenenti;
import it.bologna.ausl.model.entities.gru.QMdrResponsabili;
import it.bologna.ausl.model.entities.gru.QMdrStruttura;
import it.bologna.ausl.model.entities.gru.QMdrTrasformazioni;
import it.bologna.ausl.mongowrapper.MongoWrapper;
import it.bologna.ausl.mongowrapper.exceptions.MongoWrapperException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.supercsv.cellprocessor.Optional;
import org.supercsv.cellprocessor.constraint.StrRegEx;
import org.supercsv.cellprocessor.ift.CellProcessor;
import org.supercsv.io.CsvMapReader;
import org.supercsv.io.CsvMapWriter;
import org.supercsv.io.ICsvMapReader;
import org.supercsv.io.ICsvMapWriter;
import org.supercsv.prefs.CsvPreference;

/**
 *
 * @author mdonza
 */
@Component
public class ImportaDaCSV {

    private static final Logger log = LoggerFactory.getLogger(ImportaDaCSV.class);
    private static Map<String, Integer> map;

    @Autowired
    private MdrStrutturaRepositoryCustomImpl mdrStrutturaRepositoryCustomImpl;

    @Autowired
    private ImportazioniOrganigrammaRepository importazioniOrganigrammaRepository;

    @PersistenceContext
    private EntityManager em;

    @Autowired
    private AziendaRepository aziendaRepository;

    @Autowired
    private MdrTrasformazioniRepository mdrTrasformazioniRepository;

    @Autowired
    private MdrAppartenentiRepository mdrAppartenentiRepository;

    @Autowired
    private MdrAnagraficaRepository mdrAnagraficaRepository;

    @Autowired
    private MdrResponsabiliRepository mdrResponsabiliRepository;

    @Autowired
    private MdrStrutturaRepository mdrStrutturaRepository;

    @Autowired
    private UtenteRepository utenteRepository;

    @Autowired
    private PersonaRepository personaRepository;

    @Autowired
    private ReporitoryConnectionManager mongoConnectionManager;

    @Autowired
    private ParametriAziendeReader parametriAziende;

    private static String[] headersGenerator(String tipo) {
        String[] headers = null;
        switch (tipo) {
            case "APPARTENENTI":
                headers = new String[]{"codice_ente", "codice_matricola", "cognome",
                    "nome", "codice_fiscale", "id_casella", "datain", "datafi", "tipo_appartenenza",
                    "username", "data_assunzione", "data_dimissione"};
                break;
            case "ANAGRAFICA":
                headers = new String[]{"codice_ente", "codice_matricola", "cognome",
                    "nome", "codice_fiscale", "email"};
                break;
            case "RESPONSABILI":
                headers = new String[]{"codice_ente", "codice_matricola",
                    "id_casella", "datain", "datafi", "tipo"};
                break;
            case "STRUTTURA":
                headers = new String[]{"id_casella", "id_padre", "descrizione",
                    "datain", "datafi", "tipo_legame", "codice_ente"};
                break;
            case "TRASFORMAZIONI":
                headers = new String[]{"progressivo_riga", "id_casella_partenza", "id_casella_arrivo", "data_trasformazione",
                    "motivo", "datain_partenza", "dataora_oper", "codice_ente"};
                break;
            default:
                System.out.println("non dovrebbe essere");
                break;
        }
        return headers;
    }

    private static CellProcessor[] getProcessors(String tipo) {
        CellProcessor[] cellProcessor = null;

        switch (tipo) {
            case "APPARTENENTI":
                final CellProcessor[] processorsAPPARTENENTI = new CellProcessor[]{
                    // new NotNull(new StrRegEx(codiceEnteRegex, new ParseInt())), // codice_ente
                    new Optional(), // codice_ente
                    new Optional(), // codice_matricola Non Bloccante
                    new Optional(), // cognome Bloccante
                    new Optional(), // nome Bloccante
                    new Optional(), // codice_fiscale bloccante
                    new Optional(), // id_casella bloccante
                    new Optional(), // datain bloccante
                    new Optional(), // datafi
                    new Optional(), // tipo_appartenenza bloccante
                    new Optional(), // username
                    new Optional(), // data_assunzione bloccante
                    new Optional() // data_adimissione
                };
                cellProcessor = processorsAPPARTENENTI;
                break;
            case "ANAGRAFICA":
                final CellProcessor[] processorsANAGRAFICA = new CellProcessor[]{
                    // new NotNull(new StrRegEx(codiceEnteRegex, new ParseInt())), // codice_ente
                    new Optional(), // codice_ente
                    new Optional(), // codice_matricola Non Bloccante
                    new Optional(), // cognome Bloccante
                    new Optional(), // nome Bloccante
                    new Optional(), // codice_fiscale bloccante
                    new Optional(), // EMAIL bloccante
                };
                cellProcessor = processorsANAGRAFICA;
                break;
            case "RESPONSABILI":
                final CellProcessor[] processorsRESPONSABILI = new CellProcessor[]{
                    // new NotNull(new StrRegEx(codiceEnteRegex, new ParseInt())), // codice_ente
                    new Optional(), // codice_ente
                    new Optional(), // codice_matricola bloccante
                    new Optional(), // id_casella bloccante
                    new Optional(), // datain bloccante
                    new Optional(), // datafi
                    new Optional() // tipo bloccante
                };
                cellProcessor = processorsRESPONSABILI;
                break;
            case "TRASFORMAZIONI":
                CellProcessor[] processorsTRASFORMAZIONI = new CellProcessor[]{
                    new Optional(), // progressivo_riga
                    new Optional(), // id_casella_partenza
                    new Optional(), // id_casellla_arrivo
                    new Optional(), // data_trasformazione
                    new Optional(), // motivo
                    new Optional(), // datain_partenza
                    new Optional(), // dataora_oper
                    new Optional() // codice_ente
                };
                cellProcessor = processorsTRASFORMAZIONI;
                break;

            case "STRUTTURA":
                final CellProcessor[] processorsSTRUTTURA = new CellProcessor[]{
                    new Optional(), // id_casella
                    new Optional(), // id_padre
                    new Optional(), // descrizione
                    new Optional(), // datain
                    new Optional(), // datafi
                    new Optional(), // tipo_legame
                    // new NotNull(new StrRegEx(codiceEnteRegex, new ParseInt())), // codice_ente
                    new Optional() // codice_ente
                };
                cellProcessor = processorsSTRUTTURA;
                break;
            default:
                System.out.println("non dovrebbe essere altro tipo di tabella");
                break;
        }
        return cellProcessor;
    }

    private static String[] headersErrorGenerator(String tipo) {
        String[] headers = null;
        switch (tipo) {
            case "APPARTENENTI":
                headers = new String[]{"codice_ente", "codice_matricola", "cognome",
                    "nome", "codice_fiscale", "id_casella", "datain", "datafi", "tipo_appartenenza",
                    "username", "data_assunzione", "data_dimissione", "ERRORE"};
                break;
            case "ANAGRAFICA":
                headers = new String[]{"codice_ente", "codice_matricola", "cognome",
                    "nome", "codice_fiscale", "email", "ERRORE"};
                break;
            case "RESPONSABILI":
                headers = new String[]{"codice_ente", "codice_matricola",
                    "id_casella", "datain", "datafi", "tipo", "ERRORE"};
                break;
            case "STRUTTURA":
                headers = new String[]{"id_casella", "id_padre", "descrizione",
                    "datain", "datafi", "tipo_legame", "codice_ente", "ERRORE"};
                break;
            case "TRASFORMAZIONI":
                headers = new String[]{"progressivo_riga", "id_casella_partenza", "id_casella_arrivo", "data_trasformazione",
                    "motivo", "datain_partenza", "dataora_oper", "codice_ente", "ERRORE"};
                break;
            default:
                System.out.println("non dovrebbe essere");
                break;
        }
        return headers;
    }

    private static CellProcessor[] getProcessorsError(String tipo, String codiceAzienda) {
        CellProcessor[] cellProcessor = null;

        final String codiceEnteRegex = "^(" + codiceAzienda + ")[0-9]*";
        StrRegEx.registerMessage(codiceEnteRegex, "must be a valid codice ente");

        switch (tipo) {
            case "APPARTENENTI":
                final CellProcessor[] processorsAPPARTENENTI = new CellProcessor[]{
                    // new NotNull(new StrRegEx(codiceEnteRegex, new ParseInt())), // codice_ente
                    new Optional(), // codice_ente
                    new Optional(), // codice_matricola Non Bloccante
                    new Optional(), // cognome Bloccante
                    new Optional(), // nome Bloccante
                    new Optional(), // codice_fiscale bloccante
                    new Optional(), // id_casella bloccante
                    new Optional(), // datain bloccante
                    new Optional(), // datafi
                    new Optional(), // tipo_appartenenza bloccante
                    new Optional(), // username
                    new Optional(), // data_assunzione bloccante
                    new Optional(), // data_adimissione
                    new Optional() // errore
                };
                cellProcessor = processorsAPPARTENENTI;
                break;
            case "ANAGRAFICA":
                final CellProcessor[] processorsANAGRAFICA = new CellProcessor[]{
                    // new NotNull(new StrRegEx(codiceEnteRegex, new ParseInt())), // codice_ente
                    new Optional(), // codice_ente
                    new Optional(), // codice_matricola Non Bloccante
                    new Optional(), // cognome Bloccante
                    new Optional(), // nome Bloccante
                    new Optional(), // codice_fiscale bloccante
                    new Optional(), // EMAIL bloccante
                    new Optional(), // ERRORE
                };
                cellProcessor = processorsANAGRAFICA;
                break;
            case "RESPONSABILI":
                final CellProcessor[] processorsRESPONSABILI = new CellProcessor[]{
                    // new NotNull(new StrRegEx(codiceEnteRegex, new ParseInt())), // codice_ente
                    new Optional(), // codice_ente
                    new Optional(), // codice_matricola bloccante
                    new Optional(), // id_casella bloccante
                    new Optional(), // datain bloccante
                    new Optional(), // datafi
                    new Optional(), // tipo bloccante
                    new Optional() // errore
                };
                cellProcessor = processorsRESPONSABILI;
                break;
            case "TRASFORMAZIONI":
                CellProcessor[] processorsTRASFORMAZIONI = new CellProcessor[]{
                    new Optional(), // progressivo_riga
                    new Optional(), // id_casella_partenza
                    new Optional(), // id_casellla_arrivo
                    new Optional(), // data_trasformazione
                    new Optional(), // motivo
                    new Optional(), // datain_partenza
                    new Optional(), // dataora_oper
                    new Optional(), // codice_ente
                    new Optional() // errore
                };
                cellProcessor = processorsTRASFORMAZIONI;
                break;

            case "STRUTTURA":
                final CellProcessor[] processorsSTRUTTURA = new CellProcessor[]{
                    new Optional(), // id_casella
                    new Optional(), // id_padre
                    new Optional(), // descrizione
                    new Optional(), // datain
                    new Optional(), // datafi
                    new Optional(), // tipo_legame
                    // new NotNull(new StrRegEx(codiceEnteRegex, new ParseInt())), // codice_ente
                    new Optional(), // codice_ente
                    new Optional() // errore
                };
                cellProcessor = processorsSTRUTTURA;
                break;
            default:
                System.out.println("non dovrebbe essere altro tipo di tabella");
                break;
        }
        return cellProcessor;
    }

    /**
     *
     * @param file
     * @param tipo
     * @param codiceAzienda
     * @param idAzienda
     * @return uuid documento di mongo
     *
     * @throws
     * it.bologna.ausl.internauta.service.exceptions.ribaltonecsv.BaborgCSVBloccanteException
     * @throws
     * it.bologna.ausl.internauta.service.exceptions.ribaltonecsv.BaborgCSVAnomaliaException
     * @throws it.bologna.ausl.mongowrapper.exceptions.MongoWrapperException
     * @throws
     * it.bologna.ausl.internauta.service.exceptions.ribaltonecsv.BaborgCSVBloccanteRigheException
     */
    @Transactional(rollbackFor = Throwable.class, noRollbackFor = BaborgCSVAnomaliaException.class, propagation = Propagation.REQUIRES_NEW)
    public String csvTransactionalReadDeleteInsert(MultipartFile file, String tipo, String codiceAzienda, Integer idAzienda) throws BaborgCSVBloccanteException, BaborgCSVAnomaliaException, MongoWrapperException, BaborgCSVBloccanteRigheException {
        Timestamp timestamp = new Timestamp(System.currentTimeMillis());
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy_MM_dd-HH_mm_ss");
        String nameCsv = sdf.format(timestamp) + "_Error_" + tipo + ".csv";
        File csvErrorFile = new File(System.getProperty("java.io.tmpdir"), nameCsv);
        String nameCsv2 = sdf.format(timestamp) + "_Error2_" + tipo + ".csv";
        File csvErrorFile2 = new File(System.getProperty("java.io.tmpdir"), nameCsv2);

        String uuid = null;
        boolean bloccante = false;
        boolean anomalia = false;
        ICsvMapReader mapReader = null;
        ICsvMapReader mapErrorReader = null;
        ICsvMapWriter mapWriter = null;
        ICsvMapWriter mapErrorWriter = null;
        BooleanExpression predicateAzienda = null;
        Integer nRigheCSV = 0;
        Integer nRigheAnomale = 0;
        Integer tolleranza = 0;
        GestioneCodiciEnti gestioneCodiciEnti = null;
        Integer nRigheDB = 0;
        List<ParametroAziende> parameters;
        try {
            //        Reading with CsvMapReader
            //        Reading file with CsvMapReader

            InputStreamReader inputFileStreamReader = new InputStreamReader(file.getInputStream());
            CsvPreference SEMICOLON_DELIMITED = new CsvPreference.Builder('"', ';', "\r\n").build();
            mapReader = new CsvMapReader(inputFileStreamReader, SEMICOLON_DELIMITED);
            mapReader.getHeader(true);

            String[] headers = headersGenerator(tipo);
            CellProcessor[] processors = getProcessors(tipo);

            java.util.Optional<Azienda> optionalAzienda = aziendaRepository.findById(idAzienda);
            Azienda azienda = optionalAzienda.get();

            //preparo file di errore
            mapWriter = new CsvMapWriter(new FileWriter(csvErrorFile), SEMICOLON_DELIMITED);
            mapWriter.writeHeader(headersErrorGenerator(tipo));
            Boolean controlloZeroUno = false;
            parameters = parametriAziende.getParameters("controlloZeroUno", new Integer[]{idAzienda}, new String[]{Applicazione.Applicazioni.ribaltorg.toString()});
            if (parameters != null && !parameters.isEmpty()) {
                controlloZeroUno = parametriAziende.getValue(parameters.get(0), Boolean.class);
            }
            Map<String, Object> mapError = new HashMap<>();
            switch (tipo) {
                case "APPARTENENTI": {
                    parameters = parametriAziende.getParameters("tolleranzaAppartenenti", new Integer[]{idAzienda}, new String[]{Applicazione.Applicazioni.ribaltorg.toString()});
                    if (parameters != null && !parameters.isEmpty()) {
                        tolleranza = parametriAziende.getValue(parameters.get(0), Integer.class);
                    }
                    parameters = parametriAziende.getParameters("gestioneCodiciEnti", new Integer[]{idAzienda}, new String[]{Applicazione.Applicazioni.ribaltorg.toString()});
                    if (parameters != null && !parameters.isEmpty()) {
                        gestioneCodiciEnti = parametriAziende.getValue(parameters.get(0), GestioneCodiciEnti.class);
                    }

                    nRigheDB = mdrAppartenentiRepository.countRow(idAzienda);
                    nRigheCSV = 0;
                    nRigheAnomale = 0;
                    List<Map<String, Object>> listAppartenentiMap = new ArrayList<>();
                    Map<Integer, List<Map<String, Object>>> selectDateOnStruttureByIdAzienda = mdrStrutturaRepository.selectDateOnStruttureByIdAzienda(idAzienda);
                    //integer1 appartenenti, integer2 struttura, lista datain,datafi di appartenente in struttura.
                    Map<Integer, Map<Integer, List<Map<String, Object>>>> appartenentiDiretti = new HashMap<>();
                    //integer1 appartenenti, integer2 struttura, lista datain,datafi di appartenente in struttura.
                    Map<Integer, Map<Integer, List<Map<String, Object>>>> appartenentiFunzionali = new HashMap<>();
                    // Delete delle righe da sostituire
                    predicateAzienda = QMdrAppartenenti.mdrAppartenenti.idAzienda.id.eq(idAzienda);
                    mdrAppartenentiRepository.deleteByIdAzienda(idAzienda);
                    //Reading with CsvMapReader
                    Map<String, Object> appartenentiMap;
                    Integer riga;
                    List<Integer> righeAnomaleDirette = new ArrayList<>();
                    List<Integer> righeAnomaleFunzionali = new ArrayList<>();
                    anomalia = false;
                    while ((appartenentiMap = mapReader.read(headers, processors)) != null) {
                        boolean anomali;
                        mapError = new HashMap<>();
                        // Inserisco la riga
                        MdrAppartenenti mA = new MdrAppartenenti();
//                      preparo la mappa di errore
                        mapError.put("ERRORE", "");
                        mapError.put("Anomalia", "");

//                      CODICE_MATRICOLA bloccante
                        anomali = Appartenenti.checkCodiceMatricola(appartenentiMap, mapError);
                        anomalia = anomalia ? anomalia : anomali;

//                      COGNOME bloccante
                        anomali = Appartenenti.checkCognome(appartenentiMap, mapError);
                        anomalia = anomalia ? anomalia : anomali;

//                      NOME bloccante
                        anomali = Appartenenti.checkNome(appartenentiMap, mapError);
                        anomalia = anomalia ? anomalia : anomali;

//                      CODICE_FISCALE bloccante
                        anomali = Appartenenti.checkCodiceFiscale(appartenentiMap, mapError);
                        anomalia = anomalia ? anomalia : anomali;

                        String idCasella = Appartenenti.checkIdCasella(appartenentiMap, mapError, selectDateOnStruttureByIdAzienda);
                        anomalia = anomalia ? anomalia : idCasella.equals("");
                        if (appartenentiMap.get("id_casella") != null && appartenentiMap.get("id_casella") != "") {
                            if (!appartenentiMap.get("id_casella").toString().equals(idCasella)) {
                                idCasella = appartenentiMap.get("id_casella").toString();
                            }

                        }

//                      DATAIN bloccante
                        anomali = !ImportaDaCSVUtils.checkDatain(appartenentiMap, mapError, "A");
                        anomalia = anomalia ? anomalia : anomali;

                        ZonedDateTime datafi = null;
                        ZonedDateTime datain = null;
                        String datafiString = null;
                        String datainString = null;
                        //basta vedere anomali perche se ci sono problemi li ho gia controllati col checkDatainA
                        if (!anomali) {
                            datain = ImportaDaCSVUtils.formattattore(appartenentiMap.get("datain"));
                            datainString = UtilityFunctions.getZonedDateTimeString(datain);
                        }
                        if (appartenentiMap.get("datafi") != null && (!appartenentiMap.get("datafi").toString().trim().equals("") || appartenentiMap.get("datafi") != "")) {
                            datafi = ImportaDaCSVUtils.formattattore(appartenentiMap.get("datafi"));
                            datafiString = UtilityFunctions.getZonedDateTimeString(datafi);
                        }

                        if (appartenentiMap.get("datafi") == null || appartenentiMap.get("datafi").toString().trim().equals("") || appartenentiMap.get("datafi") == "") {
                            mapError.put("datafi", "");
                        } else {
                            mapError.put("datafi", appartenentiMap.get("datafi"));
                        }

                        if (ImportaDaCSVUtils.checkDateFinisconoDopoInizio(datain, datafi)) {
                            anomalia = true;
                            if (mapError.get("ERRORE") != null) {
                                mapError.put("ERRORE", mapError.get("ERRORE") + " questa riga non è valida perche la data di fine è precedente alla data di fine, ");
                            } else {
                                mapError.put("ERRORE", "questa riga non è valida perche la data di fine è precedente alla data di fine,");
                            }
                        }

                        //Codice Ente 
                        String codiceEnte = ImportaDaCSVUtils.checkCodiceEnte(appartenentiMap, mapError, codiceAzienda);
                        anomalia = anomalia ? anomalia : codiceEnte.equals("");
                        mA.setCodiceEnte(codiceEnte);

//                      TIPO_APPARTENENZA bloccante
                        anomali = Appartenenti.checkTipoAppatenenza(appartenentiMap, mapError, idCasella, datain, datafi, controlloZeroUno, appartenentiDiretti, appartenentiFunzionali, mapReader, righeAnomaleFunzionali, righeAnomaleDirette);
                        anomalia = anomalia ? anomalia : anomali;

//                      DataAssunzione bloccante
                        anomali = Appartenenti.checkDataAssunzione(appartenentiMap, mapError);
                        anomalia = anomalia ? anomalia : anomali;

//                      USERNAME lo copio
                        if (appartenentiMap.get("username") == null || appartenentiMap.get("username").toString().trim().equals("") || appartenentiMap.get("username") == "") {
                            mapError.put("username", "");
                        } else {
                            mapError.put("username", appartenentiMap.get("username"));
                        }

//                      DATA_DIMISSIONE
                        if (appartenentiMap.get("data_dimissione") == null || appartenentiMap.get("data_dimissione").toString().trim().equals("") || appartenentiMap.get("data_dimissione") == "") {
                            mapError.put("data_dimissione", "");
                        } else {
                            mapError.put("data_dimissione", appartenentiMap.get("data_dimissione"));
                        }

                        listAppartenentiMap.add(mapError);
                        nRigheCSV = mapReader.getRowNumber();
                    }

                    //se ho il caso in cui non ho appartenenti diretti per qualche appatenente funzionale
                    List<Integer> codiciMatricoleConAppFunzionaliENonDirette = Appartenenti.codiciMatricoleConAppFunzionaliENonDirette(appartenentiFunzionali, appartenentiDiretti);
                    riga = 2;

                    for (Map<String, Object> appMapWithErrorAndAnomalia : listAppartenentiMap) {
                        if (codiciMatricoleConAppFunzionaliENonDirette.contains(Integer.parseInt(appMapWithErrorAndAnomalia.get("codice_matricola").toString()))) {
                            appMapWithErrorAndAnomalia.put("ERRORE", appMapWithErrorAndAnomalia.get("ERRORE") + " appartenente con appartenenze funzionali ma senza appartenente dirette");
                            nRigheAnomale++;
                            anomalia = true;
                            appMapWithErrorAndAnomalia.put("Anomalia", "true");
                        }
                        if (righeAnomaleDirette.contains(riga)) {
                            boolean codiceEnteAndsWith = appMapWithErrorAndAnomalia.get("codice_ente").toString().endsWith("01");
                            if (controlloZeroUno && codiceEnteAndsWith) {
                                appMapWithErrorAndAnomalia.put("ERRORE", appMapWithErrorAndAnomalia.get("ERRORE") + " appartenente con piu afferenze Dirette per lo stesso periodo,");
                                nRigheAnomale++;
                                anomalia = true;
                                appMapWithErrorAndAnomalia.put("Anomalia", "true");
                            }
                        }

                        if (righeAnomaleFunzionali.contains(riga)) {
                            appMapWithErrorAndAnomalia.put("ERRORE", appMapWithErrorAndAnomalia.get("ERRORE") + " appartenente con piu afferenze funzionali per lo stesso periodo e nella stessa struttura");
                            nRigheAnomale++;
                            anomalia = true;
                            appMapWithErrorAndAnomalia.put("Anomalia", "true");
                        }
                        if (!appMapWithErrorAndAnomalia.get("Anomalia").toString().equalsIgnoreCase("true")) {
                            MdrAppartenenti mA = new MdrAppartenenti();
                            mA.setIdAzienda(azienda);
//                      "codice_ente",
                            mA.setCodiceEnte(!appMapWithErrorAndAnomalia.get("codice_ente").toString().equals("") ? appMapWithErrorAndAnomalia.get("codice_ente").toString() : null);
//                      "codice_matricola",
                            mA.setCodiceMatricola(!appMapWithErrorAndAnomalia.get("codice_matricola").toString().equals("") ? Integer.parseInt(appMapWithErrorAndAnomalia.get("codice_matricola").toString()) : null);
//                      "cognome",
                            mA.setCognome(!appMapWithErrorAndAnomalia.get("cognome").toString().equals("") ? appMapWithErrorAndAnomalia.get("cognome").toString() : null);
//                      "nome",
                            mA.setNome(!appMapWithErrorAndAnomalia.get("nome").toString().equals("") ? appMapWithErrorAndAnomalia.get("nome").toString() : null);
//                      "codice_fiscale",
                            mA.setCodiceFiscale(!appMapWithErrorAndAnomalia.get("codice_fiscale").toString().equals("") ? appMapWithErrorAndAnomalia.get("codice_fiscale").toString() : null);
//                      "id_casella",
                            mA.setIdCasella(!appMapWithErrorAndAnomalia.get("id_casella").toString().equals("") ? Integer.parseInt(appMapWithErrorAndAnomalia.get("id_casella").toString()) : null);
//                      "datain",
                            mA.setDatain(!appMapWithErrorAndAnomalia.get("datain").toString().equals("") ? ImportaDaCSVUtils.formattattore(appMapWithErrorAndAnomalia.get("datain")) : null);
//                      "datafi",
                            mA.setDatafi(!appMapWithErrorAndAnomalia.get("datafi").toString().equals("") ? ImportaDaCSVUtils.formattattore(appMapWithErrorAndAnomalia.get("datafi")) : null);
//                      "tipo_appartenenza",
                            mA.setTipoAppartenenza(!appMapWithErrorAndAnomalia.get("tipo_appartenenza").toString().equals("") ? appMapWithErrorAndAnomalia.get("tipo_appartenenza").toString() : null);
//                      "username",
                            mA.setUsername(!appMapWithErrorAndAnomalia.get("username").toString().equals("") ? appMapWithErrorAndAnomalia.get("username").toString() : null);
//                      "data_assunzione",
                            mA.setDataAssunzione(!appMapWithErrorAndAnomalia.get("data_assunzione").toString().equals("") ? ImportaDaCSVUtils.formattattore(appMapWithErrorAndAnomalia.get("data_assunzione")) : null);
//                      "data_dimissione"
                            mA.setDataDimissione(!appMapWithErrorAndAnomalia.get("data_dimissione").toString().equals("") ? ImportaDaCSVUtils.formattattore(appMapWithErrorAndAnomalia.get("data_dimissione")) : null);

                            em.persist(mA);
                        } else {
                            log.info("anomalia sulla riga: " + riga);
                            nRigheAnomale++;
                            anomalia = true;
                        }
                        appMapWithErrorAndAnomalia.remove("Anomalia");
                        mapWriter.write(appMapWithErrorAndAnomalia, headersErrorGenerator(tipo), getProcessorsError(tipo, codiceAzienda));
                        riga++;
                    }
                    log.info("ora fine: " + LocalDateTime.now());
                }
                break;
                case "ANAGRAFICA": {
                    parameters = parametriAziende.getParameters("tolleranzaAnagrafica", new Integer[]{idAzienda}, new String[]{Applicazione.Applicazioni.ribaltorg.toString()});
                    if (parameters != null && !parameters.isEmpty()) {
                        tolleranza = parametriAziende.getValue(parameters.get(0), Integer.class);
                    }
                    nRigheDB = mdrAnagraficaRepository.countRow(idAzienda);
                    nRigheCSV = 0;
                    nRigheAnomale = 0;
                    List<Map<String, Object>> listAnagraficaMap = new ArrayList<>();
                    // Delete delle righe da sostituire
                    predicateAzienda = QMdrAnagrafica.mdrAnagrafica.idAzienda.id.eq(idAzienda);
                    mdrAnagraficaRepository.deleteByIdAzienda(idAzienda);
                    //Reading with CsvMapReader
                    Map<String, Object> anagraficaMap;
                    Integer riga;
                    Boolean anomaliaRiga = false;
                    while ((anagraficaMap = mapReader.read(headers, processors)) != null) {
                        boolean anomali = false;
                        mapError = new HashMap<>();
                        riga = mapReader.getLineNumber();
                        log.info("getLineNumber: " + mapReader.getLineNumber());
                        // Inserisco la riga
                        MdrAnagrafica mAn = new MdrAnagrafica();
//                      preparo la mappa di errore
                        mapError.put("ERRORE", "");
                        mapError.put("Anomalia", "");

//                      CODICE_MATRICOLA bloccante
                        anomali = Appartenenti.checkCodiceMatricola(anagraficaMap, mapError);
                        anomaliaRiga = anomaliaRiga ? anomaliaRiga : anomali;
                        mAn.setCodiceMatricola(Integer.parseInt(mapError.get("codice_matricola").toString()));

                        if (anagraficaMap.get("email") != null && !anagraficaMap.get("email").toString().trim().equals("") && anagraficaMap.get("email") != "") {
                            mAn.setEmail(anagraficaMap.get("email").toString());
                            mapError.put("email", anagraficaMap.get("email"));
                        } else {
                            mAn.setEmail("");
                        }
//                      COGNOME bloccante
                        anomali = Appartenenti.checkCognome(anagraficaMap, mapError);
                        anomaliaRiga = anomaliaRiga ? anomaliaRiga : anomali;
                        mAn.setCognome(mapError.get("cognome").toString());
//                      NOME bloccante
                        anomali = Appartenenti.checkNome(anagraficaMap, mapError);
                        anomaliaRiga = anomaliaRiga ? anomaliaRiga : anomali;
                        mAn.setNome(mapError.get("nome").toString());

//                      CODICE_FISCALE bloccante
                        anomali = Appartenenti.checkCodiceFiscale(anagraficaMap, mapError);
                        anomaliaRiga = anomaliaRiga ? anomaliaRiga : anomali;
                        mAn.setCodiceFiscale(mapError.get("codice_fiscale").toString());

                        mAn.setIdAzienda(azienda);
                        //Codice Ente 
                        String codiceEnte = ImportaDaCSVUtils.checkCodiceEnte(anagraficaMap, mapError, codiceAzienda);
                        anomaliaRiga = anomaliaRiga ? anomaliaRiga : codiceEnte.equals("");
                        mAn.setCodiceEnte(codiceEnte);
                        if (anomaliaRiga) {
                            anomalia = true;
                            nRigheAnomale++;
                            log.error("errore alla riga " + mapReader.getLineNumber());
                        } else {
                            em.persist(mAn);

                        }
                        anomaliaRiga = false;
                        mapWriter.write(mapError, headersErrorGenerator(tipo), getProcessorsError(tipo, codiceAzienda));
                        nRigheCSV = mapReader.getRowNumber();
                    }
                    log.info("ora fine: " + LocalDateTime.now());
                }
                break;
                case "RESPONSABILI": {
                    parameters = parametriAziende.getParameters("tolleranzaResponsabili", new Integer[]{idAzienda}, new String[]{Applicazione.Applicazioni.ribaltorg.toString()});
                    if (parameters != null && !parameters.isEmpty()) {
                        tolleranza = parametriAziende.getValue(parameters.get(0), Integer.class);
                    }
                    nRigheDB = mdrResponsabiliRepository.countRow(idAzienda);
                    // Delete delle righe da sostituire
                    predicateAzienda = QMdrResponsabili.mdrResponsabili.idAzienda.id.eq(idAzienda);
                    mdrResponsabiliRepository.deleteByIdAzienda(idAzienda);
                    //Reading with CsvMapReader
                    Map<String, Object> responsabiliMap = null;
                    Map<Integer, List<Map<String, Object>>> selectDateOnAppartenentiByIdAzienda = mdrAppartenentiRepository.selectDateOnAppartenentiByIdAzienda(idAzienda);
                    Map<Integer, List<Map<String, Object>>> selectDateOnStruttureByIdAzienda = mdrStrutturaRepository.selectDateOnStruttureByIdAzienda(idAzienda);
                    anomalia = false;
                    Boolean anomali = false;
                    Boolean anomaliaRiga = false;
                    while ((responsabiliMap = mapReader.read(headers, processors)) != null) {
//                      preparo mappa di errore
                        mapError.put("ERRORE", "");
                        // Inserisco la riga
                        MdrResponsabili mR = new MdrResponsabili();
//                      inizio a settare i dati
//                      CODICE_ENTE preso da interfaccia

//                      CODICE_MATRICOLA bloccante
                        String codice_matricola = Responsabili.checkCodiceMatricola(responsabiliMap, mapError, selectDateOnAppartenentiByIdAzienda);
                        if (codice_matricola.equals("")) {
                            mR.setCodiceMatricola(null);
                            //nRigheAnomale++;
                            anomalia = true;
                            anomaliaRiga = true;
                        } else {
                            mR.setCodiceMatricola(Integer.parseInt(codice_matricola));

                        }

//                      DATAIN bloccante
                        anomali = !ImportaDaCSVUtils.checkDatain(responsabiliMap, mapError, "R");
                        anomalia = anomalia ? anomalia : anomali;
                        anomaliaRiga = anomaliaRiga ? anomaliaRiga : anomali;
                        //nRigheAnomale = anomali ? nRigheAnomale++ : nRigheAnomale;
                        mR.setDatain(!anomali ? ImportaDaCSVUtils.formattattore(responsabiliMap.get("datain")) : null);

                        ZonedDateTime datafi = null;
                        ZonedDateTime datain = null;
                        String datafiString = null;
                        String datainString = null;

                        if (responsabiliMap.get("datafi") != null && (!responsabiliMap.get("datafi").toString().trim().equals("") || responsabiliMap.get("datafi") == "")) {
                            datafi = ImportaDaCSVUtils.formattattore(responsabiliMap.get("datafi"));
                            datafiString = UtilityFunctions.getZonedDateTimeString(datafi);
                        }

                        if (!anomali) {
                            datain = mR.getDatain();
                            datainString = UtilityFunctions.getZonedDateTimeString(datain);
                        }

//                      ID_CASELLA bloccante
                        String id_casella;
                        try {
                            id_casella = Responsabili.checkIdCasella(responsabiliMap, mapError, selectDateOnStruttureByIdAzienda, idAzienda);
                            if (!id_casella.equals("")) {
                                mR.setIdCasella(Integer.parseInt(id_casella));
                            }
                        } catch (RibaltoneCSVCheckException e) {
                            id_casella = e.getDato().toString();
                            anomaliaRiga = true;
                            anomalia = true;
                            mapError.put("ERRORE", e.getMessage());
                        }
//                      DATAFI non bloccante
                        if (responsabiliMap.get("datafi") == null || responsabiliMap.get("datafi").toString().trim().equals("") || responsabiliMap.get("datafi") == "") {
                            mapError.put("datafi", "");
                            mR.setDatafi(null);
                        } else {
                            mapError.put("datafi", responsabiliMap.get("datafi"));
                            mR.setDatafi(ImportaDaCSVUtils.formattattore(responsabiliMap.get("datafi")));
                        }

                        if (ImportaDaCSVUtils.checkDateFinisconoDopoInizio(datain, datafi)) {
                            anomaliaRiga = true;
                            anomalia = true;
                            if (mapError.get("ERRORE") != null) {
                                mapError.put("ERRORE", mapError.get("ERRORE") + "questa riga non è valida perche la data di fine è precedente alla data di fine,");
                            } else {
                                mapError.put("ERRORE", "questa riga non è valida perche la data di fine è precedente alla data di fine,");
                            }
                        }
//TODO si possono usare le mappe anche qui
                        if (id_casella != null && mdrResponsabiliRepository.countMultiReponsabilePerStruttura(codiceAzienda,
                                Integer.parseInt(id_casella),
                                datafiString,
                                datainString) > 0) {
                            anomaliaRiga = true;
                            anomalia = true;
                            mapError.put("ERRORE", mapError.get("ERRORE") + " la struttura di questo responsabile è già assegnata ad un altro respondabile,");
                        }
//                      TIPO bloccante
                        String tipoR = Responsabili.checkTipo(responsabiliMap, mapError);
                        mR.setTipo(tipoR);
                        anomalia = tipoR == null ? true : anomalia;
                        anomaliaRiga = tipoR == null ? true : anomaliaRiga;
//                        nRigheAnomale = tipoR == null ? nRigheAnomale++ : nRigheAnomale;

//                      CODICE ENTE
                        String CodiceEnte = ImportaDaCSVUtils.checkCodiceEnte(responsabiliMap, mapError, codiceAzienda);
                        mR.setCodiceEnte(CodiceEnte);
                        anomalia = Objects.equals(CodiceEnte, codiceAzienda) ? true : anomalia;
                        anomaliaRiga = Objects.equals(CodiceEnte, codiceAzienda) ? true : anomaliaRiga;
                        //nRigheAnomale = Objects.equals(CodiceEnte, codiceAzienda) ? nRigheAnomale++ : nRigheAnomale;

                        mR.setIdAzienda(azienda);
                        if (!anomaliaRiga) {
                            mdrResponsabiliRepository.save(mR);
                        } else {
                            nRigheAnomale++;
                        }
                        anomaliaRiga = false;
                        mapWriter.write(mapError, headersErrorGenerator(tipo), getProcessorsError(tipo, codiceAzienda));
                        nRigheCSV = mapReader.getRowNumber();
                    }
                }
                break;

                case "STRUTTURA": {
                    parameters = parametriAziende.getParameters("tolleranzaStrutture", new Integer[]{idAzienda}, new String[]{Applicazione.Applicazioni.ribaltorg.toString()});
                    if (parameters != null && !parameters.isEmpty()) {
                        tolleranza = parametriAziende.getValue(parameters.get(0), Integer.class);
                    }
                    nRigheDB = mdrStrutturaRepository.countRow(idAzienda);
                    anomalia = false;
                    bloccante = false;
                    // Delete delle righe da sostituire
                    predicateAzienda = QMdrStruttura.mdrStruttura.idAzienda.id.eq(idAzienda);
                    mdrStrutturaRepository.deleteByIdAzienda(idAzienda);
                    // Reading with CsvMapReader
                    Map<String, Object> strutturaMap = null;
                    Map<Integer, List<Map<String, Object>>> strutturaCheckDateMap = new HashMap();
                    Map<Integer, String> multidefinizioneStruttura = new HashMap();
                    while ((strutturaMap = mapReader.read(headers, processors)) != null) {
//                      inizio a creare la mappa degli errori e
                        mapError.put("ERRORE", "");
                        // Inserisco la riga
                        MdrStruttura mS = new MdrStruttura();
                        ZonedDateTime datafi = null;
                        ZonedDateTime datain = null;
                        String datafiString = null;
                        String datainString = null;

                        boolean anomali = !ImportaDaCSVUtils.checkDatain(strutturaMap, mapError, "S");
                        if (anomali) {
                            mS.setDatain(null);
                            bloccante = true;
                            nRigheAnomale++;

                            if (mapError.get("ERRORE") == null || "".equals(mapError.get("ERRORE").toString().trim())) {
                                mapError.put("ERRORE", "la data di inizio è vuota");
                            } else {
                                mapError.put("ERRORE", mapError.get("ERRORE").toString() + ", la data di inizio è vuota");
                            }
                        } else {
                            datain = ImportaDaCSVUtils.formattattore(strutturaMap.get("datain"));
                            datainString = UtilityFunctions.getZonedDateTimeString(datain);
                            mS.setDatain(datain);

                        }

                        datafi = Strutture.checkDatafi(strutturaMap, mapError);
                        mS.setDatafi(datafi);
                        datafiString = datafi != null ? UtilityFunctions.getZonedDateTimeString(datafi) : null;

                        String id_casella = Strutture.checkIdCasella(strutturaMap, mapError, mapReader.getLineNumber(), strutturaCheckDateMap);
                        mS.setIdCasella(id_casella.equals("") ? null : Integer.parseInt(id_casella));
                        bloccante = id_casella.equals("") ? true : bloccante;
                        //per mettere il bloccante su strutture definite piu volte
                        bloccante = mapError.get("ERRORE").toString().contains("struttura definita piu volte nello stesso arco temporale,") ? true : bloccante;

                        if (strutturaMap.get("id_padre") == null || strutturaMap.get("id_padre").toString().trim().equals("") || strutturaMap.get("id_padre") == "") {
                            mapError.put("id_padre", "");
                            mS.setIdPadre(null);
                        } else {
                            mapError.put("id_padre", strutturaMap.get("id_padre"));
                            mS.setIdPadre(Integer.parseInt(strutturaMap.get("id_padre").toString()));
                        }

                        String descrizione = Strutture.checkDescrizione(strutturaMap, mapError, mapReader.getLineNumber());
                        mS.setDescrizione(descrizione.equals("") ? null : descrizione);
                        bloccante = descrizione.equals("") ? true : bloccante;

                        if (strutturaMap.get("tipo_legame") == null || strutturaMap.get("tipo_legame").toString().trim().equals("") || strutturaMap.get("tipo_legame") == "") {
                            mapError.put("tipo_legame", "");
                            mS.setTipoLegame(null);
                        } else {
                            mapError.put("tipo_legame", strutturaMap.get("tipo_legame"));
                            mS.setTipoLegame(strutturaMap.get("tipo_legame").toString());
                        }

                        String codiceEnte = ImportaDaCSVUtils.checkCodiceEnte(strutturaMap, mapError, codiceAzienda);
                        mS.setCodiceEnte(codiceEnte);
                        anomali = codiceEnte.equals(codiceAzienda) ? true : anomali;
                        nRigheAnomale = codiceEnte.equals(codiceAzienda) ? nRigheAnomale++ : nRigheAnomale;
                        mS.setIdAzienda(azienda);
                        em.persist(mS);
                        //mdrStrutturaRepository.save(mS);
                        if (bloccante || anomalia) {
                            multidefinizioneStruttura.put(Integer.parseInt(id_casella), mapError.get("ERRORE").toString());
                        }
                        mapWriter.write(mapError, headersErrorGenerator(tipo), getProcessorsError(tipo, codiceAzienda));
                        nRigheCSV = mapReader.getRowNumber();
                    }

                    //struttura padre non trovata
                    Map<Integer, List<Map<String, Object>>> listaStrutture = mdrStrutturaRepository.selectDateOnStruttureByIdAzienda(idAzienda);

                    mapWriter.close();
                    mapReader.close();

                    try ( InputStreamReader csvErrorFileRIP = new InputStreamReader(new FileInputStream(csvErrorFile));) {

                        mapErrorReader = new CsvMapReader(csvErrorFileRIP, SEMICOLON_DELIMITED);
                        mapErrorReader.getHeader(true);

                        mapErrorWriter = new CsvMapWriter(new FileWriter(csvErrorFile2), SEMICOLON_DELIMITED);
                        mapErrorWriter.writeHeader(headersErrorGenerator(tipo));
                        Integer i = 0;
                        Map<String, Object> strutturaErrorMap;
                        while ((strutturaErrorMap = mapErrorReader.read(headersErrorGenerator(tipo), getProcessorsError(tipo, codiceAzienda))) != null) {
                            Map<String, Object> strutturaErrorMapWrite = new HashMap();
                            //struttura padre non trovata
                            strutturaErrorMapWrite.putAll(strutturaErrorMap);
                            if (strutturaErrorMap.get("id_padre") != null && strutturaErrorMap.get("id_padre") != "" && !strutturaErrorMap.get("id_padre").equals("0")) {
                                //System.out.println("contatore" + (i++).toString());
                                if (!listaStrutture.containsKey(Integer.parseInt(strutturaErrorMap.get("id_padre").toString()))) {
                                    bloccante = true;
                                    log.error("Importa CSV --Struttura-- errore alla righa:" + mapReader.getLineNumber() + "  padre non presente");
                                    strutturaErrorMapWrite.put("ERRORE", strutturaErrorMap.get("ERRORE") != null ? strutturaErrorMap.get("ERRORE") : "" + " padre non presente,");
                                } else {
                                    List<Map<String, Object>> elementi = listaStrutture.get(Integer.parseInt(strutturaErrorMap.get("id_padre").toString()));

                                    if ((strutturaErrorMap.get("datain") != null) && (!ImportaDaCSVUtils.isPeriodiSovrapposti(elementi, ImportaDaCSVUtils.formattattore(strutturaErrorMap.get("datain")), ImportaDaCSVUtils.formattattore(strutturaErrorMap.get("datafi"))))) {
                                        bloccante = true;
                                        log.error("Importa CSV --Struttura-- errore alla righa:" + mapReader.getLineNumber() + " non rispetta l'arco temporale del padre");
                                        if (strutturaErrorMap.get("ERRORE") != null) {
                                            strutturaErrorMapWrite.put("ERRORE", strutturaErrorMap.get("ERRORE") + " non rispetta l'arco temporale del padre,");
                                        } else {
                                            strutturaErrorMapWrite.put("ERRORE", " non rispetta l'arco temporale del padre,");
                                        }
                                    }
                                    Map<String, ZonedDateTime> maxMin = ImportaDaCSVUtils.maxMin(elementi);
                                    if (!ImportaDaCSVUtils.controllaEstremi(maxMin.get("min"), maxMin.get("max"), ImportaDaCSVUtils.formattattore(strutturaErrorMap.get("datain")), ImportaDaCSVUtils.formattattore(strutturaErrorMap.get("datafi")))) {
                                        strutturaErrorMapWrite.put("ERRORE", " non rispetta l'arco temporale del padre,");
                                        log.error("Importa CSV --Struttura-- errore alla righa:" + mapReader.getLineNumber() + " non rispetta l'arco temporale del padre");
                                        bloccante = true;
                                    }
                                }
                            }
                            //struttura definita piu volte
                            if (multidefinizioneStruttura.get(Integer.parseInt(strutturaErrorMap.get("id_casella").toString())) != null
                                    && multidefinizioneStruttura.get(Integer.parseInt(strutturaErrorMap.get("id_casella").toString())).contains("struttura definita piu volte nello stesso arco temporale,")) {
                                if (strutturaErrorMap.get("ERRORE") != null && strutturaErrorMap.get("ERRORE").toString().contains("struttura definita piu volte nello stesso arco temporale,")) {
                                    strutturaErrorMapWrite.put("ERRORE", strutturaErrorMap.get("ERRORE"));
                                } else if (strutturaErrorMap.get("ERRORE") != null && !strutturaErrorMap.get("ERRORE").toString().contains("struttura definita piu volte nello stesso arco temporale,")) {
                                    strutturaErrorMapWrite.put("ERRORE", strutturaErrorMap.get("ERRORE") + "struttura definita piu volte nello stesso arco temporale,");
                                } else {
                                    strutturaErrorMapWrite.put("ERRORE", "struttura definita piu volte nello stesso arco temporale,");

                                }
                            }
                            mapErrorWriter.write(strutturaErrorMapWrite, headersErrorGenerator(tipo), getProcessorsError(tipo, codiceAzienda));

                        }
                    } catch (Exception ex) {
                        bloccante = true;
                        log.error("Importa CSV -- error generic");
                        System.out.println("ex:" + ex);
                    }
                }
                break;

                case "TRASFORMAZIONI": {
                    nRigheDB = mdrTrasformazioniRepository.countRow(idAzienda);
                    anomalia = false;
                    parameters = parametriAziende.getParameters("tolleranzaResponsabili", new Integer[]{idAzienda}, new String[]{Applicazione.Applicazioni.ribaltorg.toString()});
                    if (parameters != null && !parameters.isEmpty()) {
                        tolleranza = parametriAziende.getValue(parameters.get(0), Integer.class);
                    }
                    //TODO per ottimizzazioni successive decommentare riga successiva
                    //Map<Integer, List<Map<String, Object>>> selectDateOnStruttureByIdAzienda1 = mdrStrutturaRepository.selectDateOnStruttureByIdAzienda(idAzienda);
                    // Delete delle righe da sostituire
                    predicateAzienda = QMdrTrasformazioni.mdrTrasformazioni.idAzienda.id.eq(idAzienda);
                    mdrTrasformazioniRepository.deleteByIdAzienda(idAzienda);
                    Map<Integer, List<Map<String, Object>>> listaStruttureConDate = mdrStrutturaRepository.selectDateOnStruttureByIdAzienda(idAzienda);
                    //Reading with CsvMapReader
                    Map<String, Object> trasformazioniMap;
                    while ((trasformazioniMap = mapReader.read(headers, processors)) != null) {
                        log.info("mapReader.getLineNumber()" + mapReader.getLineNumber());
                        Boolean tempi_ok = true;
                        Boolean dataTrasformazione = true;
                        Boolean dataInPartenza = true;
                        // Inserisco la riga
                        MdrTrasformazioni mT = new MdrTrasformazioni();
                        mapError.put("ERRORE", "");
                        //PROGRESSIVO RIGA
                        Integer progressivoRiga = Trasformazioni.checkProgressivoRiga(trasformazioniMap, mapError, mapReader);
                        mT.setProgressivoRiga(progressivoRiga);
                        bloccante = progressivoRiga == null ? true : bloccante;

//                      DATA TRASFORMAZIONE DEVE ESISTERE SEMPRE
                        ZonedDateTime dataTrasformazioneT = Trasformazioni.checkDataTrasformazione(trasformazioniMap, mapError, mapReader);
                        bloccante = dataTrasformazioneT == null ? true : bloccante;
                        dataTrasformazione = dataTrasformazioneT == null ? false : dataTrasformazione;
                        mT.setDataTrasformazione(dataTrasformazioneT);
//                       DATA IN PARTENZA DEVE ESISTERE SEMPRE
//                       PER MOTIVO DI "X", "T","R" E "U" è LA DATA INIZIO DELLA CASELLA DI PARTENZA
//                      AGGIUNGERE BOOLEANO TEMPI_CASELLA_OK
                        ZonedDateTime dataInPartenzaT = Trasformazioni.checkDataInPartenza(trasformazioniMap, mapError, mapReader);
                        bloccante = dataInPartenzaT == null ? true : bloccante;
                        dataInPartenza = dataInPartenzaT == null ? false : dataInPartenza;
                        mT.setDatainPartenza(dataInPartenzaT);

                        //ID CASELLA DI PARTENZA
                        //SEMPRE SPENTO IL GIORNO PRIMA DELLA DATA DI TRASFORMAZIONE
                        //DI CONSEGUENZA DEVE ESISTERE
                        Integer idCasellaPartenza = Trasformazioni.checkIdCasellaPartenza(trasformazioniMap, mapError, mapReader);
                        mT.setIdCasellaPartenza(idCasellaPartenza == -1 ? null : idCasellaPartenza);
                        bloccante = idCasellaPartenza == null ? true : bloccante;

                        if (dataInPartenza && dataTrasformazione && idCasellaPartenza != -1) {

                            if (!listaStruttureConDate.containsKey(idCasellaPartenza)) {
                                log.error("Importa CSV --Trasformazioni-- errore alla righa:" + mapReader.getLineNumber() + " casella di partenza non trovata");
                                bloccante = true;
                                tempi_ok = false;
                                mapError.put("ERRORE", mapError.get("ERRORE") + " casella di partenza non trovata,");
                            } else {
                                boolean blocco = Strutture.checkAccesaSpentaMale(listaStruttureConDate.get(idCasellaPartenza), ImportaDaCSVUtils.formattattore(trasformazioniMap.get("data_trasformazione").toString()), ImportaDaCSVUtils.formattattore(trasformazioniMap.get("datain_partenza").toString()));
                                if (blocco) {
                                    log.error("Importa CSV --Trasformazioni-- errore alla righa:" + mapReader.getLineNumber() + " periodi temporali della casella di partenza non sono validi");
                                    bloccante = true;
                                    tempi_ok = false;
                                    mapError.put("ERRORE", mapError.get("ERRORE") + " periodi temporali della casella di partenza non sono validi,");
                                }
                            }
                        }

//                      DATA ORA OPERAZIONE
                        ZonedDateTime dataOraOper = Trasformazioni.checkDataOraOper(trasformazioniMap, mapError);
                        mT.setDataoraOper(dataOraOper);
                        boolean buono = trasformazioniMap.get("dataora_oper") == null || trasformazioniMap.get("dataora_oper").toString().trim().equals("");
                        anomalia = buono ? true : anomalia;
                        nRigheAnomale = buono ? nRigheAnomale++ : nRigheAnomale;

//                      CODICE ENTE
                        String codiceEnte = ImportaDaCSVUtils.checkCodiceEnte(trasformazioniMap, mapError, codiceAzienda);
                        mT.setCodiceEnte(codiceEnte);
                        anomalia = codiceEnte.equals(codiceAzienda) ? true : anomalia;
                        nRigheAnomale = codiceEnte.equals(codiceAzienda) ? nRigheAnomale++ : nRigheAnomale;

//                      MOTIVO
                        if (trasformazioniMap.get("motivo") == null
                                || trasformazioniMap.get("motivo").toString().trim().equals("")
                                || trasformazioniMap.get("motivo") == ""
                                || !(trasformazioniMap.get("motivo").toString().trim().equalsIgnoreCase("X")
                                || trasformazioniMap.get("motivo").toString().trim().equalsIgnoreCase("R")
                                || trasformazioniMap.get("motivo").toString().trim().equalsIgnoreCase("T")
                                || trasformazioniMap.get("motivo").toString().trim().equalsIgnoreCase("U"))) {
                            mapError.put("ERRORE", mapError.get("ERRORE") + " MOTIVO,");
                            mapError.put("motivo", "");
                            mT.setMotivo(null);
                            log.error("Importa CSV --Trasformazioni-- errore alla righa:" + mapReader.getLineNumber() + " id_casella_arrivo assente");
                            bloccante = true;
                            //non ci sta un motivo copio paripari id casella di arrivo non ho elementi per sapere se ci dovrebbe o meno essere qualcosa
                            mapError.put("id_casella_arrivo", trasformazioniMap.get("id_casella_arrivo"));
                            mT.setIdCasellaArrivo(Integer.parseInt(trasformazioniMap.get("id_casella_arrivo").toString()));
                        } else {
                            mapError.put("motivo", trasformazioniMap.get("motivo"));
                            mT.setMotivo(trasformazioniMap.get("motivo").toString());

                            if (trasformazioniMap.get("motivo").toString().trim().equalsIgnoreCase("X")) {
                                mapError.put("id_casella_arrivo", trasformazioniMap.get("id_casella_arrivo"));
                                if (tempi_ok) {
                                    if (trasformazioniMap.get("id_casella_arrivo") == null || trasformazioniMap.get("id_casella_arrivo").toString().trim().equals("")) {
                                        mapError.put("ERRORE", mapError.get("ERRORE") + " casella di arrivo non presente nella tabella struttura,");
                                        mapError.put("id_casella_arrivo", "");
                                        mT.setIdCasellaArrivo(null);
                                        bloccante = true;
                                        log.error("Importa CSV --Trasformazioni-- errore alla righa:" + mapReader.getLineNumber() + " id_casella_arrivo assente");
                                    } else {
                                        mapError.put("id_casella_arrivo", trasformazioniMap.get("id_casella_arrivo"));
                                        mT.setIdCasellaArrivo(Integer.parseInt(trasformazioniMap.get("id_casella_arrivo").toString()));
                                        if (!trasformazioniMap.get("id_casella_arrivo").equals(trasformazioniMap.get("partenza"))) {
                                            mT.setIdCasellaArrivo(Integer.parseInt(trasformazioniMap.get("id_casella_arrivo").toString()));
                                            //TODO usare metodo appartenenti per ottimizzare
                                            if (dataTrasformazioneT != null) {
                                                Integer accesaIntervalloByIdAzienda = mdrTrasformazioniRepository.isAccesaIntervalloByIdAzienda(idAzienda, Integer.parseInt(trasformazioniMap.get("id_casella_arrivo").toString()), dataTrasformazioneT);
                                                if (accesaIntervalloByIdAzienda != 1) {
                                                    bloccante = true;
                                                    log.error("Importa CSV --Trasformazioni-- errore alla righa:" + mapReader.getLineNumber() + " casella di arrivo non valida nella data di trasformazione");
                                                    mapError.put("ERRORE", mapError.get("ERRORE") + " casella di arrivo non valida nella data di trasformazione,");
                                                }
                                            }
                                        } else {
                                            log.error("Importa CSV --Trasformazioni-- errore alla righa:" + mapReader.getLineNumber() + " casella di arrivo e di partenza sono uguali");
                                            bloccante = true;
                                            mapError.put("ERRORE", mapError.get("ERRORE") + " casella di arrivo e di partenza sono uguali,");
                                        }
                                    }
                                }
                            } else if (trasformazioniMap.get("motivo").toString().trim().equalsIgnoreCase("R")
                                    || (trasformazioniMap.get("motivo").toString().trim().equalsIgnoreCase("T"))) {
                                if (trasformazioniMap.get("id_casella_arrivo") == null || trasformazioniMap.get("id_casella_arrivo").toString().trim().equals("")) {
                                    mapError.put("id_casella_arrivo", "");
                                    mT.setIdCasellaArrivo(null);

                                } else {
                                    mapError.put("id_casella_arrivo", trasformazioniMap.get("id_casella_arrivo"));
                                    mT.setIdCasellaArrivo(Integer.parseInt(trasformazioniMap.get("id_casella_arrivo").toString()));
                                    if (!trasformazioniMap.get("id_casella_partenza").equals(trasformazioniMap.get("id_casella_arrivo"))) {
                                        bloccante = true;
                                        log.error("Importa CSV --Trasformazioni-- errore alla righa:" + mapReader.getLineNumber() + " id_casella_arrivo diversa da id_casella_partenza");
                                        mapError.put("ERRORE", mapError.get("ERRORE") + " id_casella_arrivo diversa da id_casella_partenza,");
                                    } else {
                                        Integer accesaBeneByIdAzienda = mdrTrasformazioniRepository.isAccesaBeneByIdAzienda(idAzienda, Integer.parseInt(trasformazioniMap.get("id_casella_partenza").toString()), ImportaDaCSVUtils.formattattore(trasformazioniMap.get("data_trasformazione")));
                                        if (accesaBeneByIdAzienda != 1) {
                                            bloccante = true;
                                            log.error("Importa CSV --Trasformazioni-- errore alla righa:" + mapReader.getLineNumber() + " casella di partenza non valida nella data di trasformazione");
                                            mapError.put("ERRORE", mapError.get("ERRORE") + " casella di partenza non valida nella data di trasformazione,");
                                        }

                                    }
                                }

                            }

                        }
                        mT.setIdAzienda(azienda);
                        mdrTrasformazioniRepository.save(mT);
                        mapWriter.write(mapError, headersErrorGenerator(tipo), getProcessorsError(tipo, codiceAzienda));
                        nRigheCSV = mapReader.getRowNumber();
                    }
                }
                break;

                default:
                    System.out.println("non dovrebbe essere");
                    break;
            }

        } catch (Exception e) {
            if (!tipo.equals("STRUTTURA")) {
                log.error("ERRORE GENERICO---", e);
                throw new BaborgCSVBloccanteException(csvErrorFile.getAbsolutePath(), e);
            } else {
                log.error("ERRORE GENERICO STRUTTURA---", e);
                throw new BaborgCSVBloccanteException(csvErrorFile2.getAbsolutePath(), e);

            }
        } finally {
            if (mapReader != null) {
                try {
                    mapReader.close();
                } catch (IOException ex) {
                    log.error("mapReader non chiudibile", ex);
                }
            }
            if (mapWriter != null) {
                try {
                    mapWriter.close();
                    if (!tipo.equals("STRUTTURA")) {
                        MongoWrapper mongoWrapper = mongoConnectionManager.getRepositoryWrapperByIdAzienda(idAzienda);
                        uuid = mongoWrapper.put(csvErrorFile, csvErrorFile.getName(), "/importazioniCSV/csv_error_GRU", true);
                    }

                } catch (IOException ex) {
                    log.error("mapWriter non chiudibile", ex);
                }
            }
            if (mapErrorWriter != null) {
                try {
                    mapErrorWriter.close();
                    MongoWrapper mongoWrapper = mongoConnectionManager.getRepositoryWrapperByIdAzienda(idAzienda);
                    uuid = mongoWrapper.put(csvErrorFile2, csvErrorFile2.getName(), "/importazioniCSV/csv_error_GRU", true);

                } catch (IOException ex) {
                    log.error("mapWriter non chiudibile", ex);
                }
            }

        }
        Integer rigeDaImportare = nRigheCSV - nRigheAnomale;
        if (nRigheDB > 0) {
            if (100 - (rigeDaImportare * 100 / nRigheDB) > tolleranza) {
                throw new BaborgCSVBloccanteRigheException(uuid);
            }
        }
//        csvErrorFile.delete();
//        csvErrorFile2.delete();
        if (bloccante) {
            throw new BaborgCSVBloccanteException(uuid);
        }

        if (anomalia) {
            throw new BaborgCSVAnomaliaException(uuid);
        }

        return uuid;
    }
}
