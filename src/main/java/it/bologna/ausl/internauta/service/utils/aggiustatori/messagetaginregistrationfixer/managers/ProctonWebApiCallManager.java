/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.bologna.ausl.internauta.service.utils.aggiustatori.messagetaginregistrationfixer.managers;

import static it.bologna.ausl.middelmine.builders.RequestBodyBuilder.JSON;
import it.bologna.ausl.model.entities.baborg.Azienda;
import java.io.IOException;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.apache.http.client.HttpResponseException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 *
 * @author Salo
 */
@Component
public class ProctonWebApiCallManager {

    private static final Logger log = LoggerFactory.getLogger(ProctonWebApiCallManager.class);

    @Value("${babelsuite.webapi.procton.getdatiprotocollazionedocumento.url}")
    String getDatiProtocollazioneDocumentoWebApiUrl;

    private final String BASE_PATH_KEY = "basePath";
    private final String BABEL_SUITE_API_URL = "babelSuiteWebApiUrl";

    private final String DOCUMENT_DATA_URL_PARAM_KEY = "documentData";

    private final String NUMERO_PROPOSTA_URL_PARAM_KEY = "numeroProposta";

    private final String X_HTTP_METHOD_OVERRIDE = "X-Http-Method-Override";

    public JSONObject getDatiProtocollazioneDocumento(Azienda azienda, String numeroProposta) throws IOException {
        String basePath = new JSONObject(azienda.getParametri()).getString(BABEL_SUITE_API_URL);
        HashMap headers = new HashMap();
        headers.put(X_HTTP_METHOD_OVERRIDE, "getRegisteringDocumentData");
        headers.put("content-type", "application/json");

        JSONObject numeroPropostaJsonParam = new JSONObject();
        numeroPropostaJsonParam.put(NUMERO_PROPOSTA_URL_PARAM_KEY, numeroProposta);
        JSONObject urlParams = new JSONObject();
        urlParams.put(DOCUMENT_DATA_URL_PARAM_KEY, numeroPropostaJsonParam.toString());
        String url = basePath + getDatiProtocollazioneDocumentoWebApiUrl;
        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(60, TimeUnit.SECONDS) // connection timeout
                .writeTimeout(60, TimeUnit.SECONDS) // (probabilmente non serve, ma mettiamolo lo stesso)
                .readTimeout(60, TimeUnit.SECONDS) // socket timeout
                .build();
        RequestBody body = RequestBody.create(JSON, urlParams.toString().getBytes("UTF-8"));
        Request request = new Request.Builder()
                .url(url)
                .addHeader(X_HTTP_METHOD_OVERRIDE, "getRegisteringDocumentData")
                .post(body)
                .build();

        JSONObject documentData = new JSONObject();
        String resBody = null;
        Response response = client.newCall(request).execute();
        if (response.isSuccessful()) {
            resBody = response.body().string();
            log.info("Response body ->\n" + resBody);
            JSONObject resBodyJsonObject = new JSONObject(resBody);
            documentData = new JSONObject((String) resBodyJsonObject.get("Message"));
        } else {
            throw new HttpResponseException(response.code(),
                    "Errore nella ricerca del documento: " + response.message());
        }
        return documentData;
    }

}
