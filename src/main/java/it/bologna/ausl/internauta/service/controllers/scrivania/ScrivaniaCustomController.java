package it.bologna.ausl.internauta.service.controllers.scrivania;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.querydsl.core.types.dsl.BooleanExpression;
import it.bologna.ausl.blackbox.exceptions.BlackBoxPermissionException;
import it.bologna.ausl.internauta.service.argo.bollovirtuale.BolloVirtualeManager;
import it.bologna.ausl.internauta.service.argo.bollovirtuale.DatoBolloVirtuale;
import it.bologna.ausl.internauta.service.authorization.AuthenticatedSessionData;
import it.bologna.ausl.internauta.service.authorization.AuthenticatedSessionDataBuilder;
import it.bologna.ausl.internauta.utils.bds.types.CategoriaPermessiStoredProcedure;
import it.bologna.ausl.internauta.utils.bds.types.PermessoEntitaStoredProcedure;
import it.bologna.ausl.internauta.utils.bds.types.PermessoStoredProcedure;
import it.bologna.ausl.internauta.service.authorization.UserInfoService;
import it.bologna.ausl.internauta.service.configuration.nextsdr.RestControllerEngineImpl;
import it.bologna.ausl.internauta.service.configuration.utils.PostgresConnectionManager;
import it.bologna.ausl.internauta.service.exceptions.http.ControllerHandledExceptions;
import it.bologna.ausl.internauta.service.exceptions.http.Http400ResponseException;
import it.bologna.ausl.internauta.service.exceptions.http.Http403ResponseException;
import it.bologna.ausl.internauta.service.exceptions.http.Http404ResponseException;
import it.bologna.ausl.internauta.service.exceptions.http.Http500ResponseException;
import it.bologna.ausl.internauta.service.exceptions.http.HttpInternautaResponseException;
import it.bologna.ausl.internauta.service.repositories.baborg.AziendaRepository;
import it.bologna.ausl.internauta.service.repositories.baborg.PersonaRepository;
import it.bologna.ausl.internauta.service.repositories.baborg.StrutturaRepository;
import it.bologna.ausl.internauta.service.repositories.configurazione.ApplicazioneRepository;
import it.bologna.ausl.internauta.service.repositories.diagnostica.ReportRepository;
import it.bologna.ausl.internauta.service.repositories.scrivania.AttivitaRepository;
import it.bologna.ausl.internauta.service.scrivania.anteprima.BabelDownloader;
import it.bologna.ausl.internauta.service.scrivania.anteprima.BabelDownloaderResponseBody;
import it.bologna.ausl.internauta.service.utils.CachedEntities;
import it.bologna.ausl.internauta.service.utils.InternautaUtils;
import it.bologna.ausl.model.entities.baborg.Azienda;
import it.bologna.ausl.model.entities.baborg.AziendaParametriJson;
import it.bologna.ausl.model.entities.baborg.Persona;
import it.bologna.ausl.model.entities.baborg.Struttura;
import it.bologna.ausl.model.entities.baborg.Utente;
import it.bologna.ausl.model.entities.configurazione.Applicazione;
import it.bologna.ausl.model.entities.diagnostica.Report;
import it.bologna.ausl.model.entities.scrivania.Attivita;
import it.bologna.ausl.model.entities.scrivania.Attivita.TipoAttivita;
import it.bologna.ausl.model.entities.scrivania.QAttivita;
import it.bologna.ausl.rubrica.maven.client.RestClientException;
import it.nextsw.common.annotations.NextSdrRepository;
import it.nextsw.common.controller.exceptions.NotFoundResourceException;
import it.nextsw.common.controller.exceptions.RestControllerEngineException;
import it.nextsw.common.interceptors.exceptions.AbortSaveInterceptorException;
import it.nextsw.common.utils.CommonUtils;
import it.nextsw.common.utils.EntityReflectionUtils;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import okhttp3.Call;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.jose4j.json.internal.json_simple.JSONArray;
import org.jose4j.json.internal.json_simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StreamUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.sql2o.Connection;
import org.sql2o.Query;
import org.sql2o.Sql2o;

/**
 *
 * @author gdm
 */
@RestController
@RequestMapping(value = "${scrivania.mapping.url.root}")
public class ScrivaniaCustomController implements ControllerHandledExceptions {

