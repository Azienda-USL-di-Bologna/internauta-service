/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.bologna.ausl.internauta.service.utils.aggiustatori.messagetaginregistrationfixer.managers;

import it.bologna.ausl.internauta.service.externalcommunications.GenericHttpCallManager;
import it.bologna.ausl.internauta.service.externalcommunications.RequestBodyBuilder;
import it.bologna.ausl.model.entities.baborg.Azienda;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.HashMap;
import okhttp3.HttpUrl;
import okhttp3.Request;
import okhttp3.Response;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
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

    private final String DOCUMENT_DATA_URL_PARAM_KEY = "documentData";

    private final String NUMERO_PROPOSTA_URL_PARAM_KEY = "numeroProposta";

    private final String X_HTTP_METHOD_OVERRIDE = "X-Http-Method-Override";

    public void getDatiProtocollazioneDocumento(Azienda azienda, String numeroProposta) throws IOException {
        String basePath = new JSONObject(azienda.getParametri()).getString(BASE_PATH_KEY);
        HashMap headers = new HashMap();
        headers.put(X_HTTP_METHOD_OVERRIDE, "getRegisteringDocumentData");
        headers.put("content-type", "application/json");
        JSONObject numeroPropostaJsonParam = new JSONObject();
        numeroPropostaJsonParam.put(NUMERO_PROPOSTA_URL_PARAM_KEY, numeroProposta);
        String url = basePath + getDatiProtocollazioneDocumentoWebApiUrl;
        log.info(url);

        HashMap urlParams = new HashMap();
        urlParams.put(DOCUMENT_DATA_URL_PARAM_KEY, numeroPropostaJsonParam.toString());
        url = "http://gdml:8080/Procton/GetDatiProtocollazioneDocumento";
        HttpUrl buildedHttpUrl = GenericHttpCallManager.buildHttpUrl(url, urlParams);

        Request getRequest = GenericHttpCallManager.getGetRequest(buildedHttpUrl.toString(), headers);
        log.info(getRequest.toString());

        Response response = GenericHttpCallManager.requestCall(getRequest);
        //log.info("Response -> " + response.toString());
        try {
            InputStream byteStream = response.body().byteStream();
            StringBuilder res = new StringBuilder();
            try (Reader reader = new InputStreamReader(byteStream);) {
                int c = 0;
                while ((c = reader.read()) != -1) {
                    res.append((char) c);
                }
            }
            System.out.println("RES -> " + res.toString());
        } catch (Throwable t) {
            t.printStackTrace();
            throw t;
        }

    }

}
