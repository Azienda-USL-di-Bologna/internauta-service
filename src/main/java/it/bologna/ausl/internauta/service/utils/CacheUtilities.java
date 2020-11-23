package it.bologna.ausl.internauta.service.utils;

import it.bologna.ausl.model.entities.baborg.Utente;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

/**
 *
 * @author gusgus
 */
@Component
public class CacheUtilities {

    private final String PREFIX_PERMESSI = "getPermessi*__ribaltorg__";
    private final String PREFIX_RUOLI = "getRuoli*_[ENTITY]__ribaltorg__";

    @Value("${internauta.cache.redis.prefix}")
    private String prefixInternauta;

    @Autowired
    @Qualifier(value = "redisCache")
    private RedisTemplate redisTemplate;

    public void cleanCachePermessiUtente(Integer idUtente) {
        Set<String> keys = redisTemplate.keys(prefixInternauta + PREFIX_PERMESSI + "::" + idUtente + "*");
        redisTemplate.delete(keys);
    }
    
    public void cleanCacheRuoliUtente(Integer idUtente, Integer idPersona) {
        Set<String> keys = new HashSet();
        keys.addAll(redisTemplate.keys(prefixInternauta + PREFIX_RUOLI.replace("[ENTITY]", "utente") + "::" + idUtente + "*"));
        //keys.addAll(redisTemplate.keys(prefixInternauta + PREFIX_RUOLI.replace("[ENTITY]", "") + "::" + idUtente + "*"));
        if (idPersona != null) {
            keys.addAll(redisTemplate.keys(prefixInternauta + PREFIX_RUOLI.replace("[ENTITY]", "persona") + "::" + idPersona + "*"));
        }
        if (!keys.isEmpty()) {
            redisTemplate.delete(keys);
        }
    }
}


