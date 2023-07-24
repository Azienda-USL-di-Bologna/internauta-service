package it.bologna.ausl.internauta.service.downloader.utils;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.bologna.ausl.internauta.service.configuration.utils.HttpClientManager;
import it.bologna.ausl.internauta.service.exceptions.DownloaderUtilsException;
import it.bologna.ausl.internauta.utils.authorizationutils.DownloaderTokenCreator;
import it.bologna.ausl.internauta.utils.authorizationutils.exceptions.AuthorizationUtilsException;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.PostConstruct;
import okhttp3.Call;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.apache.tomcat.util.http.fileupload.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import org.springframework.stereotype.Component;

/**
 * Fornisce gli strumenti per l'upload dei file tramite la funzione upload del Downloader
 * @author gdm & gr
 */
@Component
public class DownloaderUtils {
    private static Logger logger = LoggerFactory.getLogger(DownloaderUtils.class);
    
    @Value("${internauta.mode:test}")
    private String internautaMode;
    
    // certificato con la chiave pubblica corrispondente alla chiave privata per la firma del token (prod)
    @Value("${internauta.downloader.public-cert-babel-prod.location}")
    private String downloaderPublicCertBabelProdLocation;

    // certificato con la chiave pubblica corrispondente alla chiave privata per la firma del token (test)
    @Value("${internauta.downloader.public-cert-babel-test.location}")
    private String downloaderPublicCertBabelTestLocation;
    
    // nell'inizializzazione viene settato con il certificato di test o di prod a seconda del mode
    private File downloaderPublicCertBabel;
    
    // chiave pubblica per la cifratura del token (prod)
    @Value("${internauta.downloader.encrypt-token-public-key-prod.location}")
    private String downloaderEncryptionPublicKeyProdLocation;
    
    // chiave pubblica per la cifratura del token (test)
    @Value("${internauta.downloader.encrypt-token-public-key-test.location}")
    private String downloaderEncryptionPublicKeyTestLocation;
    
    // nell'inizializzazione viene settato con la chiave di test o di prod a seconda del mode
    private File downloaderEncryptionPublicKey;
    
    // chiave privata per la firma del token
    @Value("${internauta.downloader.sign-token-private-key-file.location}")
    private String signTokenPrivateKeyFileLocation;
    
    // alias della chiave all'interno del p12
    @Value("${internauta.downloader.sign-token-private-key-file.key-alias}")
    private String signTokenPrivateKeyAlias;
    
    // password del p12 e della chiave (sono uguali)
    @Value("${internauta.downloader.sign-token-private-key-file.password}")
    private String signTokenPrivateKeyPassword;
    
    // durata del token
    @Value("${internauta.downloader.token-expire-seconds:60}")
    private Integer tokenExpireSeconds;
      
    @Autowired
    private ObjectMapper objectMapper;
    
    @Autowired
    private HttpClientManager httpClientManager;
     
    public static enum DonwloaderTarget {
        MinIO, Default
    }
    
    public Integer getTokenExpireSeconds() {
        return tokenExpireSeconds;
    }
    
    /**
     * in fase di avvio dell'applicazione setta la chiave e il certificato corretti a seconda che siamo in test o in prod (basandosi sul parametro internautaMode)
     * @throws DownloaderUtilsException 
     */
    @PostConstruct
    public void initialize() throws DownloaderUtilsException {
         switch (internautaMode.toLowerCase()) {
            case "test": // se sono in modalità di test prendo il certificato con la chiave pubblica di test e la chiave per cifrare il token di test
                this.downloaderPublicCertBabel = new File(this.downloaderPublicCertBabelTestLocation);
                this.downloaderEncryptionPublicKey = new File(this.downloaderEncryptionPublicKeyTestLocation);
                break;
            case "prod": // se sono in modalità di test prendo il certificato con la chiave pubblica di prod e la chiave per cifrare il token di prod
                this.downloaderPublicCertBabel = new File(this.downloaderPublicCertBabelProdLocation);
                this.downloaderEncryptionPublicKey = new File(this.downloaderEncryptionPublicKeyProdLocation);
                break;
            default:
                String errorMessage = String.format("internauta mode deve essere \"%s\" o \"%s\". Valore trovato \"%s\"", "test", "prod", internautaMode);
                logger.error(errorMessage);
                throw new DownloaderUtilsException(errorMessage);
        }
    }
    
