/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.bologna.ausl.baborg.config.olingo;

import org.apache.cxf.bus.spring.SpringBus;
import org.apache.cxf.endpoint.Server;
import org.apache.cxf.jaxrs.JAXRSServerFactoryBean;
import org.apache.cxf.jaxrs.lifecycle.ResourceProvider;
import org.apache.cxf.jaxrs.spring.SpringResourceFactory;
import org.apache.cxf.transport.servlet.CXFServlet;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;

import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;

import javax.ws.rs.Path;
import java.util.*;

@Configuration
//@ComponentScan(basePackages = "it.nextsw")
//@EnableAutoConfiguration
//@ImportResource({"classpath:META-INF/cxf/cxf.xml", "classpath:META-INF/cxf/cxf-servlet.xml"})
public class CxfServletRegister2 {

	/**
	 * Indirizzo base del servizio Olingo (definizione nel file application.properties)
	 */
	@Value("${odata.mapping.url.root}")
	private String odataMappingUrlRoot;


	public static void main(String[] args) {
		ApplicationContext ctx = SpringApplication.run(CxfServletRegister2.class, args);
	}

	/**
	 * Viene registrata la servlet che risponderà all'indirizzo base del servizio Olingo, viene lanciata una servlet Spring
	 * con dei parametri di inizializzazione specificati nel file spring.xml (TODO: andare a fondo sul link tra questa classe
	 * e il tag jaxrs:server)
	 *
	 * @return
	 */
	@Bean
	public ServletRegistrationBean cxfServletRegistrationBean(){
		ServletRegistrationBean registrationBean = new ServletRegistrationBean(new CXFServlet(), odataMappingUrlRoot);

//		registrationBean.setAsyncSupported(true);
//		registrationBean.setLoadOnStartup(1);
		registrationBean.setName("CXFServlet");

		return registrationBean;
	}


	/**
	 * Probabile modo alternativo per assegnare i serviceBeans piuttosto che tramite l'xml spring.xml (TODO: da verificare)
	 */
	//http://stackoverflow.com/questions/13520821/autodiscover-jax-rs-resources-with-cxf-in-a-spring-application
	//http://svn.apache.org/viewvc/cxf/trunk/rt/frontend/jaxrs/src/main/java/org/apache/cxf/jaxrs/spring/SpringResourceServer.java?revision=1548504&view=co&pathrev=1548504
//	@Bean
//	public Server jaxRsServer() {
//		List<ResourceProvider> resourceProviders = new LinkedList<ResourceProvider>();
//		for (String beanName : ctx.getBeanDefinitionNames()) {
//			if (ctx.findAnnotationOnBean(beanName, Path.class) != null) {
//				SpringResourceFactory factory = new SpringResourceFactory(beanName);
//				factory.setApplicationContext(ctx);
//				resourceProviders.add(factory);
//			}
//		}
//		if (resourceProviders.size() > 0) {
//			JAXRSServerFactoryBean factory = new JAXRSServerFactoryBean();
//			factory.setBus(ctx.getBean(SpringBus.class));
//			//factory.setProviders(Arrays.asList(new JacksonJsonProvider()));
//			factory.setResourceProviders(resourceProviders);
//			return factory.create();
//		} else {
//			return null;
//		}
//	}

}