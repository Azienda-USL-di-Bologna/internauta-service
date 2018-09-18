package it.bologna.ausl.internauta.service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication(scanBasePackages = {"it.bologna.ausl", "it.nextsw"})
//@EnableJpaRepositories("it.bologna.ausl.baborg.service.repositories")
@EntityScan("it.bologna.ausl.model.entities")
@EnableCaching
public class InternautaApplication {

    public static void main(String[] args) {
        SpringApplication.run(InternautaApplication.class, args);
    }
}