    private static final Logger LOGGER = LoggerFactory.getLogger(ScrivaniaCustomController.class);

    @Autowired
    private BabelDownloader babelDownloader;

    @Autowired
    private AziendaRepository aziendaRepository;

    @Autowired
    private StrutturaRepository strutturaRepository;

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    protected CachedEntities cachedEntities;

    @Autowired
    UserInfoService userInfoService;

    @Autowired
    ApplicazioneRepository applicazioneRepository;

    @Autowired
    protected PersonaRepository personaRepository;

    @Autowired
    protected AttivitaRepository attivitaRepository;

    @Autowired
    private CommonUtils commonUtils;

    @Autowired
    private InternautaUtils internautaUtils;

    @Autowired
    private RestControllerEngineImpl restControllerEngine;

    @Autowired
    private AuthenticatedSessionDataBuilder authenticatedSessionDataBuilder;

    @Autowired
    private PostgresConnectionManager postgresConnectionManager;

    @Autowired
    private GestioneMenu gestioneMenu;
    
    @Autowired
    private ReportRepository reportRepository;
    
    @Value("${babelsuite.webapi.eliminaattivitadainternauta.url}")
    private String EliminaAttivitaDaInternauta;

    @Value("${babelsuite.webapi.eliminaattivitadainternauta.method}")
    private String eliminaAttivitaDaInternauta;

    private final String CMD_APRI_FIRMONE = "?CMD=open_firmone_local";
    private final String CMD_APRI_PRENDONE = "?CMD=open_prendone_local";
    private static final String FROM = "&from=INTERNAUTA";
    private final String HTTPS = "https://";
    
    private static final Logger log = LoggerFactory.getLogger(ScrivaniaCustomController.class);

    @RequestMapping(value = {"getAnteprima"}, method = RequestMethod.GET)
    public void getAnteprima(
            @RequestParam(required = true) String guid,
            @RequestParam(required = true) String tipologia,
            @RequestParam(required = true) Integer idAzienda,
            @RequestParam(required = true) String idApplicazione,
            @RequestParam(required = true) String fileName,
            HttpServletRequest request,
            HttpServletResponse response) throws HttpInternautaResponseException, IOException, BlackBoxPermissionException {

        BabelDownloaderResponseBody downloadUrlRsponseBody = babelDownloader.getDownloadUrl(babelDownloader.createRquestBody(guid, tipologia), idAzienda, idApplicazione);
        switch (downloadUrlRsponseBody.getStatus()) {
            case OK:
                if (!StringUtils.hasText(downloadUrlRsponseBody.getUrl())) {
                    throw new Http500ResponseException("8", downloadUrlRsponseBody.getMessage());
                }
                try (Response downloadStream = babelDownloader.getDownloadStream(downloadUrlRsponseBody.getUrl())) {
                    try (OutputStream out = response.getOutputStream()) {
                        response.setHeader(guid, guid);
                        response.setHeader("Content-Type", "application/pdf");
                        response.setHeader("X-Frame-Options", "sameorigin");
                        response.setHeader("Content-Disposition", ";filename=" + fileName + ".pdf");
                        StreamUtils.copy(downloadStream.body().byteStream(), out);
                    }
                }
                break;

            case BAD_REQUEST:
                throw new Http400ResponseException("1", downloadUrlRsponseBody.getMessage());
            case FORBIDDEN:
                throw new Http403ResponseException("2", downloadUrlRsponseBody.getMessage());
            case USER_NOT_FOUND:
                throw new Http404ResponseException("3", downloadUrlRsponseBody.getMessage());
            case FILE_NOT_FOUND:
                throw new Http404ResponseException("4", downloadUrlRsponseBody.getMessage());
            case GENERAL_ERROR:
                throw new Http500ResponseException("5", downloadUrlRsponseBody.getMessage());
            default:
                throw new Http500ResponseException("6", downloadUrlRsponseBody.getMessage());
        }
    }

