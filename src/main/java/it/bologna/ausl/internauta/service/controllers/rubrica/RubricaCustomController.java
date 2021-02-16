package it.bologna.ausl.internauta.service.controllers.rubrica;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import java.util.concurrent.TimeUnit;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.bologna.ausl.blackbox.PermissionManager;
import it.bologna.ausl.blackbox.exceptions.BlackBoxPermissionException;
import it.bologna.ausl.blackbox.utils.UtilityFunctions;
import it.bologna.ausl.eml.handler.EmlHandlerException;
import it.bologna.ausl.internauta.service.authorization.AuthenticatedSessionData;
import it.bologna.ausl.internauta.service.authorization.AuthenticatedSessionDataBuilder;
import it.bologna.ausl.internauta.service.authorization.UserInfoService;
import it.bologna.ausl.internauta.service.authorization.utils.UtenteProcton;
import it.bologna.ausl.internauta.service.configuration.nextsdr.RestControllerEngineImpl;
import it.bologna.ausl.internauta.service.configuration.utils.PostgresConnectionManager;
import it.bologna.ausl.internauta.service.configuration.utils.RubricaRestClientConnectionManager;
import it.bologna.ausl.internauta.service.controllers.permessi.PermessiCustomController;
import it.bologna.ausl.internauta.service.exceptions.GruppiException;
import it.bologna.ausl.internauta.service.exceptions.http.ControllerHandledExceptions;
import it.bologna.ausl.internauta.service.exceptions.http.Http404ResponseException;
import it.bologna.ausl.internauta.service.exceptions.http.Http500ResponseException;
import it.bologna.ausl.internauta.service.repositories.baborg.AziendaRepository;
import it.bologna.ausl.internauta.service.repositories.baborg.PersonaRepository;
import it.bologna.ausl.internauta.service.repositories.baborg.StrutturaRepository;
import it.bologna.ausl.internauta.service.repositories.baborg.UtenteRepository;
import it.bologna.ausl.internauta.service.repositories.baborg.UtenteStrutturaRepository;
import it.bologna.ausl.internauta.service.repositories.rubrica.ContattoRepository;
import it.bologna.ausl.internauta.service.repositories.rubrica.DettaglioContattoRepository;
import it.bologna.ausl.internauta.service.rubrica.utils.similarity.SqlSimilarityResults;
import it.bologna.ausl.internauta.service.utils.CachedEntities;
import it.bologna.ausl.internauta.service.utils.MasterChefUtils;
import it.bologna.ausl.internauta.service.utils.rubrica.CreatoreJsonPermessiContatto;
import it.bologna.ausl.model.entities.baborg.Azienda;
import it.bologna.ausl.model.entities.baborg.AziendaParametriJson;
import it.bologna.ausl.model.entities.baborg.Persona;
import it.bologna.ausl.model.entities.baborg.Struttura;
import it.bologna.ausl.model.entities.baborg.Utente;
import it.bologna.ausl.model.entities.configuration.Applicazione;
import it.bologna.ausl.model.entities.rubrica.Contatto;
import it.bologna.ausl.model.entities.rubrica.DettaglioContatto;
import it.bologna.ausl.model.entities.rubrica.Email;
import it.bologna.ausl.model.entities.rubrica.GruppiContatti;
import it.bologna.ausl.model.entities.rubrica.Indirizzo;
import it.bologna.ausl.model.entities.rubrica.Telefono;
import it.bologna.ausl.model.entities.rubrica.projections.generated.ContattoWithDettaglioContattoList;
import it.bologna.ausl.rubrica.maven.client.RestClient;
import it.bologna.ausl.rubrica.maven.client.RestClientException;
import it.bologna.ausl.rubrica.maven.resources.EmailResource;
import it.bologna.ausl.rubrica.maven.resources.FullContactResource;
import it.nextsw.common.projections.ProjectionsInterceptorLauncher;
import it.nextsw.common.utils.CommonUtils;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.InvocationTargetException;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.servlet.http.HttpServletRequest;
import okhttp3.Call;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.projection.ProjectionFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.sql2o.Connection;
import org.sql2o.Query;
import org.sql2o.Sql2o;

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
    RestControllerEngineImpl restControllerEngine;

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
    AziendaRepository aziendaRepository;

    @Autowired
    StrutturaRepository strutturaRepository;

    @Autowired
    UtenteStrutturaRepository utenteStrutturaRepository;

    @Autowired
    RubricaRestClientConnectionManager rubricaRestClientConnectionManager;

    @Autowired
    private ContattoRepository contattoRepository;

    @Autowired
    private DettaglioContattoRepository dettaglioContattoRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    ProjectionFactory projectionFactory;

    @Autowired
    private PermessiCustomController permessiCustomController;

    @Autowired
    private AuthenticatedSessionDataBuilder authenticatedSessionDataBuilder;

    @Autowired
    private ProjectionsInterceptorLauncher projectionsInterceptorLauncher;

    @PersistenceContext
    private EntityManager em;

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

    @Transactional(rollbackFor = Throwable.class)
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

        List<Contatto> estemporaneiToAddToRubricaAsProtocontatti = data.getEstemporaneiToAddToRubrica();
        String glogParams = data.getGlogParams();

        Persona getPersona = personaRepository.findById(persona.getId()).get();
        Utente getUtente = utenteRepository.findById(utente.getId()).get();

        if (estemporaneiToAddToRubricaAsProtocontatti != null && !estemporaneiToAddToRubricaAsProtocontatti.isEmpty()) {
            List<Contatto> listContattiAsProtocontattiDaSalvare = new ArrayList<Contatto>();

            ObjectMapper mapper = new ObjectMapper();
//            mapper.configure(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES, true);
//            mapper.setPropertyNamingStrategy(PropertyNamingStrategy.UPPER_CAMEL_CASE);
            SelectedContactsLists selectedContactsLists = mapper.readValue(data.getSelectedContactsLists(), SelectedContactsLists.class);

//            liste
            List<SelectedContact> allSelectedContactsMITTENTE = selectedContactsLists.getMITTENTE();
            List<SelectedContact> allSelectedContactsA = selectedContactsLists.getA();
            List<SelectedContact> allSelectedContactsCC = selectedContactsLists.getCC();

            for (Contatto contattoAsProtocontatto : estemporaneiToAddToRubricaAsProtocontatti) {
                final List<Email> emailList = contattoAsProtocontatto.getEmailList();
                if (!emailList.isEmpty()) {
                    for (Email email : emailList) {
                        email.setIdContatto(contattoAsProtocontatto);
                        email.getIdDettaglioContatto().setIdContatto(contattoAsProtocontatto);
                    }
                }
                final List<Telefono> telefonoList = contattoAsProtocontatto.getTelefonoList();
                if (!telefonoList.isEmpty()) {
                    for (Telefono tel : telefonoList) {
                        tel.setIdContatto(contattoAsProtocontatto);
                        tel.getIdDettaglioContatto().setIdContatto(contattoAsProtocontatto);
                    }
                }
                final List<Indirizzo> indirizziList = contattoAsProtocontatto.getIndirizziList();
                if (!indirizziList.isEmpty()) {
                    for (Indirizzo indirizzi : indirizziList) {
                        indirizzi.setIdContatto(contattoAsProtocontatto);
                        indirizzi.getIdDettaglioContatto().setIdContatto(contattoAsProtocontatto);
                    }
                }

                contattoAsProtocontatto.setIdPersonaCreazione(getPersona);
                contattoAsProtocontatto.setIdUtenteCreazione(getUtente);

//                listContattiAsProtocontattiDaSalvare.add(contattoAsProtocontatto);
                Contatto savedContatto = contattoRepository.save(contattoAsProtocontatto);

                log.info("Contatto as protocontatto è stato salvato");
                //      to do enum selectionMode, MITTENTE, DESTINATARI
                //      set status INSERTED on contatti salvati
                if (data.getMode().equals("MITTENTE")) {
                    allSelectedContactsMITTENTE = getSelectedContactsListAndSetAsInsertedToRubrica(allSelectedContactsMITTENTE, savedContatto);

                }
                if (data.getMode().equals("DESTINATARI")) {
                    if (data.getApp().equals("procton")) {
                        allSelectedContactsA = getSelectedContactsListAndSetAsInsertedToRubrica(allSelectedContactsA, savedContatto);
                        allSelectedContactsCC = getSelectedContactsListAndSetAsInsertedToRubrica(allSelectedContactsCC, savedContatto);
                    } else {
                        allSelectedContactsA = getSelectedContactsListAndSetAsInsertedToRubrica(allSelectedContactsA, savedContatto);
                    }
                }

            }
//            contattoRepository.saveAll(listContattiAsProtocontattiDaSalvare);
            selectedContactsLists.setA(allSelectedContactsA);
            selectedContactsLists.setCC(allSelectedContactsCC);
            selectedContactsLists.setMITTENTE(allSelectedContactsMITTENTE);

            String selectedContactsListsAsString = mapper.writeValueAsString(selectedContactsLists);
//            log.info("selectedContactsListsAsString to send at inde: " + selectedContactsListsAsString);
            data.setSelectedContactsLists(selectedContactsListsAsString);
        }
        log.info("set estemporaneiToAddToRubrica to null");
        data.setEstemporaneiToAddToRubrica(null);

        log.info("Metto faccio una Lista di persone a cui aggiornare la videata");
        //List<String> cfPersoneDiCuiAggiornareLaVideataList = Arrays.asList(getPersona.getCodiceFiscale());
        List<String> cfPersoneDiCuiAggiornareLaVideataList = new ArrayList<>();
        cfPersoneDiCuiAggiornareLaVideataList.add(persona.getCodiceFiscale());

        log.info("Cerco il realUser");
        Utente realUser = authenticatedUserProperties.getRealUser();
        // Se i due utenti sono diversi, allora devo caricare la persona reale
        if (!realUser.getId().equals(getUtente.getId())) {
            log.info("L'utente è impersonato: carico realPersona");
            try {
                Persona realPersona = personaRepository.findById(realUser.getIdPersona().getId()).get();
                String realPersonCf = realPersona.getCodiceFiscale();
                cfPersoneDiCuiAggiornareLaVideataList.add(realPersonCf);
            } catch (NoSuchElementException ex) {
                log.error("Real persona non trovata...");
            }
        }

        if (glogParams.isEmpty() || StringUtils.isEmpty(glogParams)) {
            log.info("set Glog Params to null");
            data.setGlogParams(null);
        }

        okhttp3.RequestBody requestBody = okhttp3.RequestBody.create(
                okhttp3.MediaType.get("application/json; charset=utf-8"),
                objectMapper.writeValueAsString(data));
        OkHttpClient client = new OkHttpClient.Builder().connectTimeout(2, TimeUnit.MINUTES).build();

        Request request = new Request.Builder()
                .url(buildGestisciDestinatariDaRubricaInternautarUrl(azienda, data.getApp()))
                .post(requestBody)
                .addHeader("X-HTTP-Method-Override", manageDestinatariMethod)
                .build();

        log.info("Chiamo l'applicazione inde per salvare i contatti selezionati");
        Call call = client.newCall(request);
        try (Response response = call.execute();) {
            int responseCode = response.code();
            if (response.isSuccessful()) {
                log.info("Chiamata a webapi inde effettuata con successo");
                refreshDestinatari(cfPersoneDiCuiAggiornareLaVideataList, azienda, data.getGuid());
            } else {
                log.info("Errore nella chiamata alla webapi InDe: " + responseCode + " " + response.message());
                throw new IOException(String.format("Errore nella chiamata alla WepApi InDe: %s", response.message()));
            }
        }

        return new ResponseEntity(data, HttpStatus.OK);
    }

    private List<SelectedContact> getSelectedContactsListAndSetAsInsertedToRubrica(List<SelectedContact> selectedContactsList, Contatto savedContatto) {
        if (!selectedContactsList.isEmpty()) {
            return setSelectedContactAsInsertedToRubrica(selectedContactsList, savedContatto);
        } else {
            return selectedContactsList;
        }
    }

    private List<SelectedContact> setSelectedContactAsInsertedToRubrica(List<SelectedContact> selectedContactsList, Contatto savedContatto) {
        for (SelectedContact selectedContactEstemporaneo : selectedContactsList) {
            log.info("Loop Selected Contatti, update keys, Id, estemporaneo, addToRubrica, status");
            Contatto selectedContact = objectMapper.convertValue(selectedContactEstemporaneo.getContact(), Contatto.class);
            DettaglioContatto selectedAddress = objectMapper.convertValue(selectedContactEstemporaneo.getAddress(), DettaglioContatto.class);
//            if (selectedContact.getDescrizione().equals(savedContatto.getDescrizione()) && ((!selectedContact.getEmailList().isEmpty() && selectedAddressDescrizione.equals(selectedContact.getEmailList().get(0)))
//                    || (!selectedContact.getTelefonoList().isEmpty() && selectedAddressDescrizione.equals(selectedContact.getTelefonoList().get(0)))
//                    || (!selectedContact.getIndirizziList().isEmpty() && selectedAddressDescrizione.equals(selectedContact.getIndirizziList().get(0))))) {

            if (selectedContact.getDescrizione().equals(savedContatto.getDescrizione())
                    && selectedContactEstemporaneo.getAddToRubrica() != null && selectedContactEstemporaneo.getAddToRubrica()
                    && selectedContactEstemporaneo.getStatus() != null && selectedContactEstemporaneo.getStatus().equals(SelectedContactStatus.INITIAL)
                    && selectedContactEstemporaneo.getEstemporaneo() != null && selectedContactEstemporaneo.getEstemporaneo()) {
                selectedContact.setId(savedContatto.getId());
                selectedAddress.setIdContatto(savedContatto);
                log.info("Update selected contact DESCRIZIONE:" + savedContatto.getDescrizione() + " - INDIRIZZO: " + selectedAddress.getDescrizione());
                selectedContactEstemporaneo.setAddToRubrica(Boolean.FALSE);
                selectedContactEstemporaneo.setStatus(SelectedContactStatus.INSERTED);
                selectedContactEstemporaneo.setEstemporaneo(Boolean.FALSE);
                selectedContactEstemporaneo.setContact(selectedContact);
                selectedContactEstemporaneo.setAddress(selectedAddress);
            }
        }
        return selectedContactsList;
    }

    private String buildGestisciDestinatariDaRubricaInternautarUrl(Azienda azienda, String idApplicazione) throws IOException {
        Applicazione applicazione = cachedEntities.getApplicazione(idApplicazione);
        AziendaParametriJson parametriAzienda = AziendaParametriJson.parse(objectMapper, azienda.getParametri());
        String url = String.format("%s%s%s", parametriAzienda.getBabelSuiteWebApiUrl(), applicazione.getBaseUrl(), manageDestinatariUrl);
        //url = "http://localhost:8080/Deli/GestisciDestinatariDaRubricaInternauta";
        log.info("Url da chiamare: " + url);
        return url;
    }

    private void refreshDestinatari(List<String> cfPersoneDiCuiAggiornareLaVideataList, Azienda azienda, String guid) throws IOException {
        log.info("Inserisco su redis il comando di refresh dei destinatari");

        Map<String, Object> primusCommandParams = new HashMap();
        primusCommandParams.put("refreshDestinatari", guid);
        AziendaParametriJson aziendaParametriJson = AziendaParametriJson.parse(objectMapper, azienda.getParametri());
        AziendaParametriJson.MasterChefParmas masterchefParams = aziendaParametriJson.getMasterchefParams();
        MasterChefUtils.MasterchefJobDescriptor masterchefJobDescriptor
                = masterChefUtils.buildPrimusMasterchefJob(MasterChefUtils.PrimusCommands.refreshDestinatari,
                        primusCommandParams, "1", "1", cfPersoneDiCuiAggiornareLaVideataList, "*"
                );
        masterChefUtils.sendMasterChefJob(masterchefJobDescriptor, masterchefParams);

    }

