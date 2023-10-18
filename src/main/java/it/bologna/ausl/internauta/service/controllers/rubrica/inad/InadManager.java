package it.bologna.ausl.internauta.service.controllers.rubrica.inad;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import it.bologna.ausl.internauta.utils.authorizationutils.exceptions.AuthorizationUtilsException;
import it.bologna.ausl.internauta.utils.parameters.manager.ParametriAziendeReader;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jose4j.base64url.internal.apache.commons.codec.binary.Base64;
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
            
            
            InadExtractResponse inadExtractResponse = new InadExtractResponse();
            inadExtractResponse.setCodiceFiscale(clientAssertion);
            return inadExtractResponse;
        } catch (Exception ex) {
            Logger.getLogger(InadManager.class.getName()).log(Level.SEVERE, null, ex);
        }
    return null;
    }
    
   
    
    
    
}
