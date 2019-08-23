package it.bologna.ausl.internauta.service.controllers.scrivania;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.querydsl.core.types.dsl.BooleanExpression;
import it.bologna.ausl.blackbox.exceptions.BlackBoxPermissionException;
import it.bologna.ausl.internauta.service.authorization.AuthenticatedSessionData;
import it.bologna.ausl.internauta.service.authorization.AuthenticatedSessionDataBuilder;
import it.bologna.ausl.internauta.utils.bds.types.CategoriaPermessiStoredProcedure;
import it.bologna.ausl.internauta.utils.bds.types.PermessoEntitaStoredProcedure;
import it.bologna.ausl.internauta.utils.bds.types.PermessoStoredProcedure;
import it.bologna.ausl.internauta.service.authorization.UserInfoService;
import it.bologna.ausl.internauta.service.configuration.nextsdr.RestControllerEngineImpl;
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
import it.bologna.ausl.internauta.service.repositories.scrivania.AttivitaRepository;
import it.bologna.ausl.internauta.service.scrivania.anteprima.BabelDownloader;
import it.bologna.ausl.internauta.service.scrivania.anteprima.BabelDownloaderResponseBody;
import it.bologna.ausl.internauta.service.utils.CachedEntities;
import it.bologna.ausl.model.entities.baborg.Azienda;
import it.bologna.ausl.model.entities.baborg.AziendaParametriJson;
import it.bologna.ausl.model.entities.baborg.Persona;
import it.bologna.ausl.model.entities.baborg.Struttura;
import it.bologna.ausl.model.entities.baborg.Utente;
import it.bologna.ausl.model.entities.configuration.Applicazione;
import it.bologna.ausl.model.entities.scrivania.Attivita;
import it.bologna.ausl.model.entities.scrivania.Attivita.TipoAttivita;
import it.bologna.ausl.model.entities.scrivania.QAttivita;
import it.nextsw.common.annotations.NextSdrRepository;
import it.nextsw.common.controller.exceptions.NotFoundResourceException;
import it.nextsw.common.controller.exceptions.RestControllerEngineException;
import it.nextsw.common.interceptors.exceptions.AbortSaveInterceptorException;
import it.nextsw.common.utils.CommonUtils;
import it.nextsw.common.utils.EntityReflectionUtils;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import okhttp3.Response;
import org.jose4j.json.internal.json_simple.JSONArray;
import org.jose4j.json.internal.json_simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StreamUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

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
    private RestControllerEngineImpl restControllerEngine;
    
    @Autowired
    private AuthenticatedSessionDataBuilder authenticatedSessionDataBuilder;
    
    private final String CMD_APRI_FIRMONE = "?CMD=open_firmone_local";
    private final String CMD_APRI_PRENDONE = "?CMD=open_prendone_local";
    private static final String FROM = "&from=INTERNAUTA";
    private final String HTTPS = "https://";
   
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
                try(Response downloadStream = babelDownloader.getDownloadStream(downloadUrlRsponseBody.getUrl())) {
                    try(OutputStream out = response.getOutputStream()) {
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
    public void getFirmoneUrls(HttpServletRequest request, HttpServletResponse response) throws IOException, BlackBoxPermissionException{
        AuthenticatedSessionData authenticatedSessionData = authenticatedSessionDataBuilder.getAuthenticatedUserProperties();
        Utente utente = authenticatedSessionData.getUser();
        Persona persona = personaRepository.getOne(utente.getIdPersona().getId());
        Azienda aziendaUtenteConnesso = utente.getIdAzienda();
        AziendaParametriJson parametriAziendaUtenteConnesso = AziendaParametriJson.parse(this.objectMapper, aziendaUtenteConnesso.getParametri());
        List<Azienda> aziende = new ArrayList<>();
        for (Utente u : persona.getUtenteList()) {
            List<Azienda> aziendeUtente = getAziendePerCuiFirmo(u);
            if(aziendeUtente.size() > 0){
                aziende.addAll(aziendeUtente);
            }
        }
        
        JSONObject objResponse = new JSONObject();
        JSONArray jsonArrayAziende = new JSONArray();
        Integer numeroAziende = 0;
        if(aziende.size() > 0){
            for (Azienda azienda : aziende) {
                Boolean alreadyAdded = false;
                for (int i = 0; i < jsonArrayAziende.size(); i++) {
                    JSONObject j = (JSONObject) jsonArrayAziende.get(i);
                    if(j.get("nome") == azienda.getNome()){
                        alreadyAdded = !alreadyAdded;
                        break;
                    }
                }
                if(!alreadyAdded){
                    numeroAziende++;
                    try{
                        jsonArrayAziende.add(buildAziendaUrl(azienda, CMD_APRI_FIRMONE, utente, parametriAziendaUtenteConnesso));
                    }catch(IOException e){
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
        Azienda aziendaUtenteConnesso = utente.getIdAzienda();
        AziendaParametriJson parametriAziendaUtenteConnesso = AziendaParametriJson.parse(this.objectMapper, aziendaUtenteConnesso.getParametri());
        List<Azienda> aziendePersona = userInfoService.getAziendePersona(persona);
        
        JSONObject objResponse = new JSONObject();
        JSONArray jsonArrayAziende =  new JSONArray();
        Integer numeroAziende = 0;
        for (Azienda azienda : aziendePersona) {
            numeroAziende++;
            try{
                jsonArrayAziende.add(buildAziendaUrl(azienda, CMD_APRI_PRENDONE, utente, parametriAziendaUtenteConnesso));
            } catch(IOException | BlackBoxPermissionException e){
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
    
    private JSONObject buildAziendaUrl(Azienda azienda, String command, Utente utente, AziendaParametriJson parametriAziendaUtenteConnesso) throws UnsupportedEncodingException, IOException, BlackBoxPermissionException{       
        String stringToEncode = command;
        AuthenticatedSessionData authenticatedSessionData = authenticatedSessionDataBuilder.getAuthenticatedUserProperties();
        Utente realUser = authenticatedSessionData.getRealUser();
        Persona realPerson = authenticatedSessionData.getRealPerson();
        Persona person = authenticatedSessionData.getPerson();
        int idSessionLog = authenticatedSessionData.getIdSessionLog();
        if(person.getCodiceFiscale() != null && person.getCodiceFiscale().length() > 0){
            stringToEncode += stringToEncode.length() > 0 ? "&utente=" : "?utente="; // non so se serve alle applicazioni INDE o a internauta o a tutti e 2
            stringToEncode += person.getCodiceFiscale();
        }
        
        if (realPerson != null) {
            stringToEncode += "&realUser=" + realPerson.getCodiceFiscale();
            stringToEncode += "&impersonatedUser=" + person.getCodiceFiscale();
            stringToEncode += "&utenteLogin=" + realPerson.getCodiceFiscale(); // serve alle applicazioni INDE
        } else {
            stringToEncode += "&user=" + person.getCodiceFiscale();
            stringToEncode += "&utenteLogin=" + person.getCodiceFiscale(); // serve alle applicazioni INDE
        }
        stringToEncode += "&utenteImpersonato=" + person.getCodiceFiscale(); // serve alle applicazioni INDE
        stringToEncode += "&idSessionLog=" + idSessionLog;
        stringToEncode += FROM;
        stringToEncode += "&modalitaAmministrativa=0";
        String encodedParams = "";
        try {
            encodedParams = URLEncoder.encode(stringToEncode, "UTF-8");
        } catch (UnsupportedEncodingException ex) {
            LOGGER.error("errore nella creazione del link", ex);
            throw new UnsupportedEncodingException("errore nell'encoding dell'url");
        }
        String assembledURL = "";
        try {
            AziendaParametriJson parametriAzienda = AziendaParametriJson.parse(this.objectMapper, azienda.getParametri());
            String targetLoginPath = parametriAzienda.getLoginPath();
            Applicazione applicazione = applicazioneRepository.getOne("babel");
            String applicationURL = applicazione.getBaseUrl() + "/" + applicazione.getIndexPage();
            assembledURL = parametriAzienda.getCrossLoginUrlTemplate().
                replace("[target-login-path]", targetLoginPath).
                replace("[entity-id]", parametriAziendaUtenteConnesso.getEntityId()).
                replace("[app]", applicationURL).
                replace("[encoded-params]", encodedParams);
        } catch (IOException ex) {
            LOGGER.error("errore nella lettura dei parametri dell'azienda target", ex);
            throw new IOException("errore nella lettura dei parametri dell'azienda target", ex);
        }
        JSONObject objAzienda = new JSONObject();
        objAzienda.put("nome", azienda.getNome());
        objAzienda.put("url", assembledURL);
        return objAzienda;
    }
    
    private List<Azienda> getAziendePerCuiFirmo(Utente utente) throws BlackBoxPermissionException{
        List<PermessoEntitaStoredProcedure> permessiDiFlusso = userInfoService.getPermessiDiFlusso(utente);
        List<Azienda> aziende = new ArrayList<>();
        if(permessiDiFlusso != null && permessiDiFlusso.size() > 0){ 
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
    public void cancellaNotifiche(HttpServletRequest request, HttpServletResponse response) throws IOException, BlackBoxPermissionException, RestControllerEngineException, AbortSaveInterceptorException, NotFoundResourceException, ClassNotFoundException{
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Utente utente = (Utente) authentication.getPrincipal();
        Persona persona = personaRepository.getOne(utente.getIdPersona().getId());
        BooleanExpression notifichePersona = QAttivita.attivita.idPersona.id.eq(persona.getId()).and(QAttivita.attivita.tipo.eq(TipoAttivita.NOTIFICA.toString()));
        Iterable<Attivita> notificheList = attivitaRepository.findAll(notifichePersona);
        for(Attivita notifica : notificheList) {
            String attivitaPath = commonUtils.resolvePlaceHolder(EntityReflectionUtils.getFirstAnnotationOverHierarchy(attivitaRepository.getClass(), NextSdrRepository.class).repositoryPath());
            restControllerEngine.delete(notifica.getId(), request, null, attivitaPath, false, null);
            //ttivitaRepository.delete(notifica);
        }
    }
    
    public enum ScrivaniaCommonParameters {
        BABEL_APPLICATION
    }
}
