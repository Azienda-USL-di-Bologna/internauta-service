/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.bologna.ausl.internauta.service.utils.iac.rest;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import okhttp3.Call;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 *
 * @author Salo
 */
@Component
public class InternautaArgoCommunicatorRestCaller {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(InternautaArgoCommunicatorRestCaller.class);
    
    public Response doGetCallToInternautaArgoCommunicator(String appUrl, Map<String, String> headers) throws IOException {
        LOGGER.info("doGetCallToInternautaArgoCommunicator: url " + appUrl);
        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(60, TimeUnit.SECONDS) // connection timeout
                .writeTimeout(60, TimeUnit.SECONDS) // (probabilmente non serve, ma mettiamolo lo stesso)
                .readTimeout(60, TimeUnit.SECONDS) // socket timeout
                .build();
        Request.Builder getBuilder = new Request.Builder()
                .url(appUrl)
                .get();
        
        if (headers != null) {
            for (Map.Entry<String, String> header : headers.entrySet()) {
                String key = header.getKey();
                String val = header.getValue();
                getBuilder = getBuilder.header(key, val);
            }
        }
        getBuilder.addHeader("Content-Type", "application/json");
        
        Request request = getBuilder.build();
        Response res = client.newCall(request).execute();
        if (res.isSuccessful()) {
            LOGGER.info("Chiamata avvenuta con successo");
        } else {
            LOGGER.error("Chiamata fallita");
        }
        return res;
    }
}
