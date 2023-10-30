package it.bologna.ausl.internauta.service.controllers.rubrica.inad;

import it.bologna.ausl.blackbox.exceptions.BlackBoxPermissionException;
import it.bologna.ausl.internauta.service.authorization.AuthenticatedSessionData;
import it.bologna.ausl.internauta.service.authorization.AuthenticatedSessionDataBuilder;
import it.bologna.ausl.internauta.service.exceptions.http.ControllerHandledExceptions;
import it.bologna.ausl.internauta.service.repositories.rubrica.ContattoRepository;
import it.bologna.ausl.internauta.service.repositories.rubrica.DettaglioContattoRepository;
import it.bologna.ausl.internauta.service.repositories.rubrica.EmailRepository;
import it.bologna.ausl.internauta.service.utils.CachedEntities;
import it.bologna.ausl.model.entities.baborg.Azienda;
import it.bologna.ausl.model.entities.baborg.Utente;
import it.bologna.ausl.model.entities.rubrica.Contatto;
import it.bologna.ausl.model.entities.rubrica.Email;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
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
    CachedEntities cachedEntities;
    
    @Autowired
    ContattoRepository contattoRepository;
    
    @Autowired
    DettaglioContattoRepository dettaglioContattoRepository;
    
    @Autowired
    EmailRepository emailRepository;
    
    @Autowired
    private AuthenticatedSessionDataBuilder authenticatedSessionDataBuilder;
    
    //controller chiamato dal frontend al caricamento dei dettagli contatto che controlla che ci siano domini digitali per il contatto
    //se il contatto ne ha uno, lo salva e ritorna al frontend il nuovo dettaglio dominio digitale
    @RequestMapping(value = "getDomicilioDigitaleFromCF", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> getDomicilioDigitaleFromCF(
            @RequestParam("idContatto") Integer idContatto,
            HttpServletRequest request) throws BlackBoxPermissionException{
        
        AuthenticatedSessionData authenticatedUserProperties = authenticatedSessionDataBuilder.getAuthenticatedUserProperties();
        Utente utente = authenticatedUserProperties.getUser();
        Azienda azienda = cachedEntities.getAzienda(utente.getIdAzienda().getId());

        Contatto contattoDaVerificare = contattoRepository.getById(idContatto);
        String codiceFiscaleContatto = contattoDaVerificare.getCodiceFiscale();
        
        if(codiceFiscaleContatto != null && !"".equals(codiceFiscaleContatto)) {

            List<Email> emailContattoDaRitornare = InadManager.getDomicilioDigitaleFromCF(
                    contattoDaVerificare, 
                    dettaglioContattoRepository,
                    emailRepository);
            
            return new ResponseEntity(emailContattoDaRitornare,  HttpStatus.OK);

        }
 
        return new ResponseEntity(null,  HttpStatus.OK);

    }
// /verify/{codice_fiscale}
    
}