    @RequestMapping(value = {"getFirmoneUrls"}, method = RequestMethod.GET)
    public void getFirmoneUrls(HttpServletRequest request, HttpServletResponse response) throws IOException, BlackBoxPermissionException {
        AuthenticatedSessionData authenticatedSessionData = authenticatedSessionDataBuilder.getAuthenticatedUserProperties();
        Utente utente = authenticatedSessionData.getUser();
        Persona persona = personaRepository.getOne(utente.getIdPersona().getId());
        List<Azienda> aziende = new ArrayList<>();
        for (Utente u : persona.getUtenteList()) {
            List<Azienda> aziendeUtente = getAziendePerCuiFirmo(u);
            if (aziendeUtente.size() > 0) {
                aziende.addAll(aziendeUtente);
            }
        }

        JSONObject objResponse = new JSONObject();
        JSONArray jsonArrayAziende = new JSONArray();
        Integer numeroAziende = 0;
        if (aziende.size() > 0) {
            for (Azienda azienda : aziende) {
                Boolean alreadyAdded = false;
                for (int i = 0; i < jsonArrayAziende.size(); i++) {
                    JSONObject j = (JSONObject) jsonArrayAziende.get(i);
                    if (j.get("nome") == azienda.getNome()) {
                        alreadyAdded = !alreadyAdded;
                        break;
                    }
                }
                if (!alreadyAdded) {
                    numeroAziende++;
                    try {
                        jsonArrayAziende.add(buildAziendaUrl(azienda, CMD_APRI_FIRMONE));
                    } catch (IOException e) {
                        LOGGER.error("errore nella creazione del link", e);
                        throw new IOException("errore nella creazione del link", e);
                    }
                }
            }
        }
        objResponse.put("size", numeroAziende);
        objResponse.put("aziende", jsonArrayAziende);
        response.setHeader(HttpHeaders.CONTENT_TYPE, "application/json");
        ServletOutputStream out = response.getOutputStream();
        out.print(objResponse.toJSONString());
        out.flush();
    }

    @RequestMapping(value = {"getPrendoneUrls"}, method = RequestMethod.GET)
    public void getPrendoneUrls(HttpServletRequest request, HttpServletResponse response) throws IOException, BlackBoxPermissionException {
        AuthenticatedSessionData authenticatedSessionData = authenticatedSessionDataBuilder.getAuthenticatedUserProperties();
        Utente utente = authenticatedSessionData.getUser();
        Persona persona = personaRepository.getOne(utente.getIdPersona().getId());
        List<Azienda> aziendePersona = userInfoService.getAziendePersona(persona);

        JSONObject objResponse = new JSONObject();
        JSONArray jsonArrayAziende = new JSONArray();
        Integer numeroAziende = 0;
        for (Azienda azienda : aziendePersona) {
            numeroAziende++;
            try {
                jsonArrayAziende.add(buildAziendaUrl(azienda, CMD_APRI_PRENDONE));
            } catch (IOException | BlackBoxPermissionException e) {
                LOGGER.error("errore nella creazione del link", e);
                throw e;
            }
        }
        objResponse.put("size", numeroAziende);
        objResponse.put("aziende", jsonArrayAziende);
        response.setHeader(HttpHeaders.CONTENT_TYPE, "application/json");
        ServletOutputStream out = response.getOutputStream();
        out.print(objResponse.toJSONString());
        out.flush();
    }

    private JSONObject buildAziendaUrl(Azienda aziendaTarget, String command) throws UnsupportedEncodingException, IOException, BlackBoxPermissionException {
        AuthenticatedSessionData authenticatedSessionData = authenticatedSessionDataBuilder.getAuthenticatedUserProperties();
        Applicazione applicazione = cachedEntities.getApplicazione(Applicazione.Applicazioni.babel.toString());
        String assembledURL = internautaUtils.getUrl(authenticatedSessionData, command, Applicazione.Applicazioni.babel.toString(), aziendaTarget);
        JSONObject objAzienda = new JSONObject();
        objAzienda.put("nome", aziendaTarget.getNome());
        objAzienda.put("url", assembledURL);
        objAzienda.put("urlGenerationStrategy", applicazione.getUrlGenerationStrategy());
        return objAzienda;
    }

