package it.bologna.ausl.internauta.service.intasker.configuration;

import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.jedis.JedisClientConfiguration;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.JdkSerializationRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializer;
import redis.clients.jedis.JedisPoolConfig;

/**
 *
 * @author gdm
 */
//@Configuration
public class RedisConfig {
    
    @Value("${intasker.redis.host}")
    private String redisIntaskerHost;
    @Value("${intasker.redis.port:6379}")
    private Integer redisIntaskerPort;
    @Value("${intasker.redis.db:3}")
    private Integer redisIntaskerDb;
    @Value("${intasker.redis.timeout-millis}")
    private Integer redisIntaskerTimeoutMillis;

    
    public RedisConnectionFactory jedisIntaskerConnectionFactory() {
        JedisPoolConfig poolConfig = new JedisPoolConfig();
        poolConfig.setMaxTotal(128);
        poolConfig.setBlockWhenExhausted(false);

        RedisStandaloneConfiguration redisStandaloneConfiguration = new RedisStandaloneConfiguration(redisIntaskerHost, redisIntaskerPort);
        redisStandaloneConfiguration.setDatabase(redisIntaskerDb);
        JedisClientConfiguration jedisClientConfiguration = JedisClientConfiguration
                .builder()
                .connectTimeout(Duration.ofMillis(redisIntaskerTimeoutMillis))
                .readTimeout(Duration.ofMillis(redisIntaskerTimeoutMillis))
                .usePooling()
                .poolConfig(poolConfig)
                .build();

        JedisConnectionFactory jedisConnectionFactory = new JedisConnectionFactory(redisStandaloneConfiguration, jedisClientConfiguration);
        return jedisConnectionFactory;
    }
    
//    @Bean(name = "redisIntasker")
    public RedisTemplate<String, Object> redisIntasker() {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(jedisIntaskerConnectionFactory());
        RedisSerializer<Object> defaultSerializer = new JdkSerializationRedisSerializer(getClass().getClassLoader());
        template.setKeySerializer(defaultSerializer);
        template.setHashKeySerializer(defaultSerializer);
        template.setHashValueSerializer(defaultSerializer);
        template.setValueSerializer(defaultSerializer);
        return template;
    }
}
