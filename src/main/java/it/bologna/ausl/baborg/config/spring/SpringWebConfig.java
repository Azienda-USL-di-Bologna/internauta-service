package it.bologna.ausl.baborg.config.spring;

import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@Configuration
@EnableJpaRepositories(basePackages = {
    "it.bologna.ausl.baborg.service",
    "it.bologna.ausl.entities.repository"
})
@EntityScan(basePackages = {
    "it.bologna.ausl.entities",
    "it.bologna.ausl.views"
})
//@ImportResource({
//    "classpath:spring/spring.xml"
//})
public class SpringWebConfig {
}