    private List<Azienda> getAziendePerCuiFirmo(Utente utente) throws BlackBoxPermissionException {
        List<PermessoEntitaStoredProcedure> permessiDiFlusso = userInfoService.getPermessiDiFlusso(utente);
        List<Azienda> aziende = new ArrayList<>();
        if (permessiDiFlusso != null && permessiDiFlusso.size() > 0) {
            for (PermessoEntitaStoredProcedure permesso : permessiDiFlusso) {
                List<CategoriaPermessiStoredProcedure> categorie = permesso.getCategorie();
                if (categorie == null) {
                    break;
                }
                for (CategoriaPermessiStoredProcedure categoria : categorie) {
                    List<PermessoStoredProcedure> permessiVeri = categoria.getPermessi();
                    if (permessiVeri == null) {
                        break;
                    }
                    for (PermessoStoredProcedure permessoVero : permessiVeri) {
                        String predicato = permessoVero.getPredicato();
                        if (predicato.equals("FIRMA") || predicato.equals("AGDFIRMA")) {
                            Struttura struttura = strutturaRepository.getOne(permesso.getOggetto().getIdProvenienza());
                            Azienda azienda = aziendaRepository.getOne(struttura.getIdAzienda().getId());
                            aziende.add(azienda);
                        }
                    }
                }
            }
        }
        return aziende;
    }

    @RequestMapping(value = {"getScrivaniaCommonParameters"}, method = RequestMethod.GET)
    public Map<String, Object> getScrivaniaCommonParameters() {
        Map res = new HashMap();
        res.put(ScrivaniaCommonParameters.BABEL_APPLICATION.toString(), cachedEntities.getApplicazione("babel"));
        return res;
    }

