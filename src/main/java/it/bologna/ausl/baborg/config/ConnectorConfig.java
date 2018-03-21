package it.bologna.ausl.baborg.config;

import it.bologna.ausl.baborg.odata.processor.JPAServiceFactory;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.apache.catalina.connector.Connector;
import org.apache.cxf.endpoint.Server;
import org.apache.cxf.jaxrs.JAXRSServerFactoryBean;
import org.apache.olingo.odata2.core.rest.ODataExceptionMapperImpl;
import org.apache.olingo.odata2.core.rest.app.ODataApplication;
import org.apache.olingo.odata2.spring.OlingoRootLocator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.embedded.EmbeddedServletContainerFactory;
import org.springframework.boot.context.embedded.tomcat.TomcatEmbeddedServletContainerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;

@Configuration
public class ConnectorConfig {

    @Bean
    public EmbeddedServletContainerFactory servletContainer(@Value("${server.protocol.ajp.port:8202}") int ajpPort) {
        TomcatEmbeddedServletContainerFactory tomcat = new TomcatEmbeddedServletContainerFactory();
        Connector ajpConnector = new Connector("org.apache.coyote.ajp.AjpNioProtocol");
        ajpConnector.setProtocol("AJP/1.3");
        ajpConnector.setPort(ajpPort);
        ajpConnector.setSecure(false);
        ajpConnector.setAllowTrace(false);
        ajpConnector.setScheme("http");
        tomcat.addAdditionalTomcatConnectors(ajpConnector);
        return tomcat;
    }

    /**
     * crea il bean se setta il contesto JPA
     */
    @Bean
    public JPAServiceFactory jPAServiceFactory() {
        return new JPAServiceFactory();
    }

    /**
     * imposta come rootLocator di Olingo i settaggi contenuti in
     * JPAServiceFactory
     */
    @Bean
    public OlingoRootLocator getRootLocator() {
        OlingoRootLocator olingoRootLocator = new OlingoRootLocator();
        olingoRootLocator.setServiceFactory(jPAServiceFactory());
        return olingoRootLocator;
    }

    /**
     * crea una response di errore secondo il formato OData
     */
    @Bean
    public ODataExceptionMapperImpl getExceptionHandler() {
        return new ODataExceptionMapperImpl();
    }

    @Bean
    public ODataApplication.MyProvider getProvider() {
        return new ODataApplication.MyProvider();
    }

    /**
     * CXF aiuta a sviluppare servizi come JAX-RS. Questi servizi possono
     * dialogare con molti protocolli come SOAP, XML/HTTP, RESTful HTTP, ecc...
     */
    /**
     * JAX-RS: Java API for RESTful Web Services (JAX-RS) fornisce supporto alla
     * creazione di web services in accordo con l'architettura REST. JAX-RS usa
     * annotazioni per semplificare lo sviluppo e il deploy di web service ed
     * endpoints
     */
    @Bean
    @DependsOn("cxf")
    public Server oDataServer() {
        /**
         * JAXRSServerFactoryBean recupera la classe di servizio e per
         * reflection definisce l’infrastruttura per poter rispondere alle
         * richieste che il server riceverá (solo GET, ma anche il resto di
         * metodi esposti).
         */
        JAXRSServerFactoryBean sf = new JAXRSServerFactoryBean();

//        List providers = new ArrayList();
//        providers.add(getRootLocator());
//        providers.add(getExceptionHandler());
//        providers.add(getProvider());
        sf.setServiceBeans(Arrays.<Object>asList(getRootLocator(), getExceptionHandler(), getProvider()));
        return sf.create();
    }
}
