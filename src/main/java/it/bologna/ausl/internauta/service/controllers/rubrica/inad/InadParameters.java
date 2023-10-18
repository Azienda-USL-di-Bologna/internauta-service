package it.bologna.ausl.internauta.service.controllers.rubrica.inad;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.EncryptionMethod;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWEAlgorithm;
import com.nimbusds.jose.JWEHeader;
import com.nimbusds.jose.JWEObject;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSSigner;
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
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.security.Key;
import java.security.KeyFactory;
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
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.Formatter;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jose4j.base64url.internal.apache.commons.codec.binary.Base64;

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
    private String purposeId;

    private String keyPath = "authorizations-utils/client-test-keypair.pk8";

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

    public String getPurposeId() {
        return purposeId;
    }

    public void setPurposeId(String purposeId) {
        this.purposeId = purposeId;
    }
    

    @JsonIgnore
    public static InadParameters build(Integer idAzienda, ParametriAziendeReader parametriAziendeReader, ObjectMapper objectMapper) throws JsonProcessingException {
        List<ParametroAziende> parameters = parametriAziendeReader.getParameters(
                "inad",
                new Integer[]{idAzienda},
                new String[]{Applicazione.Applicazioni.rubrica.toString()});
        if (parameters.size() == 1) {
            ParametroAziende parametroAziende = parameters.get(0);
            InadParameters inadParameters = objectMapper.convertValue(objectMapper.readTree(parametroAziende.getValore()), InadParameters.class);
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
        try {
            PrivateKey privateKeyFromPath = getPrivateKeyFromPath(this.getKeyPath());
            clientAssertion = makeJWT(privateKeyFromPath);
            return clientAssertion;
        } catch (JOSEException ex) {
            Logger.getLogger(InadParameters.class.getName()).log(Level.SEVERE, null, ex);
        }finally{
            return clientAssertion;
        }
    }

    @JsonIgnore
    private static PrivateKey getPrivateKeyFromPath(String path) throws IOException, NoSuchAlgorithmException, InvalidKeySpecException {
        File privateKeyFile = new File(path);
        String key = new String(Files.readAllBytes(privateKeyFile.toPath()), Charset.defaultCharset());
        String privateKeyPEM = key
                .replace("-----BEGIN PRIVATE KEY-----", "")
                .replaceAll(System.lineSeparator(), "")
                .replace("-----END PRIVATE KEY-----", "");
        byte[] encoded = Base64.decodeBase64(privateKeyPEM);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(encoded);
        return keyFactory.generatePrivate(keySpec);
    }

    private String makeJWT(PrivateKey privateKey) throws JOSEException {

        String kid = "JmyAHtpYZYryoqfzCaXnThaXZl0cdOM25AZKcR5kEvE"; // Sostituisci con il tuo kid
        kid = this.getKid();
        
        JWSAlgorithm alg = JWSAlgorithm.RS256;
        JOSEObjectType typ = JOSEObjectType.JWT;
        
        String issuer = "10570c94-074b-47cc-acc1-68daa677ce92"; // Sostituisci con il tuo issuer
        issuer = this.getIssuer();
        String subject = "10570c94-074b-47cc-acc1-68daa677ce92"; // Sostituisci con il tuo subject
        subject = this.getSubject();
        String audience = "auth.uat.interop.pagopa.it/client-assertion"; // Sostituisci con il tuo audience
        audience = this.getAudience();
        
        UUID jti = UUID.randomUUID(); // Sostituisci con il tuo jti
        Instant issued = Instant.now(); // Tempo di emissione (timestamp in formato Unix)
        Instant expire_in = issued.plus(this.getDelta(), ChronoUnit.MINUTES);
        String purposeId = "d411bc9d-fc78-4373-8148-f5a5d08d63c1"; // Sostituisci con il tuo purposeId
        purposeId = this.getPurposeId();

        JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
                .issuer(issuer)
                .subject(subject)
                .audience(audience)
                .jwtID(jti.toString())
                .issueTime(new Date(issued.toEpochMilli()))
                .expirationTime(new Date(expire_in.toEpochMilli()))
                .claim("purposeId", purposeId)
                .build();

        // Crea un oggetto JWSHeader con gli header personalizzati
        JWSHeader header = new JWSHeader.Builder(alg)
                .keyID(kid)
                .type(typ)
                .build();

        // Crea un JWT firmato
        SignedJWT signedJWT = new SignedJWT(header, claimsSet);

        // Crea un oggetto RSASSASigner con la chiave privata
        JWSSigner signer = new RSASSASigner(privateKey);

        // Esegue la firma
        signedJWT.sign(signer);

        // Serializza il JWT in una stringa
        String jwtString = signedJWT.serialize();

        return jwtString;
    }

}