    public String uploadToUploader(InputStream file, String filename, String mimeType, Boolean forceDownload, String downloadUrl, String uploadUrl) throws DownloaderUtilsException {
        return uploadToUploader(file, filename, mimeType, forceDownload, downloadUrl, uploadUrl, null);
    }
    
    /**
     * carica il file passato sul nostro repository servendosi della funzione upload del Downloader e ne torna l'url per poterlo scaricare attraverso il downloader
     * @param file il file da inviare
     * @param filename il nome che il file dovrà avere sul repository
     * @param mimeType il mimeType del file
     * @param forceDownload se "true" l'url tornato forzerà il download
     * @param downloadUrl l'url da usare per generare queello di download
     * @param uploadUrl l'url da usare per generare queello di upload
     * @param downloadTokenExpireSeconds il tempo in secondi per la scadenza del token per scaricare il download
     * @return l'url per poter scaricare il file attraverso la funzione download del Downloader
     * @throws DownloaderUtilsException 
     */
    public String uploadToUploader(InputStream file, String filename, String mimeType, Boolean forceDownload, String downloadUrl, String uploadUrl, Integer downloadTokenExpireSeconds) throws DownloaderUtilsException {
        String res;
        String token;
        
        // per prima cosa creo il token da inserire per la chiamata alla funzione upload del Downloader
        try {
            token = buildToken(getUploaderPluginParams(filename, null));
        } catch (Exception ex) {
            String errorMessage = "errore nella creazione del token per l'upload";
            logger.error(errorMessage, ex);
            throw new DownloaderUtilsException(errorMessage, ex);
        }

        File tmpFileToUpload = null;
        try {
            // creo un file temporaneo dallo stream passato. Lo cancello poi alla fine (nel finally)
            tmpFileToUpload = File.createTempFile(getClass().getSimpleName() + "to_uploader_", ".tmp");
            try (FileOutputStream fos = new FileOutputStream(tmpFileToUpload)) {
                IOUtils.copy(file, fos);
            } catch (Exception ex) {
                String errorMessage = "errore nella creazione del file temporaneo per l'upload";
                logger.error(errorMessage, ex);
                throw new DownloaderUtilsException(errorMessage, ex);
            }
            
            // creo la richiesta multipart mettendo il token nei query-params
            RequestBody dataBody = RequestBody.create(okhttp3.MultipartBody.FORM, tmpFileToUpload);
            MultipartBody multipartBody = new MultipartBody.Builder()
                .addPart(MultipartBody.Part.createFormData("file", filename, dataBody))
                .build();
            Request uploadRequest = new Request.Builder()
                .url(String.format("%s?token=%s", uploadUrl, token))
                .post(multipartBody)
                .build();

            // eseguo la chiamata all'upload
            OkHttpClient httpClient = httpClientManager.getHttpClient();
            Call call = httpClient.newCall(uploadRequest);
            Response response = call.execute();

            ResponseBody content = response.body();
            if (!response.isSuccessful()) {
                if (content != null) {
                    throw new DownloaderUtilsException(String.format("errore nella chiamata all'URL: %s RESPONSE: %s", uploadUrl, content.string()));
                } else {
                    throw new DownloaderUtilsException(String.format("errore nella chiamata all'URL: %s RESPONSE: null", uploadUrl));
                }
            } else { // tutto ok
                if (content != null) {
                    // se tutto ok creo l'url per il download e lo torno
                    Map<String, Object> downloadParams = objectMapper.readValue(content.byteStream(), new TypeReference<Map<String, Object>>(){});
                    res = buildDownloadUrl(filename, mimeType, downloadParams, forceDownload, downloadUrl, downloadTokenExpireSeconds);
                }
                else {
                    throw new DownloaderUtilsException(String.format("l'upload non ha tornato risultato", uploadUrl));
                }
            }
            return res;
        }
        catch (Exception ex) {
            String errorMessage = "errore nella creazione del token per l'upload";
            logger.error(errorMessage, ex);
            throw new DownloaderUtilsException(errorMessage, ex);
        } finally { // elimina sempre il file temporaneo creato e chiude lo stream del file passato in input
            IOUtils.closeQuietly(file);
            if (tmpFileToUpload != null && tmpFileToUpload.exists()) {
                tmpFileToUpload.delete();
            }
        }
    }
    
