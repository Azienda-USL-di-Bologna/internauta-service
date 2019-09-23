package it.bologna.ausl.internauta.service;

import it.bologna.ausl.internauta.service.schedulers.MessageSenderManager;
import it.bologna.ausl.internauta.service.schedulers.workers.ShutdownThread;
import java.time.LocalDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication(scanBasePackages = {"it.bologna.ausl", "it.nextsw"})
@EnableJpaRepositories({"it.bologna.ausl.internauta.service.repositories", "it.bologna.ausl.blackbox.repositories"})
@EntityScan("it.bologna.ausl.model.entities")
@EnableCaching
//@EnableAsync
public class InternautaApplication {
    
    private static final Logger log = LoggerFactory.getLogger(InternautaApplication.class);

    @Autowired
    MessageSenderManager messageSenderManager;
    
    @Autowired
    ShutdownThread shutdownThread;
    
    @Value("${internauta.scheduled-thread-pool-executor.active}")
    Boolean poolExecutorActive;

    public static void main(String[] args) {
        SpringApplication.run(InternautaApplication.class, args);
    }
    
    @Bean
    public CommandLineRunner schedulingRunner() {

        return (String... args) -> {
            if (poolExecutorActive) {
                LocalDateTime now = LocalDateTime.now();
                log.info("schedulo i threads messageSender...");
                try {
                    messageSenderManager.scheduleMessageSenderAtBoot(now);
                } catch (Exception e) {
                    log.info("errore nella schedulazione threads messageSender.", e);
                }
                
//                log.info("schedulo i threads messageSeenCleaner...");
//                try {
//                    messageSenderManager.scheduleSeenCleanerAtBoot(now);
//                } catch (Exception e) {
//                    log.info("errore nella schedulazione threads messageSeenCleaner.", e);
//                }
//                log.info("schedulazione threads messageSeenCleaner terminata con successo.");

                log.info("imposto ShutdownHook... ");
                try {
                    Runtime.getRuntime().addShutdownHook(shutdownThread);
                } catch (Exception e) {
                    log.info("errore impostazione ShutdownHook.", e);
                }
                log.info("impostazione ShutdownHook terminata con successo.");
            } else {
                log.info("scheduled-thread-pool-executor not active");
            }
        };
    }
}
