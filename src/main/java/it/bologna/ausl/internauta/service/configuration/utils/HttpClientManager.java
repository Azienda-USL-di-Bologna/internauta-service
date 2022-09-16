package it.bologna.ausl.internauta.service.configuration.utils;

import java.time.Duration;
import java.util.concurrent.TimeUnit;
import javax.annotation.PostConstruct;
import okhttp3.ConnectionPool;
import okhttp3.OkHttpClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

/**
 *
 * @author gdm
 */
@Configuration
public class HttpClientManager {
    OkHttpClient httpClient;
    
    @Value("${internauta.okhttp.timeout-seconds:1}")
    private Integer timeoutSeconds;
    
    @Value("${internauta.okhttp.pool.size:20}")
    private Integer poolSize;
    
    @Value("${internauta.okhttp.pool.keepalive-seconds:300}")
    private Integer poolKeepAliveSeconds;
    
    @PostConstruct
    public void init() {
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(timeoutSeconds, TimeUnit.SECONDS)
                .readTimeout(timeoutSeconds, TimeUnit.SECONDS)
                .callTimeout(timeoutSeconds, TimeUnit.SECONDS)
                .connectionPool(new ConnectionPool(poolSize, poolKeepAliveSeconds, TimeUnit.SECONDS))
                .build();
    }

    public OkHttpClient getHttpClient() {
        return httpClient;
    }
    
    public OkHttpClient getHttpClient(Duration duration) {
        return httpClient.newBuilder()
                .connectTimeout(duration)
                .readTimeout(duration)
                .callTimeout(duration)
                .build();
    }
}
