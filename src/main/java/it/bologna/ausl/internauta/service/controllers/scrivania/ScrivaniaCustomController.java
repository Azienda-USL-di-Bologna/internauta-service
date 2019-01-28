package it.bologna.ausl.internauta.service.controllers.scrivania;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.jmx.snmp.ServiceName;
import it.bologna.ausl.blackbox.exceptions.BlackBoxPermissionException;
import it.bologna.ausl.blackbox.types.CategoriaPermessiStoredProcedure;
import it.bologna.ausl.blackbox.types.PermessoEntitaStoredProcedure;
import it.bologna.ausl.blackbox.types.PermessoStoredProcedure;
import it.bologna.ausl.internauta.service.authorization.TokenBasedAuthentication;
import it.bologna.ausl.internauta.service.authorization.UserInfoService;
import it.bologna.ausl.internauta.service.exceptions.ControllerHandledExceptions;
import it.bologna.ausl.internauta.service.exceptions.Http400ResponseException;
import it.bologna.ausl.internauta.service.exceptions.Http403ResponseException;
import it.bologna.ausl.internauta.service.exceptions.Http404ResponseException;
import it.bologna.ausl.internauta.service.exceptions.Http500ResponseException;
import it.bologna.ausl.internauta.service.exceptions.HttpInternautaResponseException;
import it.bologna.ausl.internauta.service.interceptors.scrivania.MenuInterceptor;
import it.bologna.ausl.internauta.service.repositories.baborg.AziendaRepository;
import it.bologna.ausl.internauta.service.repositories.baborg.PersonaRepository;
import it.bologna.ausl.internauta.service.repositories.baborg.StrutturaRepository;
import it.bologna.ausl.internauta.service.scrivania.anteprima.BabelDownloader;
import it.bologna.ausl.internauta.service.scrivania.anteprima.BabelDownloaderResponseBody;
import it.bologna.ausl.internauta.service.utils.CachedEntities;
import it.bologna.ausl.model.entities.baborg.Azienda;
import it.bologna.ausl.model.entities.baborg.AziendaParametriJson;
import it.bologna.ausl.model.entities.baborg.Persona;
import it.bologna.ausl.model.entities.baborg.Struttura;
import it.bologna.ausl.model.entities.baborg.Utente;
import it.bologna.ausl.model.entities.baborg.projections.generated.StrutturaWithIdAzienda;
import it.nextsw.common.interceptors.exceptions.AbortLoadInterceptorException;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import okhttp3.Response;
import org.apache.commons.io.IOUtils;
import org.eclipse.aether.ConfigurationProperties;
import org.jose4j.json.internal.json_simple.JSONArray;
import org.jose4j.json.internal.json_simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.projection.ProjectionFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
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
    
    private static final Logger LOGGER = LoggerFactory.getLogger(MenuInterceptor.class);
    
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
    protected PersonaRepository personaRepository;
    
    protected final ThreadLocal<TokenBasedAuthentication> threadLocalAuthentication = new ThreadLocal();
    
    private final String CMD_APRI_FIRMONE = "?CMD=open_firmone_local";
    private static final String FROM = "&from=INTERNAUTA";
    private final String HTTPS = "https://";
   
    @RequestMapping(value = {"getAnteprima"}, method = RequestMethod.GET)
    public void attivita(
        @RequestParam(required = true) String guid,
        @RequestParam(required = true) String tipologia,
        @RequestParam(required = true) Integer idAzienda,
        @RequestParam(required = true) String idApplicazione,
        @RequestParam(required = true) String fileName,
        HttpServletRequest request,
        HttpServletResponse response) throws HttpInternautaResponseException, IOException {

        JSONObject aa;
        
        
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
                        IOUtils.copy(downloadStream.body().byteStream(), out, 4096);
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
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Utente utente = (Utente) authentication.getPrincipal();
        Persona persona = personaRepository.getOne(utente.getIdPersona().getId());
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
                    String stringToEncode = CMD_APRI_FIRMONE;
                    TokenBasedAuthentication tokenBasedAuthentication = (TokenBasedAuthentication) SecurityContextHolder.getContext().getAuthentication();
                    Utente realUser = (Utente) tokenBasedAuthentication.getRealUser();
                    Persona realPerson = cachedEntities.getPersona(realUser);
                    Persona person = cachedEntities.getPersona(utente);
                    int idSessionLog = tokenBasedAuthentication.getIdSessionLog();
                    if(person.getCodiceFiscale() != null && person.getCodiceFiscale().length() > 0){
                        stringToEncode += stringToEncode.length() > 0 ? "&utente=" : "?utente=";
                        stringToEncode += person.getCodiceFiscale();
                    }
                    stringToEncode += "&utenteLogin=" + realPerson.getCodiceFiscale();
                    stringToEncode += "&utenteImpersonato=" + person.getCodiceFiscale();
                    stringToEncode += "&idSessionLog=" + idSessionLog;
                    stringToEncode += FROM;
                    stringToEncode += "&modalitaAmministrativa=0";
                    String encodedParams = "";
                    try {
                        encodedParams = URLEncoder.encode(stringToEncode, "UTF-8");
                    } catch (UnsupportedEncodingException ex) {
                        LOGGER.error("errore nella creazione del link", ex);
        //                throw new AbortLoadInterceptorException("errore nella creazione del link", ex);
                    }
                    String assembledURL = "";
                    try {
                        AziendaParametriJson parametriAzienda = AziendaParametriJson.parse(this.objectMapper, azienda.getParametri());
                        String targetLoginPath = parametriAzienda.getLoginPath();
                        String applicationURL = HTTPS + azienda.getPath()[0];
                        assembledURL = parametriAzienda.getCrossLoginUrlTemplate().
                            replace("[target-login-path]", targetLoginPath).
                            replace("[entity-id]", parametriAzienda.getEntityId()).
                            replace("[app]", applicationURL).
                            replace("[encoded-params]", encodedParams);
                    } catch (IOException ex) {
                        LOGGER.error("errore nella lettura dei parametri dell'azienda target", ex);
                    }
                    JSONObject objAzienda = new JSONObject();
                    objAzienda.put("nome", azienda.getNome());
                    objAzienda.put("url", assembledURL);
                    jsonArrayAziende.add(objAzienda);
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
    
    
}
