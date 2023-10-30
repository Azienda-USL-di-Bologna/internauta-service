package it.bologna.ausl.internauta.service.controllers.rubrica.inad;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.bologna.ausl.internauta.service.repositories.rubrica.DettaglioContattoRepository;
import it.bologna.ausl.internauta.service.repositories.rubrica.EmailRepository;
import it.bologna.ausl.internauta.utils.authorizationutils.exceptions.AuthorizationUtilsException;
import it.bologna.ausl.internauta.utils.parameters.manager.ParametriAziendeReader;
import it.bologna.ausl.model.entities.rubrica.Contatto;
import it.bologna.ausl.model.entities.rubrica.DettaglioContatto;
import it.bologna.ausl.model.entities.rubrica.Email;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 *
 * @author MicheleD'Onza
 */
@Component
public class InadManager {

    private static final org.slf4j.Logger log = LoggerFactory.getLogger(InadManager.class);
    
    public static List<Email> getDomicilioDigitaleFromCF(
            Contatto contattoDaVerificare,
            DettaglioContattoRepository dettaglioContattoRepository,
            EmailRepository emailRepository) {
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
        return emailContattoDaRitornare;
    }
    
    @Autowired 
    private ParametriAziendeReader parametriAziendeReader;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    
    
    public InadExtractResponse extract(Integer idAzienda, String codiceFiscale) throws AuthorizationUtilsException{
        
        try {
            //inizio a generare il clientAssertion
            InadParameters inadParameters = InadParameters.build(idAzienda, parametriAziendeReader, objectMapper);
            String clientAssertion = inadParameters.generateClientAssertion(idAzienda);
            //inizio a generare il jwt da mandare a 
            String tokenJWT = inadParameters.getToken(clientAssertion);
            
            
            InadExtractResponse inadExtractResponse = new InadExtractResponse();
            inadExtractResponse.setCodiceFiscale(clientAssertion);
            return inadExtractResponse;
        } catch (Exception ex) {
            Logger.getLogger(InadManager.class.getName()).log(Level.SEVERE, null, ex);
        }
    return null;
    }
    
   
    
    
    
}
