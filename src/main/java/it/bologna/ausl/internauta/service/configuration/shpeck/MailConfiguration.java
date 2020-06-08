package it.bologna.ausl.internauta.service.configuration.shpeck;

import javax.annotation.PostConstruct;
import org.springframework.context.annotation.Configuration;

/**
 *
 * @author gdm
 */
@Configuration
public class MailConfiguration {

    @PostConstruct
    public void configureMailProperties () {
        System.setProperty("mail.mime.base64.ignoreerrors", "true");
    }
}
