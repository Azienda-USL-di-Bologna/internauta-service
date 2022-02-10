package it.bologna.ausl.internauta.service.configuration.downloader;

import it.bologna.ausl.internauta.service.configuration.utils.ReporitoryConnectionManager;
import it.bologna.ausl.internauta.utils.downloader.configuration.DownloaderRepositoryConfiguration;
import javax.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;

@Configuration
public class InternautaDownloaderRepositoryConfiguration {
    
    @Autowired
    private DownloaderRepositoryConfiguration downloaderRepositoryConfiguration;
    
    @Autowired
    private ReporitoryConnectionManager reporitoryConnectionManager;

    @PostConstruct
    public void initDownloaderRepositoryConfiguration() {
        this.downloaderRepositoryConfiguration.setRepositoryManager(new InternautaRepositoryManager(reporitoryConnectionManager));
    }
}