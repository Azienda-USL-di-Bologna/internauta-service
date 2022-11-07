package it.bologna.ausl.internauta.service.configuration.versatore;

import it.bologna.ausl.internauta.service.configuration.utils.HttpClientManager;
import it.bologna.ausl.internauta.utils.versatore.configuration.VersatoreHttpClientManager;
import okhttp3.OkHttpClient;

/**
 * Implementa la configurazione del client http per il modulo versatore
  
 * @author Giuseppe Russo <g.russo@nsi.it>
 */
public class InternautaVersatoreHttpClientManager extends VersatoreHttpClientManager {
     private final HttpClientManager httpClientManager;

    public InternautaVersatoreHttpClientManager(HttpClientManager httpClientManager) {
        this.httpClientManager = httpClientManager;
    }
    
    @Override
    public OkHttpClient getOkHttpClient() {
        return httpClientManager.getHttpClient();
    }
}
