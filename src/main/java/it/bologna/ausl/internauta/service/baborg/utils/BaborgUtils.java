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
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
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
        ICsvMapWriter mapWriter = null;
        ICsvMapReader mapErrorReader = null;
        ICsvMapWriter mapErrorWriter = null;
        try {
    //        Reading with CsvMapReader
    //        Reading file with CsvMapReader

            InputStreamReader inputFileStreamReader = new InputStreamReader(file.getInputStream());
            CsvPreference SEMICOLON_DELIMITED = new CsvPreference.Builder('"', ';', "\r\n").build();
            mapReader = new CsvMapReader(inputFileStreamReader, SEMICOLON_DELIMITED);
            mapReader.getHeader(true);

            String[] headers = headersGenerator(tipo);
            CellProcessor[] processors = getProcessors(tipo, codiceAzienda);

            java.util.Optional<Azienda> optionalAzienda = aziendaRepository.findById(idAzienda);
            Azienda azienda = optionalAzienda.get();

            BooleanExpression predicateAzienda = null;

            //preparo file di errore
            mapWriter = new CsvMapWriter(new FileWriter(csvErrorFile),SEMICOLON_DELIMITED);
            mapWriter.writeHeader(headers_Error_Generator(tipo));

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
                        mapError.put("ERRORE", "");
//                      preparo la mappa di errore

//                      inizio a settare e a controllare i dati 
//                      codice ente sempre presente perche si prende da interfaccia 
                        mapError.put("CODICE_ENTE", codiceAzienda);
                        mA.setCodiceEnte(codiceAzienda);
//                      CODICE_MATRICOLA bloccante
                        String codiceMatricola = null;
                        if (appartenentiMap.get("codiceMatricola") == null || appartenentiMap.get("codiceMatricola").equals("")) {
                            mapError.put("ERRORE", mapError.get("ERRORE") + " codiceMatricola,");
                            mapError.put("CODICE_MATRICOLA", "");
                            mA.setCodiceMatricola(null);
                            codiceMatricola = "";
                        } else {
                            mapError.put("CODICE_MATRICOLA", appartenentiMap.get("codiceMatricola"));
                            codiceMatricola = appartenentiMap.get("codiceMatricola").toString();
                            mA.setCodiceMatricola(Integer.parseInt(appartenentiMap.get("codiceMatricola").toString()));
                        }
//                      COGNOME bloccante
                        if (appartenentiMap.get("cognome") == null || appartenentiMap.get("cognome").equals("")) {
                            mapError.put("ERRORE", mapError.get("ERRORE") + " COGNOME,");
                            bloccante = true;
                            mapError.put("COGNOME", "");
                            mA.setCognome(null);
                        } else {
                            mapError.put("COGNOME", appartenentiMap.get("cognome"));
                            mA.setCognome(appartenentiMap.get("cognome").toString());
                        }
//                      NOME bloccante
                        if (appartenentiMap.get("nome") == null || appartenentiMap.get("nome").equals("")) {
                            mapError.put("ERRORE", mapError.get("ERRORE") + " NOME,");
                            bloccante = true;
                            mapError.put("NOME", "");
                            mA.setNome(null);
                        } else {
                            mapError.put("NOME", appartenentiMap.get("nome"));
                            mA.setNome(appartenentiMap.get("nome").toString());
                        }
//                      CODICE_FISCALE bloccante
                        if (appartenentiMap.get("codiceFiscale") == null || appartenentiMap.get("codiceFiscale").equals("")) {
                            mapError.put("ERRORE", mapError.get("ERRORE") + " codiceFiscale,");
                            bloccante = true;
                            mapError.put("CODICE_FISCALE", "");
                            mA.setCodiceFiscale(null);
                        } else {
                            mapError.put("CODICE_FISCALE", appartenentiMap.get("codiceFiscale"));
                            mA.setCodiceFiscale(appartenentiMap.get("codiceFiscale").toString());
                        }
                        String idCasella = null;
//                      ID_CASELLA bloccante
                        if (appartenentiMap.get("idCasella") == null || appartenentiMap.get("idCasella").equals("")) {
                            mapError.put("ERRORE", mapError.get("ERRORE") + " idCasella,");
                            idCasella = "";
                            mapError.put("ID_CASELLA", "");
                            mA.setIdCasella(null);
                        } else {
                            if (mdrStrutturaRepository.selectStrutturaUtenteByIdCasellaAndIdAzienda(Integer.parseInt(appartenentiMap.get("idCasella").toString()), idAzienda) <= 0) {
                                mapError.put("ERRORE", " manca la struttura nella tabella struttura,");
                            }
                            mapError.put("ID_CASELLA", appartenentiMap.get("idCasella"));
                            mA.setIdCasella(Integer.parseInt(appartenentiMap.get("idCasella").toString()));
                            idCasella = appartenentiMap.get("idCasella").toString();

                        }
//                      DATAIN bloccante
                        if (appartenentiMap.get("datain") == null || appartenentiMap.get("datain").equals("")) {
                            mapError.put("ERRORE", mapError.get("ERRORE") + " datain,");
                            mapError.put("DATAIN", "");
                            mA.setDatain(null);
                        } else {
                            mapError.put("DATAIN", appartenentiMap.get("datain"));
                            mA.setDatain(formattattore(appartenentiMap.get("datain"), "yyyy-MM-dd hh:mm:ss"));
                        }
                        LocalDateTime datafi = null;
                        LocalDateTime datain = null;
                        String datafiString = null;
                        String datainString = null;

                        if (appartenentiMap.get("datafi") != null && appartenentiMap.get("datafi") != "") {
                            datafi = formattattore(appartenentiMap.get("datafi"), "yyyy-MM-dd hh:mm:ss");
                            datafiString = UtilityFunctions.getLocalDateTimeString(datafi);
                        }

                        if (appartenentiMap.get("datain") != null && appartenentiMap.get("datain") != "") {
                            datain = formattattore(appartenentiMap.get("datain"), "yyyy-MM-dd hh:mm:ss");
                            datainString = UtilityFunctions.getLocalDateTimeString(datain);
                        }
                        if ((!bloccante) && (mdrAppartenentiRepository.select_multidefinictions_user_byidazienda(codiceAzienda,
                                Integer.parseInt(codiceMatricola),
                                Integer.parseInt(idCasella),
                                datafiString,
                                datainString)) > 0) {
                            mapError.put("ERRORE", mapError.get("ERRORE") + " utente con piu afferenze dirette per lo stesso periodo,");
                        }
                        if (appartenentiMap.get("datafi") == null || appartenentiMap.get("datafi").equals("")) {
                            mapError.put("DATAFI", "");
                            mA.setDatafi(null);
                        } else {
                            mapError.put("DATAFI", appartenentiMap.get("datafi"));
                            mA.setDatafi(formattattore(appartenentiMap.get("datafi"), "yyyy-MM-dd hh:mm:ss"));
                        }
//                      TIPO_APPARTENENZA bloccante
                        if (appartenentiMap.get("tipoAppartenenza") == null || appartenentiMap.get("tipoAppartenenza").equals("")) {
                            mapError.put("ERRORE", mapError.get("ERRORE") + " tipoAppartenenza,");
                            mapError.put("TIPO_APPARTENENZA", "");
                            mA.setTipoAppartenenza(null);
                        } else {
                            mapError.put("TIPO_APPARTENENZA", appartenentiMap.get("tipoAppartenenza"));
                            mA.setTipoAppartenenza(appartenentiMap.get("tipoAppartenenza").toString());
                        }
//                      DataAssunzione bloccante
                        if (appartenentiMap.get("dataAssunzione") == null || appartenentiMap.get("dataAssunzione").equals("")) {
                            mapError.put("ERRORE", mapError.get("ERRORE") + " dataAssunzione,");
                            mapError.put("DATA_ASSUNZIONE", "");
                        } else {
                            mapError.put("DATA_ASSUNZIONE", appartenentiMap.get("dataAssunzione"));
                            mA.setDataAssunzione(formattattore(appartenentiMap.get("dataAssunzione"), "yyyy-MM-dd hh:mm:ss"));
                        }
//                      USERNAME bloccante
                        if (appartenentiMap.get("username") == null || appartenentiMap.get("username").equals("")) {
                            mA.setUsername("");
                            mapError.put("USERNAME", "");
                            mapError.put("ERRORE", mapError.get("ERRORE") + " manca username,");
                        } else {
                            mapError.put("USERNAME", appartenentiMap.get("username"));
                            mA.setUsername(appartenentiMap.get("username").toString());
                        }
//                      DATA_DIMISSIONE non bloccante
                        if (appartenentiMap.get("dataDimissione") == null || appartenentiMap.get("dataDimissione").equals("")) {
                            mapError.put("DATA_DIMISSIONE", appartenentiMap.get("dataDimissione"));
                            mA.setDataDimissione(null);
                        } else {
                            mapError.put("DATA_DIMISSIONE", appartenentiMap.get("dataDimissione"));
                            mA.setDataDimissione(formattattore(appartenentiMap.get("dataDimissione"), "yyyy-MM-dd hh:mm:ss"));
                        }
                        mA.setIdAzienda(azienda);
                        mdrAppartenentiRepository.save(mA);
                        mapWriter.write(mapError, headers_Error_Generator(tipo), getProcessorsError(tipo, codiceAzienda));
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
                        mapError.put("CODICE_ENTE", codiceAzienda);
                        mR.setCodiceEnte(codiceAzienda);
//                      CODICE_MATRICOLA bloccante
                        String codiceMatricola = null;
                        if (responsabiliMap.get("codiceMatricola") == null || responsabiliMap.get("codiceMatricola").equals("")) {
                            mapError.put("ERRORE", mapError.get("ERRORE") + " codiceMatricola,");
                            mapError.put("CODICE_MATRICOLA", "");
                            codiceMatricola = "";
                            mR.setCodiceMatricola(null);
                        } else {
                            mapError.put("CODICE_MATRICOLA", responsabiliMap.get("codiceMatricola"));
                            codiceMatricola = responsabiliMap.get("codiceMatricola").toString();
                            mR.setCodiceMatricola(Integer.parseInt(responsabiliMap.get("codiceMatricola").toString()));
                            //responsabile presente tra gli autenti
                            if (mdrAppartenentiRepository.countUsertByCodiceMatricola(Integer.parseInt(responsabiliMap.get("codiceMatricola").toString())) <= 0) {
                                mapError.put("ERRORE", mapError.get("ERRORE") + " codiceMatricola non trovata nella tabella appartenenti,");
                            }
                        }

//                      ID_CASELLA bloccante
                        String id_casella = null;
                        if (responsabiliMap.get("idCasella") == null || responsabiliMap.get("idCasella").equals("")) {
                            mapError.put("ERRORE", mapError.get("ERRORE") + " idCasella,");
                            id_casella = "";
                            mapError.put("ID_CASELLA", "");
                            mR.setIdCasella(null);
                        } else {
                            mapError.put("ID_CASELLA", responsabiliMap.get("idCasella"));
                            id_casella = responsabiliMap.get("idCasella").toString();
                            mR.setIdCasella(Integer.parseInt(responsabiliMap.get("idCasella").toString()));
                            if (mdrStrutturaRepository.selectStrutturaUtenteByIdCasellaAndIdAzienda(Integer.parseInt(responsabiliMap.get("idCasella").toString()), idAzienda) <= 0) {
                                mapError.put("ERRORE", mapError.get("ERRORE") + " idCasella non trovata nella tabella strutture,");
                            }
                        }
//                      DATAIN bloccante                        
                        if (responsabiliMap.get("datain") == null || responsabiliMap.get("datain").equals("")) {
                            mapError.put("ERRORE", mapError.get("ERRORE") + " datain,");
                            mapError.put("DATAIN", "");
                            mR.setDatain(null);
                        } else {
                            mapError.put("DATAIN", responsabiliMap.get("datain"));
                            mR.setDatain(formattattore(responsabiliMap.get("datain"), "yyyy-MM-dd hh:mm:ss"));
                        }
                        LocalDateTime datafi = null;
                        LocalDateTime datain = null;
                        String datafiString = null;
                        String datainString = null;
                        if (responsabiliMap.get("datafi") != null && responsabiliMap.get("datafi") != "") {
                            datafi = formattattore(responsabiliMap.get("datafi"), "yyyy-MM-dd hh:mm:ss");
                            datafiString = UtilityFunctions.getLocalDateTimeString(datafi);
                        }

                        if (responsabiliMap.get("datain") != null && responsabiliMap.get("datain") != "") {
                            datain = formattattore(responsabiliMap.get("datain"), "yyyy-MM-dd hh:mm:ss");
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
                            mapError.put("DATAFI", "");
                            mR.setDatafi(null);
                        } else {
                            mapError.put("DATAFI", responsabiliMap.get("datafi"));
                            mR.setDatafi(formattattore(responsabiliMap.get("datafi"), "yyyy-MM-dd hh:mm:ss"));
                        }
//                      TIPO bloccante                        
                        if (responsabiliMap.get("tipo") == null || responsabiliMap.get("tipo").equals("")) {
                            mapError.put("ERRORE", mapError.get("ERRORE") + " tipo,");
                            mR.setTipo(null);
                        } else {
                            mapError.put("TIPO", responsabiliMap.get("tipo"));
                            mR.setTipo(responsabiliMap.get("tipo").toString());
                        }

                        mR.setIdAzienda(azienda);
                        mdrResponsabiliRepository.save(mR);
                        mapWriter.write(mapError, headers_Error_Generator(tipo), getProcessorsError(tipo, codiceAzienda));
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
                        if (strutturaMap.get("datafi") != null && strutturaMap.get("datafi") != "") {
                            datafi = formattattore(strutturaMap.get("datafi"), "yyyy-MM-dd hh:mm:ss");
                            datafiString = UtilityFunctions.getLocalDateTimeString(datafi);
                        }

                        if (strutturaMap.get("datain") != null && strutturaMap.get("datain") != "") {
                            datain = formattattore(strutturaMap.get("datain"), "yyyy-MM-dd hh:mm:ss");
                            datainString = UtilityFunctions.getLocalDateTimeString(datain);
                        }
                        String id_casella = null;
                        if (strutturaMap.get("idCasella") == null || strutturaMap.get("idCasella").equals("")) {
                            mapError.put("ERRORE", mapError.get("ERRORE") + " idCasella assente,");
                            bloccante = true;
                            id_casella = "";
                            mapError.put("ID_CASELLA", "");
                            mS.setIdCasella(null);
                        } else {
                            mapError.put("ID_CASELLA", strutturaMap.get("idCasella"));
                            id_casella = strutturaMap.get("idCasella").toString();
                            mS.setIdCasella(Integer.parseInt(strutturaMap.get("idCasella").toString()));
                            //struttura definita piu volte nello stesso arco temporale
                            if (mdrStrutturaRepository.selectMultiDefinictionsStructureByIdAzienda(idAzienda, Integer.parseInt(id_casella), datainString, datainString) > 0) {
                                bloccante = true;
                                mapError.put("ERRORE", mapError.get("ERRORE") + " struttura definita piu volte nello stesso arco temporale,");
                            }
                        }

                        if (strutturaMap.get("idPadre") == null || strutturaMap.get("idPadre").equals("")) {
                            mapError.put("ID_PADRE", "");
                            mS.setIdPadre(null);
                        } else {
                            mapError.put("ID_PADRE", strutturaMap.get("idPadre"));
                            mS.setIdPadre(Integer.parseInt(strutturaMap.get("idPadre").toString()));
                        }

                        if (strutturaMap.get("descrizione") == null || strutturaMap.get("descrizione").equals("")) {
                            mapError.put("ERRORE", mapError.get("ERRORE") + " descrizione assente,");
                            mapError.put("DESCRIZIONE", "");
                            mS.setDescrizione(null);
                            bloccante = true;
                        } else {
                            mapError.put("DESCRIZIONE", strutturaMap.get("descrizione"));
                            mS.setDescrizione(strutturaMap.get("descrizione").toString());
                        }

                        if (strutturaMap.get("datain") == null || strutturaMap.get("datain").equals("")) {
                            mapError.put("ERRORE", mapError.get("ERRORE") + " datain,");
                            mapError.put("DATAIN", "");
                            mS.setDatain(null);
                            bloccante = true;
                        } else {
                            mapError.put("DATAIN", strutturaMap.get("datain"));
                            mS.setDatain(datain);
                        }

                        if (strutturaMap.get("datafi") == null || strutturaMap.get("datafi").equals("")) {
                            mapError.put("DATAFI", "");
                            mS.setDatafi(null);
                        } else {
                            mapError.put("DATAFI", strutturaMap.get("datafi"));
                            mS.setDatafi(datafi);
                        }

                        if (strutturaMap.get("tipoLegame") == null || strutturaMap.get("tipoLegame").equals("")) {
                            mapError.put("TIPO_LEGAME", "");
                            mS.setTipoLegame(null);
                        } else {
                            mapError.put("TIPO_LEGAME", strutturaMap.get("tipoLegame"));
                            mS.setTipoLegame(strutturaMap.get("tipoLegame").toString());
                        }
                        mapError.put("CODICE_ENTE", codiceAzienda);
                        mS.setCodiceEnte(codiceAzienda);

                        mS.setIdAzienda(azienda);
                        mdrStrutturaRepository.save(mS);
                        mapWriter.write(mapError, headers_Error_Generator(tipo), getProcessorsError(tipo, codiceAzienda));
                    }
                    Boolean riciclo=false;
                    //struttura padre non trovata
                    List<Integer> selectDaddyByIdAzienda = mdrStrutturaRepository.selectDaddyByIdAzienda(idAzienda);
                    if (selectDaddyByIdAzienda != null && selectDaddyByIdAzienda.size() > 0 && selectDaddyByIdAzienda.get(0) != null) {
                        //mapError.put("ERRORE", mapError.get("ERRORE") + " non trovati i padri " + selectDaddyByIdAzienda + " nella tabella strutture,");
                        //mapWriter.write(mapError, headers_Error_Generator(tipo), getProcessorsError(tipo, codiceAzienda));
                        bloccante = true;
                        
                    }
                    //strutture che non rispettano il padre
                    List<Integer> caselleInvalide = mdrStrutturaRepository.caselleInvalide(idAzienda);
                    if (caselleInvalide != null && caselleInvalide.size() > 0 && caselleInvalide.get(0) != null) {
                        //mapError.put("ERRORE", mapError.get("ERRORE") + " caselle che non rispettano l'arco temporale del padre: " + caselleInvalide + ",");
                        //mapWriter.write(mapError, headers_Error_Generator(tipo), getProcessorsError(tipo, codiceAzienda));
                        bloccante = true;
                        
                        
                    }
                    mapWriter.close();
                    try (InputStreamReader csvErrorFileRIP = new InputStreamReader(new FileInputStream(csvErrorFile));){

                        mapErrorReader = new CsvMapReader(csvErrorFileRIP, SEMICOLON_DELIMITED);
                        mapErrorReader.getHeader(true);

                        mapErrorWriter = new CsvMapWriter(new FileWriter(csvErrorFile),SEMICOLON_DELIMITED);
                        mapErrorWriter.writeHeader(headers_Error_Generator(tipo));

                        Map<String, Object> strutturaErrorMap = null;
                        while ((strutturaErrorMap = mapErrorReader.read(headers_Error_Generator(tipo), getProcessorsError(tipo, codiceAzienda))) != null) {
                            Map<String, Object> strutturaErrorMapWrite = null;
                            strutturaErrorMapWrite=strutturaErrorMap;
                            if (caselleInvalide.contains(Integer.parseInt(strutturaErrorMap.get("ID_CASELLA").toString()))){
                                if (strutturaErrorMap.get("ERRORE")!= null){
                                    strutturaErrorMapWrite.put("ERRORE", strutturaErrorMap.get("ERRORE") + " non rispetta l'arco temporale del padre,");
                                }else{
                                    strutturaErrorMapWrite.put("ERRORE", " non rispetta l'arco temporale del padre,");
                                
                                }
                            }
                            if (selectDaddyByIdAzienda.contains(strutturaErrorMap.get("ID_PADRE")) && selectDaddyByIdAzienda.get(0)!=null){
                                if (strutturaErrorMap.get("ERRORE")!=null){
                                    strutturaErrorMapWrite.put("ERRORE", strutturaErrorMap.get("ERRORE") + " padre non presente,");
                                }else{
                                    strutturaErrorMapWrite.put("ERRORE", " padre non presente,");
                                }

                            }
                            mapErrorWriter.write(strutturaErrorMapWrite, headers_Error_Generator(tipo), getProcessorsError(tipo, codiceAzienda));

                        }
                    } catch(Exception ex) {
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

                        if (trasformazioniMap.get("progressivoRiga") == null || trasformazioniMap.get("progressivoRiga").equals("")) {
                            mapError.put("ERRORE", mapError.get("ERRORE") + " progressivoRiga,");
                            mapError.put("PROGRESSIVO_RIGA", "");
                            mT.setProgressivoRiga(null);
                            bloccante = true;
                        } else {
                            mapError.put("PROGRESSIVO_RIGA", trasformazioniMap.get("progressivoRiga"));
                            mT.setProgressivoRiga(Integer.parseInt(trasformazioniMap.get("progressivoRiga").toString()));
                        }

                        if (trasformazioniMap.get("idCasellaPartenza") == null || trasformazioniMap.get("idCasellaPartenza").equals("")) {
                            mapError.put("ERRORE", mapError.get("ERRORE") + " ID_CASELLA_PARTENZA,");
                            mapError.put("ID_CASELLA_PARTENZA", "");
                            mT.setIdCasellaPartenza(null);
                            bloccante = true;
                        } else {
                            mapError.put("ID_CASELLA_PARTENZA", trasformazioniMap.get("idCasellaPartenza"));
                            mT.setIdCasellaPartenza(Integer.parseInt(trasformazioniMap.get("idCasellaPartenza").toString()));
                            if (mdrTrasformazioniRepository.isTransformableByIdAzienda(idAzienda, Integer.parseInt(trasformazioniMap.get("idCasellaPartenza").toString()), formattattore(trasformazioniMap.get("dataTrasformazione"), "yyyy-MM-dd hh:mm:ss")) <= 0) {
                                bloccante = true;
                                mapError.put("ERRORE", mapError.get("ERRORE") + " casella di partenza non trovata nella tabella strutture,");
                            }
                        }

                        if (trasformazioniMap.get("dataTrasformazione") == null || trasformazioniMap.get("dataTrasformazione").equals("")) {
                            mapError.put("ERRORE", mapError.get("ERRORE") + " DATA_TRASFORMAZIONE,");
                            mapError.put("DATA_TRASFORMAZIONE", "");
                            mT.setDataTrasformazione(null);
                        } else {
                            mapError.put("DATA_TRASFORMAZIONE", trasformazioniMap.get("dataTrasformazione"));
                            mT.setDataTrasformazione(formattattore(trasformazioniMap.get("dataTrasformazione"), "yyyy-MM-dd hh:mm:ss"));
                        }

                        if (trasformazioniMap.get("motivo") == null || trasformazioniMap.get("motivo").equals("")) {
                            mapError.put("ERRORE", mapError.get("ERRORE") + " MOTIVO,");
                            mapError.put("MOTIVO", "");
                            mT.setMotivo(null);
                            bloccante = true;
                            //non ci sta un motivo copio paripari id casella di arrivo non ho elementi per sapere se ci dovrebbe o meno essere qualcosa
                            mapError.put("ID_CASELLA_ARRIVO", trasformazioniMap.get("idCasellaArrivo"));
                            mT.setIdCasellaArrivo(Integer.parseInt(trasformazioniMap.get("idCasellaArrivo").toString()));
                        } else {
                            mapError.put("MOTIVO", trasformazioniMap.get("motivo"));
                            mT.setMotivo(trasformazioniMap.get("motivo").toString());
                            //TODO controllo su casella di arrivo
                            if (trasformazioniMap.get("motivo").toString().equalsIgnoreCase("x")) {
                                if (trasformazioniMap.get("idCasellaArrivo") == null || trasformazioniMap.get("idCasellaArrivo").equals("")) {
                                    mapError.put("ERRORE", mapError.get("ERRORE") + " ID_CASELLA_ARRIVO,");
                                    mapError.put("ID_CASELLA_ARRIVO", "");
                                    mT.setIdCasellaArrivo(null);
                                    bloccante = true;
                                } else {
                                    mapError.put("ID_CASELLA_ARRIVO", trasformazioniMap.get("idCasellaArrivo"));
                                    mT.setIdCasellaArrivo(Integer.parseInt(trasformazioniMap.get("idCasellaArrivo").toString()));
                                    if (mdrTrasformazioniRepository.isTransformableByIdAzienda(idAzienda, Integer.parseInt(trasformazioniMap.get("idCasellaArrivo").toString()), formattattore(trasformazioniMap.get("dataTrasformazione"), "yyyy-MM-dd hh:mm:ss")) <= 0) {
                                        bloccante = true;
                                        mapError.put("ERRORE", mapError.get("ERRORE") + " casella di arrivo non trovata nella tabella strutture,");
                                    }
                                }
                            } else {
                                if (trasformazioniMap.get("idCasellaArrivo") == null || trasformazioniMap.get("idCasellaArrivo").equals("")) {
                                    mapError.put("ID_CASELLA_ARRIVO", "");
                                    mT.setIdCasellaArrivo(null);
                                } else {
                                    mapError.put("ID_CASELLA_ARRIVO", trasformazioniMap.get("idCasellaArrivo"));
                                    mapError.put("ERRORE", mapError.get("ERRORE") + " casella di trovata una casella di arrivo ma il motivo non è valido ,");
                                    mT.setIdCasellaArrivo(Integer.parseInt(trasformazioniMap.get("idCasellaArrivo").toString()));
                                }
                            }
                        }

                        if (trasformazioniMap.get("datainPartenza") == null || trasformazioniMap.get("datainPartenza").equals("")) {
                            mapError.put("ERRORE", mapError.get("ERRORE") + " DATAIN_PARTENZA,");
                            mapError.put("DATAIN_PARTENZA", "");
                            mT.setDatainPartenza(null);
                            bloccante = true;
                        } else {
                            mapError.put("DATAIN_PARTENZA", trasformazioniMap.get("datainPartenza"));
                            mT.setDatainPartenza(formattattore(trasformazioniMap.get("datainPartenza"), "yyyy-MM-dd hh:mm:ss"));
                        }

                        if (trasformazioniMap.get("dataoraOper") == null || trasformazioniMap.get("dataoraOper").equals("")) {
                            mapError.put("ERRORE", mapError.get("ERRORE") + " DATAORA_OPER inserito automaticamente,");
                            LocalDateTime now = LocalDateTime.now();
                            mapError.put("DATAORA_OPER", now.toString());
                            mT.setDataoraOper(now);
                        } else {
                            mapError.put("DATAORA_OPER", trasformazioniMap.get("dataoraOper"));
                            mT.setDataoraOper(formattattore(trasformazioniMap.get("dataoraOper"), "yyyy-MM-dd hh:mm:ss"));
                        }
                        mapError.put("CODICE_ENTE", codiceAzienda);
                        mT.setCodiceEnte(codiceAzienda);

                        mT.setIdAzienda(azienda);

                        mdrTrasformazioniRepository.save(mT);
                        mapWriter.write(mapError, headers_Error_Generator(tipo), getProcessorsError(tipo, codiceAzienda));
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
                    if (!tipo.equals("STRUTTURA")){
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
    private static CellProcessor[] getProcessors(String tipo, Number codiceAzienda) {
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
                headers = new String[]{"codiceEnte", "codiceMatricola", "cognome",
                    "nome", "codiceFiscale", "idCasella", "datain", "datafi", "tipoAppartenenza",
                    "username", "dataAssunzione", "dataDimissione"};
                break;
            case "RESPONSABILI":
                headers = new String[]{"codiceEnte", "codiceMatricola",
                    "idCasella", "datain", "datafi", "tipo"};
                break;
            case "STRUTTURA":
                headers = new String[]{"idCasella", "idPadre", "descrizione",
                    "datain", "datafi", "tipoLegame", "codiceEnte"};
                break;
            case "TRASFORMAZIONI":
                headers = new String[]{"progressivoRiga", "idCasellaPartenza", "idCasellaArrivo", "dataTrasformazione",
                    "motivo", "datainPartenza", "dataoraOper", "codiceEnte"};
                break;
            default:
                System.out.println("non dovrebbe essere");
                break;
        }
        return headers;
    }

    private static String[] headers_Error_Generator(String tipo) {
        String[] headers = null;
        switch (tipo) {
            case "APPARTENENTI":
                headers = new String[]{"CODICE_ENTE", "CODICE_MATRICOLA", "COGNOME",
                    "NOME", "CODICE_FISCALE", "ID_CASELLA", "DATAIN", "DATAFI", "TIPO_APPARTENENZA",
                    "USERNAME", "DATA_ASSUNZIONE", "DATA_DIMISSIONE", "ERRORE"};
                break;
            case "RESPONSABILI":
                headers = new String[]{"CODICE_ENTE", "CODICE_MATRICOLA",
                    "ID_CASELLA", "DATAIN", "DATAFI", "TIPO", "ERRORE"};
                break;
            case "STRUTTURA":
                headers = new String[]{"ID_CASELLA", "ID_PADRE", "DESCRIZIONE",
                    "DATAIN", "DATAFI", "TIPO_LEGAME", "CODICE_ENTE", "ERRORE"};
                break;
            case "TRASFORMAZIONI":
                headers = new String[]{"PROGRESSIVO_RIGA", "ID_CASELLA_PARTENZA", "ID_CASELLA_ARRIVO", "DATA_TRASFORMAZIONE",
                    "MOTIVO", "DATAIN_PARTENZA", "DATAORA_OPER", "CODICE_ENTE", "ERRORE"};
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
     *
     * @param o
     * @param formatoDestinazione
     * @return
     * @throws ParseException
     */
    public LocalDateTime formattattore(Object o, String formatoDestinazione) throws ParseException {
        if (o != null) {
            try {
                Instant toInstant = new SimpleDateFormat(formatoDestinazione).parse(o.toString()).toInstant();
                return LocalDateTime.ofInstant(toInstant, ZoneId.systemDefault());
            } catch (ParseException e) {
                //non è stato parsato
            }
            
        }
        return null;
    }
}