    @Transactional
    @RequestMapping(value = {"cancellaNotifiche"}, method = RequestMethod.GET)
    public void cancellaNotifiche(HttpServletRequest request, HttpServletResponse response) throws IOException, BlackBoxPermissionException, RestControllerEngineException, AbortSaveInterceptorException, NotFoundResourceException, ClassNotFoundException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Utente utente = (Utente) authentication.getPrincipal();
        Persona persona = personaRepository.getOne(utente.getIdPersona().getId());
        //BooleanExpression notifichePersona = QAttivita.attivita.idPersona.id.eq(persona.getId());
        BooleanExpression notifichePersona = QAttivita.attivita.idPersona.id.eq(persona.getId()).and(QAttivita.attivita.tipo.eq(TipoAttivita.NOTIFICA.toString()));
        Iterable<Attivita> notificheList = attivitaRepository.findAll(notifichePersona);
        for (Attivita notifica : notificheList) {
            String attivitaPath = commonUtils.resolvePlaceHolder(EntityReflectionUtils.getFirstAnnotationOverHierarchy(attivitaRepository.getClass(), NextSdrRepository.class).repositoryPath());
            restControllerEngine.delete(notifica.getId(), request, null, attivitaPath, false, null);
            //ttivitaRepository.delete(notifica);
        }
    }
    
    @Transactional
    @RequestMapping(value = {"cancellaattivita"}, method = RequestMethod.POST)
    public ResponseEntity<?> cancellaAttivita(
            @RequestParam("id_attivita") String idAttivita,
            @RequestParam("id_applicazione") String idApplicazione,
            @RequestParam("id_azienda") String idAzienda) throws  Throwable {
        AuthenticatedSessionData authenticatedUserProperties = authenticatedSessionDataBuilder.getAuthenticatedUserProperties();

        Azienda azienda = aziendaRepository.findById(Integer.parseInt(idAzienda)).get();
        AziendaParametriJson parametriAzienda = AziendaParametriJson.parse(objectMapper, azienda.getParametri());
       
        
        String cf = "";
        String cfReale = "";
        try {
            cf = authenticatedUserProperties.getPerson().getCodiceFiscale();
            cfReale = authenticatedUserProperties.getRealPerson().getCodiceFiscale();
        } catch (Exception e) {
            cfReale = authenticatedUserProperties.getPerson().getCodiceFiscale();
        }
        Applicazione applicazione = cachedEntities.getApplicazione(idApplicazione);
        //String idAttivita = datiAggiutiviJson.get("id_attivita_babel").toString();
//        ("id_attivita_babel");
        
        String url = String.format("%s%s%s", parametriAzienda.getBabelSuiteWebApiUrl(), applicazione.getBaseUrl(), EliminaAttivitaDaInternauta);
        //String url = "http://localhost:8080/Babel/EliminaAttivitaDaInternauta";
        
        Map<String, String> hm = new HashMap<String, String>();
        hm.put("idAttivita", idAttivita);
        hm.put("idApplicazione", idApplicazione);
        hm.put("cf", cf);
        hm.put("cfReale", cfReale);
        okhttp3.RequestBody requestBody = okhttp3.RequestBody.create(
                okhttp3.MediaType.get("application/json; charset=utf-8"),
                objectMapper.writeValueAsString(hm));
        OkHttpClient client = new OkHttpClient.Builder().connectTimeout(12, TimeUnit.MINUTES).build();

        Request request = new Request.Builder()
                .url(url)
                .post(requestBody)
                .addHeader("X-HTTP-Method-Override", eliminaAttivitaDaInternauta)
                .build();

        Call call = client.newCall(request);
        HashMap readValue = null;
        try (Response response = call.execute();) {
            int responseCode = response.code();
            if (response.isSuccessful()) {
                readValue = objectMapper.readValue(response.body().string(), HashMap.class);
//                r.put(resp);
                log.info("Chiamata a webapi inde effettuata con successo");
            } else {
                log.info("Errore nella chiamata alla webapi InDe: " + responseCode + " " + response.message());
                throw new IOException(String.format("Errore nella chiamata alla WepApi InDe: %s", response.message()));
            }

        } catch (Exception ex) {
            throw new Exception(ex.getMessage());
        }

        return new ResponseEntity(readValue, HttpStatus.OK);
        
    }

    @RequestMapping(value = {"getDatiBolloByAzienda"}, method = RequestMethod.GET)
    public List<DatoBolloVirtuale> getDatiBolloByAzienda(@RequestParam("codiceAzienda") String codiceAzienda,
            @RequestParam("from") String from,
            @RequestParam("to") String to,
            HttpServletRequest request) throws Http500ResponseException, Http404ResponseException, RestClientException {

        // Prendo la connessione dal connection manager
        Sql2o dbConnection = postgresConnectionManager.getDbConnection(codiceAzienda);
        dbConnection.setDefaultColumnMappings(BolloVirtualeManager.mapQueryGetDatiBolliVirtuali());

        List<DatoBolloVirtuale> datiBolloVirtuale;

        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");

        try (Connection conn = (Connection) dbConnection.open()) {
            Query queryWithParams = conn.createQuery(BolloVirtualeManager.queryGetDatiBolliVirtuali())
                    .addParameter("from", dateFormat.parse(from))
                    .addParameter("to", dateFormat.parse(to));
            LOGGER.info("esecuzione query getDatiBolloByAzienda: " + queryWithParams.toString());
            datiBolloVirtuale = (List<DatoBolloVirtuale>) queryWithParams.executeAndFetch(DatoBolloVirtuale.class);
        } catch (Exception e) {
            LOGGER.error("errore nell'esecuzione della query getDatiBolloByAzienda", e);
            throw new Http500ResponseException("1", "Errore nell'escuzione della query getDatiBolloByAzienda");
        }
        return datiBolloVirtuale;
    }

    public enum ScrivaniaCommonParameters {
        BABEL_APPLICATION
    }

    @RequestMapping(value = {"getMenuScrivania"}, method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<ItemMenu>> getMenuScrivania(HttpServletRequest request, HttpServletResponse response) throws IOException, BlackBoxPermissionException {
        try {
            AuthenticatedSessionData authenticatedSessionData = authenticatedSessionDataBuilder.getAuthenticatedUserProperties();
            Utente utente = authenticatedSessionData.getUser();
            Persona persona = personaRepository.getOne(utente.getIdPersona().getId());
    //        Persona persona = utente.getIdPersona();

            List<ItemMenu> buildMenu = gestioneMenu.buildMenu(persona);
            return new ResponseEntity(objectMapper.writeValueAsString(buildMenu), HttpStatus.OK);
        } catch (Throwable t) {
            Report report = new Report();
            report.setTipologia("GET_MENU_SCRIVANIA_ERROR");
            Map<String, String> additionalData = new HashMap();
            additionalData.put("message", t.getMessage());
            additionalData.put("toString", t.toString());
            t.printStackTrace();
            report.setAdditionalData(objectMapper.writeValueAsString(additionalData));
            reportRepository.save(report);
            return new ResponseEntity("Not so good madafaca :D", HttpStatus.OK);
        }
    }
}
