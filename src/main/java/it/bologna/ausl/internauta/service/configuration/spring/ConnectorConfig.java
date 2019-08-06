package it.bologna.ausl.internauta.service.configuration.spring;

import org.apache.catalina.connector.Connector;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 *
 * @author gdm
 */
@Configuration
public class ConnectorConfig {

    @Bean
    public WebServerFactoryCustomizer<TomcatServletWebServerFactory> servletContainer(
            @Value("${internauta.server.protocol.ajp.port:8202}") int ajpPort,
            @Value("${internauta.server.max-post-size-byte:2097152}") int maxPostSize
            ) {
        return server -> {
            if (server instanceof TomcatServletWebServerFactory) {
                ((TomcatServletWebServerFactory) server).addAdditionalTomcatConnectors(redirectConnector(ajpPort, maxPostSize));
                
            }
        };
    }

    private Connector redirectConnector(int ajpPort, int maxPostSize) {
        Connector connector = new Connector("AJP/1.3");
        connector.setScheme("http");
        connector.setPort(ajpPort);
        connector.setSecure(false);
        connector.setAllowTrace(false);
        connector.setMaxPostSize(maxPostSize);
        return connector;
    }

}
