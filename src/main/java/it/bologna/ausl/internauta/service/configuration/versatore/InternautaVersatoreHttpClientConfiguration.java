package it.bologna.ausl.internauta.service.configuration.versatore;

import it.bologna.ausl.internauta.service.configuration.utils.HttpClientManager;
import it.bologna.ausl.internauta.utils.versatore.configuration.VersatoreHttpClientConfiguration;
import javax.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;

/**
 *
 * @author Giuseppe Russo <g.russo@nsi.it>
 */
@Configuration
public class InternautaVersatoreHttpClientConfiguration {
    
    @Autowired
    private VersatoreHttpClientConfiguration httpClientConfiguration;
    
    @Autowired
    private HttpClientManager httpClientManager;

    @PostConstruct
    public void initHttpClientConfiguration() {
        this.httpClientConfiguration.setHttpClientManager(new InternautaVersatoreHttpClientManager(httpClientManager));
    }
    
}
