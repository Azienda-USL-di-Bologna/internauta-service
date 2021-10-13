package it.bologna.ausl.internauta.service.ribaltone;

import com.querydsl.core.types.dsl.BooleanExpression;
import it.bologna.ausl.blackbox.utils.UtilityFunctions;
import it.bologna.ausl.internauta.service.baborg.utils.BaborgUtils;
import it.bologna.ausl.internauta.service.configuration.utils.ReporitoryConnectionManager;
import it.bologna.ausl.internauta.service.exceptions.ribaltonecsv.BaborgCSVAnomaliaException;
import it.bologna.ausl.internauta.service.exceptions.ribaltonecsv.BaborgCSVBloccanteException;
import it.bologna.ausl.internauta.service.exceptions.ribaltonecsv.BaborgCSVBloccanteRigheException;
import it.bologna.ausl.internauta.service.exceptions.ribaltonecsv.RibaltoneCSVCheckException;
import it.bologna.ausl.internauta.service.repositories.baborg.AziendaRepository;
import it.bologna.ausl.internauta.service.repositories.baborg.ImportazioniOrganigrammaRepository;
import it.bologna.ausl.internauta.service.repositories.baborg.PersonaRepository;
import it.bologna.ausl.internauta.service.repositories.baborg.UtenteRepository;
import it.bologna.ausl.internauta.service.repositories.gru.MdrAppartenentiRepository;
import it.bologna.ausl.internauta.service.repositories.gru.MdrResponsabiliRepository;
import it.bologna.ausl.internauta.service.repositories.gru.MdrStrutturaRepository;
import it.bologna.ausl.internauta.service.repositories.gru.MdrStrutturaRepositoryCustomImpl;
import it.bologna.ausl.internauta.service.repositories.gru.MdrTrasformazioniRepository;
import it.bologna.ausl.internauta.service.utils.ParametriAziendeReader;
import it.bologna.ausl.model.entities.baborg.Azienda;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.BeanFactory;
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

    private static final Logger log = LoggerFactory.getLogger(BaborgUtils.class);
    private static Map<String, Integer> map;

    @Autowired
    MdrStrutturaRepositoryCustomImpl mdrStrutturaRepositoryCustomImpl;

    @Autowired
    ImportazioniOrganigrammaRepository importazioniOrganigrammaRepository;

    @PersistenceContext
    EntityManager em;

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
    ReporitoryConnectionManager mongoConnectionManager;

    @Autowired
    ParametriAziendeReader parametriAziende;

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
            }

        }
        return null;
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

    public Boolean overlap(ZonedDateTime dataInizioA, ZonedDateTime dataFineA, ZonedDateTime dataInizioB, ZonedDateTime dataFineB) {

        if (dataFineA == null) {
            dataFineA = ZonedDateTime.of(LocalDateTime.MAX, ZoneId.systemDefault());
        }
        if (dataFineB == null) {
            dataFineB = ZonedDateTime.of(LocalDateTime.MAX, ZoneId.systemDefault());
        }
        return (dataInizioA.compareTo(dataFineB) <= 0 && dataFineA.compareTo(dataInizioB) >= 0) && dataInizioA.compareTo(dataInizioB) <= 0;
    }

    /**
     *
     * @param elementi
     * @param dataInizio
     * @param dataFine
     * @return false se elementi è vuoto
     */
    public Boolean isPeriodiSovrapposti(List<Map<String, Object>> elementi, ZonedDateTime dataInizio, ZonedDateTime dataFine) {
        if (elementi.isEmpty()) {
            return false;
        }
        return elementi.stream().anyMatch(elemento -> overlap(formattattore(elemento.get("datain")), formattattore(elemento.get("datafi")), dataInizio, dataFine));
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

    private String getConsonanti(String string) {

        return string.trim()
                .replaceAll("YẙỲỴỶỸŶŸÝ", "Y")
                .replaceAll("[^qQwWrRtTpPsSdDfFgGhHkKlLzZxXcCvVbBnNmMjJYy]", "");
    }

    private String getVocali(String string) {
        return string.trim().toUpperCase()
                .replaceAll("[ÀÁÂÃÄÅĀĂĄǺȀȂẠẢẤẦẨẪẬẮẰẲẴẶḀ]", "A")
                .replaceAll("[ÆǼEȄȆḔḖḘḚḜẸẺẼẾỀỂỄỆĒĔĖĘĚÈÉÊË]", "E")
                .replaceAll("[IȈȊḬḮỈỊĨĪĬĮİÌÍÎÏĲ]", "I")
                .replaceAll("OŒØǾȌȎṌṎṐṒỌỎỐỒỔỖỘỚỜỞỠỢŌÒÓŎŐÔÕÖ", "O")
                .replaceAll("[UŨŪŬŮŰŲÙÚÛÜȔȖṲṴṶṸṺỤỦỨỪỬỮỰ]", "U")
                //.replaceAll("YẙỲỴỶỸŶŸÝ", "Y")
                .replaceAll("[^aAeEiIoOuU]", "");

    }

    private String codiceCognome(String cognome) {
        String vocali_COGNOME = getVocali(cognome).concat("XXX");
        String consonanti_COGNOME = getConsonanti(cognome);
        String s = "";
        if (consonanti_COGNOME.length() >= 3) {
            for (int i = 0; i < 3; i++) {
                s = s + consonanti_COGNOME.charAt(i);
            }
            return s;
        }
        if (consonanti_COGNOME.length() == 2) {
            if (vocali_COGNOME.length() > 0) {
                s = s + consonanti_COGNOME.charAt(0) + consonanti_COGNOME.charAt(1) + vocali_COGNOME.charAt(0);
            }
            return s;
        }
        if (consonanti_COGNOME.length() == 1) {
            if (vocali_COGNOME.length() >= 2) {
                s = s + consonanti_COGNOME.charAt(0) + vocali_COGNOME.charAt(0) + vocali_COGNOME.charAt(1);
            }
            return s;
        } else {
            for (int i = 0; i < 3; i++) {
                s = s + vocali_COGNOME.charAt(i);
            }

            return s;
        }
    }

    private String codiceNome(String nome) {
        String vocali_NOME = getVocali(nome).concat("XXX");
        String consonanti_NOME = getConsonanti(nome);

        String s = "";
        if (consonanti_NOME.length() > 3) {
            s = s + consonanti_NOME.charAt(0) + consonanti_NOME.charAt(2) + consonanti_NOME.charAt(3);
            return s;
        }
        if (consonanti_NOME.length() == 3) {
            for (int i = 0; i < 3; i++) {
                s = s + consonanti_NOME.charAt(i);
            }
            return s;
        }
        if (consonanti_NOME.length() == 2) {
            s = s + consonanti_NOME.charAt(0) + consonanti_NOME.charAt(1) + vocali_NOME.charAt(0);
            return s;
        }
        if (consonanti_NOME.length() == 1) {
            s = s + consonanti_NOME.charAt(0) + vocali_NOME.charAt(0) + vocali_NOME.charAt(1);
            return s;
        } else {
            for (int i = 0; i < 3; i++) {
                s = s + vocali_NOME.charAt(i);
            }
            return s;
        }
    }

    private String partialCF(String nome, String cognome) {
        return codiceCognome(cognome.trim()).concat(codiceNome(nome.trim()));
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
        BooleanExpression predicateAzienda = null;
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
                        riga = mapReader.getLineNumber();
                        log.info("getLineNumber: " + mapReader.getLineNumber());
                        // Inserisco la riga
                        MdrAppartenenti mA = new MdrAppartenenti();
//                      preparo la mappa di errore
                        mapError.put("ERRORE", "");
                        mapError.put("Anomalia", "");

//                      CODICE_MATRICOLA bloccante
                        anomali = checkCodiceMatricolaA(appartenentiMap, mapError);
                        anomalia = anomalia ? anomalia : anomali;

//                      COGNOME bloccante
                        anomali = checkCognomeA(appartenentiMap, mapError);
                        anomalia = anomalia ? anomalia : anomali;

//                      NOME bloccante
                        anomali = checkNomeA(appartenentiMap, mapError);
                        anomalia = anomalia ? anomalia : anomali;

//                      CODICE_FISCALE bloccante
                        anomali = checkCodiceFiscaleA(appartenentiMap, mapError);
                        anomalia = anomalia ? anomalia : anomali;

                        String idCasella = checkIdCasellaA(appartenentiMap, mapError, selectDateOnStruttureByIdAzienda);
                        anomalia = anomalia ? anomalia : idCasella.equals("");
                        if (appartenentiMap.get("id_casella") != null && appartenentiMap.get("id_casella") != "") {
                            if (!appartenentiMap.get("id_casella").toString().equals(idCasella)) {
                                idCasella = appartenentiMap.get("id_casella").toString();
                            }

                        }
//                      ID_CASELLA bloccante

//                      DATAIN bloccante
                        anomali = checkDatainA(appartenentiMap, mapError);
                        anomalia = anomalia ? anomalia : anomali;

                        ZonedDateTime datafi = null;
                        ZonedDateTime datain = null;
                        String datafiString = null;
                        String datainString = null;
                        //basta vedere anomali perche se ci sono problemi li ho gia controllati col checkDatainA
                        if (!anomali) {
                            datain = formattattore(appartenentiMap.get("datain"));
                            datainString = UtilityFunctions.getZonedDateTimeString(datain);
                        }
                        if (appartenentiMap.get("datafi") != null && (!appartenentiMap.get("datafi").toString().trim().equals("") || appartenentiMap.get("datafi") != "")) {
                            datafi = formattattore(appartenentiMap.get("datafi"));
                            datafiString = UtilityFunctions.getZonedDateTimeString(datafi);
                        }

                        if (appartenentiMap.get("datafi") == null || appartenentiMap.get("datafi").toString().trim().equals("") || appartenentiMap.get("datafi") == "") {
                            mapError.put("datafi", "");
                        } else {
                            mapError.put("datafi", appartenentiMap.get("datafi"));
                        }

                        if (checkDateFinisconoDopoInizio(datain, datafi)) {
                            anomalia = true;
                            if (mapError.get("ERRORE") != null) {
                                mapError.put("ERRORE", mapError.get("ERRORE") + " questa riga non è valida perche la data di fine è precedente alla data di fine, ");
                            } else {
                                mapError.put("ERRORE", "questa riga non è valida perche la data di fine è precedente alla data di fine,");
                            }
                        }

                        //Codice Ente 
                        Integer codiceEnte = checkCodiceEnte(appartenentiMap, mapError, codiceAzienda);
                        anomalia = anomalia ? anomalia : codiceEnte.equals("");
                        mA.setCodiceEnte(codiceEnte);

//                      TIPO_APPARTENENZA bloccante
                        anomali = checkTipoAppatenenza(appartenentiMap, mapError, idCasella, datain, datafi, controlloZeroUno, codiceEnte, appartenentiDiretti, appartenentiFunzionali, mapReader, righeAnomaleFunzionali, righeAnomaleDirette);
                        anomalia = anomalia ? anomalia : anomali;

//                      DataAssunzione bloccante
                        anomali = checkDataAssunzioneA(appartenentiMap, mapError);
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
                    List<Integer> codiciMatricoleConAppFunzionaliENonDirette = codiciMatricoleConAppFunzionaliENonDirette(appartenentiFunzionali, appartenentiDiretti);
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
                        String codice_matricola = checkCodiceMatricolaR(responsabiliMap, mapError, selectDateOnAppartenentiByIdAzienda);
                        if (codice_matricola.equals("")) {
                            mR.setCodiceMatricola(null);
                            //nRigheAnomale++;
                            anomalia = true;
                            anomaliaRiga = true;
                        } else {
                            mR.setCodiceMatricola(Integer.parseInt(codice_matricola));

                        }

//                      DATAIN bloccante
                        anomali = !checkDatainR(responsabiliMap, mapError);
                        anomalia = anomalia ? anomalia : anomali;
                        anomaliaRiga = anomaliaRiga ? anomaliaRiga : anomali;
                        //nRigheAnomale = anomali ? nRigheAnomale++ : nRigheAnomale;
                        mR.setDatain(!anomali ? formattattore(responsabiliMap.get("datain")) : null);

                        ZonedDateTime datafi = null;
                        ZonedDateTime datain = null;
                        String datafiString = null;
                        String datainString = null;

                        if (responsabiliMap.get("datafi") != null && (!responsabiliMap.get("datafi").toString().trim().equals("") || responsabiliMap.get("datafi") == "")) {
                            datafi = formattattore(responsabiliMap.get("datafi"));
                            datafiString = UtilityFunctions.getZonedDateTimeString(datafi);
                        }

                        if (!anomali) {
                            datain = mR.getDatain();
                            datainString = UtilityFunctions.getZonedDateTimeString(datain);
                        }

//                      ID_CASELLA bloccante
                        String id_casella;
                        try {
                            id_casella = checkIdCasellaR(responsabiliMap, mapError, selectDateOnStruttureByIdAzienda, idAzienda);
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
                            mR.setDatafi(formattattore(responsabiliMap.get("datafi")));
                        }

                        if (checkDateFinisconoDopoInizio(datain, datafi)) {
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
                        String tipoR = checkTipoR(responsabiliMap, mapError);
                        mR.setTipo(tipoR);
                        anomalia = tipoR == null ? true : anomalia;
                        anomaliaRiga = tipoR == null ? true : anomaliaRiga;
//                        nRigheAnomale = tipoR == null ? nRigheAnomale++ : nRigheAnomale;

//                      CODICE ENTE
                        Integer CodiceEnte = checkCodiceEnte(responsabiliMap, mapError, codiceAzienda);
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

                        boolean anomali = checkDatainS(strutturaMap, mapError);
                        if (!anomali) {
                            datain = formattattore(strutturaMap.get("datain"));
                            datainString = UtilityFunctions.getZonedDateTimeString(datain);
                        }
                        bloccante = anomali ? anomali : bloccante;
                        mS.setDatain(anomali ? null : datain);
                        if (anomali) {
                            log.error("Importa CSV --Struttura-- errore alla righa:" + mapReader.getLineNumber() + " Errore bloccante su data inizio vuota");
                        }

                        datafi = checkDatafi(strutturaMap, mapError);
                        mS.setDatafi(datafi);
                        datafiString = datafi != null ? UtilityFunctions.getZonedDateTimeString(datafi) : null;

                        String id_casella = checkIdCasellaS(strutturaMap, mapError, mapReader.getLineNumber(), strutturaCheckDateMap);
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

                        String descrizione = checkDescrizioneS(strutturaMap, mapError, mapReader.getLineNumber());
                        mS.setDescrizione(descrizione.equals("") ? null : descrizione);
                        bloccante = descrizione.equals("") ? true : bloccante;

                        if (strutturaMap.get("tipo_legame") == null || strutturaMap.get("tipo_legame").toString().trim().equals("") || strutturaMap.get("tipo_legame") == "") {
                            mapError.put("tipo_legame", "");
                            mS.setTipoLegame(null);
                        } else {
                            mapError.put("tipo_legame", strutturaMap.get("tipo_legame"));
                            mS.setTipoLegame(strutturaMap.get("tipo_legame").toString());
                        }

                        Integer codiceEnte = checkCodiceEnte(strutturaMap, mapError, codiceAzienda);
                        mS.setCodiceEnte(codiceEnte);
                        anomali = codiceEnte == codiceAzienda ? true : anomali;
                        nRigheAnomale = codiceEnte == codiceAzienda ? nRigheAnomale++ : nRigheAnomale;
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

                    try (InputStreamReader csvErrorFileRIP = new InputStreamReader(new FileInputStream(csvErrorFile));) {

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
                                    strutturaErrorMapWrite.put("ERRORE", strutturaErrorMap.get("ERRORE") + " padre non presente,");
                                } else {
                                    List<Map<String, Object>> elementi = listaStrutture.get(Integer.parseInt(strutturaErrorMap.get("id_padre").toString()));

                                    if ((strutturaErrorMap.get("datain") != null) && (!isPeriodiSovrapposti(elementi, formattattore(strutturaErrorMap.get("datain")), formattattore(strutturaErrorMap.get("datafi"))))) {
                                        bloccante = true;
                                        log.error("Importa CSV --Struttura-- errore alla righa:" + mapReader.getLineNumber() + " non rispetta l'arco temporale del padre");
                                        if (strutturaErrorMap.get("ERRORE") != null) {
                                            strutturaErrorMapWrite.put("ERRORE", strutturaErrorMap.get("ERRORE") + " non rispetta l'arco temporale del padre,");
                                        } else {
                                            strutturaErrorMapWrite.put("ERRORE", " non rispetta l'arco temporale del padre,");
                                        }
                                    }
                                    Map<String, ZonedDateTime> maxMin = maxMin(elementi);
                                    if (!controllaEstremi(maxMin.get("min"), maxMin.get("max"), formattattore(strutturaErrorMap.get("datain")), formattattore(strutturaErrorMap.get("datafi")))) {
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
//                        csvErrorFile.deleteOnExit();
//                        csvErrorFile2.deleteOnExit();
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
                        Integer progressivoRiga = checkProgressivoRigaR(trasformazioniMap, mapError, mapReader);
                        mT.setProgressivoRiga(progressivoRiga);
                        bloccante = progressivoRiga == null ? true : bloccante;

//                      DATA TRASFORMAZIONE DEVE ESISTERE SEMPRE
                        ZonedDateTime dataTrasformazioneT = checkDataTrasformazione(trasformazioniMap, mapError, mapReader);
                        bloccante = dataTrasformazioneT == null ? true : bloccante;
                        dataTrasformazione = dataTrasformazioneT == null ? false : dataTrasformazione;
                        mT.setDataTrasformazione(dataTrasformazioneT);
//                       DATA IN PARTENZA DEVE ESISTERE SEMPRE
//                       PER MOTIVO DI "X", "T","R" E "U" è LA DATA INIZIO DELLA CASELLA DI PARTENZA
//                      AGGIUNGERE BOOLEANO TEMPI_CASELLA_OK
                        ZonedDateTime dataInPartenzaT = checkDataInPartenza(trasformazioniMap, mapError, mapReader);
                        bloccante = dataInPartenzaT == null ? true : bloccante;
                        dataInPartenza = dataInPartenzaT == null ? false : dataInPartenza;
                        mT.setDatainPartenza(dataInPartenzaT);

                        //ID CASELLA DI PARTENZA
                        //SEMPRE SPENTO IL GIORNO PRIMA DELLA DATA DI TRASFORMAZIONE
                        //DI CONSEGUENZA DEVE ESISTERE
                        Integer idCasellaPartenza = checkIdCasellaPartenza(trasformazioniMap, mapError, mapReader);
                        mT.setIdCasellaPartenza(idCasellaPartenza == -1 ? null : idCasellaPartenza);
                        bloccante = idCasellaPartenza == null ? true : bloccante;

                        if (dataInPartenza && dataTrasformazione && idCasellaPartenza != -1) {

                            if (!listaStruttureConDate.containsKey(idCasellaPartenza)) {
                                log.error("Importa CSV --Trasformazioni-- errore alla righa:" + mapReader.getLineNumber() + " casella di partenza non trovata");
                                bloccante = true;
                                tempi_ok = false;
                                mapError.put("ERRORE", mapError.get("ERRORE") + " casella di partenza non trovata,");
                            } else {
                                boolean blocco = checkAccesaSpentaMale(listaStruttureConDate.get(idCasellaPartenza), formattattore(trasformazioniMap.get("data_trasformazione").toString()), formattattore(trasformazioniMap.get("datain_partenza").toString()));
                                if (blocco) {
                                    log.error("Importa CSV --Trasformazioni-- errore alla righa:" + mapReader.getLineNumber() + " periodi temporali della casella di partenza non sono validi");
                                    bloccante = true;
                                    tempi_ok = false;
                                    mapError.put("ERRORE", mapError.get("ERRORE") + " periodi temporali della casella di partenza non sono validi,");
                                }
                            }
                        }

//                      DATA ORA OPERAZIONE
                        ZonedDateTime dataOraOper = checkDataOraOper(trasformazioniMap, mapError);
                        mT.setDataoraOper(dataOraOper);
                        boolean buono = trasformazioniMap.get("dataora_oper") == null || trasformazioniMap.get("dataora_oper").toString().trim().equals("");
                        anomalia = buono ? true : anomalia;
                        nRigheAnomale = buono ? nRigheAnomale++ : nRigheAnomale;

//                      CODICE ENTE
                        Integer codiceEnte = checkCodiceEnte(trasformazioniMap, mapError, codiceAzienda);
                        mT.setCodiceEnte(codiceEnte);
                        anomalia = codiceEnte == codiceAzienda ? true : anomalia;
                        nRigheAnomale = codiceEnte == codiceAzienda ? nRigheAnomale++ : nRigheAnomale;

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

    private boolean checkCodiceMatricolaA(Map<String, Object> appartenentiMap, Map<String, Object> mapError) {
        if (appartenentiMap.get("codice_matricola") == null || appartenentiMap.get("codice_matricola").toString().trim().equals("") || appartenentiMap.get("codice_matricola") == "") {
            mapError.put("Anomalia", "true");
            mapError.put("ERRORE", mapError.get("ERRORE") + " codice_matricola,");
            mapError.put("codice_matricola", "");
            return true;
        } else {
            mapError.put("codice_matricola", appartenentiMap.get("codice_matricola"));
        }
        return false;
    }

    private boolean checkCognomeA(Map<String, Object> appartenentiMap, Map<String, Object> mapError) {
        if (appartenentiMap.get("cognome") == null || appartenentiMap.get("cognome").toString().trim().equals("") || appartenentiMap.get("cognome") == "") {
            mapError.put("ERRORE", mapError.get("ERRORE") + " cognome,");
            mapError.put("Anomalia", "true");
            mapError.put("cognome", "");
            return true;
        } else {
            mapError.put("cognome", appartenentiMap.get("cognome"));
        }
        return false;
    }

    private boolean checkNomeA(Map<String, Object> appartenentiMap, Map<String, Object> mapError) {
        if (appartenentiMap.get("nome") == null || appartenentiMap.get("nome").toString().trim().equals("") || appartenentiMap.get("nome") == "") {
            mapError.put("ERRORE", mapError.get("ERRORE") + " nome,");
            mapError.put("Anomalia", "true");
            mapError.put("nome", "");
            return true;
        } else {
            mapError.put("nome", appartenentiMap.get("nome"));
        }
        return false;
    }

    private boolean checkCodiceFiscaleA(Map<String, Object> appartenentiMap, Map<String, Object> mapError) {
        if (appartenentiMap.get("codice_fiscale") == null || appartenentiMap.get("codice_fiscale").toString().trim().equals("") || appartenentiMap.get("codice_fiscale") == "") {
            mapError.put("ERRORE", mapError.get("ERRORE") + " CODICE FISCALE,");
            mapError.put("Anomalia", "true");
            mapError.put("codice_fiscale", "");
            return true;

        } else {
            if (appartenentiMap.get("nome") != null && appartenentiMap.get("cognome") != null) {
                if (appartenentiMap.get("codice_fiscale").toString().startsWith(partialCF(appartenentiMap.get("nome").toString(), appartenentiMap.get("cognome").toString()))) {
                    mapError.put("codice_fiscale", appartenentiMap.get("codice_fiscale"));
                } else {
                    mapError.put("ERRORE", mapError.get("ERRORE") + " CODICE FISCALE errato,");
                    mapError.put("Anomalia", "true");
                    mapError.put("codice_fiscale", appartenentiMap.get("codice_fiscale"));
                    return true;
                }

            }
        }
        return false;
    }

    private String checkIdCasellaA(Map<String, Object> appartenentiMap, Map<String, Object> mapError, Map<Integer, List<Map<String, Object>>> selectDateOnStruttureByIdAzienda) {
        if (appartenentiMap.get("id_casella") == null || appartenentiMap.get("id_casella").toString().trim().equals("") || appartenentiMap.get("id_casella") == "") {

            mapError.put("Anomalia", "true");
            mapError.put("ERRORE", mapError.get("ERRORE") + " IDCASELLA,");
            mapError.put("id_casella", "");
            return "";

        } else {
            mapError.put("id_casella", appartenentiMap.get("id_casella").toString());
            if (!selectDateOnStruttureByIdAzienda.containsKey(Integer.parseInt(appartenentiMap.get("id_casella").toString()))) {
                mapError.put("ERRORE", " manca la struttura nella tabella struttura,");
                mapError.put("Anomalia", "true");

                return "";
            } else {
                if (!isPeriodiSovrapposti(selectDateOnStruttureByIdAzienda.get(Integer.parseInt(appartenentiMap.get("id_casella").toString())), formattattore(appartenentiMap.get("datain")), formattattore(appartenentiMap.get("datafi")))) {
                    mapError.put("ERRORE", mapError.get("ERRORE") + " non rispetta l arco temporale della struttura,");
                    mapError.put("Anomalia", "true");

                    return "";
                } else {
                    List<Map<String, Object>> elementi = selectDateOnStruttureByIdAzienda.get(Integer.parseInt(appartenentiMap.get("id_casella").toString()));
                    Map<String, ZonedDateTime> maxMin = maxMin(elementi);
                    if (!controllaEstremi(maxMin.get("min"), maxMin.get("max"), formattattore(appartenentiMap.get("datain")), formattattore(appartenentiMap.get("datafi")))) {
                        mapError.put("ERRORE", mapError.get("ERRORE") + " non rispetta l'arco temporale della struttura, ");
                        mapError.put("Anomalia", "true");

                        return "";
                    }
                }
            }

            return appartenentiMap.get("id_casella").toString();
        }
    }

    private boolean checkDatainA(Map<String, Object> appartenentiMap, Map<String, Object> mapError) {
        if (appartenentiMap.get("datain") == null || appartenentiMap.get("datain").toString().trim().equals("") || appartenentiMap.get("datain") == "") {
            mapError.put("ERRORE", mapError.get("ERRORE") + " datain,");
            mapError.put("datain", "");
            mapError.put("Anomalia", "true");
            return true;
        } else {
            if (formattattore(appartenentiMap.get("datain").toString()) != null) {
                mapError.put("datain", appartenentiMap.get("datain"));
            } else {
                mapError.put("datain", appartenentiMap.get("datain"));
                mapError.put("Anomalia", "true");
                mapError.put("ERRORE", mapError.get("ERRORE") + " datain non riconosciuta,");
                return true;
            }
        }
        return false;
    }

    private Integer checkCodiceEnte(Map<String, Object> xmap, Map<String, Object> mapError, Integer codiceAzienda) {
        if (xmap.get("codice_ente") == null || xmap.get("codice_ente").toString().trim().equals("") || xmap.get("codice_ente") == "") {
            mapError.put("codice_ente", "");
            mapError.put("ERRORE", mapError.get("Errore") + "codice ente assente,");
            mapError.put("Anomalia", "true");
            return codiceAzienda;
        } else {
            mapError.put("codice_ente", xmap.get("codice_ente"));
            return Integer.parseInt(xmap.get("codice_ente").toString());
        }
    }

    private boolean checkTipoAppatenenza(
            Map<String, Object> appartenentiMap,
            Map<String, Object> mapError,
            String idCasella,
            ZonedDateTime datain,
            ZonedDateTime datafi,
            Boolean controlloZeroUno,
            Integer codiceEnte,
            Map<Integer, Map<Integer, List<Map<String, Object>>>> appartenentiDiretti,
            Map<Integer, Map<Integer, List<Map<String, Object>>>> appartenentiFunzionali,
            ICsvMapReader mapReader,
            List<Integer> righeAnomaleFunzionali,
            List<Integer> righeAnomaleDirette) {

        Boolean anomalia = false;
        if (appartenentiMap.get("tipo_appartenenza") == null || appartenentiMap.get("tipo_appartenenza").toString().trim().equals("") || appartenentiMap.get("tipo_appartenenza") == "" || idCasella.equals("")) {
            mapError.put("ERRORE", mapError.get("ERRORE") + " tipo appartenenza assente,");
            mapError.put("tipo_appartenenza", "");
            mapError.put("Anomalia", "true");
            return true;

        } else {
            mapError.put("tipo_appartenenza", appartenentiMap.get("tipo_appartenenza"));
//                            mA.setTipoAppartenenza(appartenentiMap.get("tipo_appartenenza").toString());
            if (appartenentiMap.get("codice_ente") != null && !appartenentiMap.get("codice_ente").toString().trim().equals("") && appartenentiMap.get("codice_ente") != "") {
                boolean codiceEnteEndsWith = appartenentiMap.get("codice_ente").toString().endsWith("01");
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

                                if (!afferenzaDiretta && isPeriodiSovrapposti(listaCasella.getValue(), datain, datafi)) {
                                    if (!righeAnomaleDirette.contains(mapReader.getLineNumber())) {
                                        righeAnomaleDirette.add(mapReader.getLineNumber());
                                    }
                                    mapError.put("Anomalia", "true");
                                    afferenzaDiretta = true;
                                    List<Integer> righeAnomaleDaControllare = arco(listaCasella.getValue(), datain, datafi);
                                    for (Integer rigaAnomala : righeAnomaleDaControllare) {
                                        if (!righeAnomaleDirette.contains(rigaAnomala)) {
                                            righeAnomaleDirette.add(rigaAnomala);
                                        }
                                    }
                                    anomalia = true;
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
                                if (!afferenzaDiretta && isPeriodiSovrapposti(periodoCasellato, datain, datafi)) {
                                    if (!righeAnomaleDirette.contains(mapReader.getLineNumber())) {
                                        righeAnomaleDirette.add(mapReader.getLineNumber());
                                    }
                                    mapError.put("Anomalia", "true");
                                    List<Integer> righeAnomaleDaControllare = arco(periodoCasellato, datain, datafi);
                                    for (Integer rigaAnomala : righeAnomaleDaControllare) {
                                        if (!righeAnomaleDirette.contains(rigaAnomala)) {
                                            righeAnomaleDirette.add(rigaAnomala);
                                        }
                                    }
                                    anomalia = true;
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

                            if (isPeriodiSovrapposti(periodoCasellato, datain, datafi)) {
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
                                anomalia = true;

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
        return anomalia;
    }

    private boolean checkDataAssunzioneA(
            Map<String, Object> appartenentiMap,
            Map<String, Object> mapError) {
        if (appartenentiMap.get("data_assunzione") == null || appartenentiMap.get("data_assunzione").toString().trim().equals("") || appartenentiMap.get("data_assunzione") == "") {
            mapError.put("ERRORE", mapError.get("ERRORE") + " data_assunzione,");
            mapError.put("data_assunzione", "");
            mapError.put("Anomalia", "true");
            return true;

        } else {
            mapError.put("data_assunzione", appartenentiMap.get("data_assunzione"));
        }
        return false;
    }

    private List<Integer> codiciMatricoleConAppFunzionaliENonDirette(Map<Integer, Map<Integer, List<Map<String, Object>>>> appartenentiFunzionali, Map<Integer, Map<Integer, List<Map<String, Object>>>> appartenentiDiretti) {
        List<Integer> codiciMatricoleConAppFunzionaliENonDirette = new ArrayList<>();
        for (Integer codiceMatricola : appartenentiFunzionali.keySet()) {
            if (!appartenentiDiretti.containsKey(codiceMatricola)) {
                codiciMatricoleConAppFunzionaliENonDirette.add(codiceMatricola);
            }
        }
        return codiciMatricoleConAppFunzionaliENonDirette;
    }

    private String checkCodiceMatricolaR(Map<String, Object> responsabiliMap, Map<String, Object> mapError, Map<Integer, List<Map<String, Object>>> selectDateOnAppartenentiByIdAzienda) {
        if (responsabiliMap.get("codice_matricola") == null || responsabiliMap.get("codice_matricola").toString().trim().equals("") || responsabiliMap.get("codice_matricola") == "") {
            mapError.put("ERRORE", mapError.get("ERRORE") + " codice_matricola,");
            mapError.put("codice_matricola", "");

            return "";
        } else {
            mapError.put("codice_matricola", responsabiliMap.get("codice_matricola"));
            //responsabile presente tra gli autenti
            if (!selectDateOnAppartenentiByIdAzienda.containsKey(Integer.parseInt(responsabiliMap.get("codice_matricola").toString()))) {
                mapError.put("ERRORE", mapError.get("ERRORE") + " codice_matricola non trovata nella tabella appartenenti,");
                return "";
            }
            return responsabiliMap.get("codice_matricola").toString();
        }
    }

    private Boolean checkDatainR(Map<String, Object> responsabiliMap, Map<String, Object> mapError) {
        if (responsabiliMap.get("datain") == null || responsabiliMap.get("datain").toString().trim().equals("") || responsabiliMap.get("datain") == "") {
            mapError.put("ERRORE", mapError.get("ERRORE") + " datain non presente,");
            mapError.put("datain", "");
            return false;
        } else {
            mapError.put("datain", responsabiliMap.get("datain"));
            return true;
        }
    }

    private String checkIdCasellaR(Map<String, Object> responsabiliMap, Map<String, Object> mapError, Map<Integer, List<Map<String, Object>>> selectStruttureUtentiByIdAzienda, Integer idAzienda) throws RibaltoneCSVCheckException {
        if (responsabiliMap.get("id_casella") == null || responsabiliMap.get("id_casella").toString().trim().equals("") || responsabiliMap.get("id_casella") == "") {
            mapError.put("ERRORE", mapError.get("ERRORE") + " id_casella assente,");
            mapError.put("id_casella", "");
            return "";
        } else {
            mapError.put("id_casella", responsabiliMap.get("id_casella"));
//                            mR.setIdCasella(Integer.parseInt(responsabiliMap.get("id_casella").toString())); faccio il check della casella le domande sono queste esiste? vale nel periodo temporale? 
            if (!selectStruttureUtentiByIdAzienda.containsKey(Integer.parseInt(responsabiliMap.get("id_casella").toString()))) {
                mapError.put("ERRORE", mapError.get("ERRORE") + " casella non trovata nella tabella strutture,");
                throw new RibaltoneCSVCheckException("checkIdCasella", responsabiliMap.get("id_casella").toString(), " casella non trovata nella tabella strutture,");
            } else {
                List<Map<String, Object>> mieiPadri = selectStruttureUtentiByIdAzienda.get(Integer.parseInt(responsabiliMap.get("id_casella").toString()));
                if (responsabiliMap.get("datain") != null && !responsabiliMap.get("datain").toString().trim().equals("") && responsabiliMap.get("datain") != "") {
                    if (!isPeriodiSovrapposti(mieiPadri, formattattore(responsabiliMap.get("datain")), formattattore(responsabiliMap.get("datafi")))) {
                        mapError.put("ERRORE", mapError.get("ERRORE") + " casella non valida per periodo temporale,");
                        mapError.put("Anomalia", "true");
                        throw new RibaltoneCSVCheckException("checkIdCasella", responsabiliMap.get("id_casella").toString(), " casella non valida per periodo temporale,");

                    } else {
                        Map<String, ZonedDateTime> maxMin = maxMin(mieiPadri);
                        if (!controllaEstremi(maxMin.get("min"), maxMin.get("max"), formattattore(responsabiliMap.get("datain")), formattattore(responsabiliMap.get("datafi")))) {
                            mapError.put("ERRORE", mapError.get("ERRORE") + " casella non rispetta l'arco temporale della struttura,");
                            mapError.put("Anomalia", "true");
                            throw new RibaltoneCSVCheckException("checkIdCasella", responsabiliMap.get("id_casella").toString(), " non rispetta l'arco temporale della struttura,");
                        }
                    }
                }
            }
            return responsabiliMap.get("id_casella").toString();
        }
    }

    private String checkTipoR(Map<String, Object> responsabiliMap, Map<String, Object> mapError) {
        if (responsabiliMap.get("tipo") == null || responsabiliMap.get("tipo").toString().trim().equals("") || responsabiliMap.get("tipo") == "") {
            mapError.put("ERRORE", mapError.get("ERRORE") + " manca il tipo afferenza,");
            return null;
        } else {
            mapError.put("tipo", responsabiliMap.get("tipo"));
            return responsabiliMap.get("tipo").toString();
        }
    }

    private boolean checkDatainS(Map<String, Object> strutturaMap, Map<String, Object> mapError) {
        if (strutturaMap.get("datain") == null || strutturaMap.get("datain").toString().trim().equals("") || strutturaMap.get("datain") == "") {
            mapError.put("ERRORE", mapError.get("ERRORE") + " datain,");
            mapError.put("datain", "");
            return true;
        } else {
            mapError.put("datain", strutturaMap.get("datain"));
            return false;
        }
    }

    private ZonedDateTime checkDatafi(Map<String, Object> strutturaMap, Map<String, Object> mapError) {
        if (strutturaMap.get("datafi") == null
                || strutturaMap.get("datafi").toString().trim().equals("")
                || strutturaMap.get("datafi") == ""
                || strutturaMap.get("datafi").toString().trim().equals("3000-12-31")
                || strutturaMap.get("datafi").toString().trim().equals("31/12/3000")) {
            mapError.put("datafi", "");
            return null;
        } else {
            mapError.put("datafi", strutturaMap.get("datafi"));
            return formattattore(strutturaMap.get("datafi"));
        }
    }

    private String checkIdCasellaS(Map<String, Object> strutturaMap,
            Map<String, Object> mapError,
            Integer lineNumber,
            Map<Integer, List<Map<String, Object>>> strutturaCheckDateMap) {

        if (strutturaMap.get("id_casella") == null || strutturaMap.get("id_casella").toString().trim().equals("")) {
            mapError.put("ERRORE", mapError.get("ERRORE") + " id_casella assente,");
            log.error("Importa CSV --Struttura-- errore alla righa:" + lineNumber + " idCasella vuota");
            mapError.put("id_casella", "");
            return "";
        } else {
            Integer idCasella = Integer.parseInt(strutturaMap.get("id_casella").toString());
            mapError.put("id_casella", strutturaMap.get("id_casella"));

            if (strutturaCheckDateMap.get(idCasella) == null) {
                List<Map<String, Object>> listaMapDataInDataFi = new ArrayList();
                Map<String, Object> mapDataInDataFi = new HashMap();
                mapDataInDataFi.put("datain", strutturaMap.get("datain"));
                mapDataInDataFi.put("datafi", strutturaMap.get("datafi"));
                listaMapDataInDataFi.add(mapDataInDataFi);
                strutturaCheckDateMap.put(idCasella, listaMapDataInDataFi);
            } else {
                //struttura definita piu volte nello stesso arco temporale
                if ((strutturaMap.get("datain") != null) && (isPeriodiSovrapposti(strutturaCheckDateMap.get(idCasella), formattattore(strutturaMap.get("datain")), formattattore(strutturaMap.get("datafi"))))) {
                    log.error("Importa CSV --Struttura-- errore alla righa:" + lineNumber.toString() + " idCasella definita piu volte");
                    if (strutturaMap.get("ERRORE") != null) {
                        mapError.put("ERRORE", mapError.get("ERRORE") + " struttura definita piu volte nello stesso arco temporale,");
                    } else {
                        mapError.put("ERRORE", " struttura definita piu volte nello stesso arco temporale,");
                    }
                }

                Map<String, Object> mapDataInDataFi = new HashMap();
                mapDataInDataFi.put("datain", strutturaMap.get("datain"));
                mapDataInDataFi.put("datafi", strutturaMap.get("datafi"));
                strutturaCheckDateMap.get(idCasella).add(mapDataInDataFi);
            }
            return strutturaMap.get("id_casella").toString();
        }
    }

    private String checkDescrizioneS(Map<String, Object> strutturaMap, Map<String, Object> mapError, Integer lineNumber) {
        if (strutturaMap.get("descrizione") == null || strutturaMap.get("descrizione").toString().trim().equals("") || strutturaMap.get("descrizione") == "") {
            mapError.put("ERRORE", mapError.get("ERRORE") + " descrizione assente,");
            mapError.put("descrizione", "");
            log.error("Importa CSV --Struttura-- errore alla righa:" + lineNumber.toString() + " descrizione vuota");
            return "";
        } else {
            mapError.put("descrizione", strutturaMap.get("descrizione"));
            return strutturaMap.get("descrizione").toString().replaceAll("(\\n\\r+)|(\\n+)", " ");

        }
    }

    private Integer checkProgressivoRigaR(Map<String, Object> trasformazioniMap, Map<String, Object> mapError, ICsvMapReader mapReader) {
        if (trasformazioniMap.get("progressivo_riga") == null || trasformazioniMap.get("progressivo_riga").toString().trim().equals("") || trasformazioniMap.get("progressivo_riga") == "") {
            mapError.put("ERRORE", mapError.get("ERRORE") + " progressivo_riga,");
            mapError.put("progressivo_riga", "");
            log.error("Importa CSV --Trasformazioni-- errore alla righa:" + mapReader.getLineNumber() + " progressivo_riga assente");
            return null;
        } else {
            mapError.put("progressivo_riga", trasformazioniMap.get("progressivo_riga"));
            return Integer.parseInt(trasformazioniMap.get("progressivo_riga").toString());
        }
    }

    private ZonedDateTime checkDataTrasformazione(Map<String, Object> trasformazioniMap, Map<String, Object> mapError, ICsvMapReader mapReader) {
        if (trasformazioniMap.get("data_trasformazione") == null || trasformazioniMap.get("data_trasformazione").toString().trim().equals("") || trasformazioniMap.get("data_trasformazione") == "") {
            mapError.put("ERRORE", mapError.get("ERRORE") + " data_trasformazione assente,");
            mapError.put("data_trasformazione", "");
            log.error("Importa CSV --Trasformazioni-- errore alla righa:" + mapReader.getLineNumber() + " data_trasformazione assente");
            return null;
        } else {
            mapError.put("data_trasformazione", trasformazioniMap.get("data_trasformazione"));
            return formattattore(trasformazioniMap.get("data_trasformazione"));
        }
    }

    private ZonedDateTime checkDataInPartenza(Map<String, Object> trasformazioniMap, Map<String, Object> mapError, ICsvMapReader mapReader) {
        if (trasformazioniMap.get("datain_partenza") == null || trasformazioniMap.get("datain_partenza").toString().trim().equals("") || trasformazioniMap.get("datain_partenza") == "") {
            mapError.put("ERRORE", mapError.get("ERRORE") + " datain_partenza assente,");
            mapError.put("datain_partenza", "");
            log.error("Importa CSV --Trasformazioni-- errore alla righa:" + mapReader.getLineNumber() + " datain_partenza assente");
            return null;
        } else {
            mapError.put("datain_partenza", trasformazioniMap.get("datain_partenza"));
            return formattattore(trasformazioniMap.get("datain_partenza"));
        }
    }

    private Integer checkIdCasellaPartenza(Map<String, Object> trasformazioniMap, Map<String, Object> mapError, ICsvMapReader mapReader) {
        if (trasformazioniMap.get("id_casella_partenza") == null || trasformazioniMap.get("id_casella_partenza").toString().trim().equals("") || trasformazioniMap.get("id_casella_partenza") == "") {
            mapError.put("ERRORE", mapError.get("ERRORE") + " id_casella_partenza,");
            mapError.put("id_casella_partenza", "");
            log.error("Importa CSV --Trasformazioni-- errore alla righa:" + mapReader.getLineNumber() + " id_casella_partenza assente");
            return -1;
        } else {
            mapError.put("id_casella_partenza", trasformazioniMap.get("id_casella_partenza"));
            return Integer.parseInt(trasformazioniMap.get("id_casella_partenza").toString());
        }
    }

    private ZonedDateTime checkDataOraOper(Map<String, Object> trasformazioniMap, Map<String, Object> mapError) {
        if (trasformazioniMap.get("dataora_oper") == null || trasformazioniMap.get("dataora_oper").toString().trim().equals("")) {
            mapError.put("ERRORE", mapError.get("ERRORE") + " DATAORA_OPER inserito automaticamente,");
            ZonedDateTime now = ZonedDateTime.now();
            mapError.put("dataora_oper", now.toString());
            return now;
        } else {
            mapError.put("dataora_oper", trasformazioniMap.get("dataora_oper"));
            return formattattore(trasformazioniMap.get("dataora_oper"));
        }
    }

    private boolean checkAccesaSpentaMale(List<Map<String, Object>> date, ZonedDateTime dataTrasformazione, ZonedDateTime dataInPartenza) {
        for (Map<String, Object> data : date) {
            if (formattattore(data.get("datain").toString()).equals(dataInPartenza) && formattattore(data.get("datafi")).equals(dataTrasformazione.minusDays(1))) {
                return false;
            }
        }
        return true;
    }

    private boolean checkDateFinisconoDopoInizio(ZonedDateTime datain, ZonedDateTime datafi) {
        if (datain != null) {
            if (datafi == null) {
                return false;
            } else {
                return (datafi.isBefore(datain));
            }
        } else {
            return true;
        }
    }
}
