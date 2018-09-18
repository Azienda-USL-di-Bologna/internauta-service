package it.bologna.ausl.internauta.service.configuration.cache;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.Cache;
import org.springframework.cache.annotation.CachingConfigurerSupport;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.cache.interceptor.SimpleCacheErrorHandler;
import org.springframework.context.annotation.Configuration;

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
