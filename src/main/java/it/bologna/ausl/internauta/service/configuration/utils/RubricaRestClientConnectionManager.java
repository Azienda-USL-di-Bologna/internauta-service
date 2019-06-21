package it.bologna.ausl.internauta.service.configuration.utils;

import com.mongodb.MongoException;
import it.bologna.ausl.internauta.service.repositories.baborg.AziendaRepository;
import it.bologna.ausl.model.entities.baborg.Azienda;
import it.bologna.ausl.mongowrapper.exceptions.MongoWrapperException;
import it.bologna.ausl.rubrica.maven.client.RestClient;
import java.io.IOException;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.sql2o.Connection;
import org.sql2o.Sql2o;

/**
 *
 * @author gusgus
 */
@Configuration
public class RubricaRestClientConnectionManager {
    
    @Autowired
    private AziendaRepository aziendaRepository;
    
    @Autowired
    PostgresConnectionManager postgresConnectionManager;
    
    private final Map<Integer, RubricaRestParams> rubricaRestParamsMap = new HashMap<>();

    @PostConstruct
    public void init() throws UnknownHostException, IOException, MongoException, MongoWrapperException {
        List<Azienda> aziende = aziendaRepository.findAll();
        aziende.forEach((azienda) -> {
            rubricaRestParamsMap.put(azienda.getId(), getRestClientParam(azienda));
        });
    }
    
    private RubricaRestParams getRestClientParam(Azienda azienda) {
        String query = "SELECT "
                + "rubrica_url.val_parametro as rubricaUrl, "
                + "rubrica_username.val_parametro as rubricaUsername, "
                + "rubrica_password.val_parametro as rubricaPassword\n" +
            "FROM bds_tools.parametri_pubblici rubrica_url\n" +
            "CROSS JOIN bds_tools.parametri_pubblici rubrica_username \n" +
            "CROSS JOIN bds_tools.parametri_pubblici rubrica_password \n" +
            "WHERE rubrica_url.nome_parametro = 'rubricaUrl'\n" +
            "AND rubrica_username.nome_parametro = 'rubricaUsername'\n" +
            "AND rubrica_password.nome_parametro = 'rubricaPassword'\n" +
            "LIMIT 1";
        
        // Prendo la connessione dal connection manager
        Sql2o dbConnection = postgresConnectionManager.getDbConnection(azienda.getCodice());
        
        // Prendo i parametri di connessione del restClient
        RubricaRestParams rubricaRestParams = null;
        try (Connection conn = (Connection) dbConnection.open()) {
            rubricaRestParams = conn.createQuery(query)
                    .executeAndFetchFirst(RubricaRestParams.class);
        }
        
        // Creo la connessione al restClient
        // RestClient restClient = new RestClient();
        rubricaRestParams.setRestClient(new RestClient());
//        this.url = rubricaRestParams.getRubricaUrl();
//        this.password = rubricaRestParams.getRubricaPassword();
//        this.username = rubricaRestParams.getRubricaUsername();
        //restClient.init(rubricaRestParams.getRubricaUrl(), rubricaRestParams.getRubricaUsername(), rubricaRestParams.getRubricaPassword());
        
        return rubricaRestParams;
    }
    
    public RestClient getConnection(Integer idAzienda) {
        RubricaRestParams params = rubricaRestParamsMap.get(idAzienda);
        params.init();
        return params.getRestClient();
    }
}
