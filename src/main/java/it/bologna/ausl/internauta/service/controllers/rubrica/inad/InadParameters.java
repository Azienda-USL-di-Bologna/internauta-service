package it.bologna.ausl.internauta.service.controllers.rubrica.inad;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.JsonProcessingException;
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
import it.bologna.ausl.model.entities.configurazione.Applicazione;
import it.bologna.ausl.model.entities.configurazione.ParametroAziende;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.security.KeyFactory;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
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
    private JWSAlgorithm alg;
    private JOSEObjectType typ;
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

    public JWSAlgorithm getAlg() {
        return alg;
    }

    public void setAlg(JWSAlgorithm alg) {
        this.alg = alg;
    }

    public JOSEObjectType getTyp() {
        return typ;
    }

    public void setTyp(JOSEObjectType typ) {
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
//            if (idAzienda ==3){
//            inadParameters.setKeyPath("authorizations-utils/client-test-keypair.rsa.pk8");
//            }
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
    public String generateClientAssertion(Integer idAzienda) throws AuthorizationUtilsException, KeyStoreException, UnrecoverableKeyException, IOException, FileNotFoundException, CertificateException, NoSuchAlgorithmException, InvalidKeySpecException, InvalidKeySpecException {
        String clientAssertion = "";
        try {
            PrivateKey privateKeyFromPath = getPrivateKeyFromPath(this.getKeyPath());
            clientAssertion = makeJWT(privateKeyFromPath, idAzienda);
            return clientAssertion;
        } catch (JOSEException ex) {
            Logger.getLogger(InadParameters.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
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

    private String makeJWT(PrivateKey privateKey,Integer idAzienda) throws JOSEException {

        JWSAlgorithm alg = JWSAlgorithm.RS256;
        JOSEObjectType typ = JOSEObjectType.JWT;

        UUID jti = UUID.randomUUID(); // Sostituisci con il tuo jti
        Instant issued = Instant.now(); // Tempo di emissione (timestamp in formato Unix)
        Instant expire_in = issued.plus(this.getDelta(), ChronoUnit.MINUTES);

        JWTClaimsSet.Builder claimBuild = new JWTClaimsSet.Builder()
                .issuer(this.getIssuer())
                .subject(this.getSubject())
                .audience(this.getAudience())
                .jwtID(jti.toString())
                .issueTime(new Date(issued.toEpochMilli()))
                .expirationTime(new Date(expire_in.toEpochMilli()));
                
        
        if (idAzienda != 3){
        claimBuild.claim("purposeId", this.getPurposeId());
        }
        
        JWTClaimsSet claimsSet = claimBuild.build();

        // Crea un oggetto JWSHeader con gli header personalizzati
        JWSHeader header = new JWSHeader.Builder(alg)
                .keyID(this.getKid())
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

    @JsonIgnore
    String getToken(String clientAssertion) {
//        headersJWT = {'Content-Type': 'application/x-www-form-urlencoded'}       
//        payloadJWT = {'client_id': issuer,
//                  'client_assertion': client_assertion,
//                  'client_assertion_type': 'urn:ietf:params:oauth:client-assertion-type:jwt-bearer',
//                  'grant_type': 'client_credentials'
//                  }
//       jwt_response = requests.post(url=url_voucher, data=payloadJWT, headers=headersJWT)
        OkHttpClient client = new OkHttpClient();

        // Costruisco il body della post
        RequestBody requestBody = new FormBody.Builder()
                .add("client_id", issuer)
                .add("client_assertion", clientAssertion)
                .add("client_assertion_type", "urn:ietf:params:oauth:client-assertion-type:jwt-bearer")
                .add("grant_type", "client_credentials")
                .build();

        Request request = new Request.Builder()
                .url(this.getUrlVoucher())
                .post(requestBody)
                .addHeader("Content-Type", "application/x-www-form-urlencoded")
                .build();
        String token = "";
        try (Response response = client.newCall(request).execute()) {
            if (response.isSuccessful()) {
                String responseBody = response.body().string();
                System.out.println("Response: " + responseBody);
            } else {
                // Gestione degli errori qui
                System.err.println("Errore: " + response.code() + " - " + response.message());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return token;
    }

}
