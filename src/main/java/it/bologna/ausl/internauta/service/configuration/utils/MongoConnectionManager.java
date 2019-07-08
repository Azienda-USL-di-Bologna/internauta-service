package it.bologna.ausl.internauta.service.configuration.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.MongoException;
import it.bologna.ausl.internauta.service.repositories.baborg.AziendaRepository;
import it.bologna.ausl.model.entities.baborg.Azienda;
import it.bologna.ausl.model.entities.baborg.AziendaParametriJson;
import it.bologna.ausl.mongowrapper.MongoWrapper;
import it.bologna.ausl.mongowrapper.exceptions.MongoWrapperException;
import java.io.IOException;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;

/**
 *
 * @author Top
 */
@Configuration
public class MongoConnectionManager {
    
    @Autowired
    private AziendaRepository aziendaRepository;
    
    @Autowired
    ObjectMapper objectMapper;
    
    private Map<Integer, MongoWrapper> connections = new HashMap<>();

    @PostConstruct
    public void init() throws UnknownHostException, IOException, MongoException, MongoWrapperException {
        List<Azienda> aziende = aziendaRepository.findAll();
        for(Azienda azienda: aziende) {
            AziendaParametriJson parametriAzienda = AziendaParametriJson.parse(objectMapper, azienda.getParametri());
            AziendaParametriJson.MongoParams mongoParams = parametriAzienda.getMongoParams();
            MongoWrapper m = new MongoWrapper(mongoParams.getConnectionString());
            connections.put(azienda.getId(), m);
        }
    }
    
    public MongoWrapper getConnection(Integer idAzienda) {
        return connections.get(idAzienda);
    }
}
