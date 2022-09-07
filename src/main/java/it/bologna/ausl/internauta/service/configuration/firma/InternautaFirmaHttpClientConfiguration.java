package it.bologna.ausl.internauta.service.configuration.firma;

import it.bologna.ausl.internauta.service.configuration.utils.HttpClientManager;
import it.bologna.ausl.internauta.utils.firma.configuration.FirmaHttpClientConfiguration;
import javax.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;

@Configuration
public class InternautaFirmaHttpClientConfiguration {
    
    @Autowired
    private FirmaHttpClientConfiguration firmaHttpClientConfiguration;
    
    @Autowired
    private HttpClientManager httpClientManager;

    @PostConstruct
    public void initDownloaderRepositoryConfiguration() {
        this.firmaHttpClientConfiguration.setHttpClientManager(new InternautaFirmaHttpClientManager(httpClientManager));
    }
}