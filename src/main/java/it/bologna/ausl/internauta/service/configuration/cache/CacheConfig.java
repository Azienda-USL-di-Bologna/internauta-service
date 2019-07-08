package it.bologna.ausl.internauta.service.configuration.cache;

import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.jedis.JedisClientConfiguration;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.JdkSerializationRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.RedisSerializer;
import redis.clients.jedis.JedisPoolConfig;

/**
 *
 * @author andrea
 */
@Configuration
public class CacheConfig {

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

    @Bean
    public RedisConnectionFactory jedisConnectionFactory() {
        JedisPoolConfig poolConfig = new JedisPoolConfig();
        poolConfig.setMaxTotal(128);
        poolConfig.setBlockWhenExhausted(false);

        RedisStandaloneConfiguration redisStandaloneConfiguration = new RedisStandaloneConfiguration(redisHost, redisPort);
        JedisClientConfiguration jedisClientConfiguration = JedisClientConfiguration
                .builder()
                .connectTimeout(Duration.ofMillis(timeoutMillis))
                .readTimeout(Duration.ofMillis(timeoutMillis))
                .usePooling()
                .poolConfig(poolConfig)
                .build();

        JedisConnectionFactory jedisConnectionFactory = new JedisConnectionFactory(redisStandaloneConfiguration, jedisClientConfiguration);

        return jedisConnectionFactory;
    }

    @Bean
    @Primary
    public CacheManager defaultCacheManager(RedisConnectionFactory jedisConnectionFactory) {

        RedisSerializer<Object> defaultSerializer = new JdkSerializationRedisSerializer(getClass().getClassLoader());
        RedisSerializer<Object> jsonSerializer = new GenericJackson2JsonRedisSerializer();
        RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofSeconds(expireTime))
                .computePrefixWith((cacheName) -> {
                    return "internauta_cache_" + cacheName + "::";
                });
//                .disableCachingNullValues();
        if (jsonSerialization) {
            config = config.serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(jsonSerializer));
        } else {
            config = config.serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(defaultSerializer));
        }
        RedisCacheManager cm = RedisCacheManager.builder(jedisConnectionFactory)
                .cacheDefaults(config)
                .withInitialCacheConfigurations(Collections.singletonMap("predefined", config))
//                .withInitialCacheConfigurations(cacheNamesConfigurationMap)
                .transactionAware()
                .build();
        return cm;
    }
    
    @Bean
    public CacheManager expirationOneMinuteCacheManager(RedisConnectionFactory jedisConnectionFactory) {
        RedisSerializer<Object> defaultSerializer = new JdkSerializationRedisSerializer(getClass().getClassLoader());
        RedisSerializer<Object> jsonSerializer = new GenericJackson2JsonRedisSerializer();
        RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofSeconds(60))
                .computePrefixWith((cacheName) -> {
                    return "internauta_cache_one_minute_ttl_" + cacheName + "::";
                });
//                .disableCachingNullValues();
        if (jsonSerialization) {
            config = config.serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(jsonSerializer));
        } else {
            config = config.serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(defaultSerializer));
        }
       
        Map<String, RedisCacheConfiguration> cacheNamesConfigurationMap = new HashMap<>();
        
        RedisCacheManager cm = RedisCacheManager.builder(jedisConnectionFactory)
                .cacheDefaults(config)
                .withInitialCacheConfigurations(Collections.singletonMap("predefined", config))
                .withInitialCacheConfigurations(cacheNamesConfigurationMap)
                .transactionAware()
                .build();
        
        return cm;
    }
    
    @Bean
    public CacheManager emlCacheManager(RedisConnectionFactory jedisConnectionFactory) {
        RedisSerializer<Object> defaultSerializer = new JdkSerializationRedisSerializer(getClass().getClassLoader());
        RedisSerializer<Object> jsonSerializer = new GenericJackson2JsonRedisSerializer();
        RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(15))
                .computePrefixWith((cacheName) -> {
                    return "internauta_cache_eml_" + cacheName + "::";
                });
//                .disableCachingNullValues();
        if (jsonSerialization) {
            config = config.serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(jsonSerializer));
        } else {
            config = config.serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(defaultSerializer));
        }
       
        Map<String, RedisCacheConfiguration> cacheNamesConfigurationMap = new HashMap<>();
        
        RedisCacheManager cm = RedisCacheManager.builder(jedisConnectionFactory)
                .cacheDefaults(config)
                .withInitialCacheConfigurations(Collections.singletonMap("predefined", config))
                .withInitialCacheConfigurations(cacheNamesConfigurationMap)
                .transactionAware()
                .build();
        
        return cm;
    }
}
