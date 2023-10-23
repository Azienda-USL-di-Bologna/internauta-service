package it.bologna.ausl.internauta.service.controllers.rubrica.inad;

import it.bologna.ausl.blackbox.exceptions.BlackBoxPermissionException;
import it.bologna.ausl.internauta.service.authorization.AuthenticatedSessionData;
import it.bologna.ausl.internauta.service.authorization.AuthenticatedSessionDataBuilder;
import it.bologna.ausl.internauta.service.authorization.UserInfoService;
import it.bologna.ausl.internauta.service.authorization.utils.UtenteProcton;
import it.bologna.ausl.internauta.service.exceptions.http.ControllerHandledExceptions;
import it.bologna.ausl.internauta.service.repositories.rubrica.ContattoRepository;
import it.bologna.ausl.internauta.service.repositories.rubrica.DettaglioContattoRepository;
import it.bologna.ausl.internauta.service.repositories.rubrica.EmailRepository;
import it.bologna.ausl.internauta.service.utils.CachedEntities;
import it.bologna.ausl.model.entities.baborg.Azienda;
import it.bologna.ausl.model.entities.baborg.Utente;
import it.bologna.ausl.model.entities.rubrica.Contatto;
import it.bologna.ausl.model.entities.rubrica.DettaglioContatto;
import it.bologna.ausl.model.entities.rubrica.Email;
import java.time.ZonedDateTime;
import java.util.ArrayList;
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
        
        if(codiceFiscaleContatto!= null && !"".equals(codiceFiscaleContatto)) {

            //chiedo a inad i contatti del codice fiscale 
//            InadExtractResponse responseObj = InadManager.extract(codiceFiscaleContatto, azienda.getId());
            

//          finchè non funziona la chiamata ad inad ne faccio una finta
            InadExtractResponse responseObj = new InadExtractResponse();
            DigitalAddress digitalAddress = new DigitalAddress();
            UsageInfo usageInfo = new UsageInfo();
            digitalAddress.setDigitalAddress("pippopiudipippobaudo@pec.it");
            digitalAddress.setPracticedProfession("POCOPROFESSIONISTAMOLTOLIBERO");
            usageInfo.setDateEndValidity(ZonedDateTime.now());
            usageInfo.setMotivation("CESSAZIONE_VOLONTARIA");
            digitalAddress.setUsageInfo(usageInfo);
            responseObj.setCodiceFiscale("LZZCHR97M43A944X");
            List<DigitalAddress> digitalAddresses2 = new ArrayList<>();
            digitalAddresses2.add(digitalAddress);
            responseObj.setDigitalAddresses(digitalAddresses2);
            
            List<DigitalAddress> digitalAddresses = responseObj.getDigitalAddresses();
            
            
            List<Email> emailContattoDaRitornare = new ArrayList<>();

            //se trovo dei domini digitali li metto dentro una lista di indirizzi che poi confronto
            //con i dettagli contatto già presenti sulla rubrica
            //se l'indirizzo esiste già, controllo che sia già segnato come contatto digitale
            //se l'indirizzo non esiste, creo il dettagli contatto giusto 
            if (!digitalAddresses.isEmpty()) {
                String indirizzoDomicilioDigitale = digitalAddresses.get(0).getDigitalAddress(); 
                
                List<DettaglioContatto> dettagliContatto = contattoDaVerificare.getDettaglioContattoList();
                //l'unico caso in cui non è da aggiungere è se lo abbiamo già
                Boolean isIndirizzoDaAggiungere = true;
                
                if (!dettagliContatto.isEmpty()) {
                    for (DettaglioContatto dc : dettagliContatto) {
                        if (indirizzoDomicilioDigitale.equals(dc.getDescrizione())) {
                            isIndirizzoDaAggiungere = false;
                            
                            //controllo che sia già un domicilio digitale, sennò lo rendo tale
                            if (!dc.getDomicilioDigitale()) {
                                dc.setDomicilioDigitale(Boolean.TRUE);
                                emailContattoDaRitornare.add(dc.getEmail());
                            }
                        } else {
                            
                            //controllo non ci sia un altro dettaglio che è un domicilio digitale,
                            //nel caso lo setto come non dominio digitale
                            if (dc.getDomicilioDigitale()) {
                                dc.setDomicilioDigitale(Boolean.FALSE);
                                emailContattoDaRitornare.add(dc.getEmail());
                            }
                        }
                    }
                }
                
                if(!emailContattoDaRitornare.isEmpty()) {
                    for(Email emailContatto : emailContattoDaRitornare) {
                        dettaglioContattoRepository.save(emailContatto.getIdDettaglioContatto());
                    }
                }
                //aggiungo il dettaglio del domicilio digitale al contatto
                if (isIndirizzoDaAggiungere) {
                    
                    Email emailDaAggiungere = new Email();
                    emailDaAggiungere.setEmail(indirizzoDomicilioDigitale);
                    emailDaAggiungere.setDescrizione(indirizzoDomicilioDigitale);
                    emailDaAggiungere.setIdContatto(contattoDaVerificare);
                    emailDaAggiungere.setPec(Boolean.TRUE);
                    emailDaAggiungere.setProvenienza("inad");
                    emailDaAggiungere.setPrincipale(Boolean.FALSE);
                    
                    
                    DettaglioContatto dettaglioDomicilioDigitale = new DettaglioContatto();
                    dettaglioDomicilioDigitale.setDescrizione(indirizzoDomicilioDigitale);
                    dettaglioDomicilioDigitale.setIdContatto(contattoDaVerificare);
                    dettaglioDomicilioDigitale.setDomicilioDigitale(Boolean.TRUE);
                    dettaglioDomicilioDigitale.setEmail(emailDaAggiungere);
                    emailDaAggiungere.setIdDettaglioContatto(dettaglioDomicilioDigitale);
                    
                    
                    emailRepository.save(emailDaAggiungere);
                    dettaglioContattoRepository.save(dettaglioDomicilioDigitale);
                    
                    emailContattoDaRitornare.add(emailDaAggiungere);
                }
                   
            } else {
                for (DettaglioContatto dc: contattoDaVerificare.getDettaglioContattoList()) {
                    if (dc.getDomicilioDigitale()) {
                        dc.setDomicilioDigitale(Boolean.FALSE);
                        dettaglioContattoRepository.save(dc);
                        emailContattoDaRitornare.add(dc.getEmail());
                    }
                }
                
            }
            
            return new ResponseEntity(emailContattoDaRitornare,  HttpStatus.OK);

        }
 
        return new ResponseEntity(null,  HttpStatus.OK);

    }
// /verify/{codice_fiscale}
    
}
