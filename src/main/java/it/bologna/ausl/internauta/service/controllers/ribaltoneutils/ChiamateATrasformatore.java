package it.bologna.ausl.internauta.service.controllers.ribaltoneutils;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.bologna.ausl.internauta.service.utils.CachedEntities;
import it.bologna.ausl.internauta.utils.parameters.manager.ParametriAziendeReader;
import it.bologna.ausl.model.entities.baborg.Azienda;
import it.bologna.ausl.model.entities.configurazione.Applicazione;
import it.bologna.ausl.model.entities.configurazione.ParametroAziende;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import okhttp3.Call;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 *
 * @author Mido
 */
@Component
public class ChiamateATrasformatore {

    private static final Logger log = LoggerFactory.getLogger(ChiamateATrasformatore.class);

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CachedEntities cachedEntities;

    @Autowired
    private ParametriAziendeReader parametriAziende;

    public void lanciaTrasformatore(
            Integer idAzienda,
            Boolean ribaltaArgo,
            Boolean ribaltaInternauta,
            String email,
            String fonteRibaltone,
            Boolean trasforma,
            Integer IdUtente,
            String note) throws Throwable {
        String url = "";
        Azienda azienda = null;
        try {
            azienda = cachedEntities.getAzienda(idAzienda);
            List<ParametroAziende> parameters = parametriAziende.getParameters("urlTrasformatore", new Integer[]{azienda.getId()}, new String[]{Applicazione.Applicazioni.trasformatore.toString()});
            if (parameters != null && !parameters.isEmpty()) {
                url = parametriAziende.getValue(parameters.get(0), new TypeReference<String>() {
                });

            }
            Map<String, Object> hm = new HashMap();
            hm.put("app", "avec");
            hm.put("codice_ente", azienda.getCodice());
            hm.put("ribalta_argo", ribaltaArgo);
            hm.put("ribalta_internauta", ribaltaInternauta);
            hm.put("email", email);
            hm.put("fonte_ribaltone", fonteRibaltone);
            hm.put("id_azienda", azienda.getId());
            hm.put("trasforma", trasforma);
            hm.put("id_utente", IdUtente);
            hm.put("note", note);

            okhttp3.RequestBody requestBody = okhttp3.RequestBody.create(
                    okhttp3.MediaType.get("application/json; charset=utf-8"),
                    objectMapper.writeValueAsString(hm));
            OkHttpClient client = new OkHttpClient.Builder()
                    .connectTimeout(300, TimeUnit.SECONDS) // connection timeout
                    .writeTimeout(300, TimeUnit.SECONDS) // (probabilmente non serve, ma mettiamolo lo stesso)
                    .readTimeout(300, TimeUnit.SECONDS) // socket timeout
                    .build();

            Request request = new Request.Builder()
                    .url(url)
                    .post(requestBody)
                    .build();

            Call call = client.newCall(request);
            HashMap readValue = null;
            try (Response response = call.execute();) {
                int responseCode = response.code();
                if (response.isSuccessful()) {
                    readValue = objectMapper.readValue(response.body().string(), HashMap.class);
                    log.info("Chiamata a webapi Trasformatore effettuata con successo");
                } else {
                    log.error("Errore nella chiamata al Trasformatore: " + responseCode + " " + response.message());
                    throw new Throwable("Errore nella chiamata al Trasformatore: " + responseCode + " " + response.message());
                }
            }

        } catch (Throwable t) {
            log.info("urlTrasformatore: " + url);
            log.info("Codice azienda" + ((azienda != null) ? azienda.getCodice() : "") );
            log.error("Errore nella chiamata al Trasformatore: ", t);
            throw new Throwable("Errore nella chiamata al Trasformatore: ", t);
        }
    }

}
