/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.bologna.ausl.internauta.service.externalcommunications;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import javax.xml.ws.http.HTTPException;
import okhttp3.Call;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 *
 * @author Salo
 */
public class GenericHttpCallManager {

    public static HttpUrl buildHttpUrl(String url, Map<String, String> queryParameters) {
        HttpUrl.Builder urlBuilder = HttpUrl.parse(url).newBuilder();
        if (queryParameters != null) {
            for (Map.Entry<String, String> entry : queryParameters.entrySet()) {
                String key = entry.getKey();
                String val = entry.getValue();
                urlBuilder = urlBuilder.addQueryParameter(key, val);
            }
        }
        return urlBuilder.build();
    }

    /**
     * Ritorna una Request creata sulla base dell'url, una request body e di una
     * mappa<String,Object> di headers.
     *
     * @param url String dell'url da chiamare
     * @param requestBody RequestBody compilato
     * @param headers HashMap<String,Object> di header
     * @return Request
     */
    public static Request getPostRequestWithHeaders(String url, RequestBody requestBody, HashMap<String, Object> headers) {
        Request.Builder post = new Request.Builder().url(url).post(requestBody);
        if (headers != null) {
            headers.entrySet().forEach((entry) -> {
                post.addHeader(entry.getKey(), (String) entry.getValue());
            });
        }
        return post.build();
    }

    /**
     * Ritorna una Request creata sulla base dell'url e di una
     * mappa<String,Object> di headers.
     *
     * @param url String dell'url da chiamare
     * @param headers HashMap<String,Object> di header
     * @return
     */
    public static Request getGetRequest(String url, HashMap<String, Object> headers) {
        Request.Builder get = new Request.Builder().url(url).get();
        headers.entrySet().forEach((entry) -> {
            get.addHeader(entry.getKey(), (String) entry.getValue());
        });
        return get.build();
    }

    /**
     * Esetue la chiamata http in base alla Request passata come parametro
     *
     * @param request
     * @return Response
     * @throws IOException
     * @throws HTTPException
     */
    public static Response requestCall(Request request) throws IOException, HTTPException {
        OkHttpClient client = new OkHttpClient();
        Call call = client.newCall(request);
        try (Response response = call.execute();) {
            return response;
        }
    }
}