//    @Transactional(rollbackFor = Throwable.class)
//    @RequestMapping(value = "insertCSVEstemporaneo",
//            method = RequestMethod.POST,
//            produces = MediaType.APPLICATION_JSON_VALUE)
//    public ResponseEntity<String> insertCSVEstemporaneo(
//            @RequestParam("idUtente") Integer idUtente,
//            @RequestParam("file") MultipartFile file,
//            @RequestParam("idAzienda") String idAzienda) throws Http400ResponseException, BlackBoxPermissionException {
//
//        if (!file.isEmpty() && (idUtente != null || !idUtente.equals("")) && (idAzienda != null || !idAzienda.equals(""))) {
//            try {
//                importaCSV(idUtente, idAzienda, file);
//            } catch (IOException ex) {
//                throw new Http400ResponseException("2", "I dati passati per l'importazione non sono corretti");
//            }
//        } else {
//            throw new Http400ResponseException("2", "I dati passati per l'importazione sono assenti");
//        }
//
//        return new ResponseEntity("0", HttpStatus.OK);
//
//    }
//
//    private void importaCSV(Integer idUtente, String idAzienda, MultipartFile fileContatti) throws IOException, BlackBoxPermissionException {
//        String provenienza = "importatoreCSV";
//        Utente u = utenteRepository.findById(idUtente).get();
//        Persona p = personaRepository.findById(u.getIdPersona().getId()).get();
//        ICsvMapReader mapReader = null;
//        InputStreamReader inputFileStreamReader = new InputStreamReader(fileContatti.getInputStream());
//        CsvPreference SEMICOLON_DELIMITED = new CsvPreference.Builder('"', ';', "\r\n").build();
//        Map<String, Object> dettagliContattiMap;
//        mapReader = new CsvMapReader(inputFileStreamReader, SEMICOLON_DELIMITED);
//        mapReader.getHeader(true);
//        boolean fax = true;
//        boolean telefono = true;
//        boolean email = true;
//        boolean indirizzo = true;
//
//        String[] headers = new String[]{"cognomeRagiorneSociale", "nome", "privatoAzienda",
//            "codiceFiscale", "pIva", "descrizione", "email", "pec",
//            "indirizzo", "civico", "comune", "cap", "provincia", "telefono", "fax"};
//        CellProcessor[] processors = new CellProcessor[]{
//            // new NotNull(new StrRegEx(codiceEnteRegex, new ParseInt())), // codice_ente
//            new Optional(), // cognomeRagiorneSociale 
//            new Optional(), // nome
//            new Optional(), // privatoAzienda
//            new Optional(), // codiceFiscale
//            new Optional(), // pIva 
//            new Optional(), // descrizione
//            new Optional(), // email
//            new Optional(), // pec
//            new Optional(), // indirizzo
//            new Optional(), // civico
//            new Optional(), // comune
//            new Optional(), // cap
//            new Optional(), // provincia
//            new Optional(), // telefono
//            new Optional() // fax
//        };
//
//        List<Contatto> cs = new ArrayList<Contatto>();
//        while ((dettagliContattiMap = mapReader.read(headers, processors)) != null) {
//            log.info("rigo: " + mapReader.getRowNumber());
//            boolean principale = true;
//            Contatto c = new Contatto();
//            DettaglioContatto dcEmail = new DettaglioContatto();
//            dcEmail.setTipo(DettaglioContatto.TipoDettaglio.EMAIL);
//            DettaglioContatto dcIndirizzo = new DettaglioContatto();
//            dcIndirizzo.setTipo(DettaglioContatto.TipoDettaglio.INDIRIZZO_FISICO);
//            DettaglioContatto dcTelefono = new DettaglioContatto();
//            dcTelefono.setTipo(DettaglioContatto.TipoDettaglio.TELEFONO);
//
//            if (dettagliContattiMap.get("privatoAzienda") != null && !dettagliContattiMap.get("privatoAzienda").toString().trim().equalsIgnoreCase("")) {
//                if (dettagliContattiMap.get("privatoAzienda").toString().equalsIgnoreCase("a")) {
//                    if (dettagliContattiMap.get("cognomeRagiorneSociale") != null && !dettagliContattiMap.get("cognomeRagiorneSociale").toString().trim().equalsIgnoreCase("")) {
//                        c.setRagioneSociale(dettagliContattiMap.get("cognomeRagiorneSociale").toString());
//                    } else {
//                        //errore RagiorneSociale vuoto
//                    }
//                    c.setTipo(Contatto.TipoContatto.AZIENDA);
//                } else if (dettagliContattiMap.get("privatoAzienda").toString().equalsIgnoreCase("p")) {
//                    if (dettagliContattiMap.get("cognomeRagiorneSociale") != null && !dettagliContattiMap.get("cognomeRagiorneSociale").toString().trim().equalsIgnoreCase("")) {
//                        c.setCognome(dettagliContattiMap.get("cognomeRagiorneSociale").toString());
//                    } else {
//                        //errore Cognome vuoto
//                    }
//                    if (dettagliContattiMap.get("nome") != null && !dettagliContattiMap.get("nome").toString().trim().equalsIgnoreCase("")) {
//                        c.setNome(dettagliContattiMap.get("nome").toString());
//                    } else {
//                        //errore nome vuoto
//                    }
//                    c.setTipo(Contatto.TipoContatto.PERSONA_FISICA);
//                } else {
//                    //errore inserito privato azienda sbagliato
//                }
//            } else {
//                //errore non inserito privato azienda 
//            }
//            if (dettagliContattiMap.get("codiceFiscale") != null && !dettagliContattiMap.get("codiceFiscale").toString().trim().equalsIgnoreCase("")) {
//                c.setCodiceFiscale(dettagliContattiMap.get("codiceFiscale").toString());
//            }
//            if (dettagliContattiMap.get("pIva") != null && !dettagliContattiMap.get("pIva").toString().trim().equalsIgnoreCase("")) {
//                c.setPartitaIva(dettagliContattiMap.get("pIva").toString());
//            }
//            if (dettagliContattiMap.get("pIva") != null && !dettagliContattiMap.get("pIva").toString().trim().equalsIgnoreCase("")) {
//                c.setPartitaIva(dettagliContattiMap.get("pIva").toString());
//            }
//
//            if (dettagliContattiMap.get("email") != null && !dettagliContattiMap.get("email").toString().trim().equalsIgnoreCase("")) {
//
//                List<Email> es = new ArrayList<>();
//                Email e = new Email();
//                if (dettagliContattiMap.get("pec") != null && dettagliContattiMap.get("pec").toString().trim().equalsIgnoreCase("S")) {
//                    e.setPec(true);
//                } else {
//                    e.setPec(false);
//                }
//                e.setPrincipale(principale);
//                dcEmail.setPrincipale(principale);
//                principale = false;
//                e.setProvenienza(provenienza);
//                e.setIdContatto(c);
//                e.setEmail(dettagliContattiMap.get("email").toString());
//                es.add(e);
//                dcEmail.setEmail(e);
//                dcEmail.setDescrizione(dettagliContattiMap.get("email").toString());
//                c.setEmailList(es);
//
//            }
//            if (dettagliContattiMap.get("indirizzo") != null && !dettagliContattiMap.get("indirizzo").toString().trim().equalsIgnoreCase("")) {
//                if (dettagliContattiMap.get("civico") != null && !dettagliContattiMap.get("civico").toString().trim().equalsIgnoreCase("")) {
//                    if (dettagliContattiMap.get("comune") != null && !dettagliContattiMap.get("civico").toString().trim().equalsIgnoreCase("")) {
//                        if (dettagliContattiMap.get("cap") != null && !dettagliContattiMap.get("civico").toString().trim().equalsIgnoreCase("")) {
//                            if (dettagliContattiMap.get("provincia") != null && !dettagliContattiMap.get("civico").toString().trim().equalsIgnoreCase("")) {
//                                Indirizzo i = new Indirizzo();
//                                i.setVia(dettagliContattiMap.get("indirizzo").toString());
//                                i.setCivico(dettagliContattiMap.get("civico").toString());
//                                i.setComune(dettagliContattiMap.get("comune").toString());
//                                i.setCap(dettagliContattiMap.get("cap").toString());
//                                i.setProvincia(dettagliContattiMap.get("provincia").toString());
//                                List<Indirizzo> indirizziList = new ArrayList<>();
//                                i.setPrincipale(principale);
//                                dcIndirizzo.setPrincipale(principale);
//                                principale = false;
//                                i.setProvenienza(provenienza);
//                                i.setIdContatto(c);
//                                indirizziList.add(i);
//                                c.setIndirizziList(indirizziList);
//                                dcIndirizzo.setIndirizzo(i);
//                                dcIndirizzo.setDescrizione(i.getDescrizione());
//                            }
//                        }
//                    }
//                }
//            }
//            if (dettagliContattiMap.get("telefono") != null && !dettagliContattiMap.get("telefono").toString().trim().equalsIgnoreCase("")) {
//                Telefono t = new Telefono();
//                t.setNumero(dettagliContattiMap.get("telefono").toString());
//                List<Telefono> telefonoList = new ArrayList<>();
//                t.setPrincipale(principale);
//                dcTelefono.setPrincipale(principale);
//                if (dettagliContattiMap.get("fax") != null && !dettagliContattiMap.get("fax").toString().trim().equalsIgnoreCase("")) {
//                    t.setFax(true);
//                } else {
//                    t.setFax(false);
//                }
//                t.setProvenienza(provenienza);
//                t.setIdContatto(c);
//                telefonoList.add(t);
//                c.setTelefonoList(telefonoList);
//                dcTelefono.setTelefono(t);
//                dcTelefono.setDescrizione(t.getDescrizione());
//
//            }
//            c.setCategoria(Contatto.CategoriaContatto.ESTERNO);
//            c.setDaVerificare(true);
//
//            c.setEliminato(false);
//            c.setModificabile(true);
//            c.setProvenienza(provenienza);
//            c.setIdUtenteCreazione(u);
//            c.setIdPersonaCreazione(p);
//            c.setRiservato(false);
//
//            if (dettagliContattiMap.get("fax") == null && dettagliContattiMap.get("telefono") == null && dettagliContattiMap.get("indirizzo") == null && dettagliContattiMap.get("mail") == null) {
//                //errore inserire almeno un campo tra tutti i mezzi
//            } else {
//                if (dettagliContattiMap.get("descrizione") != null && !dettagliContattiMap.get("descrizione").toString().trim().equalsIgnoreCase("")) {
//                    c.setDescrizione(dettagliContattiMap.get("descrizione").toString());
//                } else {
//                    c.setDescrizione("");
//                }
//                Contatto cSaved = contattoRepository.save(c);
//                dcEmail.setIdContatto(cSaved);
//                dcIndirizzo.setIdContatto(cSaved);
//                dcTelefono.setIdContatto(cSaved);
//                log.info("fin qui tutto ok");
//                log.info("email " + dcEmail.getPrincipale());
//                log.info("indirizzo " + dcIndirizzo.getPrincipale());
//                log.info("telefono " + dcTelefono.getPrincipale());
//
//                dettaglioContattoRepository.save(dcEmail);
//                dettaglioContattoRepository.save(dcIndirizzo);
//                dettaglioContattoRepository.save(dcTelefono);
//                //cs.add(c);
//            }
//        }
//
//        //contattoRepository.saveAll(cs);
//    }
    @RequestMapping(value = "salvaPermessiSuContattoEsportatoDaRubricaVecchia",
            method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> salvaPermessiSuContattoEsportatoDaRubricaVecchia(
            @RequestBody String requestData
    ) throws Throwable {
        log.info("Entrato in salvaPermessiSuContattoEsportatoDaRubricaVecchia");
        String responseMessage = "";
        log.info("requestData " + requestData);
        JSONObject data = new JSONObject(requestData);
        String cfUtenteRichiedente = data.getString("cfUtentePermesso");
        String codiceAzienda = data.getString("codiceAzienda");
        log.info("codiceAzienda " + codiceAzienda);
        Integer idContatto = data.getInt("idContatto");
        boolean daiPermessiAllaMiaStrutturaDiretta = data.getBoolean("daiPermessiAllaMiaStrutturaDiretta");
        try {
            Contatto contatto = contattoRepository.findById(idContatto).get();
            if (contatto != null) {
                if (!contatto.getRiservato()) {
                    contatto.setRiservato(true);
                    contatto = contattoRepository.save(contatto);
                }
                if (daiPermessiAllaMiaStrutturaDiretta) {
                    log.info("devo dare i permessi alla struttura di appartenenza diretta di " + cfUtenteRichiedente);
                    Azienda azienda = aziendaRepository.findByCodice(codiceAzienda);
                    Persona persona = personaRepository.findByCodiceFiscale(cfUtenteRichiedente);
                    Utente utente = utenteRepository.findByIdAziendaAndIdPersona(azienda, persona);
                    Integer idStrutturaAfferenzaDirettaAttiva = utenteStrutturaRepository.getIdStrutturaAfferenzaDirettaAttivaByIdUtente(utente.getId());
                    if (idStrutturaAfferenzaDirettaAttiva != null) {
                        Struttura struttura = strutturaRepository.findById(idStrutturaAfferenzaDirettaAttiva).get();

                        JSONObject oggettone = CreatoreJsonPermessiContatto.generaJSONObjectPerAggiuntaPermessiSuOggettoContatto(struttura, contatto);
                        log.info("Oggettone per aggiunta permessi da mappare:\n" + oggettone.toString(4));
                        Map<String, Object> params
                                = new ObjectMapper().readValue(oggettone.toString(), HashMap.class);
                        log.info("Chiamo managerPermissionAdvanced...");
                        permessiCustomController.managePermissionsAdvanced(params, null);
                        log.info("Permesso inserito, creo il messaggio di ritorno");
                        responseMessage = "Aggiunto permesso a struttura " + struttura.getNome()
                                + " su contatto " + contatto.getDescrizione()
                                + " (id " + contatto.getId() + ")";
                    }
                }

            }
        } catch (Throwable t) {
            log.error("Errore! ", t);
            throw t;
        }
        log.info("Fatto, torno il messaggio: " + responseMessage);
        return new ResponseEntity(responseMessage, HttpStatus.OK);
    }

    @RequestMapping(value = "findContattiUtentiByCodiciFiscaliForGruppoImport",
            method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> findContattiUtentiByCodiciFiscaliForGruppoImport(@RequestBody String requestData) {
        JSONArray jArrayDiRisposta = new JSONArray();
        log.info("Entrato in findContattiUtentiByCodiciFiscaliForGruppoImport");
        JSONArray requestDataJsonArray = new JSONArray(requestData);
        log.info("requestData\n" + requestDataJsonArray.toString(4));
        log.info("Ciclo gli oggetti...");
        for (int i = 0; i < requestDataJsonArray.length(); i++) {
            JSONObject objectRequested = (JSONObject) requestDataJsonArray.get(i);
            log.info("Object Requested:\n" + objectRequested.toString(4));
            JSONObject contattoTrovato = new JSONObject();
            log.info("Carico la persona by cf");
            String cfUtente = objectRequested.getString("cfUtente");
            Persona persona = personaRepository.findByCodiceFiscale(cfUtente);
            log.info("Persona trovata ", persona.getId());
            log.info("Carico il contatto della persona...");
            Contatto contatto = contattoRepository.getOne(persona.getIdContatto().getId());
            if (contatto != null) {
                JSONObject contattoJSON = new JSONObject();
                contattoJSON.put("id", contatto.getId());
                contattoJSON.put("categoria", contatto.getCategoria().toString());
                contattoJSON.put("descrizione", contatto.getDescrizione());
                ZonedDateTime contattoVersionCorrected = contatto.getVersion().withZoneSameInstant(ZoneId.of("Europe/Rome"));
                contattoJSON.put("version", contattoVersionCorrected);
                Integer idStrutturaInternauta = objectRequested.getInt("idStrutturaInternauta");
                log.info("Cerco il contatto della struttura internatua: " + idStrutturaInternauta);
                List<Contatto> listaContatti = contattoRepository.findByIdEsternoAndCategoria(idStrutturaInternauta.toString(), "STRUTTURA");
                log.info("Trovati " + listaContatti.size());
                if (listaContatti.size() == 1) {
                    Contatto contattoStruttura = listaContatti.get(0);
                    log.info("contattoStruttura: " + contattoStruttura.getId());
                    JSONObject dettaglioContattoJSON = new JSONObject();
                    log.info("Cerco il dettaglio contatto per IdContattoAndIdContattoEsterno...");
                    DettaglioContatto dettaglioContatto = dettaglioContattoRepository.
                            findByIdContattoAndIdContattoEsterno(contatto, contattoStruttura.getId());
                    if (dettaglioContatto != null) {
                        log.info("dettaglioContatto: " + dettaglioContatto.getId());
                        dettaglioContattoJSON.put("id", dettaglioContatto.getId());
                        dettaglioContattoJSON.put("tipo", dettaglioContatto.getTipo().toString());
                        dettaglioContattoJSON.put("descrizione", dettaglioContatto.getDescrizione());
                        dettaglioContattoJSON.put("principale", dettaglioContatto.getPrincipale());
                        dettaglioContattoJSON.put("version", dettaglioContatto.getVersion());
                        ZonedDateTime dettaglioContattoVersionCorrected = dettaglioContatto.getVersion().withZoneSameInstant(ZoneId.of("Europe/Rome"));
                        dettaglioContattoJSON.put("version", dettaglioContattoVersionCorrected);
                    } else {
                        log.info("Dettaglio contatto non trovato!");
                    }
                    if (dettaglioContattoJSON != null && contattoJSON != null) {
                        contattoTrovato.put("idContatto", contattoJSON);
                        contattoTrovato.put("idDettaglioContatto", dettaglioContattoJSON);
                    }
                }

            } else {
                log.info("contatt NON TROVATO");
            }
            objectRequested.put("contattoTrovato", contattoTrovato);
            log.info("Inserisco l'oggetto nella risposta\t" + objectRequested.toString(4));
            jArrayDiRisposta.put(objectRequested);
            log.info("...");
        }
        log.info("RIRTONO LA RISPOSTA\n" + jArrayDiRisposta.toString(4));
        return new ResponseEntity(jArrayDiRisposta.toString(4), HttpStatus.OK);
    }

    @Transactional(rollbackFor = Throwable.class)
    @RequestMapping(value = "salvaGruppo",
            method = RequestMethod.POST,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public void salvaGruppo(@RequestBody Contatto gruppo, HttpServletRequest request) throws GruppiException {
//    public void salvaGruppo(@RequestBody Contatto gruppo, HttpServletRequest request) throws RestControllerEngineException, AbortSaveInterceptorException, BlackBoxPermissionException {

        log.info("salvaGruppo");
        try {
            projectionsInterceptorLauncher.setRequestParams(null, request);
            AuthenticatedSessionData authenticatedUserProperties = authenticatedSessionDataBuilder.getAuthenticatedUserProperties();
            Persona idPersonaCreazione = authenticatedUserProperties.getPerson();
            Integer[] listaAziende
                    = userInfoService.getAziendePersona(idPersonaCreazione).stream().map(a -> a.getId()).collect(Collectors.toList()).toArray(new Integer[0]);
            Utente idUtenteCreazione = authenticatedUserProperties.getUser();
            idUtenteCreazione = em.find(Utente.class, idUtenteCreazione.getId());
            idPersonaCreazione = em.find(Persona.class, idPersonaCreazione.getId());
//            idUtenteCreazione = em.merge(idUtenteCreazione);
//            idPersonaCreazione = em.merge(idPersonaCreazione);

            gruppo.setIdPersonaCreazione(idPersonaCreazione);
            gruppo.setIdUtenteCreazione(idUtenteCreazione);
            gruppo.setIdAziende(listaAziende);

            //passo 1 creo i contatti
            String repositoryKey = request.getServletPath();

            int slashPos = repositoryKey.lastIndexOf("/");
            if (slashPos != -1) {
                repositoryKey = repositoryKey.substring(0, slashPos);
            }
            repositoryKey += "/" + "contatto";

            List<GruppiContatti> gruppiContattiList = new ArrayList<>();
            for (Contatto contatto : gruppo.getContattiContenuti()) {
                contatto.setIdUtenteCreazione(idUtenteCreazione);
                contatto.setIdPersonaCreazione(idPersonaCreazione);
                contatto.setIdAziende(listaAziende);
                Map<String, Object> rimuoviIdContattoAncestor = rimuoviIdContattoAncestor(contatto);
                //inserisco il contatto
                ContattoWithDettaglioContattoList contattoInserito = (ContattoWithDettaglioContattoList) restControllerEngine.insert(
                        rimuoviIdContattoAncestor,
                        request,
                        null,
                        repositoryKey,
                        false,
                        ContattoWithDettaglioContattoList.class.getSimpleName());
                GruppiContatti gruppoContatto = new GruppiContatti();

//            Contatto contattoVero= em.find(Contatto.class,contattoInserito.getId());
                DettaglioContatto dettaglioContattoVero = em.find(DettaglioContatto.class, (objectMapper.convertValue(contattoInserito.getDettaglioContattoList(), new TypeReference<List<DettaglioContatto>>() {
                })).get(0).getId());

                gruppoContatto.setIdContatto(dettaglioContattoVero.getIdContatto());
                gruppoContatto.setIdDettaglioContatto(dettaglioContattoVero);
                gruppoContatto.setIdGruppo(gruppo);
                gruppiContattiList.add(gruppoContatto);
            }
            gruppo.setContattiDelGruppoList(gruppiContattiList);

            em.persist(gruppo);

        } catch (Exception e) {
            log.debug("entro qui");
            throw new GruppiException("errore nel salvataggio del gruppo da csv", e);
        }
//        Map<String, Object> gruppoSenzaIdContattoAncestor = rimuoviIdContattoAncestor(gruppo);
//        ContattoWithContattiDelGruppoListAndDettaglioContattoListAndIdPersonaCreazione gruppoPrj = projectionFactory.createProjection(ContattoWithContattiDelGruppoListAndDettaglioContattoListAndIdPersonaCreazione.class, gruppo);

//    Map<String, Object> mappaGruppo = rimuoviIdContattoAncestor(gruppo);
//        mappaGruppo.put("contattiDelGruppoList", gruppiContattiList);
//        ContattoWithPlainFields gruppoInserito = (ContattoWithPlainFields) restControllerEngine.insert(
//                mappaGruppo,
//                request,
//                null,
//                repositoryKey,
//                false,
//                ContattoWithPlainFields.class.getSimpleName());
//        contattoRepository.findById(contattoInserito.getId())
        //passo 2 creo il gruppo
        //passo 2.1 creo la relazione tra il gruppo e i contatti 
//        Contatto gruoppo = new Contatto();
//        List<gruppocontatto> gruppocontattolist = new arraylist();
//        gruoppo.setContattidelgruppolist(list gruppi contatti);
        //gruppo da visualizzare come risposta
    }

    private Map<String, Object> rimuoviIdContattoAncestor(Contatto contatto) {

        Map<String, Object> mappaContatto = objectMapper.convertValue(contatto, new TypeReference<Map<String, Object>>() {
        });

        List<Map<String, Object>> emailList = (List<Map<String, Object>>) mappaContatto.get("emailList");
        List<Map<String, Object>> indirizziList = (List<Map<String, Object>>) mappaContatto.get("indirizziList");
        List<Map<String, Object>> telefonoList = (List<Map<String, Object>>) mappaContatto.get("telefonoList");
        List<Map<String, Object>> contattiDelGruppoList = (List<Map<String, Object>>) mappaContatto.get("contattiDelGruppoList");
        Arrays.asList(emailList, indirizziList, telefonoList, contattiDelGruppoList).stream().filter((mezzi) -> (mezzi != null)).forEach((mezzi) -> {
            for (Map<String, Object> mezzo : mezzi) {
                if (mezzo != null) {
                    if (mezzo.get("idDettaglioContatto") != null) {
                        Map<String, Object> dettaglioContatto = (Map<String, Object>) mezzo.get("idDettaglioContatto");
                        dettaglioContatto.remove("idContatto");
                    }
                    mezzo.remove("idContatto");
                }
            }
        });
        return mappaContatto;
    }

    @RequestMapping(value = "findContattiStruttureForGruppoImport",
            method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> findContattiStruttureForGruppoImport(@RequestBody String requestData) {
        JSONArray jArrayDiRisposta = new JSONArray();
        log.info("Entrato in findContattiStruttureForGruppoImport");
        JSONArray requestDataJsonArray = new JSONArray(requestData);
        log.info("RequestData\n" + requestDataJsonArray.toString(4));
        log.info("Ciclo gli oggetti");
        for (int i = 0; i < requestDataJsonArray.length(); i++) {
            JSONObject objectRequested = (JSONObject) requestDataJsonArray.get(i);
            log.info("objectRequested:\n" + objectRequested);
            JSONObject contattoTrovato = new JSONObject();
            Integer idStrutturaInternauta = objectRequested.getInt("idStrutturaInternauta");
            log.info("idStrutturaInternauta: " + idStrutturaInternauta);
            if (idStrutturaInternauta != null && idStrutturaInternauta != 0) {
                log.info("Cerco il contatto della struttura ByIdEsternoAndCategoria...");
                List<Contatto> listaContatti = contattoRepository
                        .findByIdEsternoAndCategoria(idStrutturaInternauta.toString(), "STRUTTURA");
                log.info("Trovati: " + listaContatti.size());
                if (listaContatti.size() == 1) {
                    Contatto contatto = listaContatti.get(0);
                    if (contatto != null) {
                        log.info("contatto: " + contatto.getId());
                        JSONObject contattoJSON = new JSONObject();
                        contattoJSON.put("id", contatto.getId());
                        contattoJSON.put("categoria", contatto.getCategoria().toString());
                        contattoJSON.put("descrizione", contatto.getDescrizione());
                        ZonedDateTime contattoVersionCorrected = contatto.getVersion().withZoneSameInstant(ZoneId.of("Europe/Rome"));
                        contattoJSON.put("version", contattoVersionCorrected);
                        JSONObject dettaglioContattoJSON = new JSONObject();
                        log.info("cerco il DettaglioContatto findByIdContattoAndTipo...");
                        DettaglioContatto dettaglioContatto = dettaglioContattoRepository.
                                findByIdContattoAndTipo(contatto, "STRUTTURA");
                        if (dettaglioContatto != null) {
                            dettaglioContattoJSON.put("id", dettaglioContatto.getId());
                            dettaglioContattoJSON.put("tipo", dettaglioContatto.getTipo().toString());
                            dettaglioContattoJSON.put("descrizione", dettaglioContatto.getDescrizione());
                            dettaglioContattoJSON.put("principale", dettaglioContatto.getPrincipale());
                            ZonedDateTime dettaglioContattoVersionCorrected = dettaglioContatto.getVersion().withZoneSameInstant(ZoneId.of("Europe/Rome"));
                            dettaglioContattoJSON.put("version", dettaglioContattoVersionCorrected);
                        } else {
                            log.info("DettaglioContatto non trovato!");
                        }
                        if (dettaglioContattoJSON != null && contattoJSON != null) {
                            contattoTrovato.put("idContatto", contattoJSON);
                            contattoTrovato.put("idDettaglioContatto", dettaglioContattoJSON);
                        }
                    } else {
                        log.info("Contatto non trovato!");
                    }
                }

            }

            objectRequested.put("contattoTrovato", contattoTrovato);
            log.info("Inserisco l'oggetto nella risposta\t" + objectRequested.toString(4));
            jArrayDiRisposta.put(objectRequested);
            log.info("...");
        }
        log.info("RIRTONO LA RISPOSTA\n" + jArrayDiRisposta.toString(4));
        return new ResponseEntity(jArrayDiRisposta.toString(4), HttpStatus.OK);
    }
}
