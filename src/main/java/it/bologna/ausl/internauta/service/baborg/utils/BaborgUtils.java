package it.bologna.ausl.internauta.service.baborg.utils;

import com.querydsl.core.types.dsl.BooleanExpression;
import it.bologna.ausl.blackbox.utils.UtilityFunctions;
import it.bologna.ausl.internauta.service.configuration.utils.MongoConnectionManager;
import it.bologna.ausl.internauta.service.exceptions.BaborgCSVException;
import it.bologna.ausl.internauta.service.repositories.baborg.AziendaRepository;
import it.bologna.ausl.internauta.service.repositories.baborg.ImportazioniOrganigrammaRepository;
import it.bologna.ausl.internauta.service.repositories.baborg.PersonaRepository;
import it.bologna.ausl.internauta.service.repositories.baborg.UtenteRepository;
import it.bologna.ausl.internauta.service.repositories.gru.MdrAppartenentiRepository;
import it.bologna.ausl.internauta.service.repositories.gru.MdrResponsabiliRepository;
import it.bologna.ausl.internauta.service.repositories.gru.MdrStrutturaRepository;
import it.bologna.ausl.internauta.service.repositories.gru.MdrTrasformazioniRepository;
import it.bologna.ausl.model.entities.baborg.Azienda;
import it.bologna.ausl.model.entities.baborg.ImportazioniOrganigramma;
import it.bologna.ausl.model.entities.baborg.Persona;
import it.bologna.ausl.model.entities.baborg.Utente;
import it.bologna.ausl.model.entities.gru.MdrAppartenenti;
import it.bologna.ausl.model.entities.gru.MdrResponsabili;
import it.bologna.ausl.model.entities.gru.MdrStruttura;
import it.bologna.ausl.model.entities.gru.MdrTrasformazioni;
import it.bologna.ausl.model.entities.gru.QMdrAppartenenti;
import it.bologna.ausl.model.entities.gru.QMdrResponsabili;
import it.bologna.ausl.model.entities.gru.QMdrStruttura;
import it.bologna.ausl.model.entities.gru.QMdrTrasformazioni;
import it.bologna.ausl.mongowrapper.MongoWrapper;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
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

    @Autowired
    AziendaRepository aziendaRepository;

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
    MongoConnectionManager mongoConnectionManager;

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

                row.putAll(elemento);
                if (elemento.get("datain") != null && !elemento.get("datain").equals("")) {
                    if (Timestamp.class.isAssignableFrom(elemento.get("datain").getClass())) {
                        row.put("datain", ((Timestamp) elemento.get("datain")).toLocalDateTime().toLocalDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
                    }
                }
                if (elemento.get("datafi") != null && !elemento.get("datafi").equals("")) {
                    if (Timestamp.class.isAssignableFrom(elemento.get("datafi").getClass())) {
                        row.put("datafi", ((Timestamp) elemento.get("datafi")).toLocalDateTime().toLocalDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
                    }
                }
                if (elemento.get("data_assunzione") != null && !elemento.get("data_assunzione").equals("")) {
                    if (Timestamp.class.isAssignableFrom(elemento.get("data_assunzione").getClass())) {
                        row.put("data_assunzione", ((Timestamp) elemento.get("data_assunzione")).toLocalDateTime().toLocalDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
                    }
                }
                if (elemento.get("data_dimissione") != null && !elemento.get("data_dimissione").equals("")) {
                    if (Timestamp.class.isAssignableFrom(elemento.get("data_dimissione").getClass())) {
                        row.put("data_dimissione", ((Timestamp) elemento.get("data_dimissione")).toLocalDateTime().toLocalDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
                    }
                }
                if (elemento.get("data_trasformazione") != null && !elemento.get("data_trasformazione").equals("")) {
                    if (Timestamp.class.isAssignableFrom(elemento.get("data_trasformazione").getClass())) {
                        row.put("data_trasformazione", ((Timestamp) elemento.get("data_trasformazione")).toLocalDateTime().toLocalDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
                    }
                }
                if (elemento.get("datain_partenza") != null && !elemento.get("datain_partenza").equals("")) {
                    if (Timestamp.class.isAssignableFrom(elemento.get("datain_partenza").getClass())) {
                        row.put("datain_partenza", ((Timestamp) elemento.get("datain_partenza")).toLocalDateTime().toLocalDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
                    }
                }
                if (elemento.get("dataora_oper") != null && !elemento.get("dataora_oper").equals("")) {
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
     * @throws java.io.FileNotFoundException
     */
    @Transactional(rollbackFor = Throwable.class)
    public String csvTransactionalReadDeleteInsert(MultipartFile file, String tipo, Integer codiceAzienda, Integer idAzienda) throws BaborgCSVException {
        Timestamp timestamp = new Timestamp(System.currentTimeMillis());
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy_MM_dd-HH_mm_ss");
        String nameCsv = sdf.format(timestamp) + "_Error_" + tipo + ".csv";
        File csvErrorFile = new File(System.getProperty("java.io.tmpdir"), nameCsv);
        csvErrorFile.deleteOnExit();
        String uuid = null;
        boolean bloccante = false;
        ICsvMapReader mapReader = null;
        ICsvMapReader mapErrorReader = null;
        ICsvMapWriter mapWriter = null;
        ICsvMapWriter mapErrorWriter = null;
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

            Map<String, Object> mapError = new HashMap<>();

            switch (tipo) {
                case "APPARTENENTI":
                    // Delete delle righe da sostituire
                    predicateAzienda = QMdrAppartenenti.mdrAppartenenti.idAzienda.id.eq(idAzienda);
                    mdrAppartenentiRepository.deleteByIdAzienda(idAzienda);
                    //Reading with CsvMapReader
                    Map<String, Object> appartenentiMap;
                    while ((appartenentiMap = mapReader.read(headers, processors)) != null) {
                        // Inserisco la riga
                        MdrAppartenenti mA = new MdrAppartenenti();
//                      preparo la mappa di errore
                        mapError.put("ERRORE", "");

//                      CODICE_MATRICOLA bloccante
                        String codiceMatricola = null;
                        if (appartenentiMap.get("codice_matricola") == null || appartenentiMap.get("codice_matricola").equals("")) {
                            mapError.put("ERRORE", mapError.get("ERRORE") + " codice_matricola,");
                            mapError.put("codice_matricola", "");
                            mA.setCodiceMatricola(null);
                            codiceMatricola = "";
                        } else {
                            mapError.put("codice_matricola", appartenentiMap.get("codice_matricola"));
                            codiceMatricola = appartenentiMap.get("codice_matricola").toString();
                            mA.setCodiceMatricola(Integer.parseInt(appartenentiMap.get("codice_matricola").toString()));
                        }
//                      COGNOME bloccante
                        if (appartenentiMap.get("cognome") == null || appartenentiMap.get("cognome").equals("")) {
                            mapError.put("ERRORE", mapError.get("ERRORE") + " cognome,");
                            bloccante = true;
                            mapError.put("cognome", "");
                            mA.setCognome(null);
                        } else {
                            mapError.put("cognome", appartenentiMap.get("cognome"));
                            mA.setCognome(appartenentiMap.get("cognome").toString());
                        }
//                      NOME bloccante
                        if (appartenentiMap.get("nome") == null || appartenentiMap.get("nome").equals("")) {
                            mapError.put("ERRORE", mapError.get("ERRORE") + " nome,");
                            bloccante = true;
                            mapError.put("nome", "");
                            mA.setNome(null);
                        } else {
                            mapError.put("nome", appartenentiMap.get("nome"));
                            mA.setNome(appartenentiMap.get("nome").toString());
                        }
//                      CODICE_FISCALE bloccante
                        if (appartenentiMap.get("codice_fiscale") == null || appartenentiMap.get("codice_fiscale").equals("")) {
                            mapError.put("ERRORE", mapError.get("ERRORE") + " CODICE FISCALE,");
                            bloccante = true;
                            mapError.put("codice_fiscale", "");
                            mA.setCodiceFiscale(null);
                        } else {
                            mapError.put("codice_fiscale", appartenentiMap.get("codice_fiscale"));
                            mA.setCodiceFiscale(appartenentiMap.get("codice_fiscale").toString());
                        }
                        String idCasella = null;
//                      ID_CASELLA bloccante
                        if (appartenentiMap.get("id_casella") == null || appartenentiMap.get("id_casella").equals("")) {
                            mapError.put("ERRORE", mapError.get("ERRORE") + " IDCASELLA,");
                            idCasella = "";
                            mapError.put("id_casella", "");
                            mA.setIdCasella(null);
                        } else {
                            if (mdrStrutturaRepository.selectStrutturaUtenteByIdCasellaAndIdAzienda(Integer.parseInt(appartenentiMap.get("id_casella").toString()), idAzienda) <= 0) {
                                mapError.put("ERRORE", " manca la struttura nella tabella struttura,");
                            }
                            mapError.put("id_casella", appartenentiMap.get("id_casella"));
                            idCasella = appartenentiMap.get("id_casella").toString();
                            mA.setIdCasella(Integer.parseInt(idCasella));

                        }
//                      DATAIN bloccante
                        if (appartenentiMap.get("datain") == null || appartenentiMap.get("datain").equals("")) {
                            mapError.put("ERRORE", mapError.get("ERRORE") + " datain,");
                            mapError.put("datain", "");
                            mA.setDatain(null);
                        } else {
                            mapError.put("datain", appartenentiMap.get("datain"));
                            mA.setDatain(formattattore(appartenentiMap.get("datain")));
                        }
                        LocalDateTime datafi = null;
                        LocalDateTime datain = null;
                        String datafiString = null;
                        String datainString = null;

                        if (appartenentiMap.get("datafi") != null && !appartenentiMap.get("datafi").equals("")) {
                            datafi = formattattore(appartenentiMap.get("datafi"));
                            datafiString = UtilityFunctions.getLocalDateTimeString(datafi);
                        }

                        if (appartenentiMap.get("datain") != null && !appartenentiMap.get("datain").equals("")) {
                            datain = formattattore(appartenentiMap.get("datain"));
                            datainString = UtilityFunctions.getLocalDateTimeString(datain);
                        }
                        if (appartenentiMap.get("datafi") == null || appartenentiMap.get("datafi").equals("")) {
                            mapError.put("datafi", "");
                            mA.setDatafi(null);
                        } else {
                            mapError.put("datafi", appartenentiMap.get("datafi"));
                            mA.setDatafi(formattattore(appartenentiMap.get("datafi")));
                        }
//                      TIPO_APPARTENENZA bloccante
                        if (appartenentiMap.get("tipo_appartenenza") == null || appartenentiMap.get("tipo_appartenenza").equals("")) {
                            mapError.put("ERRORE", mapError.get("ERRORE") + " tipo_appartenenza,");
                            mapError.put("tipo_appartenenza", "");
                            mA.setTipoAppartenenza(null);
                        } else {
                            mapError.put("tipo_appartenenza", appartenentiMap.get("tipo_appartenenza"));
                            mA.setTipoAppartenenza(appartenentiMap.get("tipo_appartenenza").toString());
                        }
                        //controllo multiafferenza diretta
                        if ((!bloccante) && (mdrAppartenentiRepository.select_multidefinictions_user_byidazienda(codiceAzienda,
                                Integer.parseInt(codiceMatricola),
                                Integer.parseInt(idCasella),
                                datafiString,
                                datainString)) > 0) {
                            mapError.put("ERRORE", mapError.get("ERRORE") + " utente con piu afferenze dirette per lo stesso periodo,");
                        }
//                      DataAssunzione bloccante
                        if (appartenentiMap.get("data_assunzione") == null || appartenentiMap.get("data_assunzione").equals("")) {
                            mapError.put("ERRORE", mapError.get("ERRORE") + " data_assunzione,");
                            mapError.put("data_assunzione", "");
                        } else {
                            mapError.put("data_assunzione", appartenentiMap.get("data_assunzione"));
                            mA.setDataAssunzione(formattattore(appartenentiMap.get("data_assunzione")));
                        }
//                      USERNAME bloccante
                        if (appartenentiMap.get("username") == null || appartenentiMap.get("username").equals("")) {
                            mA.setUsername("");
                            mapError.put("username", "");

                        } else {
                            mapError.put("username", appartenentiMap.get("username"));
                            mA.setUsername(appartenentiMap.get("username").toString());
                        }
//                      DATA_DIMISSIONE non bloccante
                        if (appartenentiMap.get("data_dimissione") == null || appartenentiMap.get("data_dimissione").equals("")) {
                            mapError.put("data_dimissione", appartenentiMap.get("data_dimissione"));
                            mA.setDataDimissione(null);
                        } else {
                            mapError.put("data_dimissione", appartenentiMap.get("data_dimissione"));
                            mA.setDataDimissione(formattattore(appartenentiMap.get("data_dimissione")));
                        }
                        if (appartenentiMap.get("codice_ente") == null || appartenentiMap.get("codice_ente").equals("")) {
                            mapError.put("codice_ente", codiceAzienda);
                            mA.setCodiceEnte(codiceAzienda);

                        } else {
                            mapError.put("codice_ente", appartenentiMap.get("codice_ente"));
                            mA.setCodiceEnte(Integer.parseInt(appartenentiMap.get("codice_ente").toString()));
                        }
                        mA.setIdAzienda(azienda);
                        mdrAppartenentiRepository.save(mA);
                        mapWriter.write(mapError, headersErrorGenerator(tipo), getProcessorsError(tipo, codiceAzienda));
                    }

                    //query intratabelle
                    break;

                case "RESPONSABILI":
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
                        if (responsabiliMap.get("codice_matricola") == null || responsabiliMap.get("codice_matricola").equals("")) {
                            mapError.put("ERRORE", mapError.get("ERRORE") + " codice_matricola,");
                            mapError.put("codice_matricola", "");
                            codice_matricola = "";
                            mR.setCodiceMatricola(null);
                        } else {
                            mapError.put("codice_matricola", responsabiliMap.get("codice_matricola"));
                            codice_matricola = responsabiliMap.get("codice_matricola").toString();
                            mR.setCodiceMatricola(Integer.parseInt(responsabiliMap.get("codice_matricola").toString()));
                            //responsabile presente tra gli autenti
                            if (mdrAppartenentiRepository.countUsertByCodiceMatricola(Integer.parseInt(responsabiliMap.get("codice_matricola").toString())) <= 0) {
                                mapError.put("ERRORE", mapError.get("ERRORE") + " codice_matricola non trovata nella tabella appartenenti,");
                            }
                        }

//                      ID_CASELLA bloccante
                        String id_casella = null;
                        if (responsabiliMap.get("id_casella") == null || responsabiliMap.get("id_casella").equals("")) {
                            mapError.put("ERRORE", mapError.get("ERRORE") + " id_casella,");
                            id_casella = "";
                            mapError.put("id_casella", "");
                            mR.setIdCasella(null);
                        } else {
                            mapError.put("id_casella", responsabiliMap.get("id_casella"));
                            id_casella = responsabiliMap.get("id_casella").toString();
                            mR.setIdCasella(Integer.parseInt(responsabiliMap.get("id_casella").toString()));
                            if (mdrStrutturaRepository.selectStrutturaUtenteByIdCasellaAndIdAzienda(Integer.parseInt(responsabiliMap.get("id_casella").toString()), idAzienda) <= 0) {
                                mapError.put("ERRORE", mapError.get("ERRORE") + " id_casella non trovata nella tabella strutture,");
                            }
                        }
//                      DATAIN bloccante                        
                        if (responsabiliMap.get("datain") == null || responsabiliMap.get("datain").equals("")) {
                            mapError.put("ERRORE", mapError.get("ERRORE") + " datain,");
                            mapError.put("datain", "");
                            mR.setDatain(null);
                        } else {
                            mapError.put("datain", responsabiliMap.get("datain"));
                            mR.setDatain(formattattore(responsabiliMap.get("datain")));
                        }
                        LocalDateTime datafi = null;
                        LocalDateTime datain = null;
                        String datafiString = null;
                        String datainString = null;
                        if (responsabiliMap.get("datafi") != null && !responsabiliMap.get("datafi").equals("")) {
                            datafi = formattattore(responsabiliMap.get("datafi"));
                            datafiString = UtilityFunctions.getLocalDateTimeString(datafi);
                        }

                        if (responsabiliMap.get("datain") != null && !responsabiliMap.get("datain").equals("")) {
                            datain = formattattore(responsabiliMap.get("datain"));
                            datainString = UtilityFunctions.getLocalDateTimeString(datain);
                        }
                        if (!bloccante && mdrResponsabiliRepository.countMultiReponsabilePerStruttura(codiceAzienda,
                                Integer.parseInt(id_casella),
                                datafiString,
                                datainString) > 0) {
                            mapError.put("ERRORE", mapError.get("ERRORE") + " la struttura di questo responsabile è già assegnata ad un altro respondabile,");
                        }
//                      DATAFI non bloccante
                        if (responsabiliMap.get("datafi") == null || responsabiliMap.get("datafi").equals("")) {
                            mapError.put("datafi", "");
                            mR.setDatafi(null);
                        } else {
                            mapError.put("datafi", responsabiliMap.get("datafi"));
                            mR.setDatafi(formattattore(responsabiliMap.get("datafi")));
                        }
//                      TIPO bloccante                        
                        if (responsabiliMap.get("tipo") == null || responsabiliMap.get("tipo").equals("")) {
                            mapError.put("ERRORE", mapError.get("ERRORE") + " tipo,");
                            mR.setTipo(null);
                        } else {
                            mapError.put("tipo", responsabiliMap.get("tipo"));
                            mR.setTipo(responsabiliMap.get("tipo").toString());
                        }

                        if (responsabiliMap.get("codice_ente") == null || responsabiliMap.get("codice_ente").equals("")) {
                            mapError.put("codice_ente", codiceAzienda);
                            mR.setCodiceEnte(codiceAzienda);

                        } else {
                            mapError.put("codice_ente", responsabiliMap.get("codice_ente"));
                            mR.setCodiceEnte(Integer.parseInt(responsabiliMap.get("codice_ente").toString()));
                        }

                        mR.setIdAzienda(azienda);
                        mdrResponsabiliRepository.save(mR);
                        mapWriter.write(mapError, headersErrorGenerator(tipo), getProcessorsError(tipo, codiceAzienda));
                    }
                    break;

                case "STRUTTURA":
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
                        LocalDateTime datafi = null;
                        LocalDateTime datain = null;
                        String datafiString = null;
                        String datainString = null;

                        if (strutturaMap.get("datain") != null && !strutturaMap.get("datain").equals("")) {
                            datain = formattattore(strutturaMap.get("datain"));
                            datainString = UtilityFunctions.getLocalDateTimeString(datain);
                        }

                        if (strutturaMap.get("datain") == null || strutturaMap.get("datain").equals("")) {
                            mapError.put("ERRORE", mapError.get("ERRORE") + " datain,");
                            mapError.put("datain", "");
                            mS.setDatain(null);
                            bloccante = true;
                        } else {
                            mapError.put("datain", strutturaMap.get("datain"));
                            mS.setDatain(datain);
                        }

                        if (strutturaMap.get("datafi") != null && !strutturaMap.get("datafi").equals("")) {
                            datafi = formattattore(strutturaMap.get("datafi"));
                            datafiString = UtilityFunctions.getLocalDateTimeString(datafi);
                        }

                        if (strutturaMap.get("datafi") == null || strutturaMap.get("datafi").equals("")) {
                            mapError.put("datafi", "");
                            mS.setDatafi(null);
                        } else {
                            mapError.put("datafi", strutturaMap.get("datafi"));
                            mS.setDatafi(datafi);
                        }

                        String id_casella = null;
                        if (strutturaMap.get("id_casella") == null || strutturaMap.get("id_casella").equals("")) {
                            mapError.put("ERRORE", mapError.get("ERRORE") + " id_casella assente,");
                            bloccante = true;
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
                                mapError.put("ERRORE", mapError.get("ERRORE") + " struttura definita piu volte nello stesso arco temporale,");
                            }
                        }

                        if (strutturaMap.get("id_padre") == null || strutturaMap.get("id_padre").equals("")) {
                            mapError.put("id_padre", "");
                            mS.setIdPadre(null);
                        } else {
                            mapError.put("id_padre", strutturaMap.get("id_padre"));
                            mS.setIdPadre(Integer.parseInt(strutturaMap.get("id_padre").toString()));
                        }

                        if (strutturaMap.get("descrizione") == null || strutturaMap.get("descrizione").equals("")) {
                            mapError.put("ERRORE", mapError.get("ERRORE") + " descrizione assente,");
                            mapError.put("descrizione", "");
                            mS.setDescrizione(null);
                            bloccante = true;
                        } else {
                            mapError.put("descrizione", strutturaMap.get("descrizione"));
                            mS.setDescrizione(strutturaMap.get("descrizione").toString());
                        }

                        if (strutturaMap.get("tipo_legame") == null || strutturaMap.get("tipo_legame").equals("")) {
                            mapError.put("tipo_legame", "");
                            mS.setTipoLegame(null);
                        } else {
                            mapError.put("tipo_legame", strutturaMap.get("tipo_legame"));
                            mS.setTipoLegame(strutturaMap.get("tipo_legame").toString());
                        }
                        if (strutturaMap.get("codice_ente") == null || strutturaMap.get("codice_ente").equals("")) {
                            mapError.put("codice_ente", codiceAzienda);
                            mS.setCodiceEnte(codiceAzienda);

                        } else {
                            mapError.put("codice_ente", strutturaMap.get("codice_ente"));
                            mS.setCodiceEnte(Integer.parseInt(strutturaMap.get("codice_ente").toString()));
                        }
                        mS.setIdAzienda(azienda);
                        mdrStrutturaRepository.save(mS);
                        mapWriter.write(mapError, headersErrorGenerator(tipo), getProcessorsError(tipo, codiceAzienda));
                    }

                    //struttura padre non trovata
                    List<Integer> selectDaddyByIdAzienda = mdrStrutturaRepository.selectDaddyByIdAzienda(idAzienda);
                    if (selectDaddyByIdAzienda != null && selectDaddyByIdAzienda.size() > 0 && selectDaddyByIdAzienda.get(0) != null) {
                        bloccante = true;
                    }
                    //strutture che non rispettano il padre
//                    List<Integer> caselleInvalide = mdrStrutturaRepository.caselleInvalide(idAzienda);
//
//                    if (caselleInvalide != null && caselleInvalide.size() > 0 && caselleInvalide.get(0) != null) {
//                        bloccante = true;
//
//                    }
                    mapWriter.close();
                    try (InputStreamReader csvErrorFileRIP = new InputStreamReader(new FileInputStream(csvErrorFile));) {

                        mapErrorReader = new CsvMapReader(csvErrorFileRIP, SEMICOLON_DELIMITED);
                        mapErrorReader.getHeader(true);

                        mapErrorWriter = new CsvMapWriter(new FileWriter(csvErrorFile), SEMICOLON_DELIMITED);
                        mapErrorWriter.writeHeader(headersErrorGenerator(tipo));

                        Map<String, Object> strutturaErrorMap = null;
                        while ((strutturaErrorMap = mapErrorReader.read(headersErrorGenerator(tipo), getProcessorsError(tipo, codiceAzienda))) != null) {
                            Map<String, Object> strutturaErrorMapWrite = null;
                            strutturaErrorMapWrite = strutturaErrorMap;
                            if (strutturaErrorMap.get("id_padre") != null && !strutturaErrorMap.get("id_padre").equals("0")) {
                                List<Map<String, Object>> elementi = mdrStrutturaRepository.mieiPadri(idAzienda, Integer.parseInt(strutturaErrorMap.get("id_padre").toString()));
                                if (!arco(elementi, formattattore(strutturaErrorMap.get("datain")), formattattore(strutturaErrorMap.get("datafi")))) {
                                    if (strutturaErrorMap.get("ERRORE") != null) {
                                        bloccante = true;
                                        strutturaErrorMapWrite.put("ERRORE", strutturaErrorMap.get("ERRORE") + " non rispetta l'arco temporale del padre,");
                                    } else {
                                        bloccante = true;
                                        strutturaErrorMapWrite.put("ERRORE", " non rispetta l'arco temporale del padre,");

                                    }
                                }
                            }

                            if (selectDaddyByIdAzienda.contains(strutturaErrorMap.get("id_padre")) && selectDaddyByIdAzienda.get(0) != null) {
                                if (strutturaErrorMap.get("ERRORE") != null) {
                                    strutturaErrorMapWrite.put("ERRORE", strutturaErrorMap.get("ERRORE") + " padre non presente,");
                                } else {
                                    strutturaErrorMapWrite.put("ERRORE", " padre non presente,");
                                }

                            }
                            mapErrorWriter.write(strutturaErrorMapWrite, headersErrorGenerator(tipo), getProcessorsError(tipo, codiceAzienda));

                        }
                    } catch (Exception ex) {
                        System.out.println("ex:" + ex);
                    }

                    break;

                case "TRASFORMAZIONI":

                    // Delete delle righe da sostituire
                    predicateAzienda = QMdrTrasformazioni.mdrTrasformazioni.idAzienda.id.eq(idAzienda);
                    mdrTrasformazioniRepository.deleteByIdAzienda(idAzienda);

                    //Reading with CsvMapReader
                    Map<String, Object> trasformazioniMap;
                    while ((trasformazioniMap = mapReader.read(headers, processors)) != null) {
                        // Inserisco la riga
                        MdrTrasformazioni mT = new MdrTrasformazioni();
                        mapError.put("ERRORE", "");

                        if (trasformazioniMap.get("progressivo_riga") == null || trasformazioniMap.get("progressivo_riga").equals("")) {
                            mapError.put("ERRORE", mapError.get("ERRORE") + " progressivo_riga,");
                            mapError.put("progressivo_riga", "");
                            mT.setProgressivoRiga(null);
                            bloccante = true;
                        } else {
                            mapError.put("progressivo_riga", trasformazioniMap.get("progressivo_riga"));
                            mT.setProgressivoRiga(Integer.parseInt(trasformazioniMap.get("progressivo_riga").toString()));
                        }

                        if (trasformazioniMap.get("id_casella_partenza") == null || trasformazioniMap.get("id_casella_partenza").equals("")) {
                            mapError.put("ERRORE", mapError.get("ERRORE") + " id_casella_partenza,");
                            mapError.put("id_casella_partenza", "");
                            mT.setIdCasellaPartenza(null);
                            bloccante = true;
                        } else {
                            mapError.put("id_casella_partenza", trasformazioniMap.get("id_casella_partenza"));
                            mT.setIdCasellaPartenza(Integer.parseInt(trasformazioniMap.get("id_casella_partenza").toString()));
                            if (mdrTrasformazioniRepository.isTransformableByIdAzienda(idAzienda, Integer.parseInt(trasformazioniMap.get("id_casella_partenza").toString()), formattattore(trasformazioniMap.get("data_trasformazione"))) <= 0) {
                                bloccante = true;
                                mapError.put("ERRORE", mapError.get("ERRORE") + " casella di partenza non trovata nella tabella strutture,");
                            }
                        }

                        if (trasformazioniMap.get("data_trasformazione") == null || trasformazioniMap.get("data_trasformazione").equals("")) {
                            mapError.put("ERRORE", mapError.get("ERRORE") + " data_trasformazione,");
                            mapError.put("data_trasformazione", "");
                            mT.setDataTrasformazione(null);
                        } else {
                            mapError.put("data_trasformazione", trasformazioniMap.get("data_trasformazione"));
                            mT.setDataTrasformazione(formattattore(trasformazioniMap.get("data_trasformazione")));
                        }

                        if (trasformazioniMap.get("motivo") == null || trasformazioniMap.get("motivo").equals("")) {
                            mapError.put("ERRORE", mapError.get("ERRORE") + " MOTIVO,");
                            mapError.put("motivo", "");
                            mT.setMotivo(null);
                            bloccante = true;
                            //non ci sta un motivo copio paripari id casella di arrivo non ho elementi per sapere se ci dovrebbe o meno essere qualcosa
                            mapError.put("id_casella_arrivo", trasformazioniMap.get("id_casella_arrivo"));
                            mT.setIdCasellaArrivo(Integer.parseInt(trasformazioniMap.get("id_casella_arrivo").toString()));
                        } else {
                            mapError.put("motivo", trasformazioniMap.get("motivo"));
                            mT.setMotivo(trasformazioniMap.get("motivo").toString());
                            //TODO controllo su casella di arrivo
                            if (trasformazioniMap.get("motivo").toString().equalsIgnoreCase("x")) {
                                if (trasformazioniMap.get("id_casella_arrivo") == null || trasformazioniMap.get("id_casella_arrivo").equals("")) {
                                    mapError.put("ERRORE", mapError.get("ERRORE") + " ID_CASELLA_ARRIVO,");
                                    mapError.put("id_casella_arrivo", "");
                                    mT.setIdCasellaArrivo(null);
                                    bloccante = true;
                                } else {
                                    mapError.put("id_casella_arrivo", trasformazioniMap.get("id_casella_arrivo"));
                                    mT.setIdCasellaArrivo(Integer.parseInt(trasformazioniMap.get("id_casella_arrivo").toString()));
                                    if (mdrTrasformazioniRepository.isTransformableByIdAzienda(idAzienda, Integer.parseInt(trasformazioniMap.get("id_casella_arrivo").toString()), formattattore(trasformazioniMap.get("data_trasformazione"))) <= 0) {
                                        bloccante = true;
                                        mapError.put("ERRORE", mapError.get("ERRORE") + " casella di arrivo non trovata nella tabella strutture,");
                                    }
                                }
                            } else {
                                if (trasformazioniMap.get("id_casella_arrivo") == null || trasformazioniMap.get("id_casella_arrivo").equals("")) {
                                    mapError.put("id_casella_arrivo", "");
                                    mT.setIdCasellaArrivo(null);
                                } else {
                                    mapError.put("id_casella_arrivo", trasformazioniMap.get("id_casella_arrivo"));
                                    mapError.put("ERRORE", mapError.get("ERRORE") + " casella di arrivo trovata ma il motivo non è valido ,");
                                    mT.setIdCasellaArrivo(Integer.parseInt(trasformazioniMap.get("id_casella_arrivo").toString()));
                                }
                            }
                        }

                        if (trasformazioniMap.get("datain_partenza") == null || trasformazioniMap.get("datain_partenza").equals("")) {
                            mapError.put("ERRORE", mapError.get("ERRORE") + " datain_partenza,");
                            mapError.put("datain_partenza", "");
                            mT.setDatainPartenza(null);
                            bloccante = true;
                        } else {
                            mapError.put("datain_partenza", trasformazioniMap.get("datain_partenza"));
                            mT.setDatainPartenza(formattattore(trasformazioniMap.get("datain_partenza")));
                        }

                        if (trasformazioniMap.get("dataora_oper") == null || trasformazioniMap.get("dataora_oper").equals("")) {
                            mapError.put("ERRORE", mapError.get("ERRORE") + " DATAORA_OPER inserito automaticamente,");
                            LocalDateTime now = LocalDateTime.now();
                            mapError.put("dataora_oper", now.toString());
                            mT.setDataoraOper(now);
                        } else {
                            mapError.put("dataora_oper", trasformazioniMap.get("dataora_oper"));
                            mT.setDataoraOper(formattattore(trasformazioniMap.get("dataora_oper")));
                        }

                        if (trasformazioniMap.get("codice_ente") == null || trasformazioniMap.get("codice_ente").equals("")) {
                            mapError.put("codice_ente", codiceAzienda);
                            mT.setCodiceEnte(codiceAzienda);

                        } else {
                            mapError.put("codice_ente", trasformazioniMap.get("codice_ente"));
                            mT.setCodiceEnte(Integer.parseInt(trasformazioniMap.get("codice_ente").toString()));
                        }
                        mT.setIdAzienda(azienda);
                        mdrTrasformazioniRepository.save(mT);
                        mapWriter.write(mapError, headersErrorGenerator(tipo), getProcessorsError(tipo, codiceAzienda));
                    }
                    break;

                default:
                    System.out.println("non dovrebbe essere");
                    break;
            }
        } catch (Exception e) {
            throw new BaborgCSVException(csvErrorFile.getAbsolutePath(), e);
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
                        MongoWrapper mongoWrapper = mongoConnectionManager.getConnection(idAzienda);
                        uuid = mongoWrapper.put(csvErrorFile, csvErrorFile.getName(), "/importazioniCSV/csv_error_GRU", true);
                    }

                } catch (IOException ex) {
                    log.error("mapWriter non chiudibile", ex);
                }
            }
            if (mapErrorWriter != null) {
                try {
                    mapErrorWriter.close();
                    MongoWrapper mongoWrapper = mongoConnectionManager.getConnection(idAzienda);
                    uuid = mongoWrapper.put(csvErrorFile, csvErrorFile.getName(), "/importazioniCSV/csv_error_GRU", true);

                } catch (IOException ex) {
                    log.error("mapWriter non chiudibile", ex);
                }
            }
        }
        csvErrorFile.delete();
        if (bloccante) {
            throw new BaborgCSVException(uuid);
        }
        return uuid;
    }

    LocalDateTime convertDateToLocaleDateTime(Date dateToConvert) {
        if (dateToConvert == null) {
            return null;
        }
        return new java.sql.Timestamp(
                dateToConvert.getTime()).toLocalDateTime();
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
                    "nome", "codice_fiscale", "id_azienda", "datain", "datafi", "tipo_appartenenza",
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
            newRowInCorso.setDataInserimentoRiga(LocalDateTime.now());
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

    public ImportazioniOrganigramma manageUploadFile(Integer idUser, MultipartFile file, String idAzienda, String tipo, String codiceAzienda, String fileName, Persona person, ImportazioniOrganigramma newRowInserted) throws Exception {

        int idAziendaInt = Integer.parseInt(idAzienda);
        int idAziendaCodice = Integer.parseInt(codiceAzienda);
//        System.out.println("tipo " + tipo);
//        System.out.println("codiceAzienda " + codiceAzienda);
//        System.out.println("azienda Id " + idAzienda);
//        System.out.println("file: " + file);
//        System.out.println("file content type: " + file.getContentType());
//        System.out.println("file original file name: " + file.getOriginalFilename());
//        System.out.println("file resource: " + file.getResource());
        ImportazioniOrganigramma res = null;
        try {
            String csv_error_link = csvTransactionalReadDeleteInsert(file, tipo, idAziendaCodice, idAziendaInt);
            // Update nello storico importazioni. esito: OK e Data Fine: Data.now
            res = updateEsitoImportazioneOrganigramma(newRowInserted, "Ok", csv_error_link);
        } catch (BaborgCSVException e) {
            System.out.println(e.getMessage());
            res = updateEsitoImportazioneOrganigramma(newRowInserted, "Errore", e.getMessage());
        } catch (Throwable e) {
            System.out.println(e.getMessage());
            res = updateEsitoImportazioneOrganigramma(newRowInserted, "Errore", null);
        }

        return res;
    }

    /**
     * ATTENZIONE gli elementi devono essere ordinati per datain ASC
     *
     * @param elementi lista di padri/elementi/strutture con datain e data fi
     * @param dataInizio data di inizio del figlio
     * @param dataFine data di fine del figlio
     * @return true se il figlio rispetta l'arco temporale del o dei padri nel
     * caso in cui il padre sia spezzato ma continuo
     */
    public Boolean arco(List<Map<String, Object>> elementi, LocalDateTime dataInizio, LocalDateTime dataFine) {
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
                    return arco(elementi, formattattore(elemento.get("datafi")).plusDays(1), dataFine);
                }
            } else {
                return false;
            }
        } else {
            elementi.remove(0);
            return arco(elementi, dataInizio, dataFine);
        }
    }

    /**
     *
     * @param o
     * @param formatoDestinazione
     * @return
     * @throws ParseException
     */
    public LocalDateTime formattattore(Object o) {
        if (o != null) {
            try {

                // String format = ((Timestamp) o).toLocalDateTime().toLocalDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
                Instant toInstant = new SimpleDateFormat("dd/MM/yyyy").parse(o.toString()).toInstant();
                return LocalDateTime.ofInstant(toInstant, ZoneId.systemDefault());
            } catch (ParseException e) {
                //non è stato parsato
            }
            try {
                Instant toInstant = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss").parse(o.toString()).toInstant();
                return LocalDateTime.ofInstant(toInstant, ZoneId.systemDefault());
            } catch (ParseException e) {
                //non è stato parsato
            }
            try {
                Instant toInstant = new SimpleDateFormat("dd/MM/yyyy HH:mm").parse(o.toString()).toInstant();
                return LocalDateTime.ofInstant(toInstant, ZoneId.systemDefault());
            } catch (ParseException e) {
                //non è stato parsato
            }
            try {
                String time = ((Timestamp) o).toLocalDateTime().toLocalDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
                Instant toInstant = new SimpleDateFormat("dd/MM/yyyy").parse(time).toInstant();
                return LocalDateTime.ofInstant(toInstant, ZoneId.systemDefault());
            } catch (ParseException e) {
                //non è stato parsato
            }

        }
        return null;
    }
}
