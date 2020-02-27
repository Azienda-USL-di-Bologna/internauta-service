package it.bologna.ausl.internauta.service.baborg.utils;

import com.querydsl.core.types.dsl.BooleanExpression;
import it.bologna.ausl.internauta.service.exceptions.http.Http500ResponseException;
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
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.supercsv.cellprocessor.ParseInt;
import org.supercsv.cellprocessor.ift.CellProcessor;
import org.supercsv.prefs.CsvPreference;
import org.supercsv.cellprocessor.Optional;
import org.supercsv.cellprocessor.ParseDate;
import org.supercsv.cellprocessor.constraint.NotNull;
import org.supercsv.cellprocessor.constraint.StrRegEx;
import org.supercsv.io.CsvMapReader;
import org.supercsv.io.ICsvMapReader;

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
    public void csvTransactionalReadDeleteInsert(MultipartFile file, String tipo, Integer codiceAzienda, Integer idAzienda) throws FileNotFoundException, IOException, Exception {

        InputStreamReader inputFileStreamReader = new InputStreamReader(file.getInputStream());
        CsvPreference SEMICOLON_DELIMITED = new CsvPreference.Builder('"', ';', "\r\n").build();
//        Reading with CsvMapReader
        ICsvMapReader mapReader = null;

        try {
//          Reading file with CsvMapReader
            mapReader = new CsvMapReader(inputFileStreamReader, SEMICOLON_DELIMITED);
            // First column is header, needed to skip the header
            mapReader.getHeader(true);

            String[] headers = headersGenerator(tipo);
            CellProcessor[] processors = getProcessors(tipo, codiceAzienda);

            java.util.Optional<Azienda> optionalAzienda = aziendaRepository.findById(idAzienda);
            Azienda azienda = optionalAzienda.get();

            BooleanExpression predicateAzienda = null;

            switch (tipo) {
                case "APPARTENENTI":
                    // Delete delle righe da sostituire
                    predicateAzienda = QMdrAppartenenti.mdrAppartenenti.idAzienda.id.eq(idAzienda);
                    mdrAppartenentiRepository.deleteByIdAzienda(idAzienda);

                    //Reading with CsvMapReader
                    Map<String, Object> appartenentiMap;
                    while ((appartenentiMap = mapReader.read(headers, processors)) != null) {
//                        System.out.println(String.format("lineNo=%s, rowNo=%s, customerMap=%s",
//                                mapReader.getLineNumber(), mapReader.getRowNumber(), appartenentiMap));

                        // Inserisco la riga
                        MdrAppartenenti mA = new MdrAppartenenti();
                        mA.setCodiceEnte((Integer) appartenentiMap.get("codiceEnte"));
                        mA.setCodiceMatricola((Integer) appartenentiMap.get("codiceMatricola"));
                        mA.setCognome((String) appartenentiMap.get("cognome"));
                        mA.setNome((String) appartenentiMap.get("nome"));
                        mA.setCodiceFiscale((String) appartenentiMap.get("codiceFiscale"));
                        mA.setIdCasella((Integer) appartenentiMap.get("idCasella"));
                        mA.setDatain(convertDateToLocaleDateTime((Date) appartenentiMap.get("datain")));
                        mA.setDatafi(convertDateToLocaleDateTime((Date) appartenentiMap.get("datafi")));
                        mA.setTipoAppartenenza((String) appartenentiMap.get("tipoAppartenenza"));
                        mA.setUsername((String) appartenentiMap.get("username"));
                        mA.setDataAssunzione(convertDateToLocaleDateTime((Date) appartenentiMap.get("dataAssunzione")));
                        mA.setDataDimissione(convertDateToLocaleDateTime((Date) appartenentiMap.get("dataDimissione")));
                        mA.setIdAzienda(azienda);
                        mdrAppartenentiRepository.save(mA);
                    }
                    break;
                case "RESPONSABILI":
                    // Delete delle righe da sostituire
                    predicateAzienda = QMdrResponsabili.mdrResponsabili.idAzienda.id.eq(idAzienda);
                    mdrResponsabiliRepository.deleteByIdAzienda(idAzienda);

                    //Reading with CsvMapReader
                    Map<String, Object> responsabiliMap = null;
                    while ((responsabiliMap = mapReader.read(headers, processors)) != null) {
//                        System.out.println(String.format("lineNo=%s, rowNo=%s, customerMap=%s",
//                                mapReader.getLineNumber(), mapReader.getRowNumber(), responsabiliMap));

                        // Inserisco la riga
                        MdrResponsabili mR = new MdrResponsabili();
                        mR.setCodiceEnte((Integer) responsabiliMap.get("codiceEnte"));
                        mR.setCodiceMatricola((Integer) responsabiliMap.get("codiceMatricola"));
                        mR.setIdCasella((Integer) responsabiliMap.get("idCasella"));
                        mR.setDatain(convertDateToLocaleDateTime((Date) responsabiliMap.get("datain")));
                        mR.setDatafi(convertDateToLocaleDateTime((Date) responsabiliMap.get("datafi")));
                        mR.setTipo((String) responsabiliMap.get("tipo"));
                        mR.setIdAzienda(azienda);
                        mdrResponsabiliRepository.save(mR);
                    }
                    break;

                case "STRUTTURA":
                    // Delete delle righe da sostituire
                    predicateAzienda = QMdrStruttura.mdrStruttura.idAzienda.id.eq(idAzienda);
                    mdrStrutturaRepository.deleteByIdAzienda(idAzienda);

                    // Reading with CsvMapReader
                    Map<String, Object> strutturaMap = null;
                    while ((strutturaMap = mapReader.read(headers, processors)) != null) {
//                        System.out.println(String.format("lineNo=%s, rowNo=%s, customerMap=%s",
//                                mapReader.getLineNumber(), mapReader.getRowNumber(), strutturaMap));

                        // Inserisco la riga
                        MdrStruttura mS = new MdrStruttura();
                        mS.setIdCasella((Integer) strutturaMap.get("idCasella"));
                        mS.setIdPadre((Integer) strutturaMap.get("idPadre"));
                        mS.setDescrizione((String) strutturaMap.get("descrizione"));
                        mS.setDatain(convertDateToLocaleDateTime((Date) strutturaMap.get("datain")));
                        mS.setDatafi(convertDateToLocaleDateTime((Date) strutturaMap.get("datafi")));
                        mS.setTipoLegame((String) strutturaMap.get("tipoLegame"));
                        mS.setCodiceEnte((Integer) strutturaMap.get("codiceEnte"));
                        mS.setIdAzienda(azienda);
                        mdrStrutturaRepository.save(mS);
                    }
                    break;

                case "TRASFORMAZIONI":

                    // Delete delle righe da sostituire
                    predicateAzienda = QMdrTrasformazioni.mdrTrasformazioni.idAzienda.id.eq(idAzienda);
                    mdrTrasformazioniRepository.deleteByIdAzienda(idAzienda);

                    //Reading with CsvMapReader
                    Map<String, Object> trasformazioniMap;
                    while ((trasformazioniMap = mapReader.read(headers, processors)) != null) {
//                        System.out.println(String.format("lineNo=%s, rowNo=%s, customerMap=%s",
//                                mapReader.getLineNumber(), mapReader.getRowNumber(), trasformazioniMap));

                        // Inserisco la riga
                        MdrTrasformazioni m = new MdrTrasformazioni();
                        m.setCodiceEnte((Integer) trasformazioniMap.get("codiceEnte"));
                        m.setDataTrasformazione(convertDateToLocaleDateTime((Date) trasformazioniMap.get("dataTrasformazione")));
                        m.setDatainPartenza(convertDateToLocaleDateTime((Date) trasformazioniMap.get("datainPartenza")));
                        m.setDataoraOper(convertDateToLocaleDateTime((Date) trasformazioniMap.get("dataoraOper")));
                        m.setIdAzienda(azienda);
                        m.setIdCasellaArrivo((Integer) trasformazioniMap.get("idCasellaArrivo"));
                        m.setIdCasellaPartenza((Integer) trasformazioniMap.get("idCasellaPartenza"));
                        m.setMotivo((String) trasformazioniMap.get("motivo"));
                        m.setProgressivoRiga((Integer) trasformazioniMap.get("progressivoRiga"));
                        mdrTrasformazioniRepository.save(m);
                    }
                    break;

                default:
                    System.out.println("non dovrebbe essere");
                    break;
            }

        } finally {
            if (mapReader != null) {
                mapReader.close();
            }
        }
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
                    new Optional(new ParseInt()), // codice_ente
                    new NotNull(new ParseInt()), // codice_matricola
                    new NotNull(), // cognome
                    new NotNull(), // nome
                    new NotNull(), // codice_fiscale
                    new NotNull(new ParseInt()), // id_casellla
                    new NotNull(new ParseDate("dd/mm/yyyy")), // datain
                    new Optional(new ParseDate("dd/mm/yyyy")), // datafi
                    new NotNull(), // tipo_appartenenza
                    new Optional(), // username
                    new NotNull(new ParseDate("dd/mm/yyyy")), // data_assunzione
                    new Optional(new ParseDate("dd/mm/yyyy")) // data_adimissione
                };
                cellProcessor = processorsAPPARTENENTI;
                break;
            case "RESPONSABILI":
                final CellProcessor[] processorsRESPONSABILI = new CellProcessor[]{
                    // new NotNull(new StrRegEx(codiceEnteRegex, new ParseInt())), // codice_ente
                    new Optional(new ParseInt()), // codice_ente
                    new NotNull(new ParseInt()), // codice_matricola
                    new NotNull(new ParseInt()), // id_casellla
                    new NotNull(new ParseDate("dd/mm/yyyy")), // datain
                    new Optional(new ParseDate("dd/mm/yyyy")), // datafi
                    new NotNull() // tipo
                };
                cellProcessor = processorsRESPONSABILI;
                break;
            case "TRASFORMAZIONI":
                CellProcessor[] processorsTRASFORMAZIONI = new CellProcessor[]{
                    new NotNull(new ParseInt()), // progressivo_riga
                    new NotNull(new ParseInt()), // id_casella_partenza
                    new Optional(new ParseInt()), // id_casellla_arrivo
                    new Optional(new ParseDate("dd/mm/yyyy")), // data_trasformazione
                    new NotNull(), // motivo
                    new Optional(new ParseDate("dd/mm/yyyy")), // datain_partenza
                    new Optional(new ParseDate("dd/mm/yyyy")), // dataora_oper
                    new Optional(new ParseInt()) // codice_ente
                };
                cellProcessor = processorsTRASFORMAZIONI;
                break;

            case "STRUTTURA":
                final CellProcessor[] processorsSTRUTTURA = new CellProcessor[]{
                    new NotNull(new ParseInt()), // id_casella
                    new Optional(new ParseInt()), // id_padre
                    new NotNull(), // descrizione
                    new NotNull(new ParseDate("dd/mm/yyyy")), // datain
                    new ParseDate("dd/mm/yyyy"), // datafi
                    new Optional(), // tipo_legame
                    // new NotNull(new StrRegEx(codiceEnteRegex, new ParseInt())), // codice_ente
                    new Optional(new ParseInt()) // codice_ente
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

    public ImportazioniOrganigramma updateEsitoImportazioneOrganigramma(ImportazioniOrganigramma newRowInserted, String esito) {
        // Update nello storico importazioni. esito: Errore o Ok
        Integer idNewInsertedRowImpOrg = newRowInserted.getId();
        java.util.Optional<ImportazioniOrganigramma> findById = importazioniOrganigrammaRepository.findById(idNewInsertedRowImpOrg);
        ImportazioniOrganigramma rigaImportazione = findById.get();
        rigaImportazione.setEsito(esito);
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

    @Transactional(rollbackFor = Throwable.class)
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
            csvTransactionalReadDeleteInsert(file, tipo, idAziendaCodice, idAziendaInt);
            // Update nello storico importazioni. esito: OK e Data Fine: Data.now
            res = updateEsitoImportazioneOrganigramma(newRowInserted, "Ok");
        } catch (Throwable e) {
            System.out.println(e.getMessage());
            res = updateEsitoImportazioneOrganigramma(newRowInserted, "Errore");
        }

        return res;
    }

}
