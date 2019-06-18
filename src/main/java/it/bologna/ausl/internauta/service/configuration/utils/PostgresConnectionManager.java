package it.bologna.ausl.internauta.service.configuration.utils;

import com.mongodb.MongoException;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import it.bologna.ausl.mongowrapper.MongoWrapper;
import it.bologna.ausl.mongowrapper.exceptions.MongoWrapperException;
import java.io.IOException;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.sql2o.Connection;
import org.sql2o.Sql2o;

@Service
public final class PostgresConnectionManager {
        
    private final List<AziendaParams> aziendaParamsList;
    private final Map<String, Sql2o> dbConnectionMap;
    private final Map<String, MongoWrapper> storageConnectionMap;    

    public PostgresConnectionManager(
            @Value("${spring.datasource.driver-class-name}") String driverClass,
            @Value("${spring.datasource.url}") String jdbcUrl,
            @Value("${spring.datasource.username}") String dbUsername,
            @Value("${spring.datasource.password}") String dbPassword ) throws IOException, 
                UnknownHostException, MongoException, MongoWrapperException {
        
        // carico i parametri dalle aziende
        HikariConfig hikariConfigInternauta = new HikariConfig();
        hikariConfigInternauta.setDriverClassName(driverClass);
        hikariConfigInternauta.setJdbcUrl(jdbcUrl);
        hikariConfigInternauta.setUsername(dbUsername);
        hikariConfigInternauta.setPassword(dbPassword);
        hikariConfigInternauta.setLeakDetectionThreshold(20000);
        HikariDataSource hikariDataSourceInternauta =  new HikariDataSource(hikariConfigInternauta);
      
        
        Sql2o sql2oInternauta = new Sql2o(hikariDataSourceInternauta);
        
        aziendaParamsList = getConnParams(sql2oInternauta);
        
        dbConnectionMap = new HashMap<>();
        storageConnectionMap = new HashMap<>();
        
        // popolo le mappe con le connssioni per ogni azienda
        for(AziendaParams aziendaConnParams: aziendaParamsList) {
            HikariConfig hikariConfig = new HikariConfig();
            hikariConfig.setDriverClassName(driverClass);
            hikariConfig.setJdbcUrl(aziendaConnParams.getJdbcUrl());
            hikariConfig.setUsername(aziendaConnParams.getDbUsername());
            hikariConfig.setPassword(aziendaConnParams.getDbPassword());
            hikariConfig.setLeakDetectionThreshold(20000);
            HikariDataSource hikariDataSource =  new HikariDataSource(hikariConfig);
            Sql2o sql2o = new Sql2o(hikariDataSource);
            dbConnectionMap.put(aziendaConnParams.getCodiceAzienda(), sql2o);

            
            MongoWrapper mongoWrapper = new MongoWrapper(aziendaConnParams.getStorageConnString());
            storageConnectionMap.put(aziendaConnParams.getCodiceAzienda(), mongoWrapper);
        }
               
    }
    
    public Sql2o getDbConnection(String codiceAzienda){
        return dbConnectionMap.get(codiceAzienda);
    }
    
    public MongoWrapper getStorageConnection(String codiceAzienda){
        MongoWrapper mongoWrapper = storageConnectionMap.get(codiceAzienda);
        return mongoWrapper;
    }
        
    public List<AziendaParams> getConnParams(Sql2o sql2o) {   
        String sqlAziende = "select " +
                            "codice as codiceAzienda, " +
                            "parametri -> 'dbConnParams' ->> 'jdbcUrl' as jdbcUrl, " +
                            "parametri -> 'dbConnParams' ->> 'username' as dbUsername, " +
                            "parametri -> 'dbConnParams' ->> 'password' as dbPassword, " +
                            "parametri -> 'mongoParams' ->> 'connectionString' as storageConnString, " +
                            "parametri ->> 'babelSuiteWebApiUrl' as babelSuiteWebApiUrl " +
                            "from baborg.aziende "; 
        try (Connection conn = (Connection) sql2o.open()) {
               return conn.createQuery(sqlAziende)
                       .executeAndFetch(AziendaParams.class);                       
       }
                                    
    }
    
    public AziendaParams getAziendaParam(String codiceAzienda){       
        return aziendaParamsList.stream().filter(a -> (a.getCodiceAzienda().equals(codiceAzienda))).findFirst().orElse(null);       
    }
}
