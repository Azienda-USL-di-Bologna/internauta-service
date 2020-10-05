package it.bologna.ausl.internauta.service.controllers.rubrica;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.bologna.ausl.blackbox.PermissionManager;
import it.bologna.ausl.blackbox.exceptions.BlackBoxPermissionException;
import it.bologna.ausl.blackbox.utils.UtilityFunctions;
import it.bologna.ausl.eml.handler.EmlHandlerException;
import it.bologna.ausl.internauta.service.authorization.AuthenticatedSessionData;
import it.bologna.ausl.internauta.service.authorization.AuthenticatedSessionDataBuilder;
import it.bologna.ausl.internauta.service.authorization.UserInfoService;
import it.bologna.ausl.internauta.service.authorization.utils.UtenteProcton;
import it.bologna.ausl.internauta.service.configuration.utils.PostgresConnectionManager;
import it.bologna.ausl.internauta.service.configuration.utils.RubricaRestClientConnectionManager;
import it.bologna.ausl.internauta.service.exceptions.http.ControllerHandledExceptions;
import it.bologna.ausl.internauta.service.exceptions.http.Http400ResponseException;
import it.bologna.ausl.internauta.service.exceptions.http.Http404ResponseException;
import it.bologna.ausl.internauta.service.exceptions.http.Http500ResponseException;
import it.bologna.ausl.internauta.service.repositories.baborg.PersonaRepository;
import it.bologna.ausl.internauta.service.repositories.baborg.UtenteRepository;
import it.bologna.ausl.internauta.service.repositories.rubrica.ContattoRepository;
import it.bologna.ausl.internauta.service.rubrica.utils.similarity.SqlSimilarityResults;
import it.bologna.ausl.internauta.service.utils.CachedEntities;
import it.bologna.ausl.internauta.service.utils.MasterChefUtils;
import it.bologna.ausl.model.entities.baborg.Azienda;
import it.bologna.ausl.model.entities.baborg.AziendaParametriJson;
import it.bologna.ausl.model.entities.baborg.Persona;
import it.bologna.ausl.model.entities.baborg.Utente;
import it.bologna.ausl.model.entities.configuration.Applicazione;
import it.bologna.ausl.rubrica.maven.client.RestClient;
import it.bologna.ausl.rubrica.maven.client.RestClientException;
import it.bologna.ausl.rubrica.maven.resources.EmailResource;
import it.bologna.ausl.rubrica.maven.resources.FullContactResource;
import it.nextsw.common.utils.CommonUtils;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.stream.Collectors;
import javax.servlet.http.HttpServletRequest;
import okhttp3.Call;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.sql2o.Connection;
import org.sql2o.Query;
import org.sql2o.Sql2o;
import org.supercsv.cellprocessor.Optional;
import org.supercsv.cellprocessor.ift.CellProcessor;
import org.supercsv.io.CsvMapReader;
import org.supercsv.io.ICsvMapReader;
import org.supercsv.prefs.CsvPreference;

/**
 *
 * @author gusgus
 */
@RestController
@RequestMapping(value = "${rubrica.mapping.url.root}")
public class RubricaCustomController implements ControllerHandledExceptions {

    private static final Logger log = LoggerFactory.getLogger(RubricaCustomController.class);

    @Autowired
    CommonUtils commonUtils;

    @Autowired
    MasterChefUtils masterChefUtils;

    @Autowired
    UserInfoService userInfoService;

    @Autowired
    CachedEntities cachedEntities;

    @Autowired
    PostgresConnectionManager postgresConnectionManager;

    @Autowired
    PermissionManager permissionManager;

    @Autowired
    PersonaRepository personaRepository;

    @Autowired
    UtenteRepository utenteRepository;

    @Autowired
    RubricaRestClientConnectionManager rubricaRestClientConnectionManager;

    @Autowired
    private ContattoRepository contattoRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AuthenticatedSessionDataBuilder authenticatedSessionDataBuilder;

    @Value("${babelsuite.webapi.managedestinatari.url}")
    private String manageDestinatariUrl;

    @Value("${babelsuite.webapi.managedestinatari.method}")
    private String manageDestinatariMethod;

