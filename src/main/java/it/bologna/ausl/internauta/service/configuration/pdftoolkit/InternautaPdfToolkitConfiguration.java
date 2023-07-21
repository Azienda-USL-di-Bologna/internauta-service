package it.bologna.ausl.internauta.service.configuration.pdftoolkit;

import it.bologna.ausl.internauta.service.configuration.utils.HttpClientManager;
import it.bologna.ausl.internauta.utils.pdftoolkit.configuration.PdfToolkitConfiguration;
import javax.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;

/**
 *
 * @author Giuseppe Russo <g.russo@dilaxia.com>
 */
@Configuration
public class InternautaPdfToolkitConfiguration {
    
    @Autowired
    private PdfToolkitConfiguration pdfToolkitHttpClientConfiguration;
    
    @Autowired
    private HttpClientManager httpClientManager;

    @PostConstruct
    public void initConfiguration() {
        this.pdfToolkitHttpClientConfiguration.setHttpClientManager(new InternautaPdfToolkitConfigurationManager(httpClientManager));
    }
}
