/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.bologna.ausl.internauta.service.utils.iac.datibollovirtuale;

import it.bologna.ausl.internauta.service.utils.iac.rest.InternautaArgoCommunicatorRestCaller;
import it.nextsw.common.utils.CommonUtils;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import okhttp3.Response;
import org.json.JSONArray;
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
public class GetDatiBolloVirtualeController {

    @Autowired
    InternautaArgoCommunicatorRestCaller internautaArgoCommunicatorRestCaller;

    //
    // SOLO PER TEST IN LOCALE SUL PC
    // private String localTestUrl = "http://localhost:8000";
    private String appUrl = "/get_dati_bollo_virtuale";

    @Value("${iac.localhost.url}")
    private String iacLocalhostUrl;

    private static final Logger LOGGER = LoggerFactory.getLogger(GetDatiBolloVirtualeController.class);

    public JSONArray getDatiBolloVirtualeAziende(List<String> codiciAzienda) throws IOException {
        JSONArray data = null;
        LOGGER.info("Dentro getDatiBolloVirtualeAziende...");
        String list = String.join(",", codiciAzienda);
        LOGGER.info("Aziende: " + list);
        Map<String, String> header = new HashMap<>();
        header.put("aziende", list);

        Response res = internautaArgoCommunicatorRestCaller.
                doGetCallToInternautaArgoCommunicator(iacLocalhostUrl + appUrl, header);
        LOGGER.info(res.message());
        if (res.isSuccessful()) {
            data = new JSONArray(res.body().string());
        }
        return data;
    }

}
