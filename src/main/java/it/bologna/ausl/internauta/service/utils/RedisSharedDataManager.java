package it.bologna.ausl.internauta.service.utils;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

/**
 *
 * @author gdm
 */
@Component
public class RedisSharedDataManager {
    
    @Autowired
    @Qualifier(value = "redisSharedData")
    private RedisTemplate redisSharedDataTemplate; 
    
    public void put(String key, Object field, Object value) {
        this.redisSharedDataTemplate.opsForHash().put(key, field, value);
    }
    
    public <T extends Object> T get(String key, Object field) {
        return (T) this.redisSharedDataTemplate.opsForHash().get(key, field);
    }
    
    public void remove(String key, Object field) {
        this.redisSharedDataTemplate.opsForHash().delete(key, field);
    }
    
    public void delete(String key) {
        this.redisSharedDataTemplate.delete(key);
    }
}
