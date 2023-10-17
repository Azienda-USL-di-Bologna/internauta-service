package it.bologna.ausl.internauta.service.controllers.rubrica.inad;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.bologna.ausl.internauta.utils.authorizationutils.InadTokenManager;
import it.bologna.ausl.internauta.utils.authorizationutils.exceptions.AuthorizationUtilsException;
import it.bologna.ausl.internauta.utils.parameters.manager.ParametriAziendeReader;
import java.io.IOException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 *
 * @author MicheleD'Onza
 */
@Component
public class InadManager {
    
    @Autowired 
    private ParametriAziendeReader parametriAziendeReader;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    
    
    public InadExtractResponse extract(Integer idAzienda, String codiceFiscale) throws AuthorizationUtilsException{
        
        try {
            InadParameters inadParameters = InadParameters.build(idAzienda, parametriAziendeReader, objectMapper);
            
            String clientAssertion = inadParameters.generateClientAssertion();
            
            
            
            return null;
        } catch (Exception ex) {
            Logger.getLogger(InadManager.class.getName()).log(Level.SEVERE, null, ex);
        }
    return null;
    }
    
    
}
