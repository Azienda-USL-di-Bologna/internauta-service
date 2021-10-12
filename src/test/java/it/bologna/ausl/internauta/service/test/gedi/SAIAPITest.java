package it.bologna.ausl.internauta.service.test.gedi;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.junit.Assert;
import okhttp3.Call;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.apache.commons.io.IOUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

/**
 *
 * @author gdm
 */
@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
public class SAIAPITest {
    
    private static final Logger log = LoggerFactory.getLogger(SAIAPITest.class);
    
    public final String INTERNAUTA_BASE_URL = "http://localhost:10005";
    public final String SAI_SEND_AND_ARCHIVE_API_PATH = "internauta-api/resources/shpeck/send-and-archive-pec";
    public final String LOGIN_PATH = "internauta-api/login";
    
    private final String TO = "g.demarco@ausl.bologna.it";
    private final String CC = "giuseppe.demarco@nextsw.it";
    
    @Autowired
    public ObjectMapper objectMapper;
    
    @Test
    public void testSendMailAndArchiveAPI() throws Exception {
        String senderAddress = "babel.test1@pec.ausl.bologna.it";
        String azienda = "AUSLBO";
        //codiceFiscale = "SLMLNZ85C13A944M";
        String codiceFiscale = "SLMLNZ00C13A944M";
        String numerazioneGerarchicaDelPadre = "56/2021";
        
        String token = getToken();
        
        RequestBody requestBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("senderAddress", senderAddress)
                .addFormDataPart("to", TO)
                //            .addFormDataPart("cc", CC)
                .addFormDataPart("azienda", azienda)
                .addFormDataPart("hideRecipients", "false")
                .addFormDataPart("subject", "oggetto della mail SAI di test")
                .addFormDataPart("body", "corpo della mail SAI di test")
                .addFormDataPart("userCF", codiceFiscale)
                .addFormDataPart("fascicolo", numerazioneGerarchicaDelPadre)
                .addFormDataPart("attachments", "Allegato-test-SAI.txt", RequestBody.create(MediaType.parse("application/octet-stream"), getFileBytes()))
                .build();
        
        Request request = new Request.Builder()
                .url(INTERNAUTA_BASE_URL + "/" + SAI_SEND_AND_ARCHIVE_API_PATH)
                .addHeader("Authorization", "Bearer " + token)
                .addHeader("application", "sai")
                .post(requestBody)
                .build();
        
        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build();
        Call call = client.newCall(request);
        Response response = call.execute();
        
        Assert.assertEquals("mi aspetto un http ok (200)", 200, response.code());
        Thread.sleep(80000);
    }
    
    private String getToken() throws JsonProcessingException, IOException {
        Map<String, String> params = new HashMap();
        params.put("username", "sai");
        params.put("password", "admin");
        params.put("application", "sai");
        RequestBody requestBody = RequestBody.create(MediaType.parse("application/json"), objectMapper.writeValueAsString(params));
        Request request = new Request.Builder()
                .url(INTERNAUTA_BASE_URL + "/" + LOGIN_PATH)
                .post(requestBody)
                .build();
        
        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build();
        Call call = client.newCall(request);
        Response response = call.execute();
        Map<String, Object> res = objectMapper.readValue(response.body().string(), new TypeReference<Map<String, Object>>() {
        });
        String token = (String) res.get("token");
        System.out.println("token:" + token);
        return token;
    }
    
    private byte[] getFileBytes() throws IOException {
        ClassLoader classloader = Thread.currentThread().getContextClassLoader();
        InputStream is = classloader.getResourceAsStream("test/sai/Allegato-test-SAI.txt");
        return IOUtils.toByteArray(is);
    }
    
}
