package it.bologna.ausl.internauta.service.configuration.utils;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import it.bologna.ausl.internauta.service.repositories.baborg.AziendaRepository;
import it.bologna.ausl.model.entities.baborg.Azienda;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.PostConstruct;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.sql2o.Sql2o;

@Configuration
public class PostgresConnectionManager {
    
    @Autowired
    AziendaRepository aziendaRepository;
        
    private List<AziendaParams> aziendaParamsList = null;
    private final Map<String, Sql2o> dbConnectionMap = new HashMap<>();
    @Value("${spring.datasource.driver-class-name}") String driverClass;
    @Value("${sql20.datasource.min-idle-size}") Integer sql2oMinIdleSize;
    @Value("${sql20.datasource.max-pool-size}") Integer sql2oMaxPoolSize;
          

    @PostConstruct
    public void init() {
        // Prendo i parametri di connessione delle varie aziende
        aziendaParamsList = getConnParams();
        
        // Popolo la mappa con le connssioni per ogni azienda
        for(AziendaParams aziendaConnParams: aziendaParamsList) {
            HikariConfig hikariConfig = new HikariConfig();
            hikariConfig.setDriverClassName(driverClass);
            hikariConfig.setJdbcUrl(aziendaConnParams.getJdbcUrl());
            hikariConfig.setUsername(aziendaConnParams.getDbUsername());
            hikariConfig.setPassword(aziendaConnParams.getDbPassword());
            // hikariConfig.setLeakDetectionThreshold(20000);
            hikariConfig.setMinimumIdle(sql2oMinIdleSize);
            hikariConfig.setMaximumPoolSize(sql2oMaxPoolSize);
            // hikariConfig.getConnectionTimeout();
            hikariConfig.setConnectionTimeout(60000);
            HikariDataSource hikariDataSource =  new HikariDataSource(hikariConfig);
            Sql2o sql2o = new Sql2o(hikariDataSource);
            dbConnectionMap.put(aziendaConnParams.getCodiceAzienda(), sql2o);         
        }
    }
    
    public Sql2o getDbConnection(String codiceAzienda){
        return dbConnectionMap.get(codiceAzienda);
    }
    
    public List<AziendaParams> getConnParams() {
        List<Azienda> aziende = aziendaRepository.findAll();
        List<AziendaParams> aps = new ArrayList();
        aziende.stream().forEach(azienda -> {
            AziendaParams aziendaParams = new AziendaParams();
            aziendaParams.setCodiceAzienda(azienda.getCodice());
            JSONObject parametri = new JSONObject(azienda.getParametri());
            JSONObject dbConnParams = parametri.getJSONObject("dbConnParams");
            aziendaParams.setDbPassword(dbConnParams.getString("password"));
            aziendaParams.setDbUsername(dbConnParams.getString("username"));
            aziendaParams.setJdbcUrl(dbConnParams.getString("jdbcUrl"));
            aps.add(aziendaParams);
        });
        return aps;
    }
}
