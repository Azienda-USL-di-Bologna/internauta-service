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
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.JdkSerializationRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import redis.clients.jedis.JedisPoolConfig;

/**
 *
 * @author gdm
 */
@Configuration
public class CacheConfig {

    @Value("${internauta.cache.redis.host}")
    private String redisCacheHost;
    @Value("${internauta.cache.redis.port:6379}")
    private Integer redisCachePort;
    @Value("${internauta.cache.redis.db:0}")
    private Integer redisCacheDb;
    @Value("${internauta.cache.redis.expiration-seconds:7200}")
    private Integer redisCacheExpireTime;
    @Value("${internauta.cache.redis.use-json:false}")
    private Boolean redisCacheJsonSerialization;
    @Value("${internauta.cache.redis.timeout-millis}")
    private Integer redisCacheTimeoutMillis;
    @Value("${internauta.cache.redis.prefix}")
    private String redisCachePrefix;
    
    @Value("${intimus.redis.host}")
    private String redisIntimusHost;
    @Value("${intimus.redis.port:6379}")
    private Integer redisIntimusPort;
    @Value("${intimus.redis.db:1}")
    private Integer redisIntimusDb;
    @Value("${intimus.redis.timeout-millis}")
    private Integer redisIntimusTimeoutMillis;

    @Bean(name = "jedisConnectionFactory")
    public RedisConnectionFactory jedisConnectionFactory() {
        JedisPoolConfig poolConfig = new JedisPoolConfig();
        poolConfig.setMaxTotal(128);
        poolConfig.setBlockWhenExhausted(false);

        RedisStandaloneConfiguration redisStandaloneConfiguration = new RedisStandaloneConfiguration(redisCacheHost, redisCachePort);
        redisStandaloneConfiguration.setDatabase(redisCacheDb);
        JedisClientConfiguration jedisClientConfiguration = JedisClientConfiguration
                .builder()
                .connectTimeout(Duration.ofMillis(redisCacheTimeoutMillis))
                .readTimeout(Duration.ofMillis(redisCacheTimeoutMillis))
                .usePooling()
                .poolConfig(poolConfig)
                .build();

        JedisConnectionFactory jedisConnectionFactory = new JedisConnectionFactory(redisStandaloneConfiguration, jedisClientConfiguration);

        return jedisConnectionFactory;
    }
    
//    @Bean(name = "jedisIntimusConnectionFactory")
    public RedisConnectionFactory jedisIntimusConnectionFactory() {
        JedisPoolConfig poolConfig = new JedisPoolConfig();
        poolConfig.setMaxTotal(128);
        poolConfig.setBlockWhenExhausted(false);

        RedisStandaloneConfiguration redisStandaloneConfiguration = new RedisStandaloneConfiguration(redisIntimusHost, redisIntimusPort);
        redisStandaloneConfiguration.setDatabase(redisIntimusDb);
        JedisClientConfiguration jedisClientConfiguration = JedisClientConfiguration
                .builder()
                .connectTimeout(Duration.ofMillis(redisIntimusTimeoutMillis))
                .readTimeout(Duration.ofMillis(redisIntimusTimeoutMillis))
                .usePooling()
                .poolConfig(poolConfig)
                .build();

        JedisConnectionFactory jedisConnectionFactory = new JedisConnectionFactory(redisStandaloneConfiguration, jedisClientConfiguration);
        return jedisConnectionFactory;
    }
    
    @Bean(name = "redisCache")
    public RedisTemplate<String, Object> redisCacheTemplate() {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(jedisConnectionFactory());
        template.setKeySerializer(new StringRedisSerializer());
        return template;
    }
    
    @Bean(name = "redisIntimus")
    public RedisTemplate<String, Object> redisIntimusTemplate() {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(jedisIntimusConnectionFactory());
        RedisSerializer defaultSerializer = new StringRedisSerializer();
        template.setKeySerializer(defaultSerializer);
        template.setHashKeySerializer(defaultSerializer);
        template.setHashValueSerializer(defaultSerializer);
        template.setValueSerializer(defaultSerializer);
        return template;
    }

    @Bean
    @Primary
    public CacheManager defaultCacheManager(RedisConnectionFactory jedisConnectionFactory) {

        RedisSerializer<Object> defaultSerializer = new JdkSerializationRedisSerializer(getClass().getClassLoader());
        RedisSerializer<Object> jsonSerializer = new GenericJackson2JsonRedisSerializer();
        RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofSeconds(redisCacheExpireTime))
                .computePrefixWith((cacheName) -> {
                    return redisCachePrefix + cacheName + "::";
                });
//                .disableCachingNullValues();
        if (redisCacheJsonSerialization) {
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

//    public CacheManager intimusCacheManager(RedisConnectionFactory jedisConnectionFactory) {
//
//        RedisSerializer<Object> defaultSerializer = new JdkSerializationRedisSerializer(getClass().getClassLoader());
//        RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig();
//        config = config.serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(defaultSerializer));
//        
//        RedisCacheManager cm = RedisCacheManager.builder(jedisConnectionFactory)
//                .cacheDefaults(config)
//                .withInitialCacheConfigurations(Collections.singletonMap("predefined", config))
////                .withInitialCacheConfigurations(cacheNamesConfigurationMap)
//                .transactionAware()
//                .build();
//        return cm;
//    }
    
    @Bean
    public CacheManager expirationOneMinuteCacheManager(RedisConnectionFactory jedisConnectionFactory) {
        RedisSerializer<Object> defaultSerializer = new JdkSerializationRedisSerializer(getClass().getClassLoader());
        RedisSerializer<Object> jsonSerializer = new GenericJackson2JsonRedisSerializer();
        RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofSeconds(60))
                .computePrefixWith((cacheName) -> {
                    return redisCachePrefix + "one_minute_ttl_" + cacheName + "::";
                });
//                .disableCachingNullValues();
        if (redisCacheJsonSerialization) {
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
                    return redisCachePrefix + "eml_" + cacheName + "::";
                });
//                .disableCachingNullValues();
        if (redisCacheJsonSerialization) {
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
