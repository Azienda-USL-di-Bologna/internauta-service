
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.KeyFactory;
import java.security.KeyStore;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;

/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */

/**
 *
 * @author MicheleD'Onza
 */
public class testInad {
    
    
    
    public static void main(String[] args) throws IOException, NoSuchAlgorithmException, InvalidKeySpecException {
        String keyPath = "authorizations-utils/client-test-keypair.rsa.pk8";
        byte[] keyBytes = Files.readAllBytes(Paths.get(keyPath));
        PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(keyBytes);
        KeyFactory kf = KeyFactory.getInstance("RSA");
        
        PrivateKey generatePrivate = kf.generatePrivate(spec);
        System.out.println(generatePrivate.toString());
    }
}
