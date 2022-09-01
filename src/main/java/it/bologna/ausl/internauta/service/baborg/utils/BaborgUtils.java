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
import it.bologna.ausl.internauta.utils.parameters.manager.ParametriAziendeReader;
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
        log.info("sto generando il csv del tipo" + tipo);
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
            log.error("ho fallito miseramente",e);
            System.out.println("e" + e);
            return null;
        }
        return csvFile;

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
        log.info("sto generando i processor del tipo" + tipo);
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
        log.info("sto generando l'header del tipo" + tipo);
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
            case "ANAGRAFICA":
                headers = new String[]{"codice_ente", "codice_matricola", "cognome",
                    "nome", "codice_fiscale", "email"};
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
        ImportazioniOrganigramma res = null;
        BaborgUtils bean = beanFactory.getBean(BaborgUtils.class);
        ImportaDaCSV importaDaCSVBeanSave = beanFactory.getBean(ImportaDaCSV.class);


        try {

//            String csv_error_link = bean.csvTransactionalReadDeleteInsert(file, tipo, idAziendaCodice, idAziendaInt);
            String csv_error_link = importaDaCSVBeanSave.csvTransactionalReadDeleteInsert(file, tipo, codiceAzienda, idAziendaInt);
            // Update nello storico importazioni. esito: OK e Data Fine: Data.now
            res = bean.updateEsitoImportazioneOrganigramma(newRowInserted, "Ok", csv_error_link);
        } catch (BaborgCSVBloccanteException e) {
            log.error("errore bloccante", e);
            res = bean.updateEsitoImportazioneOrganigramma(newRowInserted, "Errore Bloccante", e.getMessage());
        } catch (BaborgCSVAnomaliaException e) {
            log.error("errore anomalia", e);
            res = bean.updateEsitoImportazioneOrganigramma(newRowInserted, "Anomalia", e.getMessage());
        } catch (BaborgCSVBloccanteRigheException e) {
            log.error("errore bloccante righe", e);
            res = bean.updateEsitoImportazioneOrganigramma(newRowInserted, "Bloccante Righe", e.getMessage());
        } catch (Throwable e) {
            log.error("errore generico", e);
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