    public String buildToken(Map<String,Object> context) throws IOException, AuthorizationUtilsException, NoSuchAlgorithmException, InvalidKeySpecException {
        return buildToken(context, this.tokenExpireSeconds);
    }
    
    /**
     * Costruisce il token JWE per la chiamata al Downloader, servendosi della classe DownloaderTokenCreator fornita dal modulo authorization-utils
     * @param context il context da inserire nel token
     * @return
     * @throws IOException
     * @throws AuthorizationUtilsException
     * @throws NoSuchAlgorithmException
     * @throws InvalidKeySpecException 
     */
    private String buildToken(Map<String,Object> context, Integer tokenExpireSeconds) throws IOException, AuthorizationUtilsException, NoSuchAlgorithmException, InvalidKeySpecException {
        DownloaderTokenCreator downloaderTokenCreator = new DownloaderTokenCreator();
        PrivateKey signTokenPrivateKey = downloaderTokenCreator.getSignTokenPrivateKey(signTokenPrivateKeyFileLocation, signTokenPrivateKeyAlias, signTokenPrivateKeyPassword);
        RSAPublicKey encryptionPublicKey = downloaderTokenCreator.getEncryptionPublicKey(this.downloaderEncryptionPublicKey);
        return downloaderTokenCreator.getToken(context, this.downloaderPublicCertBabel, signTokenPrivateKey, encryptionPublicKey, tokenExpireSeconds, "uploader-internauta");
    }
    
    /**
     * Crea l'url per scaricare il file usando i params passati.
     * Da usare con i params tornati dalla chiamata all'upload
     * @param fileName
     * @param mimeType
     * @param downloadParams il risultato dell'uploader
     * @param forceDownload
     * @return
     * @throws DownloaderUtilsException
     * @throws IOException
     * @throws AuthorizationUtilsException
     * @throws NoSuchAlgorithmException
     * @throws InvalidKeySpecException 
     */
    public String buildDownloadUrl(String fileName, String mimeType, Map<String, Object> downloadParams, Boolean forceDownload, String downloadUrl, Integer downloadTokenExpireSeconds) 
            throws DownloaderUtilsException, IOException, AuthorizationUtilsException, NoSuchAlgorithmException, InvalidKeySpecException {
        Map<String, Object> downloaderContext = getDownloaderPluginParams(downloadParams, fileName, mimeType);
        String token;
        if (downloadTokenExpireSeconds != null){
            token = buildToken(downloaderContext, downloadTokenExpireSeconds);
        }else {
            token = buildToken(downloaderContext);
        }
        return String.format("%s?token=%s&forceDownload=%s", downloadUrl, token, forceDownload);
    }
    
    /**
     * Costruisce i paramsda inserire nel token per la chiamata al download
     * @param fileName il nome che si vuole far avere al file sul repository
     * @param metadata eventuali metadati da attribuire al file sul repository. Si può passare null
     * @return 
     */
    public Map<String, Object> getUploaderPluginParams(String fileName, Map<String, Object> metadata) {        
        Map<String, Object> uploaderParams = new HashMap();
        uploaderParams.put("target", DonwloaderTarget.Default);
        uploaderParams.put("fileName", fileName);
        uploaderParams.put("codiceAzienda", "uploader");
        if (metadata != null) {
            Map<String, Object> minIOParams = new HashMap();
            minIOParams.put("metadata", metadata);
            uploaderParams.put("params", minIOParams);
        }
        return uploaderParams;
    }  
    
    /**
     * Construisce i params da inserire nel token per il download
     * @param params i parametri per il plugin di scaricamento (sono quelli tornato dalla chiamata all'upload)
     * @param fileName il nome che avrà il file scaricato
     * @param mimeType il mimeType del file da scaricare
     * @return 
     */
    public Map<String, Object> getDownloaderPluginParams(Map<String, Object> params, String fileName, String mimeType) {        
        Map<String, Object> downloaderParams = new HashMap();
        downloaderParams.put("params", params);
        downloaderParams.put("fileName", fileName);
        downloaderParams.put("mimeType", mimeType);
        downloaderParams.put("source", DonwloaderTarget.Default);
        downloaderParams.put("codiceAzienda", "uploader");
        return downloaderParams;
    }  
    
}
