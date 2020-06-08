package it.bologna.ausl.internauta.service.configuration.spring;

import java.net.InetAddress;
import java.net.UnknownHostException;

import org.apache.catalina.connector.Connector;
import org.apache.coyote.ajp.AbstractAjpProtocol;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
    private static final Logger logger = LoggerFactory.getLogger(ConnectorConfig.class);

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
        Connector ajpConnector = new Connector("AJP/1.3");
        try {
        ajpConnector.setScheme("http");
        ajpConnector.setPort(ajpPort);
        ajpConnector.setSecure(false);
        ajpConnector.setAllowTrace(false);
        ajpConnector.setMaxPostSize(maxPostSize);
        ((AbstractAjpProtocol) ajpConnector.getProtocolHandler()).setSecretRequired(false);
        ((AbstractAjpProtocol) ajpConnector.getProtocolHandler()).setAddress(InetAddress.getByName("0.0.0.0"));
        ((AbstractAjpProtocol) ajpConnector.getProtocolHandler()).setAllowedRequestAttributesPattern(".*");
        } catch (Exception ex) {
            logger.error("errore nella creazione del connettore ajp",ex);
        }
        return ajpConnector;
    }

}
