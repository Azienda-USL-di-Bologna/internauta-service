package it.bologna.ausl.internauta.service.scrivania.anteprima;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.bologna.ausl.blackbox.exceptions.BlackBoxPermissionException;
import it.bologna.ausl.internauta.service.authorization.TokenBasedAuthentication;
import it.bologna.ausl.internauta.service.utils.CachedEntities;
import it.bologna.ausl.model.entities.baborg.Azienda;
import it.bologna.ausl.model.entities.baborg.AziendaParametriJson;
import it.bologna.ausl.model.entities.baborg.Persona;
import it.bologna.ausl.model.entities.baborg.Utente;
import it.bologna.ausl.model.entities.configuration.Applicazione;
import java.io.IOException;
import okhttp3.Call;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.keyvalue.annotation.KeySpace;
import org.springframework.data.redis.core.RedisHash;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 *
 * @author gdm
 */
@Component
public class BabelDownloader {

    @Autowired
    private ObjectMapper objectMapper;
    
    @Autowired
    private CachedEntities cachedEntities;
    
    @Value("${babelsuite.webapi.babeldownloader.url}")
    private String babelDownloaderUrl;

    @Value("${babelsuite.webapi.babeldownloader.downloadMetdod}")
    private String babelDownloaderDownloadMethod;
    
    AziendaParametriJson parametriAzienda;
    
    public BabelDownloader() {
    }
    
    private Utente getLoggedUser() {
        TokenBasedAuthentication authentication = (TokenBasedAuthentication) SecurityContextHolder.getContext().getAuthentication();
        Utente utente = (Utente) authentication.getPrincipal();
        return utente;
    }
    
    private String buildBabelDownladerUrl(Integer idAzienda, String idApplicazione) throws IOException {
        Azienda azienda = cachedEntities.getAzienda(idAzienda);
        Applicazione applicazione = cachedEntities.getApplicazione(idApplicazione);
        parametriAzienda = AziendaParametriJson.parse(objectMapper, azienda.getParametri());

        String url = String.format("%s%s%s", parametriAzienda.getBabelSuiteWebApiUrl(), applicazione.getBaseUrl(), babelDownloaderUrl);
        return url;
    }
    
    public BabelDownloaderRequestBody createRquestBody(String guid, String tipologia) throws BlackBoxPermissionException {
        Utente loggedUser = getLoggedUser();
        Persona persona = cachedEntities.getPersonaFromUtente(loggedUser);
        return new BabelDownloaderRequestBody(guid, BabelDownloaderRequestBody.Tipologia.valueOf(tipologia), persona.getCodiceFiscale());
    }
    
    @Cacheable(value = "BabelDownloader.getDownloadUrl", key = "{#body.toString(), #idAzienda, #idApplicazione}", cacheManager = "expirationOneMinuteCacheManager")
    public BabelDownloaderResponseBody getDownloadUrl(BabelDownloaderRequestBody body, Integer idAzienda, String idApplicazione) throws IOException {
        RequestBody requestBody = RequestBody.create(MediaType.get("application/json; charset=utf-8"), objectMapper.writeValueAsString(body));
        OkHttpClient client = new OkHttpClient();

        Request request = new Request.Builder()
                .url(buildBabelDownladerUrl(idAzienda, idApplicazione))
                .post(requestBody)
                .addHeader("X-HTTP-Method-Override", babelDownloaderDownloadMethod)
                .build();

        Call call = client.newCall(request);
        try (Response response = call.execute();) {
            if (response.isSuccessful()) {
                if (response.body() != null)
                    return objectMapper.readValue(response.body().byteStream(), BabelDownloaderResponseBody.class);
                else 
                    throw new IOException("il link tornato è nullo");
            } else
                throw new IOException(String.format("errore nella richista del link: %s", response.message()));
        }
    }

    public Response getDownloadStream(String downloadUrl) throws IOException {
        OkHttpClient client = new OkHttpClient();

        Request request = new Request.Builder()
                .url(downloadUrl)
                .get()
//                .addHeader("Authorization", "header value") //Notice this request has header if you don't need to send a header just erase this part
                .build();

        Call call = client.newCall(request);
        return call.execute();
    }
}
