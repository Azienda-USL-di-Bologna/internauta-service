package it.bologna.ausl.internauta.service.baborg.utils;

import it.bologna.ausl.internauta.service.repositories.baborg.AziendaRepository;
import it.bologna.ausl.model.entities.baborg.Azienda;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

/**
 *
 * @author guido
 */
@Component
public class BaborgUtils {

    private static final Logger log = LoggerFactory.getLogger(BaborgUtils.class);

    private static Map<String, Integer> map;

    // inizializzazione mappa statica
    static {
        map = new HashMap<>();
        map.put("pec.ospfe.it", 11);
        map.put("cert.ao.pr.it", 15);
        map.put("pec.aosp.bo.it", 10);
        map.put("pec.ior.it", 3);
        map.put("pec.ausl.bologna.it", 2);
        map.put("pec.ausl.fe.it", 6);
        map.put("pec.ausl.imola.bo.it", 12);
        map.put("pec.ausl.pr.it", 5);
    }

    @Autowired
    AziendaRepository aziendaRepository;

    public Azienda getAziendaRepositoryFromPecAddress(String address) {

        Azienda res = null;

        String domain = address.substring(address.indexOf("@") + 1);
        if (map.get(domain) != null) {
            Optional<Azienda> ar = aziendaRepository.findById(map.get(domain));

            if (ar.isPresent()) {
                res = ar.get();
            }
        }

        return res;
    }

}
