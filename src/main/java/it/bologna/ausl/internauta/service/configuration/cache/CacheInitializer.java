package it.bologna.ausl.internauta.service.configuration.cache;

import java.util.Set;
import javax.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.RedisTemplate;

/**
 *
 * @author gdm
 */
@Configuration
public class CacheInitializer {
    
    @Value("${internauta.cache.redis.prefix}")
    private String prefix;
    
    @Autowired
    @Qualifier(value = "redisCache")
    private RedisTemplate redisTemplate; 

    /**
     * pulisce la cache di internauta (cancella tutte le chiavi che iniziano con "internauta_cache_")
     */
    @PostConstruct
    public void cleanCache() {
//        redisTemplate.setDefaultSerializer(new StringRedisSerializer());
//        redisTemplate.setKeySerializer(new StringRedisSerializer());
        Set<String> keys = redisTemplate.keys(prefix + "*");
        System.out.println(String.format("cleaning redis cache with prefix: %s...", prefix));
        redisTemplate.delete(keys);
        System.out.println("redis cache cleaned");
//        Set<byte[]> keys = redisTemplate.getConnectionFactory().getConnection().keys("*".getBytes());
//
//        Iterator<byte[]> it = keys.iterator();
//
//        System.out.println("---------------------------------");
//        while(it.hasNext()){
//
//            byte[] data = (byte[])it.next();
//
//            System.out.println(new String(data, 0, data.length));
//        }
//        System.out.println("---------------------------------");
    }
}
