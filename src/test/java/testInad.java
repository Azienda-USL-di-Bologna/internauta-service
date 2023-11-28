
import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

import java.io.File;
import java.io.IOException;
import java.security.PrivateKey;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.UUID;
import org.jose4j.base64url.internal.apache.commons.codec.binary.Base64;

/**
 *
 * @author MicheleD'Onza
 */
public class testInad {

    public static void main(String[] args) throws Exception {
        //apro la chiave 
        PrivateKey privateKey = getPrivateKeyFromPath("authorizations-utils/client-test-keypair.pk8");
        System.out.println(privateKey.getAlgorithm());
        //creo il jsw
        String jws = makeJWT(privateKey);
        System.out.println(jws);
    }

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

    private static String makeJWT(PrivateKey privateKey) throws JOSEException {
        
        String kid = "JmyAHtpYZYryoqfzCaXnThaXZl0cdOM25AZKcR5kEvE"; // Sostituisci con il tuo kid
        JWSAlgorithm alg = JWSAlgorithm.RS256;
        JOSEObjectType typ = JOSEObjectType.JWT;
        String issuer = "10570c94-074b-47cc-acc1-68daa677ce92"; // Sostituisci con il tuo issuer
        String subject = "10570c94-074b-47cc-acc1-68daa677ce92"; // Sostituisci con il tuo subject
        String audience = "auth.uat.interop.pagopa.it/client-assertion"; // Sostituisci con il tuo audience
        UUID jti = UUID.randomUUID(); // Sostituisci con il tuo jti
        Instant issued = Instant.now(); // Tempo di emissione (timestamp in formato Unix)
        Instant expire_in = issued.plus(43200, ChronoUnit.MINUTES); // Scadenza in un'ora (timestamp in formato Unix)
        String purposeId = "d411bc9d-fc78-4373-8148-f5a5d08d63c1"; // Sostituisci con il tuo purposeId
        
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