    /**
     * Effettua la ricerca sulla rubrica locale dell'utente indicato
     * sull'azienda indicata
     *
     * @param toSearch
     * @param azienda
     * @param idUtente
     * @param idStruttura
     * @return
     * @throws RestClientException
     */
//    public List<FullContactResource> searchContact(String toSearch, Azienda azienda, String idUtente, String idStruttura) throws RestClientException {
//        RestClient restClient = rubricaRestClientConnectionManager.getConnection(azienda.getId());
//        return restClient.searchContact(toSearch, idUtente, idStruttura, false);
//    }
    /**
     * Questa funzione cerca nelle rubriche dei db argo usando principalmente la
     * rubricarest.La fuznione è specifica per la ricerca della mail. Restituirà
     * un oggetto composto da descrizione contatto e sua email.
     *
     * @param toSearch
     * @param request
     * @return
     * @throws EmlHandlerException
     * @throws UnsupportedEncodingException
     * @throws Http500ResponseException
     * @throws Http404ResponseException
     * @throws RestClientException
     * @throws it.bologna.ausl.blackbox.exceptions.BlackBoxPermissionException
     */
    @RequestMapping(value = "searchEmailContact", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> searchEmailContact(
            @RequestParam("toSearch") String toSearch,
            HttpServletRequest request
    ) throws EmlHandlerException, UnsupportedEncodingException, Http500ResponseException, Http404ResponseException, RestClientException, BlackBoxPermissionException {

        // Prendo le informazioni che mi servono per chiamare la rubrica
        AuthenticatedSessionData authenticatedUserProperties = authenticatedSessionDataBuilder.getAuthenticatedUserProperties();
        Utente utente = authenticatedUserProperties.getUser();
        Azienda azienda = cachedEntities.getAzienda(utente.getIdAzienda().getId());
        UtenteProcton utenteProcton = (UtenteProcton) userInfoService.getUtenteProcton(utente.getIdPersona().getId(), azienda.getCodice());

        // Faccio la chiamata alla rubrica
        RestClient restClient = rubricaRestClientConnectionManager.getConnection(azienda.getId());
        List<FullContactResource> searchContact = restClient.searchContact(toSearch, utenteProcton.getIdUtente(), utenteProcton.getIdStruttura(), false);

        List<FullContactResource> contattiTrovati = new ArrayList();
        Set<Integer> idContatti = new HashSet();

        searchContact.forEach((c) -> {
            List<EmailResource> emails = c.getEmails();
            if (emails != null && !emails.isEmpty()) {
                contattiTrovati.add(c);
                idContatti.add(c.getContatto().getId());
            }
        });

        // Devo aggiungere anche una ricerca diretta sul db, nel caso che la stringa cercata sia proprio la pec
        List<Integer> contatti = searchContactByEmail(utenteProcton.getIdUtente(), toSearch, azienda.getCodice());
        if (!contatti.isEmpty()) {
            for (Integer contatto : contatti) {
                // Controllo di non aver già aggiunto questo contatto
                if (!idContatti.contains(contatto)) {
                    idContatti.add(contatto);
                    FullContactResource contact = restClient.getContact(contatto);
                    if (contact != null) {
                        contattiTrovati.add(contact);
                    }
                }
            }
        }

        // Torno il risultato della ricerca
        return new ResponseEntity(contattiTrovati, HttpStatus.OK);
    }

    /**
     * Questa funzione si collega al db locale argo per fare una ricerca tra le
     * mail. Query copiata da inde e non controllata.
     *
     * @param idUtente
     * @param toSearch
     * @param codiceAzienda
     * @return
     */
    private List<Integer> searchContactByEmail(String idUtente, String toSearch, String codiceAzienda) throws Http500ResponseException {
        String query = "with mia_rubrica as\n"
                + "(\n"
                + "select r.id, r.idrubricasuperiore, rs.idstruttura as id_struttura\n"
                + "from rubrica.rubrica r\n"
                + "join rubrica.rubrica rs on rs.id = r.idrubricasuperiore\n"
                + "where r.nome = :idUtente\n"
                + "),\n"
                + "rubriche_della_mia_struttura as\n"
                + "(\n"
                + "select id\n"
                + "from rubrica.rubrica\n"
                + "where nome in (\n"
                + "	select id_utente from procton.utenti where id_struttura = (select id_struttura from mia_rubrica)\n"
                + "	union\n"
                + "	select id_utente from procton.appartenenze_funzionali where id_struttura = (select id_struttura from mia_rubrica)\n"
                + ")\n"
                + "), \n"
                + "rubriche_pubbliche as\n"
                + "(\n"
                + "select id\n"
                + "from rubrica.rubrica\n"
                + "where tipo = 'pubblica'\n"
                + ")\n"
                + "select idcontatto\n"
                + "from rubrica.email e\n"
                + "join rubrica.contatto c on c.id = e.idcontatto\n"
                + "where email ilike :toSearch\n"
                + "and c.idrubrica = (select id from mia_rubrica)\n"
                + "--limit 50\n"
                + "union\n"
                + "select idcontatto\n"
                + "from rubrica.email e\n"
                + "join rubrica.contatto c on c.id = e.idcontatto\n"
                + "where email ilike :toSearch\n"
                + "and c.privato = 'false'\n"
                + "and c.idrubrica in (select id from rubriche_della_mia_struttura)\n"
                + "--limit 50\n"
                + "union\n"
                + "select idcontatto\n"
                + "from rubrica.email e\n"
                + "join rubrica.contatto c on c.id = e.idcontatto\n"
                + "where email ilike :toSearch\n"
                + "and c.idrubrica in (select id from rubriche_pubbliche)\n"
                + "limit 50";

        // Prendo la connessione dal connection manager
        Sql2o dbConnection = postgresConnectionManager.getDbConnection(codiceAzienda);
        List<Integer> contatti;

        try (Connection conn = (Connection) dbConnection.open()) {
            Query addParameter = conn.createQuery(query)
                    .addParameter("idUtente", idUtente)
                    .addParameter("toSearch", toSearch);
            log.info("esecuzione query: " + addParameter.toString());
            contatti = addParameter.executeAndFetch(Integer.class);
        } catch (Exception e) {
            log.error("errore nell'esecuzione della query", e);
            throw new Http500ResponseException("1", "Errore nell'escuzione della query");
        }
        return contatti;
    }

    @RequestMapping(value = "getSimilarities", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> getSimilarities(@RequestBody Map contatto) throws JsonProcessingException, IOException, BlackBoxPermissionException, ClassNotFoundException, NoSuchMethodException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, InvocationTargetException, InvocationTargetException, InvocationTargetException {

        String contattoString = objectMapper.writeValueAsString(contatto);

        // prendo utente connesso
        AuthenticatedSessionData authenticatedUserProperties = authenticatedSessionDataBuilder.getAuthenticatedUserProperties();
        Utente utente = authenticatedUserProperties.getUser();
        Persona persona = utente.getIdPersona();

        List<Azienda> aziendePersona = userInfoService.getAziendePersona(persona);
        List<Integer> collect = aziendePersona.stream().map(p -> p.getId()).collect(Collectors.toList());
        String idAziendeStr = UtilityFunctions.getArrayString(objectMapper, collect);
        String res = contattoRepository.getSimilarContacts(contattoString, idAziendeStr);

        SqlSimilarityResults similarityResults = objectMapper.readValue(res, SqlSimilarityResults.class);
        similarityResults.filterByPermission(persona, permissionManager);
        return new ResponseEntity(similarityResults, HttpStatus.OK);
    }

    @RequestMapping(value = "sendSelectedContactsToExternalApp",
            method = RequestMethod.POST,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> sendSelectedContactsToExternalApp(@RequestBody ExternalAppData data)
            throws JsonProcessingException, IOException, BlackBoxPermissionException {

        Azienda azienda = cachedEntities.getAziendaFromCodice(data.getCodiceAzienda());
        AuthenticatedSessionData authenticatedUserProperties = authenticatedSessionDataBuilder.getAuthenticatedUserProperties();
        Utente utente = authenticatedUserProperties.getUser();
        Persona persona = utente.getIdPersona();
        data.setCfUtenteOperazione(persona.getCodiceFiscale());

        okhttp3.RequestBody requestBody = okhttp3.RequestBody.create(
                okhttp3.MediaType.get("application/json; charset=utf-8"),
                objectMapper.writeValueAsString(data));
        OkHttpClient client = new OkHttpClient();

        Request request = new Request.Builder()
                .url(buildGestisciDestinatariDaRubricaInternautarUrl(azienda, data.getApp()))
                .post(requestBody)
                .addHeader("X-HTTP-Method-Override", manageDestinatariMethod)
                .build();

        log.info("Chiamo l'applicazione inde per salvare i contatti selezionati");
        Call call = client.newCall(request);
        try (Response response = call.execute();) {
            if (response.isSuccessful()) {
                log.info("Chiamata a webapi inde effettuata con successo");
                refreshDestinatari(persona, azienda, data.getGuid());
            } else {
                log.info("Errore nella chiamata alla webapi indosa");
                throw new IOException(String.format("molto malo: %s", response.message()));
            }
        }

        return new ResponseEntity(data, HttpStatus.OK);
    }

    private String buildGestisciDestinatariDaRubricaInternautarUrl(Azienda azienda, String idApplicazione) throws IOException {
        Applicazione applicazione = cachedEntities.getApplicazione(idApplicazione);
        AziendaParametriJson parametriAzienda = AziendaParametriJson.parse(objectMapper, azienda.getParametri());
        String url = String.format("%s%s%s", parametriAzienda.getBabelSuiteWebApiUrl(), applicazione.getBaseUrl(), manageDestinatariUrl);
        return url;
    }

    private void refreshDestinatari(Persona persona, Azienda azienda, String guid) throws IOException {
        log.info("Inserisco su redis il comando di refresh dei destinatari");
        List<String> dests = Arrays.asList(persona.getCodiceFiscale());

        Map<String, Object> primusCommandParams = new HashMap();
        primusCommandParams.put("refreshDestinatari", guid);
        AziendaParametriJson aziendaParametriJson = AziendaParametriJson.parse(objectMapper, azienda.getParametri());
        AziendaParametriJson.MasterChefParmas masterchefParams = aziendaParametriJson.getMasterchefParams();
        MasterChefUtils.MasterchefJobDescriptor masterchefJobDescriptor
                = masterChefUtils.buildPrimusMasterchefJob(
                        MasterChefUtils.PrimusCommands.refreshDestinatari,
                        primusCommandParams, "1", "1", dests, "*"
                );
        masterChefUtils.sendMasterChefJob(masterchefJobDescriptor, masterchefParams);

    }

    @RequestMapping(value = "insertCSVEstemporaneo",
            method = RequestMethod.POST,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> insertCSVEstemporaneo(
            @RequestParam("idUtente") String idUtente,
            @RequestParam("file") MultipartFile file,
            @RequestParam("idAzienda") String idAzienda) throws Http400ResponseException {
        
        if (!file.isEmpty() && (idUtente != null || !idUtente.equals("")) && (idAzienda != null || !idAzienda.equals(""))) {
            try {
                importaCSV(idUtente, idAzienda, file);
            } catch (IOException ex) {
                throw new Http400ResponseException("1", "I dati passati per l'importazione non sono corretti");
            }
        } else {
            throw new Http400ResponseException("2", "I dati passati per l'importazione sono assenti o non corretti");
        }

        return new ResponseEntity("", HttpStatus.OK);

    }

    private void importaCSV(String idUtente, String idAzienda, MultipartFile fileContatti) throws IOException {
        ICsvMapReader mapReader = null;
        try {
            InputStreamReader inputFileStreamReader = new InputStreamReader(fileContatti.getInputStream());
            CsvPreference SEMICOLON_DELIMITED = new CsvPreference.Builder('"', ';', "\r\n").build();
            Map<String, Object> dettagliContattiMap;
            mapReader = new CsvMapReader(inputFileStreamReader, SEMICOLON_DELIMITED);
            mapReader.getHeader(true);
            String[] headers = new String[]{"cognomeRagiorneSociale", "nome", "privatoAzienda",
                    "codiceFiscale", "pIva", "descrizione", "email", "pec",
                    "indirizzo", "civico", "comune","cap","provincia","telefono","fax"};
            CellProcessor[] processors = new CellProcessor[]{
                    // new NotNull(new StrRegEx(codiceEnteRegex, new ParseInt())), // codice_ente
                    new Optional(), // cognomeRagiorneSociale 
                    new Optional(), // nome
                    new Optional(), // privatoAzienda
                    new Optional(), // codiceFiscale
                    new Optional(), // pIva 
                    new Optional(), // descrizione
                    new Optional(), // email
                    new Optional(), // pec
                    new Optional(), // indirizzo
                    new Optional(), // civico
                    new Optional(), // comune
                    new Optional(), // cap
                    new Optional(), // provincia
                    new Optional(), // telefono
                    new Optional()  // fax
                };
            
            while ((dettagliContattiMap = mapReader.read(headers, processors)) != null) {
                if (dettagliContattiMap.get("cognomeRagiorneSociale") == null){
                    System.out.println("è vuoto");
                }else {
                    String cognomeRagiorneSociale = dettagliContattiMap.get("cognomeRagiorneSociale").toString();
                   System.out.println("dettagliContattiMap" + cognomeRagiorneSociale);}
            }
        }catch (Exception e) {
            System.out.println("e" + e);
        }

    }
}
