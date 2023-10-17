package it.bologna.ausl.internauta.service.controllers.rubrica.inad;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.EncryptionMethod;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWEAlgorithm;
import com.nimbusds.jose.JWEHeader;
import com.nimbusds.jose.JWEObject;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.Payload;
import com.nimbusds.jose.crypto.RSAEncrypter;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import it.bologna.ausl.internauta.utils.authorizationutils.AuthorizationUtilityFunctions;
import it.bologna.ausl.internauta.utils.authorizationutils.InadTokenManager;
import it.bologna.ausl.internauta.utils.authorizationutils.exceptions.AuthorizationUtilsException;
import it.bologna.ausl.internauta.utils.parameters.manager.ParametriAziendeReader;
import it.bologna.ausl.model.entities.configurazione.Applicazione;
import it.bologna.ausl.model.entities.configurazione.ParametroAziende;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.Key;
import java.security.KeyStoreException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.Formatter;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author MicheleD'Onza classe che si usa per caricarsi dal db o da redis i
 * parametri per le chiamate
 *
 */
public class InadParameters {

    public InadParameters() {
    }

    private String kid;
    private String issuer;
    private String subject;
    private String audience;
    private String urlVoucher;
    private String alg;
    private String typ;
    private Integer delta;
    private String urlDocomicilioDigitale;
    
    private String keyPath = "authorizations-utils/client-test-keypair.rsa.pk8";

    public String getKeyPath() {
        return keyPath;
    }

    public void setKeyPath(String keyPath) {
        this.keyPath = keyPath;
    }
    
    public String getKid() {
        return kid;
    }

    public void setKid(String kid) {
        this.kid = kid;
    }

    public String getIssuer() {
        return issuer;
    }

    public void setIssuer(String issuer) {
        this.issuer = issuer;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getAudience() {
        return audience;
    }

    public void setAudience(String audience) {
        this.audience = audience;
    }

    public String getUrlVoucher() {
        return urlVoucher;
    }

    public void setUrlVoucher(String urlVoucher) {
        this.urlVoucher = urlVoucher;
    }

    public String getAlg() {
        return alg;
    }

    public void setAlg(String alg) {
        this.alg = alg;
    }

    public String getTyp() {
        return typ;
    }

    public void setTyp(String typ) {
        this.typ = typ;
    }

    public Integer getDelta() {
        return delta;
    }

    public void setDelta(Integer delta) {
        this.delta = delta;
    }

    public String getUrlDocomicilioDigitale() {
        return urlDocomicilioDigitale;
    }

    public void setUrlDocomicilioDigitale(String urlDocomicilioDigitale) {
        this.urlDocomicilioDigitale = urlDocomicilioDigitale;
    }

    @JsonIgnore
    public static InadParameters build(Integer idAzienda, ParametriAziendeReader parametriAziendeReader, ObjectMapper objectMapper) {
        List<ParametroAziende> parameters = parametriAziendeReader.getParameters(
                "inad",
                new Integer[]{idAzienda},
                new String[]{Applicazione.Applicazioni.rubrica.toString()});
        if (parameters.size() == 1) {
            ParametroAziende parametroAziende = parameters.get(0);
            InadParameters inadParameters = objectMapper.convertValue(parametroAziende, InadParameters.class);
            return inadParameters;
        }
        return null;
    }

    /**
     *
     * Funzione che ritorna il client assertion cioè la stringa che ti consente
     * di ottenere il Voucher
     *
     * @return clientAssertion
     */
    @JsonIgnore
    public String generateClientAssertion() throws AuthorizationUtilsException, KeyStoreException, UnrecoverableKeyException, IOException, FileNotFoundException, CertificateException, NoSuchAlgorithmException, InvalidKeySpecException, InvalidKeySpecException {
        String clientAssertion = "";
        PrivateKey signTokenPrivateKey =null;
        signTokenPrivateKey = InadTokenManager.getSignTokenPrivateKey(this.getKeyPath(), "", "");
//        clientAssertion = getToken(context, publicCertFile, singTokenPrivateKey, tokenEncryptionPublickey, this.getDelta(), this.getIssuer());
        System.out.println(signTokenPrivateKey.toString());
        return clientAssertion;
    }

    
}
