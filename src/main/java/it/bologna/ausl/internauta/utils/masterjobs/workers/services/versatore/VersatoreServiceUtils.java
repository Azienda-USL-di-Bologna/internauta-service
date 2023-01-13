/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package it.bologna.ausl.internauta.utils.masterjobs.workers.services.versatore;

import com.fasterxml.jackson.core.type.TypeReference;
import it.bologna.ausl.internauta.service.utils.CachedEntities;
import it.bologna.ausl.internauta.utils.parameters.manager.ParametriAziendeReader;
import it.bologna.ausl.model.entities.configurazione.ParametroAziende;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/**
 *
 * @author gdm
 */
public class VersatoreServiceUtils {
    
    /**
     * cicla su tutti i parametri "versatoreConfiguration" e crea una mappa dove per ogni idAzienda ne inserisce i 
     * parametri.
     * NB: se un'azienda è presente più volte (caso che non dovrebbe capitare) i suoi parametri saranno gli ultimi letti
     * @param parametriAziendaReader
     * @param cachedEntities
     * @return 
     */
    public static Map<Integer, Map<String, Object>> getAziendeAttiveConParametri(ParametriAziendeReader parametriAziendaReader, CachedEntities cachedEntities) {
        Map<Integer, Map<String, Object>> res = new HashMap<>();
        List<ParametroAziende> parameters = parametriAziendaReader.getParameters(ParametriAziendeReader.ParametriAzienda.versatoreConfiguration);
        parameters.stream().filter(p -> 
            ((Boolean)parametriAziendaReader.getValue(p, new TypeReference<Map<String, Object>>(){}).get("active")) == true
        ).forEach(p -> {
            Integer[] aziende;
            // se nel parametro non ci sono aziende, allora il parametro è inteso per tutte
            if (p.getIdAziende() == null || p.getIdAziende().length == 0) {
                aziende = cachedEntities.getAllAziende().stream().map(a -> a.getId()).toArray(Integer[]::new);
            } else { // se ci sono le aziende allora metto nella mappa il parametro per le aziende indicate
                aziende = p.getIdAziende();
            }
            Stream.of(p.getIdAziende()).forEach(
                    idAzienda -> res.put(
                            idAzienda, parametriAziendaReader.getValue(p, new TypeReference<Map<String, Object>>(){})
                    )
            );
        });
        return res;
    }
}
