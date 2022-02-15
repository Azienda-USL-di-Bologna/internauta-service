package it.bologna.ausl.internauta.service.configuration.downloader;

import it.bologna.ausl.internauta.service.configuration.utils.ReporitoryConnectionManager;
import it.bologna.ausl.internauta.utils.downloader.configuration.RepositoryManager;
import it.bologna.ausl.minio.manager.MinIOWrapper;
import it.bologna.ausl.mongowrapper.MongoWrapper;

/**
 *  Implementa la configurazione dei repository per il downloader
 * @author gdm
 */
public class InternautaRepositoryManager extends RepositoryManager {
    
    private final ReporitoryConnectionManager reporitoryConnectionManager;

    public InternautaRepositoryManager(ReporitoryConnectionManager reporitoryConnectionManager) {
        this.reporitoryConnectionManager = reporitoryConnectionManager;
    }
    
    @Override
    public MinIOWrapper getMinIOWrapper() {
        return reporitoryConnectionManager.getMinIOWrapper();
    }
    
    @Override
    public MongoWrapper getMongoWrapper(String codiceAzienda) {
        return reporitoryConnectionManager.getRepositoryWrapperByCodiceAzienda(codiceAzienda);
    }
    
}