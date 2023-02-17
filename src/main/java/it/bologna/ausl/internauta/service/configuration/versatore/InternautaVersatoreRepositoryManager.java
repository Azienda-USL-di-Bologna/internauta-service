package it.bologna.ausl.internauta.service.configuration.versatore;

import it.bologna.ausl.internauta.service.configuration.utils.ReporitoryConnectionManager;
import it.bologna.ausl.internauta.utils.versatore.configuration.VersatoreRepositoryManager;
import it.bologna.ausl.minio.manager.MinIOWrapper;

/**
 * Implementa la configurazione del client http per il modulo versatore
 * 
 * @author Giuseppe Russo <g.russo@nsi.it>
 */
public class InternautaVersatoreRepositoryManager extends VersatoreRepositoryManager {
    private final ReporitoryConnectionManager reporitoryConnectionManager;

    public InternautaVersatoreRepositoryManager(ReporitoryConnectionManager reporitoryConnectionManager) {
        this.reporitoryConnectionManager = reporitoryConnectionManager;
    }

    @Override
    public MinIOWrapper getMinIOWrapper() {
        return reporitoryConnectionManager.getMinIOWrapper();
    }
    
}
