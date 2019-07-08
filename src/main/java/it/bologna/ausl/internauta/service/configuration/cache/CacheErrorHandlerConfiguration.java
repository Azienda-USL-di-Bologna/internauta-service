package it.bologna.ausl.internauta.service.configuration.cache;

import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CachingConfigurerSupport;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.cache.interceptor.SimpleCacheErrorHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.JdkSerializationRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.RedisSerializer;

/**
 * Gestione errore su accesso alla cache. Se c'è un errore questo viene ignorato
 * e si va direttamente su db
 */
@Configuration
@EnableCaching
public class CacheErrorHandlerConfiguration extends CachingConfigurerSupport {

    // serve per loggare dall'applicazione
//    @Slf4j
    private static final Logger log = LoggerFactory.getLogger(CacheErrorHandlerConfiguration.class);

    @Value("${internauta.cache.redis.host}")
    private String redisHost;
    @Value("${internauta.cache.redis.port:6379}")
    private Integer redisPort;
    @Value("${internauta.cache.redis.expiration-seconds:7200}")
    private Integer expireTime;
    @Value("${internauta.cache.redis.use-json:false}")
    private Boolean jsonSerialization;
    @Value("${internauta.cache.redis.timeout-millis}")
    private Integer timeoutMillis;
    
    @Autowired
    private RedisConnectionFactory jedisConnectionFactory;
    
    
    private static class RelaxedCacheErrorHandler extends SimpleCacheErrorHandler {

        @Override
        public void handleCacheGetError(RuntimeException exception, Cache cache, Object key) {
            log.error("Error getting from cache.", exception);
        }

        @Override
        public void handleCacheClearError(RuntimeException exception, Cache cache) {
            log.error("Error clearing cache.", exception);
        }

        @Override
        public void handleCacheEvictError(RuntimeException exception, Cache cache, Object key) {
            log.error("Error evicting from cache.", exception);
        }

        @Override
        public void handleCachePutError(RuntimeException exception, Cache cache, Object key, Object value) {
            log.error("Error putting in cache.", exception);
        }

    }

    @Override
    public CacheErrorHandler errorHandler() {
        return new RelaxedCacheErrorHandler();
    }

}
