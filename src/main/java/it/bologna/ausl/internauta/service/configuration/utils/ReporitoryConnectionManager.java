package it.bologna.ausl.internauta.service.configuration.utils;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.MongoException;
import it.bologna.ausl.internauta.service.exceptions.ObjectNotFoundException;
import it.bologna.ausl.internauta.service.repositories.baborg.AziendaRepository;
import it.bologna.ausl.internauta.service.utils.InternautaConstants;
import it.bologna.ausl.internauta.service.utils.ParametriAziendeReader;
import it.bologna.ausl.minio.manager.MinIOWrapper;
import it.bologna.ausl.model.entities.baborg.Azienda;
import it.bologna.ausl.model.entities.baborg.AziendaParametriJson;
import it.bologna.ausl.model.entities.configurazione.ParametroAziende;
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
public class ReporitoryConnectionManager {

    @Autowired
    private AziendaRepository aziendaRepository;

    @Autowired
    private ParametriAziendeReader parametriAziende;

    @Autowired
    ObjectMapper objectMapper;

    private Map<String, Object> minIOConfig;

    private Map<String, AziendaParametriJson> aziendeParametriJson;

    private final Map<Integer, MongoWrapper> connections = new HashMap<>();
    private MinIOWrapper minIOWrapper = null;
    private Boolean mongoAndMinIOActive = null;

    @PostConstruct
    public void init() throws UnknownHostException, IOException, MongoException, MongoWrapperException, ObjectNotFoundException {
        List<ParametroAziende> parameters = parametriAziende.getParameters(
                InternautaConstants.Configurazione.ParametriAzienda.minIOConfig.toString());
        if (parameters == null || parameters.isEmpty() || parameters.size() > 1) {
            throw new ObjectNotFoundException("il parametro " + InternautaConstants.Configurazione.ParametriAzienda.minIOConfig.toString() + " non è stato trovato nei parametri_aziende, oppure è presente più volte per la stessa azienda");
        }
        minIOConfig = parametriAziende.getValue(parameters.get(0), new TypeReference<Map<String, Object>>() {
        });

        initAziendeParametriJson();
        initMongo(minIOConfig);
        initMinIO(minIOConfig);

    }

    private void initAziendeParametriJson() throws IOException {
        List<Azienda> aziende = aziendaRepository.findAll();
        aziendeParametriJson = new HashMap();
        for (Azienda azienda : aziende) {
            AziendaParametriJson parametriAzienda = AziendaParametriJson.parse(objectMapper, azienda.getParametri());
            aziendeParametriJson.put(azienda.getCodice(), parametriAzienda);
        }
    }

    private void initMongo(Map<String, Object> minIOConfig) throws UnknownHostException, IOException, MongoException, MongoWrapperException, ObjectNotFoundException {
        List<Azienda> aziende = aziendaRepository.findAll();
        for (Azienda azienda : aziende) {
            AziendaParametriJson parametriAzienda = aziendeParametriJson.get(azienda.getCodice());
            AziendaParametriJson.MongoParams mongoParams = parametriAzienda.getMongoParams();
            List<ParametroAziende> parameters = parametriAziende.getParameters(
                    InternautaConstants.Configurazione.ParametriAzienda.mongoAndMinIOActive.toString(),
                    new Integer[]{azienda.getId()});
            if (parameters == null || parameters.isEmpty() || parameters.size() > 1) {
                throw new ObjectNotFoundException("il parametro " + InternautaConstants.Configurazione.ParametriAzienda.mongoAndMinIOActive.toString() + " non è stato trovato nei parametri_aziende, oppure è presente più volte per la stessa azienda");
            }
            this.mongoAndMinIOActive = parametriAziende.getValue(parameters.get(0), Boolean.class);
            String minIODBDriver = (String) minIOConfig.get("DBDriver");
            String minIODBUrl = (String) minIOConfig.get("DBUrl");
            String minIODBUsername = (String) minIOConfig.get("DBUsername");
            String minIODBPassword = (String) minIOConfig.get("DBPassword");
            String mongoUrl = mongoParams.getConnectionString();
            MongoWrapper m = MongoWrapper.getWrapper(mongoAndMinIOActive, mongoUrl, minIODBDriver, minIODBUrl, minIODBUsername, minIODBPassword, azienda.getCodice(), objectMapper);
            connections.put(azienda.getId(), m);
        }
    }

    private void initMinIO(Map<String, Object> minIOConfig) throws ObjectNotFoundException {
        String minIODBDriver = (String) minIOConfig.get("DBDriver");
        String minIODBUrl = (String) minIOConfig.get("DBUrl");
        String minIODBUsername = (String) minIOConfig.get("DBUsername");
        String minIODBPassword = (String) minIOConfig.get("DBPassword");
        Integer maxPoolSize = (Integer) minIOConfig.get("maxPoolSize");
        minIOWrapper = new MinIOWrapper(minIODBDriver, minIODBUrl, minIODBUsername, minIODBPassword, maxPoolSize, objectMapper);
    }

    public MongoWrapper getRepositoryWrapper(Integer idAzienda) {
        return connections.get(idAzienda);
    }

    public MinIOWrapper getMinIOWrapper() {
        return this.minIOWrapper;
    }

    public Map<String, Object> getMinIOConfig() {
        return minIOConfig;
    }

    public Map<String, AziendaParametriJson> getAziendeParametriJson() {
        return aziendeParametriJson;
    }

    public Boolean getMongoAndMinIOActive() {
        return mongoAndMinIOActive;
    }

    public void setMongoAndMinIOActive(Boolean mongoAndMinIOActive) {
        this.mongoAndMinIOActive = mongoAndMinIOActive;
    }

}
