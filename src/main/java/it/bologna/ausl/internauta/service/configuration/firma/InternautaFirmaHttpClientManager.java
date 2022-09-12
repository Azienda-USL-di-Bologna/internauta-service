package it.bologna.ausl.internauta.service.configuration.firma;

import it.bologna.ausl.internauta.service.configuration.utils.HttpClientManager;
import it.bologna.ausl.internauta.utils.firma.configuration.FirmaHttpClientManager;
import okhttp3.OkHttpClient;

/**
 *  Implementa la configurazione del client http per il modulo di firma
 * @author gdm
 */
public class InternautaFirmaHttpClientManager extends FirmaHttpClientManager {
    
    private final HttpClientManager httpClientManager;

    public InternautaFirmaHttpClientManager(HttpClientManager httpClientManager) {
        this.httpClientManager = httpClientManager;
    }
    
    @Override
    public OkHttpClient getOkHttpClient() {
        return httpClientManager.getHttpClient();
    }
    
    
}