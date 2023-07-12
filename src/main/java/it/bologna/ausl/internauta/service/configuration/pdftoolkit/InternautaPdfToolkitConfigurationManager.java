package it.bologna.ausl.internauta.service.configuration.pdftoolkit;

import it.bologna.ausl.internauta.service.configuration.utils.HttpClientManager;
import it.bologna.ausl.internauta.utils.pdftoolkit.configuration.PdfToolkitConfigurationManager;
import okhttp3.OkHttpClient;

/**
 *
 * @author Giuseppe Russo <g.russo@dilaxia.com>
 */
public class InternautaPdfToolkitConfigurationManager extends PdfToolkitConfigurationManager {
    
    private final HttpClientManager httpClientManager;

    public InternautaPdfToolkitConfigurationManager(HttpClientManager httpClientManager) {
        this.httpClientManager = httpClientManager;
    }
    
    @Override
    public OkHttpClient getOkHttpClient() {
        return httpClientManager.getHttpClient();
    }  
}
