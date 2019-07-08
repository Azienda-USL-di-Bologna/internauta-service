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

    @Value("${internauta.security.start-node-protection}")
    private String startNodeProtection;

    @Value("${jwt.secret:secret}")
    private String secretKey;

    @Autowired
    AuthorizationUtils authorizationUtils;

    @Bean
    public FilterRegistrationBean jwtFilter() throws CertificateException, IOException {

        final FilterRegistrationBean registrationBean = new FilterRegistrationBean();

        registrationBean.setFilter(new JwtFilter(secretKey, authorizationUtils));

        // intercetta tutte le chiamate che iniziano per...
        registrationBean.addUrlPatterns(startNodeProtection);

        return registrationBean;
    }
}
