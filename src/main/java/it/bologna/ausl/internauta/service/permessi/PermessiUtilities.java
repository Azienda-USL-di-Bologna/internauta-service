package it.bologna.ausl.internauta.service.permessi;

import it.bologna.ausl.model.entities.baborg.Utente;
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
public class PermessiUtilities {

    private final String PREFIX_PERMESSI = "getPermessi*__ribaltorg__";

    @Value("${internauta.cache.redis.prefix}")
    private String prefixInternauta;

    @Autowired
    @Qualifier(value = "redisCache")
    private RedisTemplate redisTemplate;

    public void cleanCachePermessiUtente(Integer idUtente) {
        Set<String> keys = redisTemplate.keys(prefixInternauta + PREFIX_PERMESSI + "::" + idUtente + "*");
        redisTemplate.delete(keys);
    }
}


