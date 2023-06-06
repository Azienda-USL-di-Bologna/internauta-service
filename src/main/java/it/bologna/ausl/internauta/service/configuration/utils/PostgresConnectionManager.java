package it.bologna.ausl.internauta.service.configuration.utils;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import it.bologna.ausl.internauta.service.repositories.baborg.AziendaRepository;
import it.bologna.ausl.model.entities.baborg.Azienda;
import it.bologna.ausl.model.entities.baborg.AziendaParametriJson;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.sql2o.Sql2o;

@Configuration
public class PostgresConnectionManager {

    @Autowired
    private AziendaRepository aziendaRepository;

    private List<AziendaParametriJson.DbConnParams> aziendaParamsList = null;
    private final Map<String, Sql2o> dbConnectionMap = new HashMap<>();
    @Value("${spring.datasource.driver-class-name}")
    private String driverClass;
    @Value("${sql20.datasource.min-idle-size}")
    private Integer sql2oMinIdleSize;
    @Value("${sql20.datasource.max-pool-size}")
    private Integer sql2oMaxPoolSize;
    @Value("${sql20.datasource.connection-timeout}")
    private Integer sql2oConnectionTimeout;

    @PostConstruct
    public void init() {
        // Prendo i parametri di connessione delle varie aziende
        aziendaParamsList = getConnParams();

        // Popolo la mappa con le connssioni per ogni azienda
        for (AziendaParametriJson.DbConnParams aziendaConnParams : aziendaParamsList) {
            HikariConfig hikariConfig = new HikariConfig();
            hikariConfig.setDriverClassName(driverClass);
            hikariConfig.setJdbcUrl(aziendaConnParams.getJdbcUrl());
            hikariConfig.setUsername(aziendaConnParams.getUsername());
            hikariConfig.setPassword(aziendaConnParams.getPassword());
            // hikariConfig.setLeakDetectionThreshold(20000);
            hikariConfig.setMinimumIdle(sql2oMinIdleSize);
            hikariConfig.setMaximumPoolSize(sql2oMaxPoolSize);
            // hikariConfig.getConnectionTimeout();
            hikariConfig.setConnectionTimeout(sql2oConnectionTimeout);
            HikariDataSource hikariDataSource = new HikariDataSource(hikariConfig);
            Sql2o sql2o = new Sql2o(hikariDataSource);
            dbConnectionMap.put(aziendaConnParams.getCodiceAzienda(), sql2o);
        }
    }

    public Integer getIdAzienda(String codiceAzienda) {
        Azienda azienda = aziendaRepository.findByCodice(codiceAzienda);
        return azienda.getId();
    }

    /**
     * Restituisce una connessione Sql2o al database argo dell'azienda passata
     * come paramertro (per codice azienda).
     *
     * @param codiceAzienda il codice dell'azienda (es.:102, 105...)
     */
    public Sql2o getDbConnection(String codiceAzienda) {
        return dbConnectionMap.get(codiceAzienda);
    }

    public Sql2o getDbConnection(Integer idAzienda) {
        Azienda azienda = aziendaRepository.findById(idAzienda).get();
        return dbConnectionMap.get(azienda.getCodice());
    }

    public List<AziendaParametriJson.DbConnParams> getConnParams() {
        List<Azienda> aziende = aziendaRepository.findAll();
        List<AziendaParametriJson.DbConnParams> aps = new ArrayList<>();
        aziende.stream().forEach(azienda -> {
            AziendaParametriJson aziendaParametriJson = azienda.getParametri();
            AziendaParametriJson.DbConnParams aziendaParams = aziendaParametriJson.getDbConnParams();
            aziendaParams.setCodiceAzienda(azienda.getCodice());
            aps.add(aziendaParams);
        });
        return aps;
    }
}
