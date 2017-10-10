/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.nextsw.config.olingo;

import java.util.HashMap;
import java.util.Map;

import org.apache.cxf.jaxrs.servlet.CXFNonSpringJaxrsServlet;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


import org.apache.olingo.odata2.core.rest.app.ODataApplication;

/**
 * Servlet non spring, come da documentazione
 */
//@Configuration
public class CxfServletRegister {

    @Value("${odata.mapping.url.root}")
    private String odataMappingUrlRoot;

    @Bean
    public ServletRegistrationBean getODataServletRegistrationBean() {
        ServletRegistrationBean odataServletRegistrationBean = new ServletRegistrationBean(new CXFNonSpringJaxrsServlet(), odataMappingUrlRoot);
        Map<String, String> initParameters = new HashMap<>();
        initParameters.put("javax.ws.rs.Application", "org.apache.olingo.odata2.core.rest.app.ODataApplication");
        initParameters.put("org.apache.olingo.odata2.service.factory", "it.nextsw.odata.processor.JPAServiceFactory");
        initParameters.put("dispatchOptionsRequest", "true");
        odataServletRegistrationBean.setInitParameters(initParameters);

        return odataServletRegistrationBean;
    }
}
