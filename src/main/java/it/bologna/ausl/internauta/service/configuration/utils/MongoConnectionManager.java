package it.bologna.ausl.internauta.service.configuration.utils;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.MongoException;
import it.bologna.ausl.internauta.service.exceptions.ObjectNotFoundException;
import it.bologna.ausl.internauta.service.repositories.baborg.AziendaRepository;
import it.bologna.ausl.internauta.service.utils.InternautaConstants;
import it.bologna.ausl.internauta.service.utils.ParametriAziende;
import it.bologna.ausl.internauta.utils.bds.types.PermessoEntitaStoredProcedure;
import it.bologna.ausl.model.entities.baborg.Azienda;
import it.bologna.ausl.model.entities.baborg.AziendaParametriJson;
import it.bologna.ausl.model.entities.configuration.ParametroAziende;
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
    private ParametriAziende parametriAziende;
    
    @Autowired
    ObjectMapper objectMapper;
    
    private final Map<Integer, MongoWrapper> connections = new HashMap<>();

    @PostConstruct
    public void init() throws UnknownHostException, IOException, MongoException, MongoWrapperException, ObjectNotFoundException {
        List<Azienda> aziende = aziendaRepository.findAll();
        for(Azienda azienda: aziende) {
            AziendaParametriJson parametriAzienda = AziendaParametriJson.parse(objectMapper, azienda.getParametri());
            AziendaParametriJson.MongoParams mongoParams = parametriAzienda.getMongoParams();
            List<ParametroAziende> parameters = parametriAziende.getParameters(
                    InternautaConstants.Configurazione.ParametriAzienda.minIOConfig.toString(),
                    new Integer[]{azienda.getId()});
            if (parameters==null || parameters.isEmpty() ||  parameters.size() > 1) {
                throw new ObjectNotFoundException("il parametro " +  InternautaConstants.Configurazione.ParametriAzienda.minIOConfig.toString() + " non è stato trovato nei parametri_aziende, oppure è presente più volte per la stessa azienda");
            }
            Map<String, Object> minIOConfig = parametriAziende.getValue(parameters.get(0), new TypeReference<Map<String, Object>>(){});
            Boolean minIOActive = (Boolean) minIOConfig.get("active");
            String minIODBDriver = (String) minIOConfig.get("DBDriver");
            String minIODBUrl = (String) minIOConfig.get("DBUrl");
            String minIODBUsername = (String) minIOConfig.get("DBUsername");
            String minIODBPassword = (String) minIOConfig.get("DBPassword");
            String mongoUrl = mongoParams.getConnectionString();
            MongoWrapper m = MongoWrapper.getWrapper(minIOActive, mongoUrl, minIODBDriver, minIODBUrl, minIODBUsername, minIODBPassword, azienda.getCodice(), objectMapper);
            connections.put(azienda.getId(), m);
        }
    }
    
    public MongoWrapper getConnection(Integer idAzienda) {
        return connections.get(idAzienda);
    }
}
