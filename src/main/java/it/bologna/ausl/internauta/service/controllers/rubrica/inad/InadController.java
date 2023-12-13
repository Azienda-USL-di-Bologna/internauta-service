package it.bologna.ausl.internauta.service.controllers.rubrica.inad;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.bologna.ausl.blackbox.exceptions.BlackBoxPermissionException;
import it.bologna.ausl.internauta.service.authorization.AuthenticatedSessionData;
import it.bologna.ausl.internauta.service.authorization.AuthenticatedSessionDataBuilder;
import it.bologna.ausl.internauta.service.exceptions.http.ControllerHandledExceptions;
import it.bologna.ausl.internauta.service.utils.CachedEntities;
import it.bologna.ausl.internauta.utils.authorizationutils.exceptions.AuthorizationUtilsException;
import it.bologna.ausl.internauta.utils.parameters.manager.ParametriAziendeReader;
import it.bologna.ausl.model.entities.baborg.Azienda;
import it.bologna.ausl.model.entities.baborg.Utente;
import it.bologna.ausl.model.entities.rubrica.Contatto;
import it.bologna.ausl.model.entities.rubrica.Email;
import it.bologna.ausl.model.entities.rubrica.projections.generated.EmailWithIdContattoAndIdDettaglioContatto;
import it.nextsw.common.projections.ProjectionsInterceptorLauncher;
import java.util.HashMap;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.projection.ProjectionFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 *
 * @author MicheleD'Onza
 * classe che si usa per le chiamate ai servizi INAD
 * 
 */
@RestController
@RequestMapping(value = "${rubrica.mapping.url.root}")
public class InadController implements ControllerHandledExceptions{
    
    private static final Logger log = LoggerFactory.getLogger(InadController.class);
    private static String ALG = "RS256";
    private static String TYP = "JWT";

    @Autowired
    private CachedEntities cachedEntities;
    
    @Autowired
    private AuthenticatedSessionDataBuilder authenticatedSessionDataBuilder;
    
    @Autowired
    private InadManager inadManager;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @Autowired
    private ParametriAziendeReader parametriAziendeReader;
    
    @Autowired
    private ProjectionFactory projectionFactory;
    
    @Autowired
    private ProjectionsInterceptorLauncher projectionsInterceptorLauncher;
    
    /**
     * Questa funzione torna sempre il domicilio digtale aggiornato; se già presente a db, lo aggiorna da INAD e lo ritorna, altrimenti lo chiede a INAD, lo salva a db e lo ritorna.
     * @param idContatto
     * @param request
     * @return
     * @throws BlackBoxPermissionException 
     * @throws it.bologna.ausl.internauta.utils.authorizationutils.exceptions.AuthorizationUtilsException 
     */
    @RequestMapping(value = "getAndSaveDomicilioDigitale", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<EmailWithIdContattoAndIdDettaglioContatto> getAndSaveDomicilioDigitale(
            @RequestParam("idContatto") Integer idContatto,
            HttpServletRequest request) throws BlackBoxPermissionException, AuthorizationUtilsException, InadException{
        projectionsInterceptorLauncher.setRequestParams(null, request);
        Email domicilioDigitale = inadManager.getAlwaysAndSaveDomicilioDigitale(idContatto);
        if (domicilioDigitale!= null) {
            return new ResponseEntity(projectionFactory.createProjection(EmailWithIdContattoAndIdDettaglioContatto.class, domicilioDigitale),  HttpStatus.OK);
        } else {
            return null;
        }
    }
    
    /**
     * Questa funzione torna sempre i domicili digtali aggiornato di una lista di contatti; se già presenti a db, li aggiorna da INAD e lo ritorna, altrimenti li chiede a INAD, li salva a db e li ritorna.
     * @param idContattiList
     * @param request
     * @return
     * @throws BlackBoxPermissionException 
     * @throws it.bologna.ausl.internauta.utils.authorizationutils.exceptions.AuthorizationUtilsException 
     */
    @RequestMapping(value = "getAndSaveMultiDomicilioDigitale", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<HashMap<Integer, Email>> getAndSaveMultiDomicilioDigitale(
            @RequestBody List<Integer> idContattiList,
            HttpServletRequest request) throws BlackBoxPermissionException, AuthorizationUtilsException, InadException{
        projectionsInterceptorLauncher.setRequestParams(null, request);
        HashMap<Integer, Email> domiciliDigitaliMap = inadManager.getAndSaveDomicilioDigitaleMultiConctats(idContattiList);
        if (domiciliDigitaliMap!= null && !domiciliDigitaliMap.isEmpty()) {
            return new ResponseEntity(domiciliDigitaliMap,  HttpStatus.OK);
        } else {
            return null;
        }
    }
    
    
    //controller chiamato dal frontend al caricamento dei dettagli contatto che controlla che ci siano domini digitali per il contatto
    //se il contatto ne ha uno, lo salva e ritorna al frontend il nuovo dettaglio dominio digitale
    @RequestMapping(value = "getDomicilioDigitaleFromCF", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> getDomicilioDigitaleFromCF(
            @RequestParam("idContatto") Integer idContatto,
            HttpServletRequest request) throws BlackBoxPermissionException, AuthorizationUtilsException, InadException{
        
        AuthenticatedSessionData authenticatedUserProperties = authenticatedSessionDataBuilder.getAuthenticatedUserProperties();
        Utente utente = authenticatedUserProperties.getUser();
        Azienda azienda = cachedEntities.getAzienda(utente.getIdAzienda().getId());
        
        List<Email> domiciliDigitali = inadManager.getAndSaveEmailDomicilioDigitale(idContatto, azienda);
        
        return new ResponseEntity(domiciliDigitali,  HttpStatus.OK);

    }
    
// /verify/{codice_fiscale}
    @RequestMapping(value = "extract", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public InadExtractResponse extract(
        @RequestParam("cf") String cf,
        @RequestParam("idAzienda") Integer idAzienda) throws AuthorizationUtilsException, JsonProcessingException{
        InadParameters inadParameters = InadParameters.buildParameters(idAzienda, parametriAziendeReader, objectMapper);
        return inadManager.extract(idAzienda, cf, inadParameters);
    }
    
    
    
   
}
