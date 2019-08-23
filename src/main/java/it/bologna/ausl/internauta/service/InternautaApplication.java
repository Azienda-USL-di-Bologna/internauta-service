package it.bologna.ausl.internauta.service;

import it.bologna.ausl.internauta.service.exceptions.InternautaScheduledException;
import it.bologna.ausl.internauta.service.workers.MessageSenderWorker;
import it.bologna.ausl.internauta.service.workers.ShutdownThread;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication(scanBasePackages = {"it.bologna.ausl", "it.nextsw"})
@EnableJpaRepositories({"it.bologna.ausl.internauta.service.repositories", "it.bologna.ausl.blackbox.repositories"})
@EntityScan("it.bologna.ausl.model.entities")
@EnableCaching
public class InternautaApplication {
    
    private static final Logger log = LoggerFactory.getLogger(InternautaApplication.class);

    @Autowired
    ScheduledThreadPoolExecutor scheduledThreadPoolExecutor;
    
    @Autowired
    ShutdownThread shutdownThread;
    
    public static void main(String[] args) {
        SpringApplication.run(InternautaApplication.class, args);
    }
    
    @Bean
    public CommandLineRunner schedulingRunner() {

        return (String... args) -> {
            log.info(". entrato nel run .");
            
            for (int i = 0; i < 10; i++) {
                MessageSenderWorker messageSenderWorker = new MessageSenderWorker();
                ScheduledFuture<?> schedule = scheduledThreadPoolExecutor.schedule(messageSenderWorker, 2, TimeUnit.SECONDS);
                if (i == 4)
                    schedule.cancel(false);
//                log.info(string);
            }
            
            Runtime.getRuntime().addShutdownHook(shutdownThread);
        };
    }
}
