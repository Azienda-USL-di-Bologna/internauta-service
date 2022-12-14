package it.bologna.ausl.internauta.service;

import it.bologna.ausl.internauta.service.schedulers.FascicolatoreOutboxGediLocaleManager;
import it.bologna.ausl.internauta.service.schedulers.LogoutManager;
import it.bologna.ausl.internauta.service.schedulers.MessageSenderManager;
import it.bologna.ausl.internauta.service.schedulers.workers.ShutdownThread;
import java.time.ZonedDateTime;
import it.nextsw.common.repositories.CustomJpaRepositoryFactoryBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication(scanBasePackages = {"it.bologna.ausl", "it.nextsw"})
@EnableJpaRepositories(value =  {
        "it.bologna.ausl.internauta.service.repositories", 
        "it.bologna.ausl.blackbox.repositories", 
        "it.bologna.ausl.internauta.utils.parameters.manager.repositories",
        "it.bologna.ausl.internauta.utils.firma.repositories"
},
//        repositoryBaseClass = NextQuerydslJpaPredicateExecutorImpl.class
        repositoryFactoryBeanClass = CustomJpaRepositoryFactoryBean.class
)

@EntityScan("it.bologna.ausl.model.entities")
@EnableCaching
//@EnableAsync
public class InternautaApplication {

    private static final Logger log = LoggerFactory.getLogger(InternautaApplication.class);

    @Autowired
    MessageSenderManager messageSenderManager;
    
    @Autowired
    FascicolatoreOutboxGediLocaleManager fascicolatoreOutboxGediLocaleManager;

    @Autowired
    LogoutManager logoutManager;

    @Autowired
    ShutdownThread shutdownThread;

    @Value("${internauta.scheduled-thread-pool-executor.active}")
    Boolean poolExecutorActive;

    public static void main(String[] args) {
//        System.setProperty("user.timezone", "Europe/Rome");
        SpringApplication.run(InternautaApplication.class, args);
    }

    @Bean
    public CommandLineRunner schedulingRunner() {

        return (String... args) -> {
            if (poolExecutorActive) {
                ZonedDateTime now = ZonedDateTime.now();
                log.info("schedulo i threads messageSender...");
                try {
                    messageSenderManager.scheduleMessageSenderAtBoot(now);
                    fascicolatoreOutboxGediLocaleManager.scheduleAutoFascicolazioneOutboxAtBoot();
                } catch (Exception e) {
                    log.info("errore nella schedulazione threads messageSender", e);
                }
                log.info("schedulo il thread logoutManager...");
                try {
                    logoutManager.scheduleLogoutManager();
                } catch (Exception e) {
                    log.info("errore nella schedulazione thread logoutManager", e);
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
