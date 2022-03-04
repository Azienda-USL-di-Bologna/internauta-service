package it.bologna.ausl.internauta.service.configuration.parametersmanager;

import it.bologna.ausl.internauta.service.repositories.baborg.AziendaRepository;
import it.bologna.ausl.internauta.utils.parameters.manager.configuration.ParametersManagerConfiguration;
import it.bologna.ausl.model.entities.baborg.Azienda;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;

/**
 *
 * @author gdm
 */
@Configuration
public class ParametersManagerConfigurationImpl extends ParametersManagerConfiguration {

    @Autowired
    private AziendaRepository aziendaRepository;
    
    @Override
    public Map<String, Integer> getCodiceAziendaIdAziendaMap() {
        Map<String, Integer> codiceAziendaIdAziendaMap = new HashMap();
        List<Azienda> aziende = aziendaRepository.findAll();
        aziende.stream().forEach(a -> codiceAziendaIdAziendaMap.put(a.getCodice(), a.getId()));
        return codiceAziendaIdAziendaMap;
    }
    
}
