package it.bologna.ausl.internauta.service.baborg.utils;

import com.querydsl.core.types.dsl.BooleanExpression;
import it.bologna.ausl.blackbox.utils.UtilityFunctions;
import it.bologna.ausl.internauta.service.configuration.utils.ReporitoryConnectionManager;
import it.bologna.ausl.internauta.service.exceptions.ribaltonecsv.BaborgCSVAnomaliaException;
import it.bologna.ausl.internauta.service.exceptions.ribaltonecsv.BaborgCSVBloccanteException;
import it.bologna.ausl.internauta.service.exceptions.ribaltonecsv.BaborgCSVBloccanteRigheException;
import it.bologna.ausl.internauta.service.repositories.baborg.AziendaRepository;
import it.bologna.ausl.internauta.service.repositories.baborg.ImportazioniOrganigrammaRepository;
import it.bologna.ausl.internauta.service.repositories.baborg.PersonaRepository;
import it.bologna.ausl.internauta.service.repositories.baborg.UtenteRepository;
import it.bologna.ausl.internauta.service.repositories.gru.MdrAppartenentiRepository;
import it.bologna.ausl.internauta.service.repositories.gru.MdrResponsabiliRepository;
import it.bologna.ausl.internauta.service.repositories.gru.MdrStrutturaRepository;
import it.bologna.ausl.internauta.service.repositories.gru.MdrStrutturaRepositoryCustomImpl;
import it.bologna.ausl.internauta.service.repositories.gru.MdrTrasformazioniRepository;
import it.bologna.ausl.internauta.service.ribaltone.ImportaDaCSV;
import it.bologna.ausl.internauta.service.utils.ParametriAziendeReader;
import it.bologna.ausl.model.entities.baborg.Azienda;
import it.bologna.ausl.model.entities.baborg.ImportazioniOrganigramma;
import it.bologna.ausl.model.entities.baborg.Persona;
import it.bologna.ausl.model.entities.baborg.Utente;
import it.bologna.ausl.model.entities.configurazione.Applicazione;
import it.bologna.ausl.model.entities.configurazione.ParametroAziende;
import it.bologna.ausl.model.entities.gru.MdrAppartenenti;
import it.bologna.ausl.model.entities.gru.MdrResponsabili;
import it.bologna.ausl.model.entities.gru.MdrStruttura;
import it.bologna.ausl.model.entities.gru.MdrTrasformazioni;
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
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.supercsv.cellprocessor.ift.CellProcessor;
import org.supercsv.prefs.CsvPreference;
import org.supercsv.cellprocessor.Optional;
import org.supercsv.cellprocessor.constraint.StrRegEx;
import org.supercsv.io.CsvMapReader;
import org.supercsv.io.CsvMapWriter;
import org.supercsv.io.ICsvMapReader;
import org.supercsv.io.ICsvMapWriter;

/**
 *
 * @author guido
 */
@Component
public class BaborgUtils {

    private static final Logger log = LoggerFactory.getLogger(BaborgUtils.class);

    private static Map<String, Integer> map;

    @Autowired
    MdrStrutturaRepositoryCustomImpl mdrStrutturaRepositoryCustomImpl;

    @Autowired
    ImportazioniOrganigrammaRepository importazioniOrganigrammaRepository;

    // inizializzazione mappa statica
    static {
        map = new HashMap<>();
        map.put("pec.ospfe.it", 11);
        map.put("cert.ao.pr.it", 15);
        map.put("pec.aosp.bo.it", 10);
        map.put("pec.ior.it", 3);
        map.put("pec.ausl.bologna.it", 2);
        map.put("pec.ausl.fe.it", 6);
        map.put("pec.ausl.imola.bo.it", 12);
        map.put("pec.ausl.pr.it", 5);
    }

    enum Tipo {
        APPARTENENTI,
        RESPONSABILI,
        STRUTTURA,
        TRASFORMAZIONI
    }

    @PersistenceContext
    EntityManager em;

    @Autowired
    AziendaRepository aziendaRepository;

    @Autowired
    BeanFactory beanFactory;

    @Autowired
    MdrTrasformazioniRepository mdrTrasformazioniRepository;

    @Autowired
    MdrAppartenentiRepository mdrAppartenentiRepository;

    @Autowired
    MdrResponsabiliRepository mdrResponsabiliRepository;

    @Autowired
    MdrStrutturaRepository mdrStrutturaRepository;

    @Autowired
    UtenteRepository utenteRepository;

    @Autowired
    PersonaRepository personaRepository;

    @Autowired
    ReporitoryConnectionManager mongoConnectionManager;

    @Autowired
    ParametriAziendeReader parametriAziende;

    public Azienda getAziendaRepositoryFromPecAddress(String address) {

        Azienda res = null;

        String domain = address.substring(address.indexOf("@") + 1);
        if (map.get(domain) != null) {
            java.util.Optional<Azienda> ar = aziendaRepository.findById(map.get(domain));

            if (ar.isPresent()) {
                res = ar.get();
            }
        }

        return res;
    }

