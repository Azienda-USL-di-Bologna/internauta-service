package it.bologna.ausl.internauta.service.configuration.versatore;

import it.bologna.ausl.internauta.service.configuration.utils.ReporitoryConnectionManager;
import it.bologna.ausl.internauta.utils.versatore.configuration.VersatoreRepositoryConfiguration;
import javax.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;

/**
 *
 * @author Giuseppe Russo <g.russo@nsi.it>
 */
@Configuration
public class InternautaVersatoreRepositoryConfiguration {
    
    @Autowired
    private VersatoreRepositoryConfiguration versatoreRepositoryConfiguration;
    
    @Autowired
    private ReporitoryConnectionManager reporitoryConnectionManager;

    @PostConstruct
    public void initVersatoreRepositoryConfiguration() {
        this.versatoreRepositoryConfiguration.setVersatoreRepositoryManager(new InternautaVersatoreRepositoryManager(reporitoryConnectionManager));
    }
    
}
