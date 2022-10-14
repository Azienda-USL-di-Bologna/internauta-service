package it.bologna.ausl.internauta.service.configuration.authorization;

import it.bologna.ausl.internauta.service.authorization.jwt.AuthorizationUtils;
import it.bologna.ausl.internauta.service.authorization.jwt.JwtFilter;
import java.io.IOException;
import java.security.cert.CertificateException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 *
 * @author spritz
 */
@Configuration
public class RegistrationBean {

    @Value("${internauta.security.start-nodes-protection}")
    private String startNodesProtection;
    
    @Value("${internauta.security.passtoken-path}")
    private String passTokenPath;
    
    @Value("${security.logout.path}")
    private String logoutPath;
    
    @Value("${security.refresh-session.path}")
    private String refreshSessionPath;

    @Value("${jwt.secret:secret}")
    private String secretKey;

    @Autowired
    private AuthorizationUtils authorizationUtils;

    @Bean
    public FilterRegistrationBean jwtFilter() throws CertificateException, IOException {

        final FilterRegistrationBean registrationBean = new FilterRegistrationBean();

        registrationBean.setFilter(new JwtFilter(secretKey, authorizationUtils));

        // intercetta tutte le chiamate che iniziano per...
        registrationBean.addUrlPatterns(startNodesProtection.split(","));
        // e anche il path che genera il passtoken
        registrationBean.addUrlPatterns(passTokenPath);
        // e anche il path di logout
        registrationBean.addUrlPatterns(logoutPath);
        // e anche il path di refresh session
        registrationBean.addUrlPatterns(refreshSessionPath);

        return registrationBean;
    }
}
