package it.bologna.ausl.internauta.service.utils;

import java.util.HashSet;
import java.util.Set;
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
        
        Set<String> keysWithBrackets = redisTemplate.keys(prefixInternauta + PREFIX_PERMESSI + "::*Utente*id=" + idUtente + "*");
        // TODO: Se passi di qui. e hai voglia. prova a vedere come mai non funziona la seguente invece della precedente.
        //         Set<String> keysWithBrackets = redisTemplate.keys("\"" + prefixInternauta + PREFIX_PERMESSI + "::*Utente\\\\[*id=" + idUtente + "*\\\\]*" + "\"");

        redisTemplate.delete(keysWithBrackets);
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