    public File buildCSV(List<Map<String, Object>> elementi, String tipo) {

        Timestamp timestamp = new Timestamp(System.currentTimeMillis());
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy_MM_dd-HH_mm_ss");
        String nameCsv = sdf.format(timestamp) + "_" + tipo + ".csv";
        File csvFile = new File(System.getProperty("java.io.tmpdir" + "/csv/"), nameCsv);
        csvFile.deleteOnExit();
        CsvPreference SEMICOLON_DELIMITED = new CsvPreference.Builder('"', ';', "\r\n").build();
        Map<String, Object> row = new HashMap<>();
        try (CsvMapWriter mapWriter = new CsvMapWriter(new FileWriter(csvFile), SEMICOLON_DELIMITED)) {

            mapWriter.writeHeader(headersGenerator(tipo));
            for (Map<String, Object> elemento : elementi) {
                //.equals potrebbe essere un problema
                row.putAll(elemento);
                if (elemento.get("datain") != null && !elemento.get("datain").toString().trim().equals("")) {
                    if (Timestamp.class.isAssignableFrom(elemento.get("datain").getClass())) {
                        row.put("datain", ((Timestamp) elemento.get("datain")).toLocalDateTime().toLocalDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
                    }
                }
                if (elemento.get("datafi") != null && !elemento.get("datafi").toString().trim().equals("")) {
                    if (Timestamp.class.isAssignableFrom(elemento.get("datafi").getClass())) {
                        row.put("datafi", ((Timestamp) elemento.get("datafi")).toLocalDateTime().toLocalDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
                    }
                }
                if (elemento.get("data_assunzione") != null && !elemento.get("data_assunzione").toString().trim().equals("")) {
                    if (Timestamp.class.isAssignableFrom(elemento.get("data_assunzione").getClass())) {
                        row.put("data_assunzione", ((Timestamp) elemento.get("data_assunzione")).toLocalDateTime().toLocalDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
                    }
                }
                if (elemento.get("data_dimissione") != null && !elemento.get("data_dimissione").toString().trim().equals("")) {
                    if (Timestamp.class.isAssignableFrom(elemento.get("data_dimissione").getClass())) {
                        row.put("data_dimissione", ((Timestamp) elemento.get("data_dimissione")).toLocalDateTime().toLocalDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
                    }
                }
                if (elemento.get("data_trasformazione") != null && !elemento.get("data_trasformazione").toString().trim().equals("")) {
                    if (Timestamp.class.isAssignableFrom(elemento.get("data_trasformazione").getClass())) {
                        row.put("data_trasformazione", ((Timestamp) elemento.get("data_trasformazione")).toLocalDateTime().toLocalDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
                    }
                }
                if (elemento.get("datain_partenza") != null && !elemento.get("datain_partenza").toString().trim().equals("")) {
                    if (Timestamp.class.isAssignableFrom(elemento.get("datain_partenza").getClass())) {
                        row.put("datain_partenza", ((Timestamp) elemento.get("datain_partenza")).toLocalDateTime().toLocalDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
                    }
                }
                if (elemento.get("dataora_oper") != null && !elemento.get("dataora_oper").toString().trim().equals("")) {
                    if (Timestamp.class.isAssignableFrom(elemento.get("dataora_oper").getClass())) {
                        row.put("dataora_oper", ((Timestamp) elemento.get("dataora_oper")).toLocalDateTime().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")));
                    }
                }

                mapWriter.write(row, headersGenerator(tipo), getProcessors(tipo));
                row.clear();
            }

        } catch (Exception e) {
            System.out.println("e" + e);
            return null;
        }
        return csvFile;

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
     * it.bologna.ausl.internauta.service.exceptions.BaborgCSVBloccanteException
     * @throws
     * it.bologna.ausl.internauta.service.exceptions.BaborgCSVAnomaliaException
     */
    @Transactional(rollbackFor = Throwable.class, noRollbackFor = BaborgCSVAnomaliaException.class, propagation = Propagation.REQUIRES_NEW)
    public String csvTransactionalReadDeleteInsert(MultipartFile file, String tipo, Integer codiceAzienda, Integer idAzienda) throws BaborgCSVBloccanteException, BaborgCSVAnomaliaException, MongoWrapperException, BaborgCSVBloccanteRigheException {
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
        Integer nRigheCSV = 0;
        Integer nRigheAnomale = 0;
        Integer tolleranza = 0;
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

            BooleanExpression predicateAzienda = null;

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
                case "APPARTENENTI":
                    parameters = parametriAziende.getParameters("tolleranzaAppartenenti", new Integer[]{idAzienda}, new String[]{Applicazione.Applicazioni.ribaltorg.toString()});
                    if (parameters != null && !parameters.isEmpty()) {
                        tolleranza = parametriAziende.getValue(parameters.get(0), Integer.class);
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
                    LocalDateTime inizio = LocalDateTime.now();
//                    log.info("ora inizio: " + LocalDateTime.now());
//                    List<Integer> codiciMatricoleConMultiafferenzaDiretta = new ArrayList<>();

//                    List<Integer> codiciMatricoleConMultiafferenzaFunzionale = new ArrayList<>();
                    List<Integer> righeAnomaleDirette = new ArrayList<>();
                    List<Integer> righeAnomaleFunzionali = new ArrayList<>();
                    while ((appartenentiMap = mapReader.read(headers, processors)) != null) {

                        mapError = new HashMap<>();
                        riga = mapReader.getLineNumber();
                        log.info("getLineNumber: " + mapReader.getLineNumber());
                        // Inserisco la riga
                        MdrAppartenenti mA = new MdrAppartenenti();
//                      preparo la mappa di errore
                        mapError.put("ERRORE", "");
                        mapError.put("Anomalia", "");
                        //List<Map<String, Object>> selectDatebyMatricolaAndIdAziendaAndAfferenzaDiretta = null;
//                      CODICE_MATRICOLA bloccante
                        String codiceMatricola = null;
                        if (appartenentiMap.get("codice_matricola") == null || appartenentiMap.get("codice_matricola").toString().trim().equals("") || appartenentiMap.get("codice_matricola") == "") {
                            anomalia = true;
                            mapError.put("Anomalia", "true");

                            mapError.put("ERRORE", mapError.get("ERRORE") + " codice_matricola,");
                            mapError.put("codice_matricola", "");
//                            mA.setCodiceMatricola(null);
                            codiceMatricola = "";
                        } else {
                            mapError.put("codice_matricola", appartenentiMap.get("codice_matricola"));
                            codiceMatricola = appartenentiMap.get("codice_matricola").toString();
//                            mA.setCodiceMatricola(Integer.parseInt(appartenentiMap.get("codice_matricola").toString()));
//                            selectDatebyMatricolaAndIdAziendaAndAfferenzaDiretta = mdrAppartenentiRepository.selectDatebyMatricolaAndIdAziendaAndAfferenzaDiretta(Integer.parseInt(appartenentiMap.get("codice_matricola").toString()), idAzienda);
                        }
//                      COGNOME bloccante
                        if (appartenentiMap.get("cognome") == null || appartenentiMap.get("cognome").toString().trim().equals("") || appartenentiMap.get("cognome") == "") {
                            mapError.put("ERRORE", mapError.get("ERRORE") + " cognome,");
                            anomalia = true;
                            mapError.put("Anomalia", "true");

                            mapError.put("cognome", "");
//                            mA.setCognome(null);
                        } else {
                            mapError.put("cognome", appartenentiMap.get("cognome"));
//                            mA.setCognome(appartenentiMap.get("cognome").toString());
                        }
//                      NOME bloccante
                        if (appartenentiMap.get("nome") == null || appartenentiMap.get("nome").toString().trim().equals("") || appartenentiMap.get("nome") == "") {
                            mapError.put("ERRORE", mapError.get("ERRORE") + " nome,");
                            anomalia = true;
                            mapError.put("Anomalia", "true");
                            mapError.put("nome", "");
//                            mA.setNome(null);
                        } else {
                            mapError.put("nome", appartenentiMap.get("nome"));
//                            mA.setNome(appartenentiMap.get("nome").toString());
                        }
//                      CODICE_FISCALE bloccante
                        if (appartenentiMap.get("codice_fiscale") == null || appartenentiMap.get("codice_fiscale").toString().trim().equals("") || appartenentiMap.get("codice_fiscale") == "") {
                            mapError.put("ERRORE", mapError.get("ERRORE") + " CODICE FISCALE,");
                            anomalia = true;
                            mapError.put("Anomalia", "true");
                            mapError.put("codice_fiscale", "");
//                            mA.setCodiceFiscale(null);
                        } else {
                            mapError.put("codice_fiscale", appartenentiMap.get("codice_fiscale"));
//                            mA.setCodiceFiscale(appartenentiMap.get("codice_fiscale").toString());
                        }
                        String idCasella = null;
//                      ID_CASELLA bloccante
                        if (appartenentiMap.get("id_casella") == null || appartenentiMap.get("id_casella").toString().trim().equals("") || appartenentiMap.get("id_casella") == "") {
                            anomalia = true;
                            mapError.put("Anomalia", "true");
                            mapError.put("ERRORE", mapError.get("ERRORE") + " IDCASELLA,");
                            idCasella = "";
                            mapError.put("id_casella", "");
//                            mA.setIdCasella(null);
                        } else {
                            if (!selectDateOnStruttureByIdAzienda.containsKey(Integer.parseInt(appartenentiMap.get("id_casella").toString()))) {
//                            if (mdrStrutturaRepository.selectStrutturaUtenteByIdCasellaAndIdAzienda(Integer.parseInt(appartenentiMap.get("id_casella").toString()), idAzienda) <= 0) {
                                mapError.put("ERRORE", " manca la struttura nella tabella struttura,");
                                anomalia = true;
                                mapError.put("Anomalia", "true");

                            } else {
//                                List<Map<String, Object>> mieiPadri = mdrStrutturaRepository.mieiPadri(idAzienda, Integer.parseInt(appartenentiMap.get("id_casella").toString()));
                                if (!arcoBool(selectDateOnStruttureByIdAzienda.get(Integer.parseInt(appartenentiMap.get("id_casella").toString())), formattattore(appartenentiMap.get("datain")), formattattore(appartenentiMap.get("datafi")))) {
                                    mapError.put("ERRORE", mapError.get("ERRORE") + " non rispetta l arco temporale della struttura,");
                                    anomalia = true;
                                    mapError.put("Anomalia", "true");

                                } else {
                                    List<Map<String, Object>> elementi = selectDateOnStruttureByIdAzienda.get(Integer.parseInt(appartenentiMap.get("id_casella").toString()));
                                    Map<String, ZonedDateTime> maxMin = maxMin(elementi);
                                    if (!controllaEstremi(maxMin.get("min"), maxMin.get("max"), formattattore(appartenentiMap.get("datain")), formattattore(appartenentiMap.get("datafi")))) {

                                        mapError.put("ERRORE", mapError.get("ERRORE") + " non rispetta l'arco temporale della struttura, ");
                                        anomalia = true;
                                        mapError.put("Anomalia", "true");
                                    }
//                                    LocalDateTime dataMax = elementi.stream().map(u -> formattattore(u.get("datafi"))).max(LocalDateTime::compareTo).get();
//                                    LocalDateTime dataMax = LocalDateTime.MIN;
//
//                                    for (Map<String, Object> e : elementi) {
//                                        LocalDateTime dataFineElemento = formattattore(e.get("datafi"));
//                                        if (dataFineElemento == null) {
//                                            dataMax = LocalDateTime.MAX;
//                                            break;
//                                        }
//
//                                        if (dataFineElemento.compareTo(dataMax) > 0) {
//                                            dataMax = dataFineElemento;
//                                        }
//                                    }
//                                    LocalDateTime dataFineUtente = formattattore(appartenentiMap.get("datafi"));
//                                    if (dataFineUtente == null) {
//                                        dataFineUtente = LocalDateTime.MAX;
//                                    }
//                                    if (dataMax.compareTo(dataFineUtente) < 0) {
//                                        mapError.put("ERRORE", mapError.get("ERRORE") + " non rispetta l'arco temporale della struttura,");
//                                        anomalia = true;
//                                        mapError.put("Anomalia", "true");
//                                    }
                                }
                            }
                            mapError.put("id_casella", appartenentiMap.get("id_casella"));
                            idCasella = appartenentiMap.get("id_casella").toString();
//                            mA.setIdCasella(Integer.parseInt(idCasella));

                        }
//                      DATAIN bloccante
                        if (appartenentiMap.get("datain") == null || appartenentiMap.get("datain").toString().trim().equals("") || appartenentiMap.get("datain") == "") {
                            mapError.put("ERRORE", mapError.get("ERRORE") + " datain,");
                            mapError.put("datain", "");
//                            mA.setDatain(null);
                            anomalia = true;
                            mapError.put("Anomalia", "true");

                        } else {
                            mapError.put("datain", appartenentiMap.get("datain"));
//                            mA.setDatain(formattattore(appartenentiMap.get("datain")));
                        }
                        ZonedDateTime datafi = null;
                        ZonedDateTime datain = null;
                        String datafiString = null;
                        String datainString = null;

                        if (appartenentiMap.get("datafi") != null && (!appartenentiMap.get("datafi").toString().trim().equals("") || appartenentiMap.get("datafi") != "")) {
                            datafi = formattattore(appartenentiMap.get("datafi"));
                            datafiString = UtilityFunctions.getZonedDateTimeString(datafi);
                        }

                        if (appartenentiMap.get("datain") != null && (!appartenentiMap.get("datain").toString().trim().equals("") || appartenentiMap.get("datain") != "")) {
                            datain = formattattore(appartenentiMap.get("datain"));
                            datainString = UtilityFunctions.getZonedDateTimeString(datain);
                        }
                        if (appartenentiMap.get("datafi") == null || appartenentiMap.get("datafi").toString().trim().equals("") || appartenentiMap.get("datafi") == "") {
                            mapError.put("datafi", "");
//                           mA.setDatafi(null);
                        } else {
                            mapError.put("datafi", appartenentiMap.get("datafi"));
//                            mA.setDatafi(formattattore(appartenentiMap.get("datafi")));
                        }
                        //Codice Ente 
                        String codiceEnte = "";
                        if (appartenentiMap.get("codice_ente") == null || appartenentiMap.get("codice_ente").toString().trim().equals("") || appartenentiMap.get("codice_ente") == "") {
                            mapError.put("codice_ente", codiceAzienda);
//                            mA.setCodiceEnte(codiceAzienda);
                            mapError.put("ERRORE", mapError.get("Errore") + "codice ente assente,");
                            anomalia = true;

                            mapError.put("Anomalia", "true");

                        } else {
                            mapError.put("codice_ente", appartenentiMap.get("codice_ente"));
                            codiceEnte = appartenentiMap.get("codice_ente").toString();
                            //90901 90904 909
//                           if (!appartenentiMap.get("codice_ente").toString().startsWith(codiceAzienda.toString())){
//                                mapError.put("ERRORE", mapError.get("Errore") + "codice ente errato,");
//                            }
//                            mA.setCodiceEnte(Integer.parseInt(appartenentiMap.get("codice_ente").toString()));
                        }
//                      TIPO_APPARTENENZA bloccante
                        if (appartenentiMap.get("tipo_appartenenza") == null || appartenentiMap.get("tipo_appartenenza").toString().trim().equals("") || appartenentiMap.get("tipo_appartenenza") == "") {
                            mapError.put("ERRORE", mapError.get("ERRORE") + " tipo_appartenenza,");
                            mapError.put("tipo_appartenenza", "");
//                            mA.setTipoAppartenenza(null);
                            anomalia = true;
                            mapError.put("Anomalia", "true");

                        } else {
                            mapError.put("tipo_appartenenza", appartenentiMap.get("tipo_appartenenza"));
//                            mA.setTipoAppartenenza(appartenentiMap.get("tipo_appartenenza").toString());
                            if (appartenentiMap.get("codice_ente") != null && !appartenentiMap.get("codice_ente").toString().trim().equals("") && appartenentiMap.get("codice_ente") != "") {
                                boolean codiceEnteEndsWith = codiceEnte.endsWith("01");
                                if (appartenentiMap.get("tipo_appartenenza").toString().trim().equalsIgnoreCase("T")) {
                                    Map<Integer, List<Map<String, Object>>> appDiretto = appartenentiDiretti.get(Integer.parseInt(appartenentiMap.get("codice_matricola").toString()));

                                    if (codiceEnteEndsWith && controlloZeroUno) {

                                        if (appDiretto == null) {
                                            //non ho quella matricola nella mappa
                                            //creo tutti i contenuti della matricola nuova
                                            appDiretto = new HashMap();
                                            List<Map<String, Object>> periodoCasellato = new ArrayList<>();
                                            Map<String, Object> periodoDaCasellare = new HashMap();
                                            Integer idCasellaInt = Integer.parseInt(idCasella);
                                            periodoDaCasellare.put("datain", appartenentiMap.get("datain"));
                                            periodoDaCasellare.put("datafi", appartenentiMap.get("datafi"));
                                            periodoDaCasellare.put("riga", mapReader.getLineNumber());
                                            periodoCasellato.add(periodoDaCasellare);
                                            appDiretto.put(idCasellaInt, periodoCasellato);
                                            appartenentiDiretti.put(Integer.parseInt(appartenentiMap.get("codice_matricola").toString()), appDiretto);
                                        } else {
                                            Boolean afferenzaDiretta = false;
                                            //l'appartenente c'è devo ciclare su tutte le strutture per verificare che non abbia piu afferenze dirette

                                            for (Map.Entry<Integer, List<Map<String, Object>>> listaCasella : appDiretto.entrySet()) {

                                                if (!afferenzaDiretta && arcoBool(listaCasella.getValue(), datain, datafi)) {
                                                    if (!righeAnomaleDirette.contains(mapReader.getLineNumber())) {
                                                        righeAnomaleDirette.add(mapReader.getLineNumber());
                                                    }
                                                    anomalia = true;
                                                    mapError.put("Anomalia", "true");
                                                    afferenzaDiretta = true;
                                                    List<Integer> righeAnomaleDaControllare = arco(listaCasella.getValue(), datain, datafi);
                                                    for (Integer rigaAnomala : righeAnomaleDaControllare) {
                                                        if (!righeAnomaleDirette.contains(rigaAnomala)) {
                                                            righeAnomaleDirette.add(rigaAnomala);
                                                        }
                                                    }
                                                }
                                            }

                                            //integer1 appartenenti, integer2 struttura, lista datain,datafi di appartenente in struttura.
                                            //Da modificare
                                            List<Map<String, Object>> periodoCasellato = appDiretto.get(Integer.parseInt(appartenentiMap.get("id_casella").toString()));
                                            if (periodoCasellato == null) {
                                                periodoCasellato = new ArrayList<>();
                                                Map<String, Object> periodoDaCasellare = new HashMap();
                                                periodoDaCasellare.put("datain", appartenentiMap.get("datain"));
                                                periodoDaCasellare.put("datafi", appartenentiMap.get("datafi"));
                                                periodoDaCasellare.put("riga", mapReader.getLineNumber());
                                                periodoCasellato.add(periodoDaCasellare);
                                                appDiretto.put(Integer.parseInt(appartenentiMap.get("id_casella").toString()), periodoCasellato);
                                            } else {
                                                if (!afferenzaDiretta && arcoBool(periodoCasellato, datain, datafi)) {
                                                    if (!righeAnomaleDirette.contains(mapReader.getLineNumber())) {
                                                        righeAnomaleDirette.add(mapReader.getLineNumber());
                                                    }
                                                    anomalia = true;
                                                    mapError.put("Anomalia", "true");
                                                    List<Integer> righeAnomaleDaControllare = arco(periodoCasellato, datain, datafi);
                                                    for (Integer rigaAnomala : righeAnomaleDaControllare) {
                                                        if (!righeAnomaleDirette.contains(rigaAnomala)) {
                                                            righeAnomaleDirette.add(rigaAnomala);
                                                        }
                                                    }
                                                }
                                                Map<String, Object> periodoDaCasellare = new HashMap();
                                                periodoDaCasellare.put("datain", appartenentiMap.get("datain"));
                                                periodoDaCasellare.put("datafi", appartenentiMap.get("datafi"));
                                                periodoDaCasellare.put("riga", mapReader.getLineNumber());
                                                periodoCasellato.add(periodoDaCasellare);
                                            }
                                        }
                                    }
                                    //cazzo di Ferrarra di merda
                                    if (!controlloZeroUno || !codiceEnteEndsWith) {
                                        if (appDiretto == null) {
                                            //non ho quella matricola nella mappa
                                            //creo tutti i contenuti della matricola nuova
                                            appDiretto = new HashMap();
                                            List<Map<String, Object>> periodoCasellato = new ArrayList<>();
                                            Map<String, Object> periodoDaCasellare = new HashMap();
                                            Integer idCasellaInt = Integer.parseInt(idCasella);
                                            periodoDaCasellare.put("datain", appartenentiMap.get("datain"));
                                            periodoDaCasellare.put("datafi", appartenentiMap.get("datafi"));
                                            periodoDaCasellare.put("riga", mapReader.getLineNumber());
                                            periodoCasellato.add(periodoDaCasellare);
                                            appDiretto.put(idCasellaInt, periodoCasellato);
                                            appartenentiDiretti.put(Integer.parseInt(appartenentiMap.get("codice_matricola").toString()), appDiretto);
                                        } else {
                                            Boolean afferenzaDiretta = false;

                                            List<Map<String, Object>> periodoCasellato = appDiretto.get(Integer.parseInt(appartenentiMap.get("id_casella").toString()));
                                            if (periodoCasellato == null) {
                                                periodoCasellato = new ArrayList<>();
                                                Map<String, Object> periodoDaCasellare = new HashMap();
                                                periodoDaCasellare.put("datain", appartenentiMap.get("datain"));
                                                periodoDaCasellare.put("datafi", appartenentiMap.get("datafi"));
                                                periodoDaCasellare.put("riga", mapReader.getLineNumber());
                                                periodoCasellato.add(periodoDaCasellare);
                                                appDiretto.put(Integer.parseInt(appartenentiMap.get("id_casella").toString()), periodoCasellato);
                                            } else {

                                                Map<String, Object> periodoDaCasellare = new HashMap();
                                                periodoDaCasellare.put("datain", appartenentiMap.get("datain"));
                                                periodoDaCasellare.put("datafi", appartenentiMap.get("datafi"));
                                                periodoDaCasellare.put("riga", mapReader.getLineNumber());
                                                periodoCasellato.add(periodoDaCasellare);
                                            }
                                        }
                                    }
                                } else {
                                    Map<Integer, List<Map<String, Object>>> appFunzionale = appartenentiFunzionali.get(Integer.parseInt(appartenentiMap.get("codice_matricola").toString()));
                                    if (appFunzionale == null) {
                                        //non ho quella matricola nella mappa
                                        //adda crea tutto
                                        appFunzionale = new HashMap();
                                        List<Map<String, Object>> periodoCasellato = new ArrayList<>();
                                        appFunzionale.put(Integer.parseInt(appartenentiMap.get("id_casella").toString()), periodoCasellato);
                                        Map<String, Object> periodoDaCasellare = new HashMap();
                                        periodoDaCasellare.put("datain", appartenentiMap.get("datain"));
                                        periodoDaCasellare.put("datafi", appartenentiMap.get("datafi"));
                                        periodoDaCasellare.put("riga", mapReader.getLineNumber());
                                        periodoCasellato.add(periodoDaCasellare);
                                        appartenentiFunzionali.put(Integer.parseInt(appartenentiMap.get("codice_matricola").toString()), appFunzionale);
                                    } else {
                                        List<Map<String, Object>> periodoCasellato = appFunzionale.get(Integer.parseInt(appartenentiMap.get("id_casella").toString()));
                                        if (periodoCasellato == null) {
                                            periodoCasellato = new ArrayList<>();
                                            Map<String, Object> periodoDaCasellare = new HashMap();
                                            periodoDaCasellare.put("datain", appartenentiMap.get("datain"));
                                            periodoDaCasellare.put("datafi", appartenentiMap.get("datafi"));
                                            periodoDaCasellare.put("riga", mapReader.getLineNumber());
                                            periodoCasellato.add(periodoDaCasellare);
                                            appFunzionale.put(Integer.parseInt(appartenentiMap.get("id_casella").toString()), periodoCasellato);
                                        } else {

                                            if (arcoBool(periodoCasellato, datain, datafi)) {
                                                anomalia = true;
                                                mapError.put("Anomalia", "true");

                                                if (!righeAnomaleFunzionali.contains(mapReader.getLineNumber())) {
                                                    righeAnomaleFunzionali.add(mapReader.getLineNumber());
                                                }
                                                List<Integer> righeAnomaleDaControllare = arco(periodoCasellato, datain, datafi);
                                                for (Integer rigaAnomala : righeAnomaleDaControllare) {
                                                    if (!righeAnomaleFunzionali.contains(rigaAnomala)) {
                                                        righeAnomaleFunzionali.add(rigaAnomala);
                                                    }
                                                }

                                            }
                                            Map<String, Object> periodoDaCasellare = new HashMap();
                                            periodoDaCasellare.put("datain", appartenentiMap.get("datain"));
                                            periodoDaCasellare.put("datafi", appartenentiMap.get("datafi"));
                                            periodoDaCasellare.put("riga", mapReader.getLineNumber());
                                            periodoCasellato.add(periodoDaCasellare);
                                        }
                                    }

                                }
                            }
                        }

//                      DataAssunzione bloccante
                        if (appartenentiMap.get("data_assunzione") == null || appartenentiMap.get("data_assunzione").toString().trim().equals("") || appartenentiMap.get("data_assunzione") == "") {
                            mapError.put("ERRORE", mapError.get("ERRORE") + " data_assunzione,");
                            mapError.put("data_assunzione", "");
                            anomalia = true;
                            mapError.put("Anomalia", "true");

//                            mA.setDataAssunzione(null);
                        } else {
                            mapError.put("data_assunzione", appartenentiMap.get("data_assunzione"));
//                            mA.setDataAssunzione(formattattore(appartenentiMap.get("data_assunzione")));
                        }
//                      USERNAME
                        if (appartenentiMap.get("username") == null || appartenentiMap.get("username").toString().trim().equals("") || appartenentiMap.get("username") == "") {
                            mA.setUsername("");
                            mapError.put("username", "");

                        } else {
                            mapError.put("username", appartenentiMap.get("username"));
//                            mA.setUsername(appartenentiMap.get("username").toString());
                        }
//                      DATA_DIMISSIONE
                        if (appartenentiMap.get("data_dimissione") == null || appartenentiMap.get("data_dimissione").toString().trim().equals("") || appartenentiMap.get("data_dimissione") == "") {
                            mapError.put("data_dimissione", "");
//                            mA.setDataDimissione(null);
                        } else {
                            mapError.put("data_dimissione", appartenentiMap.get("data_dimissione"));
                            mA.setDataDimissione(formattattore(appartenentiMap.get("data_dimissione")));
                        }

//                        mA.setIdAzienda(azienda);
                        listAppartenentiMap.add(mapError);
                        nRigheCSV = mapReader.getRowNumber();
//                        if (!anomalia){ em.persist(mA); }
//                        mapError.remove("Anomalia");
//                       mapWriter.write(mapError, headersErrorGenerator(tipo), getProcessorsError(tipo, codiceAzienda));

                    }

                    //se ho il caso in cui non  ho appartenenti diretti per qualche appatenente funzionale
                    List<Integer> codiciMatricoleConAppFunzionaliENonDirette = new ArrayList<>();
                    for (Integer codiceMatricola : appartenentiFunzionali.keySet()) {
                        if (!appartenentiDiretti.containsKey(codiceMatricola)) {
                            codiciMatricoleConAppFunzionaliENonDirette.add(codiceMatricola);
                        }
                    }
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
                        //DA CHIEDERE A GUS
                        if (righeAnomaleFunzionali.contains(riga)) {
                            appMapWithErrorAndAnomalia.put("ERRORE", appMapWithErrorAndAnomalia.get("ERRORE") + " appartenente con piu afferenze funzionali per lo stesso periodo e nella stessa struttura");
                            nRigheAnomale++;
                            anomalia = true;
                            appMapWithErrorAndAnomalia.put("Anomalia", "true");
                        }
                        if (!appMapWithErrorAndAnomalia.get("Anomalia").toString().equalsIgnoreCase("true")) {
//                            log.info("tutto ok sulla riga: " + riga);
                            MdrAppartenenti mA = new MdrAppartenenti();
                            mA.setIdAzienda(azienda);
//                      "codice_ente",
                            mA.setCodiceEnte(!appMapWithErrorAndAnomalia.get("codice_ente").toString().equals("") ? Integer.parseInt(appMapWithErrorAndAnomalia.get("codice_ente").toString()) : null);
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
                            mA.setDatain(!appMapWithErrorAndAnomalia.get("datain").toString().equals("") ? formattattore(appMapWithErrorAndAnomalia.get("datain")) : null);
//                      "datafi",
                            mA.setDatafi(!appMapWithErrorAndAnomalia.get("datafi").toString().equals("") ? formattattore(appMapWithErrorAndAnomalia.get("datafi")) : null);
//                      "tipo_appartenenza",
                            mA.setTipoAppartenenza(!appMapWithErrorAndAnomalia.get("tipo_appartenenza").toString().equals("") ? appMapWithErrorAndAnomalia.get("tipo_appartenenza").toString() : null);
//                      "username",
                            mA.setUsername(!appMapWithErrorAndAnomalia.get("username").toString().equals("") ? appMapWithErrorAndAnomalia.get("username").toString() : null);
//                      "data_assunzione",
                            mA.setDataAssunzione(!appMapWithErrorAndAnomalia.get("data_assunzione").toString().equals("") ? formattattore(appMapWithErrorAndAnomalia.get("data_assunzione")) : null);
//                      "data_dimissione"
                            mA.setDataDimissione(!appMapWithErrorAndAnomalia.get("data_dimissione").toString().equals("") ? formattattore(appMapWithErrorAndAnomalia.get("data_dimissione")) : null);

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
                    LocalDateTime fine = LocalDateTime.now();
                    log.info("ora fine: " + LocalDateTime.now());

                    break;

                case "RESPONSABILI":
                    parameters = parametriAziende.getParameters("tolleranzaResponsabili", new Integer[]{idAzienda}, new String[]{Applicazione.Applicazioni.ribaltorg.toString()});
                    if (parameters != null && !parameters.isEmpty()) {
                        tolleranza = parametriAziende.getValue(parameters.get(0), Integer.class);
                    }
                    nRigheDB = mdrResponsabiliRepository.countRow(idAzienda);
                    Boolean anomaliaRiga = false;
                    // Delete delle righe da sostituire
                    predicateAzienda = QMdrResponsabili.mdrResponsabili.idAzienda.id.eq(idAzienda);
                    mdrResponsabiliRepository.deleteByIdAzienda(idAzienda);
                    //Reading with CsvMapReader
                    Map<String, Object> responsabiliMap = null;
                    while ((responsabiliMap = mapReader.read(headers, processors)) != null) {
//                      preparo mappa di errore
                        mapError.put("ERRORE", "");
                        // Inserisco la riga
                        MdrResponsabili mR = new MdrResponsabili();
//                      inizio a settare i dati
//                      CODICE_ENTE preso da interfaccia

//                      CODICE_MATRICOLA bloccante
                        String codice_matricola = null;
                        if (responsabiliMap.get("codice_matricola") == null || responsabiliMap.get("codice_matricola").toString().trim().equals("") || responsabiliMap.get("codice_matricola") == "") {
                            mapError.put("ERRORE", mapError.get("ERRORE") + " codice_matricola,");
                            mapError.put("codice_matricola", "");
                            codice_matricola = "";
                            mR.setCodiceMatricola(null);
                            nRigheAnomale++;
                            anomalia = true;
                            anomaliaRiga = true;
                        } else {
                            mapError.put("codice_matricola", responsabiliMap.get("codice_matricola"));
                            codice_matricola = responsabiliMap.get("codice_matricola").toString();
                            mR.setCodiceMatricola(Integer.parseInt(responsabiliMap.get("codice_matricola").toString()));
                            //responsabile presente tra gli autenti
                            if (mdrAppartenentiRepository.countUsertByCodiceMatricola(Integer.parseInt(responsabiliMap.get("codice_matricola").toString())) <= 0) {
                                mapError.put("ERRORE", mapError.get("ERRORE") + " codice_matricola non trovata nella tabella appartenenti,");
                                nRigheAnomale++;
                                anomalia = true;
                                anomaliaRiga = true;
                            }
                        }

//                      DATAIN bloccante
                        if (responsabiliMap.get("datain") == null || responsabiliMap.get("datain").toString().trim().equals("") || responsabiliMap.get("datain") == "") {
                            mapError.put("ERRORE", mapError.get("ERRORE") + " datain non presente,");
                            nRigheAnomale++;
                            anomalia = true;
                            anomaliaRiga = true;
                            mapError.put("datain", "");
                            mR.setDatain(null);
                        } else {
                            mapError.put("datain", responsabiliMap.get("datain"));
                            mR.setDatain(formattattore(responsabiliMap.get("datain")));
                        }
                        ZonedDateTime datafi = null;
                        ZonedDateTime datain = null;
                        String datafiString = null;
                        String datainString = null;
                        if (responsabiliMap.get("datafi") != null && (!responsabiliMap.get("datafi").toString().trim().equals("") || responsabiliMap.get("datafi") == "")) {
                            datafi = formattattore(responsabiliMap.get("datafi"));
                            datafiString = UtilityFunctions.getZonedDateTimeString(datafi);
                        }

                        if (responsabiliMap.get("datain") != null && (!responsabiliMap.get("datain").toString().trim().equals("") || responsabiliMap.get("datain") == "")) {
                            datain = formattattore(responsabiliMap.get("datain"));
                            datainString = UtilityFunctions.getZonedDateTimeString(datain);
                        }

//                      ID_CASELLA bloccante
                        String id_casella = null;
                        if (responsabiliMap.get("id_casella") == null || responsabiliMap.get("id_casella").toString().trim().equals("") || responsabiliMap.get("id_casella") == "") {
                            mapError.put("ERRORE", mapError.get("ERRORE") + " id_casella,");
                            id_casella = "";
                            mapError.put("id_casella", "");
                            mR.setIdCasella(null);
                            nRigheAnomale++;
                            anomalia = true;
                            anomaliaRiga = true;
                        } else {
                            mapError.put("id_casella", responsabiliMap.get("id_casella"));
                            id_casella = responsabiliMap.get("id_casella").toString();
                            mR.setIdCasella(Integer.parseInt(responsabiliMap.get("id_casella").toString()));

                            if (mdrStrutturaRepository.selectStrutturaUtenteByIdCasellaAndIdAzienda(Integer.parseInt(responsabiliMap.get("id_casella").toString()), idAzienda) <= 0) {
                                mapError.put("ERRORE", mapError.get("ERRORE") + " id_casella non trovata nella tabella strutture,");
                                nRigheAnomale++;
                                anomalia = true;
                                anomaliaRiga = true;
                            } else {
                                List<Map<String, Object>> mieiPadri = mdrStrutturaRepository.mieiPadri(idAzienda, Integer.parseInt(responsabiliMap.get("id_casella").toString()));
                                if (responsabiliMap.get("datain") != null && !responsabiliMap.get("datain").toString().trim().equals("") && responsabiliMap.get("datain") != "") {
                                    if (!arcoBool(mieiPadri, formattattore(responsabiliMap.get("datain")), formattattore(responsabiliMap.get("datafi")))) {
                                        mapError.put("ERRORE", mapError.get("ERRORE") + " id_casella non valida per periodo temporale,");
                                        nRigheAnomale++;
                                        anomalia = true;
                                        anomaliaRiga = true;

                                    } else {
                                        if (!controllaEstremi(formattattore(mieiPadri.get(0).get("datain")), formattattore(mieiPadri.get(mieiPadri.size() - 1).get("datafi")), formattattore(responsabiliMap.get("datain")), formattattore(responsabiliMap.get("datafi")))) {
                                            mapError.put("ERRORE", mapError.get("ERRORE") + " non rispetta l'arco temporale della struttura,");
                                            nRigheAnomale++;
                                            anomalia = true;
                                            anomaliaRiga = true;
                                            mapError.put("Anomalia", "true");
                                        }
                                    }
                                }
                            }
                        }
//
                        if (mdrResponsabiliRepository.countMultiReponsabilePerStruttura(codiceAzienda,
                                Integer.parseInt(id_casella),
                                datafiString,
                                datainString) > 0) {
                            nRigheAnomale++;
                            anomalia = true;
                            mapError.put("ERRORE", mapError.get("ERRORE") + " la struttura di questo responsabile è già assegnata ad un altro respondabile,");
                        }
//                      DATAFI non bloccante
                        if (responsabiliMap.get("datafi") == null || responsabiliMap.get("datafi").toString().trim().equals("") || responsabiliMap.get("datafi") == "") {
                            mapError.put("datafi", "");
                            mR.setDatafi(null);
                        } else {
                            mapError.put("datafi", responsabiliMap.get("datafi"));
                            mR.setDatafi(formattattore(responsabiliMap.get("datafi")));
                        }
//                      TIPO bloccante
                        if (responsabiliMap.get("tipo") == null || responsabiliMap.get("tipo").toString().trim().equals("") || responsabiliMap.get("tipo") == "") {
                            mapError.put("ERRORE", mapError.get("ERRORE") + " tipo,");
                            mR.setTipo(null);
                            nRigheAnomale++;
                            anomalia = true;
                            anomaliaRiga = true;
                        } else {
                            mapError.put("tipo", responsabiliMap.get("tipo"));
                            mR.setTipo(responsabiliMap.get("tipo").toString());
                        }

                        if (responsabiliMap.get("codice_ente") == null || responsabiliMap.get("codice_ente").toString().trim().equals("") || responsabiliMap.get("codice_ente") == "") {
                            mapError.put("codice_ente", codiceAzienda);
                            mR.setCodiceEnte(codiceAzienda);
                            mapError.put("ERRORE", mapError.get("ERRORE") + " codice ente assente,");
                            nRigheAnomale++;
                            anomalia = true;
                            anomaliaRiga = true;

                        } else {
                            mapError.put("codice_ente", responsabiliMap.get("codice_ente"));
                            mR.setCodiceEnte(Integer.parseInt(responsabiliMap.get("codice_ente").toString()));
                        }

                        mR.setIdAzienda(azienda);
                        if (!anomaliaRiga) {
                            mdrResponsabiliRepository.save(mR);
                        }
                        anomaliaRiga = false;
                        mapWriter.write(mapError, headersErrorGenerator(tipo), getProcessorsError(tipo, codiceAzienda));
                        nRigheCSV = mapReader.getRowNumber();
                    }
                    break;

                case "STRUTTURA":
                    parameters = parametriAziende.getParameters("tolleranzaResponsabili", new Integer[]{idAzienda}, new String[]{Applicazione.Applicazioni.ribaltorg.toString()});
                    if (parameters != null && !parameters.isEmpty()) {
                        tolleranza = parametriAziende.getValue(parameters.get(0), Integer.class);
                    }
                    nRigheDB = mdrStrutturaRepository.countRow(idAzienda);
                    bloccante = false;
                    // Delete delle righe da sostituire
                    predicateAzienda = QMdrStruttura.mdrStruttura.idAzienda.id.eq(idAzienda);
                    mdrStrutturaRepository.deleteByIdAzienda(idAzienda);
                    // Reading with CsvMapReader
                    Map<String, Object> strutturaMap = null;
                    while ((strutturaMap = mapReader.read(headers, processors)) != null) {
//                      inizio a creare la mappa degli errori e
                        mapError.put("ERRORE", "");
                        // Inserisco la riga
                        MdrStruttura mS = new MdrStruttura();
                        ZonedDateTime datafi = null;
                        ZonedDateTime datain = null;
                        String datafiString = null;
                        String datainString = null;

                        if (strutturaMap.get("datain") != null && (!strutturaMap.get("datain").toString().trim().equals("") || strutturaMap.get("datain") != "")) {
                            datain = formattattore(strutturaMap.get("datain"));
                            datainString = UtilityFunctions.getZonedDateTimeString(datain);
                        }

                        if (strutturaMap.get("datain") == null || strutturaMap.get("datain").toString().trim().equals("") || strutturaMap.get("datain") == "") {
                            mapError.put("ERRORE", mapError.get("ERRORE") + " datain,");
                            mapError.put("datain", "");
                            mS.setDatain(null);
                            bloccante = true;
                            log.error("Importa CSV --Struttura-- errore alla righa:" + mapReader.getLineNumber() + " Errore bloccante su data inizio vuota");
                        } else {
                            mapError.put("datain", strutturaMap.get("datain"));
                            mS.setDatain(datain);
                        }

                        if (strutturaMap.get("datafi") != null && (!strutturaMap.get("datafi").toString().trim().equals("") || strutturaMap.get("datafi") != "")) {
                            datafi = formattattore(strutturaMap.get("datafi"));
                            datafiString = UtilityFunctions.getZonedDateTimeString(datafi);
                        }

                        if (strutturaMap.get("datafi") == null
                                || strutturaMap.get("datafi").toString().trim().equals("")
                                || strutturaMap.get("datafi") == ""
                                || strutturaMap.get("datafi").toString().trim().equals("3000-12-31")
                                || strutturaMap.get("datafi").toString().trim().equals("31/12/3000")) {
                            mapError.put("datafi", "");
                            mS.setDatafi(null);
                        } else {
                            mapError.put("datafi", strutturaMap.get("datafi"));
                            mS.setDatafi(datafi);
                        }

                        String id_casella = null;
                        if (strutturaMap.get("id_casella") == null || strutturaMap.get("id_casella").toString().trim().equals("")) {
                            mapError.put("ERRORE", mapError.get("ERRORE") + " id_casella assente,");
                            bloccante = true;
                            log.error("Importa CSV --Struttura-- errore alla righa:" + mapReader.getLineNumber() + " idCasella vuota");
                            id_casella = "";
                            mapError.put("id_casella", "");
                            mS.setIdCasella(null);
                        } else {
                            mapError.put("id_casella", strutturaMap.get("id_casella"));
                            id_casella = strutturaMap.get("id_casella").toString();
                            mS.setIdCasella(Integer.parseInt(strutturaMap.get("id_casella").toString()));
                            //struttura definita piu volte nello stesso arco temporale
                            if (mdrStrutturaRepository.selectMultiDefinictionsStructureByIdAzienda(idAzienda, Integer.parseInt(id_casella), datafiString, datainString) > 0) {
                                bloccante = true;
                                log.error("Importa CSV --Struttura-- errore alla righa:" + mapReader.getLineNumber() + " idCasella definita piu volte");
                                mapError.put("ERRORE", mapError.get("ERRORE") + " struttura definita piu volte nello stesso arco temporale,");
                            }
                        }

                        if (strutturaMap.get("id_padre") == null || strutturaMap.get("id_padre").toString().trim().equals("") || strutturaMap.get("id_padre") == "") {
                            mapError.put("id_padre", "");
                            mS.setIdPadre(null);
                        } else {
                            mapError.put("id_padre", strutturaMap.get("id_padre"));
                            mS.setIdPadre(Integer.parseInt(strutturaMap.get("id_padre").toString()));
                        }

                        if (strutturaMap.get("descrizione") == null || strutturaMap.get("descrizione").toString().trim().equals("") || strutturaMap.get("descrizione") == "") {
                            mapError.put("ERRORE", mapError.get("ERRORE") + " descrizione assente,");
                            mapError.put("descrizione", "");
                            mS.setDescrizione(null);
                            bloccante = true;
                            log.error("Importa CSV --Struttura-- errore alla righa:" + mapReader.getLineNumber() + " descrizione vuota");
                        } else {
                            mapError.put("descrizione", strutturaMap.get("descrizione"));
                            mS.setDescrizione(strutturaMap.get("descrizione").toString());
                        }

                        if (strutturaMap.get("tipo_legame") == null || strutturaMap.get("tipo_legame").toString().trim().equals("") || strutturaMap.get("tipo_legame") == "") {
                            mapError.put("tipo_legame", "");
                            mS.setTipoLegame(null);
//                            nRigheAnomale++;
//                            anomalia = true;
//                            mapError.put("ERRORE", mapError.get("ERRORE") + " tipo_legame assente,");
                        } else {
                            mapError.put("tipo_legame", strutturaMap.get("tipo_legame"));
                            mS.setTipoLegame(strutturaMap.get("tipo_legame").toString());
                        }

                        if (strutturaMap.get("codice_ente") == null || strutturaMap.get("codice_ente").toString().trim().equals("") || strutturaMap.get("codice_ente") == "") {
                            mapError.put("codice_ente", codiceAzienda);
                            mS.setCodiceEnte(codiceAzienda);
                            nRigheAnomale++;
                            anomalia = true;
                            mapError.put("ERRORE", mapError.get("ERRORE") + " Codice Ente assente,");

                        } else {
                            mapError.put("codice_ente", strutturaMap.get("codice_ente"));
                            mS.setCodiceEnte(Integer.parseInt(strutturaMap.get("codice_ente").toString()));
                        }
                        mS.setIdAzienda(azienda);
                        em.persist(mS);
                        //mdrStrutturaRepository.save(mS);
                        mapWriter.write(mapError, headersErrorGenerator(tipo), getProcessorsError(tipo, codiceAzienda));
                        nRigheCSV = mapReader.getRowNumber();
                    }

                    //struttura padre non trovata
                    List<Integer> listaStrutture = mdrStrutturaRepository.listaStrutture(idAzienda);

                    mapWriter.close();
                    mapReader.close();

                    try (InputStreamReader csvErrorFileRIP = new InputStreamReader(new FileInputStream(csvErrorFile));) {

                        mapErrorReader = new CsvMapReader(csvErrorFileRIP, SEMICOLON_DELIMITED);
                        mapErrorReader.getHeader(true);

                        mapErrorWriter = new CsvMapWriter(new FileWriter(csvErrorFile2), SEMICOLON_DELIMITED);
                        mapErrorWriter.writeHeader(headersErrorGenerator(tipo));
                        Integer i = 0;
                        Map<String, Object> strutturaErrorMap;
                        while ((strutturaErrorMap = mapErrorReader.read(headersErrorGenerator(tipo), getProcessorsError(tipo, codiceAzienda))) != null) {
                            Map<String, Object> strutturaErrorMapWrite = new HashMap();

                            strutturaErrorMapWrite.putAll(strutturaErrorMap);
                            if (strutturaErrorMap.get("id_padre") != null && strutturaErrorMap.get("id_padre") != "" && !strutturaErrorMap.get("id_padre").equals("0")) {
                                //System.out.println("contatore" + (i++).toString());
                                if (!listaStrutture.contains(Integer.parseInt(strutturaErrorMap.get("id_padre").toString()))) {
                                    bloccante = true;
                                    log.error("Importa CSV --Struttura-- errore alla righa:" + mapReader.getLineNumber() + " descrizione vuota");
                                    strutturaErrorMapWrite.put("ERRORE", strutturaErrorMap.get("ERRORE") + " padre non presente,");
                                }
                                List<Map<String, Object>> elementi = mdrStrutturaRepository.mieiPadri(idAzienda, Integer.parseInt(strutturaErrorMap.get("id_padre").toString()));

                                if (!arcoBool(elementi, formattattore(strutturaErrorMap.get("datain")), formattattore(strutturaErrorMap.get("datafi")))) {
                                    bloccante = true;
                                    log.error("Importa CSV --Struttura-- errore alla righa:" + mapReader.getLineNumber() + " non rispetta l'arco temporale del padre");
                                    if (strutturaErrorMap.get("ERRORE")!=null){
                                        strutturaErrorMapWrite.put("ERRORE", strutturaErrorMap.get("ERRORE") + " non rispetta l'arco temporale del padre,");
                                    }else {
                                        strutturaErrorMapWrite.put("ERRORE", " non rispetta l'arco temporale del padre,");
                                    }
                                }
                            }

                            mapErrorWriter.write(strutturaErrorMapWrite, headersErrorGenerator(tipo), getProcessorsError(tipo, codiceAzienda));

                        }
//                        csvErrorFile.deleteOnExit();
//                        csvErrorFile2.deleteOnExit();
                    } catch (Exception ex) {
                        bloccante = true;
                        log.error("Importa CSV -- error generic");
                        System.out.println("ex:" + ex);
                    }

                    break;

                case "TRASFORMAZIONI":
                    nRigheDB = mdrTrasformazioniRepository.countRow(idAzienda);
                    parameters = parametriAziende.getParameters("tolleranzaResponsabili", new Integer[]{idAzienda}, new String[]{Applicazione.Applicazioni.ribaltorg.toString()});
                    if (parameters != null && !parameters.isEmpty()) {
                        tolleranza = parametriAziende.getValue(parameters.get(0), Integer.class);
                    }
                    //TODO per ottimizzazioni successive decommentare riga successiva
                    //Map<Integer, List<Map<String, Object>>> selectDateOnStruttureByIdAzienda1 = mdrStrutturaRepository.selectDateOnStruttureByIdAzienda(idAzienda);
                    // Delete delle righe da sostituire
                    predicateAzienda = QMdrTrasformazioni.mdrTrasformazioni.idAzienda.id.eq(idAzienda);
                    mdrTrasformazioniRepository.deleteByIdAzienda(idAzienda);

                    //Reading with CsvMapReader
                    Map<String, Object> trasformazioniMap;
                    while ((trasformazioniMap = mapReader.read(headers, processors)) != null) {

                        Boolean tempi_ok = true;
                        Boolean dataTrasformazione = true;
                        Boolean dataInPartenza = true;
                        // Inserisco la riga
                        MdrTrasformazioni mT = new MdrTrasformazioni();
                        mapError.put("ERRORE", "");
                        //PROGRESSIVO RIGA
                        if (trasformazioniMap.get("progressivo_riga") == null || trasformazioniMap.get("progressivo_riga").toString().trim().equals("") || trasformazioniMap.get("progressivo_riga") == "") {
                            mapError.put("ERRORE", mapError.get("ERRORE") + " progressivo_riga,");
                            mapError.put("progressivo_riga", "");
                            mT.setProgressivoRiga(null);
                            log.error("Importa CSV --Trasformazioni-- errore alla righa:" + mapReader.getLineNumber() + " progressivo_riga assente");
                            bloccante = true;
                        } else {
                            mapError.put("progressivo_riga", trasformazioniMap.get("progressivo_riga"));
                            mT.setProgressivoRiga(Integer.parseInt(trasformazioniMap.get("progressivo_riga").toString()));
                        }

//                      DATA TRASFORMAZIONE DEVE ESISTERE SEMPRE
                        if (trasformazioniMap.get("data_trasformazione") == null || trasformazioniMap.get("data_trasformazione").toString().trim().equals("") || trasformazioniMap.get("data_trasformazione") == "") {
                            mapError.put("ERRORE", mapError.get("ERRORE") + " data_trasformazione assente,");
                            mapError.put("data_trasformazione", "");
                            bloccante = true;
                            log.error("Importa CSV --Trasformazioni-- errore alla righa:" + mapReader.getLineNumber() + " data_trasformazione assente");
                            dataTrasformazione = false;
                            mT.setDataTrasformazione(null);
                        } else {
                            mapError.put("data_trasformazione", trasformazioniMap.get("data_trasformazione"));
                            mT.setDataTrasformazione(formattattore(trasformazioniMap.get("data_trasformazione")));
                        }
//                       DATA IN PARTENZA DEVE ESISTERE SEMPRE
//                       PER MOTIVO DI "X", "T","R" E "U" è LA DATA INIZIO DELLA CASELLA DI PARTENZA
//                      AGGIUNGERE BOOLEANO TEMPI_CASELLA_OK

                        if (trasformazioniMap.get("datain_partenza") == null || trasformazioniMap.get("datain_partenza").toString().trim().equals("") || trasformazioniMap.get("datain_partenza") == "") {
                            mapError.put("ERRORE", mapError.get("ERRORE") + " datain_partenza assente,");
                            mapError.put("datain_partenza", "");
                            mT.setDatainPartenza(null);
                            bloccante = true;
                            log.error("Importa CSV --Trasformazioni-- errore alla righa:" + mapReader.getLineNumber() + " datain_partenza assente");
                            dataInPartenza = false;
                        } else {
                            mapError.put("datain_partenza", trasformazioniMap.get("datain_partenza"));
                            mT.setDatainPartenza(formattattore(trasformazioniMap.get("datain_partenza")));
                        }

                        //ID CASELLA DI PARTENZA
                        //SEMPRE SPENTO IL GIORNO PRIMA DELLA DATA DI TRASFORMAZIONE
                        //DI CONSEGUENZA DEVE ESISTERE
                        if (trasformazioniMap.get("id_casella_partenza") == null || trasformazioniMap.get("id_casella_partenza").toString().trim().equals("") || trasformazioniMap.get("id_casella_partenza") == "") {
                            mapError.put("ERRORE", mapError.get("ERRORE") + " id_casella_partenza,");
                            mapError.put("id_casella_partenza", "");
                            mT.setIdCasellaPartenza(null);
                            bloccante = true;
                            log.error("Importa CSV --Trasformazioni-- errore alla righa:" + mapReader.getLineNumber() + " id_casella_partenza assente");
                        } else {
                            mapError.put("id_casella_partenza", trasformazioniMap.get("id_casella_partenza"));
                            mT.setIdCasellaPartenza(Integer.parseInt(trasformazioniMap.get("id_casella_partenza").toString()));
//                          DA AGGIUNGERE CONTOLLO DETTO SOPRA
//                          controllo che non ci sia un blocco precedente perche potrei non avere la data quindi la chiamata successiva potrebbe dare errore
                            if (dataInPartenza && dataTrasformazione) {
                                Integer spentaAccesaBeneByIdAzienda = mdrTrasformazioniRepository.isSpentaAccesaBeneByIdAzienda(idAzienda, Integer.parseInt(trasformazioniMap.get("id_casella_partenza").toString()), UtilityFunctions.getLocalDateString(formattattore(trasformazioniMap.get("data_trasformazione").toString()).toLocalDate()), UtilityFunctions.getLocalDateString(formattattore(trasformazioniMap.get("datain_partenza").toString()).toLocalDate()));
                                if (spentaAccesaBeneByIdAzienda != 1) {
                                    log.error("Importa CSV --Trasformazioni-- errore alla righa:" + mapReader.getLineNumber() + " periodi temporali della casella di partenza non sono validi");
                                    bloccante = true;
                                    tempi_ok = false;
                                    mapError.put("ERRORE", mapError.get("ERRORE") + " periodi temporali della casella di partenza non sono validi,");
                                }
                            }
                        }

//                      DATA ORA OPERAZIONE
                        if (trasformazioniMap.get("dataora_oper") == null || trasformazioniMap.get("dataora_oper").toString().trim().equals("")) {
                            mapError.put("ERRORE", mapError.get("ERRORE") + " DATAORA_OPER inserito automaticamente,");
                            ZonedDateTime now = ZonedDateTime.now();
                            mapError.put("dataora_oper", now.toString());
                            mT.setDataoraOper(now);
                            nRigheAnomale++;
                            anomalia = true;
                        } else {
                            mapError.put("dataora_oper", trasformazioniMap.get("dataora_oper"));
                            mT.setDataoraOper(formattattore(trasformazioniMap.get("dataora_oper")));
                        }
//                      CODICE ENTE
                        if (trasformazioniMap.get("codice_ente") == null || trasformazioniMap.get("codice_ente").toString().trim().equals("") || trasformazioniMap.get("codice_ente") == "") {
                            mapError.put("codice_ente", codiceAzienda);
                            mT.setCodiceEnte(codiceAzienda);
                            mapError.put("ERRORE", mapError.get("ERRORE") + "codice ente non presente");
                            nRigheAnomale++;
                            anomalia = true;
                        } else {
                            mapError.put("codice_ente", trasformazioniMap.get("codice_ente"));
                            mT.setCodiceEnte(Integer.parseInt(trasformazioniMap.get("codice_ente").toString()));
                        }

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
                                            Integer accesaIntervalloByIdAzienda = mdrTrasformazioniRepository.isAccesaIntervalloByIdAzienda(idAzienda, Integer.parseInt(trasformazioniMap.get("id_casella_arrivo").toString()), formattattore(trasformazioniMap.get("data_trasformazione")));
                                            if (accesaIntervalloByIdAzienda != 1) {
                                                bloccante = true;
                                                log.error("Importa CSV --Trasformazioni-- errore alla righa:" + mapReader.getLineNumber() + " casella di arrivo non valida nella data di trasformazione");
                                                mapError.put("ERRORE", mapError.get("ERRORE") + " casella di arrivo non valida nella data di trasformazione,");
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
                                        Integer accesaBeneByIdAzienda = mdrTrasformazioniRepository.isAccesaBeneByIdAzienda(idAzienda, Integer.parseInt(trasformazioniMap.get("id_casella_partenza").toString()), formattattore(trasformazioniMap.get("data_trasformazione")));
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
                    break;

                default:
                    System.out.println("non dovrebbe essere");
                    break;
            }

        } catch (Exception e) {
            if (!tipo.equals("STRUTTURA")) {
                throw new BaborgCSVBloccanteException(csvErrorFile.getAbsolutePath(), e);
            } else {
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
                        MongoWrapper mongoWrapper = mongoConnectionManager.getRepositoryWrapper(idAzienda);
                        uuid = mongoWrapper.put(csvErrorFile, csvErrorFile.getName(), "/importazioniCSV/csv_error_GRU", true);
                    }

                } catch (IOException ex) {
                    log.error("mapWriter non chiudibile", ex);
                }
            }
            if (mapErrorWriter != null) {
                try {
                    mapErrorWriter.close();
                    MongoWrapper mongoWrapper = mongoConnectionManager.getRepositoryWrapper(idAzienda);
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

    private static LocalDateTime convertDateToLocaleDateTime(Date dateToConvert) {
        if (dateToConvert == null) {
            return null;
        }
        return new java.sql.Timestamp(dateToConvert.getTime()).toLocalDateTime();
    }

    /**
     * Sets up the processors used for APPARTENENTI, RESPONSABILI, STRUTTURA,
     * TRASFORMAZIONI. There are 4 tables. Empty columns are read as null (hence
     * the NotNull() for mandatory columns).
     *
     * @return the cell processors
     */
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

    private static CellProcessor[] getProcessorsError(String tipo, Number codiceAzienda) {
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

    private static String[] headersGenerator(String tipo) {
        String[] headers = null;
        switch (tipo) {
            case "APPARTENENTI":
                headers = new String[]{"codice_ente", "codice_matricola", "cognome",
                    "nome", "codice_fiscale", "id_casella", "datain", "datafi", "tipo_appartenenza",
                    "username", "data_assunzione", "data_dimissione"};
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

    private static String[] headersErrorGenerator(String tipo) {
        String[] headers = null;
        switch (tipo) {
            case "APPARTENENTI":
                headers = new String[]{"codice_ente", "codice_matricola", "cognome",
                    "nome", "codice_fiscale", "id_casella", "datain", "datafi", "tipo_appartenenza",
                    "username", "data_assunzione", "data_dimissione", "ERRORE"};
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

    private boolean controllaEstremi(ZonedDateTime dataStrutturaInizio, ZonedDateTime dataStrutturaFine, ZonedDateTime dataAppartenenteInizio, ZonedDateTime dataAppartenenteFine) {
        if (dataAppartenenteFine == null) {
            dataAppartenenteFine = ZonedDateTime.of(LocalDateTime.MAX, ZoneId.systemDefault());
        }
        if (dataStrutturaFine == null) {
            dataStrutturaFine = ZonedDateTime.of(LocalDateTime.MAX, ZoneId.systemDefault());
        }
        if (dataStrutturaFine.compareTo(dataAppartenenteFine) < 0) {
            return false;
        }
        if (dataStrutturaInizio.compareTo(dataAppartenenteInizio) > 0) {
            return false;
        }

        return true;
    }

    @Transactional(rollbackFor = Throwable.class)
    public ImportazioniOrganigramma updateEsitoImportazioneOrganigramma(ImportazioniOrganigramma newRowInserted, String esito, String csv_error_link) {
        // Update nello storico importazioni. esito: Errore o Ok
        Integer idNewInsertedRowImpOrg = newRowInserted.getId();
        java.util.Optional<ImportazioniOrganigramma> findById = importazioniOrganigrammaRepository.findById(idNewInsertedRowImpOrg);
        ImportazioniOrganigramma rigaImportazione = findById.get();
        rigaImportazione.setEsito(esito);
        rigaImportazione.setPath_csv_error(csv_error_link);
        rigaImportazione.setIdUtente(newRowInserted.getIdUtente());
        ImportazioniOrganigramma res = importazioniOrganigrammaRepository.save(rigaImportazione);
        return res;
    }

    @Transactional(rollbackFor = Throwable.class)
    public ImportazioniOrganigramma insertNewRowImportazioneOrganigrama(Integer idUser, String idAzienda, String tipo, String codiceAzienda, String fileName, Persona person, ImportazioniOrganigramma newRowInserted) {
//        ImportazioniOrganigramma newRowInserted = null;

        int idAziendaInt = Integer.parseInt(idAzienda);
        int idAziendaCodice = Integer.parseInt(codiceAzienda);

        java.util.Optional<Azienda> azienda = aziendaRepository.findById(idAziendaInt);
        if (azienda.isPresent()) {
            ImportazioniOrganigramma newRowInCorso = new ImportazioniOrganigramma();
            newRowInCorso.setDataInserimentoRiga(ZonedDateTime.now());
            newRowInCorso.setNomeFile(fileName);
            newRowInCorso.setIdAzienda(azienda.get());
            newRowInCorso.setTipo(tipo);
            newRowInCorso.setEsito("In corso");
            java.util.Optional<Utente> u = utenteRepository.findById(idUser);
            newRowInCorso.setIdUtente(u.get());
            java.util.Optional<Persona> p = personaRepository.findById(person.getId());
            newRowInCorso.setIdPersona(p.get());
            newRowInserted = importazioniOrganigrammaRepository.save(newRowInCorso);
            System.out.println("new row inserted in Importazioni Organigramma" + newRowInserted);
        }
        return newRowInserted;
    }

    @Transactional(rollbackFor = Throwable.class)
    public ImportazioniOrganigramma manageUploadFile(Integer idUser, MultipartFile file, String idAzienda, String tipo, String codiceAzienda, String fileName, Persona person, ImportazioniOrganigramma newRowInserted) throws Exception {

        int idAziendaInt = Integer.parseInt(idAzienda);
        int idAziendaCodice = Integer.parseInt(codiceAzienda);
        ImportazioniOrganigramma res = null;
        BaborgUtils bean = beanFactory.getBean(BaborgUtils.class);
        ImportaDaCSV importaDaCSVBeanSave = beanFactory.getBean(ImportaDaCSV.class);


        try {

//            String csv_error_link = bean.csvTransactionalReadDeleteInsert(file, tipo, idAziendaCodice, idAziendaInt);
            String csv_error_link = importaDaCSVBeanSave.csvTransactionalReadDeleteInsert(file, tipo, idAziendaCodice, idAziendaInt);
            // Update nello storico importazioni. esito: OK e Data Fine: Data.now
            res = bean.updateEsitoImportazioneOrganigramma(newRowInserted, "Ok", csv_error_link);
        } catch (BaborgCSVBloccanteException e) {
            System.out.println(e.getMessage());
            res = bean.updateEsitoImportazioneOrganigramma(newRowInserted, "Errore Bloccante", e.getMessage());
        } catch (BaborgCSVAnomaliaException e) {
            System.out.println(e.getMessage());
            res = bean.updateEsitoImportazioneOrganigramma(newRowInserted, "Anomalia", e.getMessage());
        } catch (BaborgCSVBloccanteRigheException e) {
            System.out.println(e.getMessage());
            res = bean.updateEsitoImportazioneOrganigramma(newRowInserted, "Bloccante Righe", e.getMessage());
        } catch (Throwable e) {
            System.out.println(e.getMessage());
            res = bean.updateEsitoImportazioneOrganigramma(newRowInserted, "Errore", null);
        }

        return res;
    }

    /**
     * non usata ATTENZIONE gli elementi devono essere ordinati per datain ASC
     *
     * @param elementi lista di padri/elementi/strutture con datain e data fi
     * @param dataInizio data di inizio del figlio
     * @param dataFine data di fine del figlio
     * @return true se il figlio rispetta l'arco temporale del o dei padri nel
     * caso in cui il padre sia spezzato ma continuo
     */
    public Boolean arco_old(List<Map<String, Object>> elementi, ZonedDateTime dataInizio, ZonedDateTime dataFine) {
        if (elementi.isEmpty()) {
            return false;
        }
        Map<String, Object> elemento = elementi.get(0);
        if (formattattore(elemento.get("datain")).equals(dataInizio) || formattattore(elemento.get("datain")).isBefore(dataInizio)) {
            if (elemento.get("datafi") == null) {
                return true;
            } else if (dataFine != null) {
                if (formattattore(elemento.get("datafi")).equals(dataFine) || formattattore(elemento.get("datafi")).isAfter(dataFine)) {
                    return true;
                } else {
                    elementi.remove(0);
                    return arcoBool(elementi, formattattore(elemento.get("datafi")).plusDays(1), dataFine);
                }
            } else {
                elementi.remove(0);
                return arcoBool(elementi, formattattore(elemento.get("datafi")).plusDays(1), dataFine);
            }
        } else {
            elementi.remove(0);
            return arcoBool(elementi, dataInizio, dataFine);
        }
    }

    private List<Integer> arco(List<Map<String, Object>> elementi, ZonedDateTime dataInizio, ZonedDateTime dataFine) {
        List<Integer> lista = new ArrayList<>();
        if (!elementi.isEmpty()) {
            for (Map<String, Object> elemento : elementi) {
                if (overlap(formattattore(elemento.get("datain")), formattattore(elemento.get("datafi")), dataInizio, dataFine)) {
                    lista.add(Integer.parseInt(elemento.get("riga").toString()));
                }
            }
        }
        return lista;
    }

    /**
     *
     * @param elementi lista fi elementi da controllare se hanno un periodo in
     * comune con il periodo passato
     * @param dataInizio inizio intervallo
     * @param dataFine fine intervallo
     * @return true se gli intervalli si sovrappongono con l'intervallo passato
     * false se non si sovrappongo
     */
    public Boolean arcoBool(List<Map<String, Object>> elementi, ZonedDateTime dataInizio, ZonedDateTime dataFine) {
        if (elementi.isEmpty()) {
            return false;
        }
        return elementi.stream().anyMatch(elemento -> overlap(formattattore(elemento.get("datain")), formattattore(elemento.get("datafi")), dataInizio, dataFine));
    }

    /**
     *
     * @param dataInizioA data di inizio del periodo A da controllare
     * @param dataFineA data di fine del periodo da A controllare
     * @param dataInizioB data di inizio del periodo B che potrebbe coincidere
     * con A
     * @param dataFineB data di fine del periodo B che potrebbe coincidere con A
     * @return true se si sovrappongono false se non si sovrappongono
     */
    public Boolean overlap(ZonedDateTime dataInizioA, ZonedDateTime dataFineA, ZonedDateTime dataInizioB, ZonedDateTime dataFineB) {

        if (dataFineA == null) {
            dataFineA = ZonedDateTime.of(LocalDateTime.MAX, ZoneId.systemDefault());
        }
        if (dataFineB == null) {
            dataFineB = ZonedDateTime.of(LocalDateTime.MAX, ZoneId.systemDefault());
        }
        return (dataInizioA.compareTo(dataFineB) <= 0 && dataFineA.compareTo(dataInizioB) >= 0) && dataInizioA.compareTo(dataInizioB) <= 0;
    }

    private Map<String, ZonedDateTime> maxMin(List<Map<String, Object>> elementi) {
        HashMap<String, ZonedDateTime> maxmin = new HashMap<>();
        ZonedDateTime min = ZonedDateTime.of(LocalDateTime.MAX, ZoneId.systemDefault());
        ZonedDateTime max = ZonedDateTime.of(LocalDateTime.MIN, ZoneId.systemDefault());

        for (Map<String, Object> map1 : elementi) {
            if (min.compareTo(formattattore(map1.get("datain").toString())) > 0) {
                min = formattattore(map1.get("datain").toString());
            }
            if (map1.get("datafi") == null) {
                max = ZonedDateTime.of(LocalDateTime.MAX, ZoneId.systemDefault());
            } else if (max.compareTo(formattattore(map1.get("datafi").toString())) < 0) {
                max = formattattore(map1.get("datafi").toString());
            }

        }
        maxmin.put("max", max);
        maxmin.put("min", min);
        return maxmin;
    }

    /**
     *
     * @param o
     * @param formatoDestinazione
     * @return
     * @throws ParseException
     */
    public ZonedDateTime formattattore(Object o) {
        if (o != null) {
            try {
                // String format = ((Timestamp) o).toLocalDateTime().toLocalDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
//                Instant toInstant = new SimpleDateFormat("dd/MM/yyyy").parse(o.toString()).toInstant();
                return LocalDate.parse(o.toString(), DateTimeFormatter.ofPattern("dd/MM/yyyy")).atStartOfDay(ZoneId.systemDefault());
            } catch (Exception e) {

            }
            try {

                // String format = ((Timestamp) o).toLocalDateTime().toLocalDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
                Instant toInstant = new SimpleDateFormat("dd/MM/yy").parse(o.toString()).toInstant();
                return ZonedDateTime.ofInstant(toInstant, ZoneId.systemDefault());
            } catch (ParseException e) {
                //non Ã¨ stato parsato
            }
            try {
                Instant toInstant = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss").parse(o.toString()).toInstant();
                return ZonedDateTime.ofInstant(toInstant, ZoneId.systemDefault());
            } catch (ParseException e) {
                //non Ã¨ stato parsato
            }
            try {
                Instant toInstant = new SimpleDateFormat("dd/MM/yyyy HH:mm").parse(o.toString()).toInstant();
                return ZonedDateTime.ofInstant(toInstant, ZoneId.systemDefault());
            } catch (ParseException e) {
                //non Ã¨ stato parsato
            }

            try {
                String time = ((Timestamp) o).toLocalDateTime().toLocalDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
                Instant toInstant = new SimpleDateFormat("dd/MM/yyyy").parse(time).toInstant();
                return ZonedDateTime.ofInstant(toInstant, ZoneId.systemDefault());
            } catch (ParseException e) {
                //non Ã¨ stato parsato
            }
            try {
                String time = ((Timestamp) o).toLocalDateTime().toLocalDate().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
                Instant toInstant = new SimpleDateFormat("dd/MM/yyyy").parse(time).toInstant();
                return ZonedDateTime.ofInstant(toInstant, ZoneId.systemDefault());
            } catch (ParseException e) {
                //non Ã¨ stato parsato
            }

        }
        return null;
    }
}
